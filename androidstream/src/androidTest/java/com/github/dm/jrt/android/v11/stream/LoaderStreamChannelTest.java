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

import com.github.dm.jrt.android.channel.ParcelableSelectable;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.core.runner.AndroidRunners;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel.InputChannel;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.ExecutionDeadlockException;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.config.InvocationConfiguration.OrderType;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.error.TimeoutException;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.OperationInvocation;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.runner.Runners;
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

import static com.github.dm.jrt.android.v11.core.LoaderContext.loaderFrom;
import static com.github.dm.jrt.core.util.UnitDuration.minutes;
import static com.github.dm.jrt.core.util.UnitDuration.seconds;
import static com.github.dm.jrt.function.Functions.functionOperation;
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

    private static void testCollect(@NotNull final Activity activity) {

        assertThat(LoaderStreams.streamOf(new StringBuilder("test1"), new StringBuilder("test2"),
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
        assertThat(LoaderStreams.streamOf(new StringBuilder("test1"), new StringBuilder("test2"),
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

    private static void testCollectCollection(@NotNull final Activity activity) {

        assertThat(LoaderStreams.streamOf("test1", "test2", "test3")
                                .with(loaderFrom(activity))
                                .async()
                                .collect(new Supplier<List<String>>() {

                                    public List<String> get() {

                                        return new ArrayList<String>();
                                    }
                                })
                                .afterMax(seconds(10))
                                .next()).containsExactly("test1", "test2", "test3");
        assertThat(LoaderStreams.streamOf("test1", "test2", "test3")
                                .with(loaderFrom(activity))
                                .sync()
                                .collect(new Supplier<List<String>>() {

                                    public List<String> get() {

                                        return new ArrayList<String>();
                                    }
                                })
                                .afterMax(seconds(10))
                                .next()).containsExactly("test1", "test2", "test3");
    }

    private static void testCollectSeed(@NotNull final Activity activity) {

        assertThat(LoaderStreams.streamOf("test1", "test2", "test3")
                                .with(loaderFrom(activity))
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
                                .afterMax(seconds(10))
                                .all()).containsExactly("test1test2test3");
        assertThat(LoaderStreams.streamOf("test1", "test2", "test3")
                                .with(loaderFrom(activity))
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
                                .afterMax(seconds(10))
                                .all()).containsExactly("test1test2test3");
    }

    private static void testConcat(final Activity activity) {

        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(activity))
                                .concat("test2")
                                .afterMax(seconds(10))
                                .all()).containsExactly("test1", "test2");
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(activity))
                                .concat("test2", "test3")
                                .afterMax(seconds(10))
                                .all()).containsExactly("test1", "test2", "test3");
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(activity))
                                .concat(Arrays.asList("test2", "test3"))
                                .afterMax(seconds(10))
                                .all()).containsExactly("test1", "test2", "test3");
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(activity))
                                .concat(JRoutineCore.io().of("test2", "test3"))
                                .afterMax(seconds(10))
                                .all()).containsExactly("test1", "test2", "test3");
    }

    private static void testConfiguration(@NotNull final Activity activity) {

        assertThat(LoaderStreams.streamOf("test1", "test2")
                                .with(loaderFrom(activity))
                                .maxParallelInvocations(1)
                                .parallel()
                                .map(new Function<String, String>() {

                                    public String apply(final String s) {

                                        return s.toUpperCase();
                                    }
                                })
                                .afterMax(seconds(10))
                                .all()).containsOnly("TEST1", "TEST2");
        assertThat(LoaderStreams.streamOf("test1", "test2")
                                .with(loaderFrom(activity))
                                .ordered(OrderType.BY_CALL)
                                .maxParallelInvocations(1)
                                .parallel()
                                .map(new Function<String, String>() {

                                    public String apply(final String s) {

                                        return s.toUpperCase();
                                    }
                                })
                                .afterMax(seconds(10))
                                .all()).containsExactly("TEST1", "TEST2");
        assertThat(LoaderStreams.streamOf("test1", "test2")
                                .with(loaderFrom(activity))
                                .ordered(OrderType.BY_CALL)
                                .maxParallelInvocations(1)
                                .parallel()
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
        assertThat(LoaderStreams.streamOf()
                                .async()
                                .thenGet(range(1, 1000))
                                .backPressureOn(handlerRunner, 2, 10, TimeUnit.SECONDS)
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
                                .afterMax(seconds(10))
                                .next()).isCloseTo(21, Offset.offset(0.1));
    }

    private static void testConsume(@NotNull final Activity activity) {

        final List<String> list = Collections.synchronizedList(new ArrayList<String>());
        assertThat(LoaderStreams.streamOf("test1", "test2", "test3")
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
        assertThat(LoaderStreams.streamOf("test1", "test2", "test3")
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

    private static void testConsumeError(@NotNull final Activity activity) {

        try {
            LoaderStreams.streamOf("test")
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

        assertThat(LoaderStreams.streamOf("test")
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

    private static void testFlatMap(@NotNull final Activity activity) {

        assertThat(LoaderStreams.streamOf("test1", null, "test2", null)
                                .with(loaderFrom(activity))
                                .sync()
                                .flatMap(new Function<String, OutputChannel<String>>() {

                                    public OutputChannel<String> apply(final String s) {

                                        return LoaderStreams.streamOf(s)
                                                            .with(loaderFrom(activity))
                                                            .sync()
                                                            .filter(Functions.<String>isNotNull());
                                    }
                                })
                                .all()).containsExactly("test1", "test2");
        assertThat(LoaderStreams.streamOf("test1", null, "test2", null)
                                .with(loaderFrom(activity))
                                .async()
                                .flatMap(new Function<String, OutputChannel<String>>() {

                                    public OutputChannel<String> apply(final String s) {

                                        return LoaderStreams.streamOf(s)
                                                            .with(loaderFrom(activity))
                                                            .sync()
                                                            .filter(Functions.<String>isNotNull());
                                    }
                                })
                                .afterMax(seconds(10))
                                .all()).containsExactly("test1", "test2");
        assertThat(LoaderStreams.streamOf("test1", null, "test2", null)
                                .with(loaderFrom(activity))
                                .parallel()
                                .flatMap(new Function<String, OutputChannel<String>>() {

                                    public OutputChannel<String> apply(final String s) {

                                        return LoaderStreams.streamOf(s)
                                                            .with(loaderFrom(activity))
                                                            .sync()
                                                            .filter(Functions.<String>isNotNull());
                                    }
                                })
                                .afterMax(seconds(10))
                                .all()).containsOnly("test1", "test2");
        assertThat(LoaderStreams.streamOf("test1", null, "test2", null)
                                .with(loaderFrom(activity))
                                .serial()
                                .flatMap(new Function<String, OutputChannel<String>>() {

                                    public OutputChannel<String> apply(final String s) {

                                        return LoaderStreams.streamOf(s)
                                                            .with(loaderFrom(activity))
                                                            .sync()
                                                            .filter(Functions.<String>isNotNull());
                                    }
                                })
                                .afterMax(seconds(10))
                                .all()).containsOnly("test1", "test2");
    }

    private static void testInvocationDeadlock(@NotNull final Activity activity) {

        try {
            final Runner runner1 = Runners.poolRunner(1);
            final Runner runner2 = Runners.poolRunner(1);
            LoaderStreams.streamOf("test")
                         .with(loaderFrom(activity))
                         .invocationConfiguration()
                         .withRunner(runner1)
                         .applyConfiguration()
                         .map(new Function<String, Object>() {

                             public Object apply(final String s) {

                                 return LoaderStreams.streamOf(s)
                                                     .with(loaderFrom(activity))
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

    private static void testMapAllConsumer(@NotNull final Activity activity) {

        assertThat(LoaderStreams.streamOf("test1", "test2", "test3")
                                .with(loaderFrom(activity))
                                .async()
                                .mapAll(new BiConsumer<List<?
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
                                })
                                .afterMax(seconds(10))
                                .all()).containsExactly("test1test2test3");
        assertThat(LoaderStreams.streamOf("test1", "test2", "test3")
                                .with(loaderFrom(activity))
                                .sync()
                                .mapAll(new BiConsumer<List<? extends String>,
                                        ResultChannel<String>>() {

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

    private static void testMapAllFunction(@NotNull final Activity activity) {

        assertThat(LoaderStreams.streamOf("test1", "test2", "test3")
                                .with(loaderFrom(activity))
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
                                .afterMax(seconds(10))
                                .all()).containsExactly("test1test2test3");
        assertThat(LoaderStreams.streamOf("test1", "test2", "test3")
                                .with(loaderFrom(activity))
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

    private static void testMapConsumer(@NotNull final Activity activity) {

        assertThat(LoaderStreams.streamOf("test1", "test2")
                                .with(loaderFrom(activity))
                                .map(new BiConsumer<String, ResultChannel<String>>() {

                                    public void accept(final String s,
                                            final ResultChannel<String> result) {

                                        result.pass(s.toUpperCase());
                                    }
                                })
                                .afterMax(seconds(10))
                                .all()).containsExactly("TEST1", "TEST2");
        assertThat(LoaderStreams.streamOf("test1", "test2")
                                .with(loaderFrom(activity))
                                .ordered(OrderType.BY_CALL)
                                .parallel()
                                .map(new BiConsumer<String, ResultChannel<String>>() {

                                    public void accept(final String s,
                                            final ResultChannel<String> result) {

                                        result.pass(s.toUpperCase());
                                    }
                                })
                                .afterMax(seconds(10))
                                .all()).containsExactly("TEST1", "TEST2");
        assertThat(LoaderStreams.streamOf("test1", "test2")
                                .with(loaderFrom(activity))
                                .sync()
                                .map(new BiConsumer<String, ResultChannel<String>>() {

                                    public void accept(final String s,
                                            final ResultChannel<String> result) {

                                        result.pass(s.toUpperCase());
                                    }
                                })
                                .all()).containsExactly("TEST1", "TEST2");
        assertThat(LoaderStreams.streamOf("test1", "test2")
                                .with(loaderFrom(activity))
                                .ordered(OrderType.BY_CALL)
                                .serial()
                                .map(new BiConsumer<String, ResultChannel<String>>() {

                                    public void accept(final String s,
                                            final ResultChannel<String> result) {

                                        result.pass(s.toUpperCase());
                                    }
                                })
                                .afterMax(seconds(10))
                                .all()).containsExactly("TEST1", "TEST2");
    }

    private static void testMapFunction(@NotNull final Activity activity) {

        assertThat(LoaderStreams.streamOf("test1", "test2")
                                .with(loaderFrom(activity))
                                .async()
                                .map(new Function<String, String>() {

                                    public String apply(final String s) {

                                        return s.toUpperCase();
                                    }
                                })
                                .afterMax(seconds(10))
                                .all()).containsExactly("TEST1", "TEST2");
        assertThat(LoaderStreams.streamOf("test1", "test2")
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
        assertThat(LoaderStreams.streamOf("test1", "test2")
                                .with(loaderFrom(activity))
                                .sync()
                                .map(new Function<String, String>() {

                                    public String apply(final String s) {

                                        return s.toUpperCase();
                                    }
                                })
                                .all()).containsExactly("TEST1", "TEST2");
        assertThat(LoaderStreams.streamOf("test1", "test2")
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

    private static void testOrElse(final Activity activity) {

        assertThat(LoaderStreams.streamOf("test")
                                .with(loaderFrom(activity))
                                .orElse("est")
                                .afterMax(seconds(10))
                                .all()).containsExactly("test");
        assertThat(LoaderStreams.streamOf("test")
                                .with(loaderFrom(activity))
                                .orElse("est1", "est2")
                                .afterMax(seconds(10))
                                .all()).containsExactly("test");
        assertThat(LoaderStreams.streamOf("test")
                                .with(loaderFrom(activity))
                                .orElse(Arrays.asList("est1", "est2"))
                                .afterMax(seconds(10))
                                .all()).containsExactly("test");
        assertThat(LoaderStreams.streamOf("test")
                                .with(loaderFrom(activity))
                                .orElseGet(new Consumer<ResultChannel<String>>() {

                                    public void accept(final ResultChannel<String> result) {

                                        result.pass("est");
                                    }
                                })
                                .afterMax(seconds(10))
                                .all()).containsExactly("test");
        assertThat(LoaderStreams.streamOf("test")
                                .with(loaderFrom(activity))
                                .orElseGet(new Supplier<String>() {

                                    public String get() {

                                        return "est";
                                    }
                                })
                                .afterMax(seconds(10))
                                .all()).containsExactly("test");
        assertThat(LoaderStreams.streamOf()
                                .with(loaderFrom(activity))
                                .orElse("est")
                                .afterMax(seconds(10))
                                .all()).containsExactly("est");
        assertThat(LoaderStreams.streamOf()
                                .with(loaderFrom(activity))
                                .orElse("est1", "est2")
                                .afterMax(seconds(10))
                                .all()).containsExactly("est1", "est2");
        assertThat(LoaderStreams.streamOf()
                                .with(loaderFrom(activity))
                                .orElse(Arrays.asList("est1", "est2"))
                                .afterMax(seconds(10))
                                .all()).containsExactly("est1", "est2");
        assertThat(LoaderStreams.<String>streamOf().with(loaderFrom(activity))
                                                   .orElseGet(
                                                           new Consumer<ResultChannel<String>>() {

                                                               public void accept(
                                                                       final
                                                                       ResultChannel<String>
                                                                               result) {

                                                                   result.pass("est");
                                                               }
                                                           })
                                                   .afterMax(seconds(10))
                                                   .all()).containsExactly("est");
        assertThat(LoaderStreams.<String>streamOf().with(loaderFrom(activity))
                                                   .orElseGet(2,
                                                           new Consumer<ResultChannel<String>>() {

                                                               public void accept(
                                                                       final
                                                                       ResultChannel<String>
                                                                               result) {

                                                                   result.pass("est");
                                                               }
                                                           })
                                                   .afterMax(seconds(10))
                                                   .all()).containsExactly("est", "est");
        assertThat(LoaderStreams.<String>streamOf().with(loaderFrom(activity))
                                                   .orElseGet(new Supplier<String>() {

                                                       public String get() {

                                                           return "est";
                                                       }
                                                   })
                                                   .afterMax(seconds(10))
                                                   .all()).containsExactly("est");
        assertThat(LoaderStreams.<String>streamOf().with(loaderFrom(activity))
                                                   .orElseGet(2, new Supplier<String>() {

                                                       public String get() {

                                                           return "est";
                                                       }
                                                   })
                                                   .afterMax(seconds(10))
                                                   .all()).containsExactly("est", "est");
    }

    private static void testPeek(@NotNull final Activity activity) {

        final ArrayList<String> data = new ArrayList<String>();
        assertThat(LoaderStreams.streamOf("test1", "test2", "test3")
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

    private static void testReduce(@NotNull final Activity activity) {

        assertThat(LoaderStreams.streamOf("test1", "test2", "test3")
                                .with(loaderFrom(activity))
                                .async()
                                .reduce(new BiFunction<String, String, String>() {

                                    public String apply(final String s, final String s2) {

                                        return s + s2;
                                    }
                                })
                                .afterMax(seconds(10))
                                .all()).containsExactly("test1test2test3");
        assertThat(LoaderStreams.streamOf("test1", "test2", "test3")
                                .with(loaderFrom(activity))
                                .sync()
                                .reduce(new BiFunction<String, String, String>() {

                                    public String apply(final String s, final String s2) {

                                        return s + s2;
                                    }
                                })
                                .all()).containsExactly("test1test2test3");
    }

    private static void testReduceSeed(@NotNull final Activity activity) {

        assertThat(LoaderStreams.streamOf("test1", "test2", "test3")
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
        assertThat(LoaderStreams.streamOf("test1", "test2", "test3")
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

    private static void testThen(@NotNull final Activity activity) {

        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(activity))
                                .sync()
                                .thenGet(new Consumer<ResultChannel<String>>() {

                                    public void accept(final ResultChannel<String> resultChannel) {

                                        resultChannel.pass("TEST2");
                                    }
                                })
                                .all()).containsOnly("TEST2");
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(activity))
                                .sync()
                                .thenGet(new Supplier<String>() {

                                    public String get() {

                                        return "TEST2";
                                    }
                                })
                                .all()).containsOnly("TEST2");
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(activity))
                                .sync()
                                .thenGet(3, new Supplier<String>() {

                                    public String get() {

                                        return "TEST2";
                                    }
                                })
                                .afterMax(seconds(10))
                                .all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(activity))
                                .async()
                                .thenGet(new Consumer<ResultChannel<String>>() {

                                    public void accept(final ResultChannel<String> resultChannel) {

                                        resultChannel.pass("TEST2");
                                    }
                                })
                                .afterMax(seconds(10))
                                .all()).containsOnly("TEST2");
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(activity))
                                .async()
                                .thenGet(new Supplier<String>() {

                                    public String get() {

                                        return "TEST2";
                                    }
                                })
                                .afterMax(seconds(10))
                                .all()).containsOnly("TEST2");
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(activity))
                                .async()
                                .thenGet(3, new Supplier<String>() {

                                    public String get() {

                                        return "TEST2";
                                    }
                                })
                                .afterMax(seconds(10))
                                .all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(activity))
                                .parallel()
                                .thenGet(3, new Consumer<ResultChannel<String>>() {

                                    public void accept(final ResultChannel<String> resultChannel) {

                                        resultChannel.pass("TEST2");
                                    }
                                })
                                .afterMax(seconds(10))
                                .all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(activity))
                                .parallel()
                                .thenGet(3, new Supplier<String>() {

                                    public String get() {

                                        return "TEST2";
                                    }
                                })
                                .afterMax(seconds(10))
                                .all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(activity))
                                .serial()
                                .thenGet(3, new Consumer<ResultChannel<String>>() {

                                    public void accept(final ResultChannel<String> resultChannel) {

                                        resultChannel.pass("TEST2");
                                    }
                                })
                                .afterMax(seconds(10))
                                .all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(LoaderStreams.streamOf("test1")
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

    private static void testTryCatch(@NotNull final Activity activity) {

        assertThat(LoaderStreams.streamOf("test")
                                .with(loaderFrom(activity))
                                .sync()
                                .map(new Function<Object, Object>() {

                                    public Object apply(final Object o) {

                                        throw new NullPointerException();
                                    }
                                })
                                .tryCatch(new BiConsumer<RoutineException, InputChannel<Object>>() {

                                    public void accept(final RoutineException e,
                                            final InputChannel<Object> channel) {

                                        channel.pass("exception");
                                    }
                                })
                                .next()).isEqualTo("exception");

        assertThat(LoaderStreams.streamOf("test")
                                .with(loaderFrom(activity))
                                .sync()
                                .map(new Function<Object, Object>() {

                                    public Object apply(final Object o) {

                                        return o;
                                    }
                                })
                                .tryCatch(new BiConsumer<RoutineException, InputChannel<Object>>() {

                                    public void accept(final RoutineException e,
                                            final InputChannel<Object> channel) {

                                        channel.pass("exception");
                                    }
                                })
                                .next()).isEqualTo("test");

        assertThat(LoaderStreams.streamOf("test")
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

    private static void testTryFinally(@NotNull final Activity activity) {

        final AtomicBoolean isRun = new AtomicBoolean(false);
        try {
            LoaderStreams.streamOf("test")
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

        assertThat(LoaderStreams.streamOf("test")
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

    @SuppressWarnings({"ConstantConditions", "ThrowableResultOfMethodCallIgnored"})
    public void testAbort() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        final IOChannel<Object> ioChannel = JRoutineCore.io().buildChannel();
        final LoaderStreamChannel<Object> streamChannel =
                LoaderStreams.streamOf(ioChannel).with(loaderFrom(getActivity()));
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

        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(getActivity()))
                                .apply(new Function<StreamChannel<String>, StreamChannel<String>>
                                        () {

                                    public StreamChannel<String> apply(
                                            final StreamChannel<String> stream) {

                                        return stream.concat("test2");
                                    }
                                })
                                .afterMax(seconds(10))
                                .all()).containsExactly("test1", "test2");
    }

    public void testBuilder() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        assertThat(LoaderStreams.streamOf()
                                .with(loaderFrom(getActivity()))
                                .afterMax(seconds(10))
                                .all()).isEmpty();
        assertThat(LoaderStreams.streamOf("test")
                                .with(loaderFrom(getActivity()))
                                .afterMax(seconds(10))
                                .all()).containsExactly("test");
        assertThat(LoaderStreams.streamOf("test1", "test2", "test3")
                                .with(loaderFrom(getActivity()))
                                .afterMax(seconds(10))
                                .all()).containsExactly("test1", "test2", "test3");
        assertThat(LoaderStreams.streamOf(Arrays.asList("test1", "test2", "test3"))
                                .with(loaderFrom(getActivity()))
                                .afterMax(seconds(10))
                                .all()).containsExactly("test1", "test2", "test3");
        assertThat(LoaderStreams.streamOf(JRoutineCore.io().of("test1", "test2", "test3"))
                                .with(loaderFrom(getActivity()))
                                .afterMax(seconds(10))
                                .all()).containsExactly("test1", "test2", "test3");
    }

    @SuppressWarnings("ConstantConditions")
    public void testBuilderNullPointerError() {

        try {
            LoaderStreams.streamOf((OutputChannel<?>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testChannel() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        StreamChannel<String> channel =
                LoaderStreams.streamOf("test").with(loaderFrom(getActivity()));
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
        channel = LoaderStreams.streamOf("test1", "test2", "test3").with(loaderFrom(getActivity()));
        try {
            channel.remove();
            fail();

        } catch (final UnsupportedOperationException ignored) {

        }

        assertThat(channel.skipNext(1).next(1)).containsExactly("test2");
        assertThat(channel.eventuallyExit().next(4)).containsExactly("test3");
        assertThat(channel.eventuallyExit().nextOrElse("test4")).isEqualTo("test4");
        final Iterator<String> iterator = LoaderStreams.streamOf("test1", "test2", "test3")
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
        channel = LoaderStreams.streamOf(ioChannel.after(1, TimeUnit.DAYS).pass("test"));
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
            LoaderStreams.streamOf()
                         .async()
                         .with(loaderFrom(getActivity()))
                         .collect((Supplier<Collection<Object>>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .sync()
                         .with(loaderFrom(getActivity()))
                         .collect((Supplier<Collection<Object>>) null);
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
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .async()
                         .collect((BiConsumer<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .sync()
                         .collect((BiConsumer<Object, Object>) null);
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
            LoaderStreams.streamOf().with(loaderFrom(getActivity())).async().collect(null, null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf().with(loaderFrom(getActivity())).sync().collect(null, null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testConcat() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        testConcat(getActivity());
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
            LoaderStreams.streamOf().with(loaderFrom(getActivity())).onError(null);
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
            LoaderStreams.streamOf().with(loaderFrom(getActivity())).sync().onOutput(consumer);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf().with(loaderFrom(getActivity())).async().onOutput(consumer);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testFilter() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        assertThat(LoaderStreams.streamOf(null, "test")
                                .with(loaderFrom(getActivity()))
                                .async()
                                .filter(Functions.isNotNull())
                                .afterMax(seconds(10))
                                .all()).containsExactly("test");
        assertThat(LoaderStreams.streamOf(null, "test")
                                .with(loaderFrom(getActivity()))
                                .parallel()
                                .filter(Functions.isNotNull())
                                .afterMax(seconds(10))
                                .all()).containsExactly("test");
        assertThat(LoaderStreams.streamOf(null, "test")
                                .with(loaderFrom(getActivity()))
                                .sync()
                                .filter(Functions.isNotNull())
                                .all()).containsExactly("test");
        assertThat(LoaderStreams.streamOf(null, "test")
                                .with(loaderFrom(getActivity()))
                                .serial()
                                .filter(Functions.isNotNull())
                                .afterMax(seconds(10))
                                .all()).containsExactly("test");
    }

    @SuppressWarnings("ConstantConditions")
    public void testFilterNullPointerError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            LoaderStreams.streamOf().with(loaderFrom(getActivity())).async().filter(null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf().parallel().filter(null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf().with(loaderFrom(getActivity())).sync().filter(null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf().serial().filter(null);
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
            LoaderStreams.streamOf().with(loaderFrom(getActivity())).sync().flatMap(null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf().with(loaderFrom(getActivity())).async().flatMap(null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf().with(loaderFrom(getActivity())).parallel().flatMap(null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf().with(loaderFrom(getActivity())).serial().flatMap(null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testInvocationDeadlock() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        testInvocationDeadlock(getActivity());
    }

    public void testLazyBuilder() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        assertThat(LoaderStreams.lazyStreamOf()
                                .with(loaderFrom(getActivity()))
                                .afterMax(seconds(10))
                                .all()).isEmpty();
        assertThat(LoaderStreams.lazyStreamOf("test")
                                .with(loaderFrom(getActivity()))
                                .afterMax(seconds(10))
                                .all()).containsExactly("test");
        assertThat(LoaderStreams.lazyStreamOf("test1", "test2", "test3")
                                .with(loaderFrom(getActivity()))
                                .afterMax(seconds(10))
                                .all()).containsExactly("test1", "test2", "test3");
        assertThat(LoaderStreams.lazyStreamOf(Arrays.asList("test1", "test2", "test3"))
                                .with(loaderFrom(getActivity()))
                                .afterMax(seconds(10))
                                .all()).containsExactly("test1", "test2", "test3");
        assertThat(LoaderStreams.lazyStreamOf(JRoutineCore.io().of("test1", "test2", "test3"))
                                .with(loaderFrom(getActivity()))
                                .afterMax(seconds(10))
                                .all()).containsExactly("test1", "test2", "test3");
    }

    @SuppressWarnings("ConstantConditions")
    public void testLazyBuilderNullPointerError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            LoaderStreams.lazyStreamOf((OutputChannel<?>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testLimit() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        assertThat(LoaderStreams.streamOf()
                                .sync()
                                .thenGet(range(1, 10))
                                .with(loaderFrom(getActivity()))
                                .async()
                                .limit(5)
                                .afterMax(seconds(10))
                                .all()).isEqualTo(Arrays.asList(1, 2, 3, 4, 5));
        assertThat(LoaderStreams.streamOf()
                                .sync()
                                .thenGet(range(1, 10))
                                .with(loaderFrom(getActivity()))
                                .async()
                                .limit(0)
                                .afterMax(seconds(10))
                                .all()).isEmpty();
        assertThat(LoaderStreams.streamOf()
                                .sync()
                                .thenGet(range(1, 10))
                                .with(loaderFrom(getActivity()))
                                .async()
                                .limit(15)
                                .afterMax(seconds(10))
                                .all()).isEqualTo(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        assertThat(LoaderStreams.streamOf()
                                .sync()
                                .thenGet(range(1, 10))
                                .with(loaderFrom(getActivity()))
                                .async()
                                .limit(0)
                                .afterMax(seconds(10))
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
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .async()
                         .mapAll((BiConsumer<List<?>, ResultChannel<Object>>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .sync()
                         .mapAll((BiConsumer<List<?>, ResultChannel<Object>>) null);
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
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .async()
                         .mapAll((Function<List<?>, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .sync()
                         .mapAll((Function<List<?>, Object>) null);
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
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .async()
                         .map((BiConsumer<Object, ResultChannel<Object>>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .parallel()
                         .map((BiConsumer<Object, ResultChannel<Object>>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .sync()
                         .map((BiConsumer<Object, ResultChannel<Object>>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .serial()
                         .map((BiConsumer<Object, ResultChannel<Object>>) null);
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
        assertThat(LoaderStreams.streamOf("test1", "test2")
                                .with(loaderFrom(getActivity()))
                                .async()
                                .map(factory)
                                .afterMax(seconds(10))
                                .all()).containsExactly("TEST1", "TEST2");
        assertThat(LoaderStreams.streamOf("test1", "test2")
                                .with(loaderFrom(getActivity()))
                                .ordered(OrderType.BY_CALL)
                                .parallel()
                                .map(factory)
                                .afterMax(seconds(10))
                                .all()).containsExactly("TEST1", "TEST2");
        assertThat(LoaderStreams.streamOf("test1", "test2")
                                .with(loaderFrom(getActivity()))
                                .sync()
                                .map(factory)
                                .all()).containsExactly("TEST1", "TEST2");
        assertThat(LoaderStreams.streamOf("test1", "test2")
                                .with(loaderFrom(getActivity()))
                                .ordered(OrderType.BY_CALL)
                                .serial()
                                .map(factory)
                                .afterMax(seconds(10))
                                .all()).containsExactly("TEST1", "TEST2");
    }

    public void testMapContextFactoryIllegalState() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        final ContextInvocationFactory<String, String> factory =
                ContextInvocationFactory.factoryOf(UpperCase.class);
        try {
            LoaderStreams.streamOf("test").async().map(factory);
            fail();

        } catch (final IllegalStateException ignored) {

        }

        try {
            LoaderStreams.streamOf("test").sync().map(factory);
            fail();

        } catch (final IllegalStateException ignored) {

        }

        try {
            LoaderStreams.streamOf("test").parallel().map(factory);
            fail();

        } catch (final IllegalStateException ignored) {

        }

        try {
            LoaderStreams.streamOf("test").serial().map(factory);
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
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .async()
                         .map((ContextInvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .parallel()
                         .map((ContextInvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .sync()
                         .map((ContextInvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .serial()
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
        assertThat(LoaderStreams.streamOf("test1", "test2")
                                .with(loaderFrom(getActivity()))
                                .async()
                                .map(factory)
                                .afterMax(seconds(10))
                                .all()).containsExactly("TEST1", "TEST2");
        assertThat(LoaderStreams.streamOf("test1", "test2")
                                .with(loaderFrom(getActivity()))
                                .ordered(OrderType.BY_CALL)
                                .parallel()
                                .map(factory)
                                .afterMax(seconds(10))
                                .all()).containsExactly("TEST1", "TEST2");
        assertThat(LoaderStreams.streamOf("test1", "test2")
                                .with(loaderFrom(getActivity()))
                                .sync()
                                .map(factory)
                                .all()).containsExactly("TEST1", "TEST2");
        assertThat(LoaderStreams.streamOf("test1", "test2")
                                .with(loaderFrom(getActivity()))
                                .ordered(OrderType.BY_CALL)
                                .serial()
                                .map(factory)
                                .afterMax(seconds(10))
                                .all()).containsExactly("TEST1", "TEST2");
    }

    @SuppressWarnings("ConstantConditions")
    public void testMapFactoryNullPointerError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .async()
                         .map((InvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .parallel()
                         .map((InvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .sync()
                         .map((InvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .serial()
                         .map((InvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testMapFilter() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        assertThat(LoaderStreams.streamOf("test1", "test2")
                                .with(loaderFrom(getActivity()))
                                .async()
                                .map(new UpperCase())
                                .afterMax(seconds(10))
                                .all()).containsExactly("TEST1", "TEST2");
        assertThat(LoaderStreams.streamOf("test1", "test2")
                                .with(loaderFrom(getActivity()))
                                .ordered(OrderType.BY_CALL)
                                .parallel()
                                .map(new UpperCase())
                                .afterMax(seconds(10))
                                .all()).containsExactly("TEST1", "TEST2");
        assertThat(LoaderStreams.streamOf("test1", "test2")
                                .with(loaderFrom(getActivity()))
                                .sync()
                                .map(new UpperCase())
                                .all()).containsExactly("TEST1", "TEST2");
        assertThat(LoaderStreams.streamOf("test1", "test2")
                                .with(loaderFrom(getActivity()))
                                .ordered(OrderType.BY_CALL)
                                .serial()
                                .map(new UpperCase())
                                .afterMax(seconds(10))
                                .all()).containsExactly("TEST1", "TEST2");
    }

    @SuppressWarnings("ConstantConditions")
    public void testMapFilterNullPointerError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .async()
                         .map((OperationInvocation<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .parallel()
                         .map((OperationInvocation<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .sync()
                         .map((OperationInvocation<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .serial()
                         .map((OperationInvocation<Object, Object>) null);
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
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .async()
                         .map((Function<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .parallel()
                         .map((Function<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .sync()
                         .map((Function<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .serial()
                         .map((Function<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testMapRoutine() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        final Routine<String, String> routine = JRoutineCore.on(new UpperCase())
                                                            .invocationConfiguration()
                                                            .withOutputOrder(OrderType.BY_CALL)
                                                            .applyConfiguration()
                                                            .buildRoutine();
        assertThat(LoaderStreams.streamOf("test1", "test2")
                                .with(loaderFrom(getActivity()))
                                .async()
                                .map(routine)
                                .afterMax(seconds(10))
                                .all()).containsExactly("TEST1", "TEST2");
        assertThat(LoaderStreams.streamOf("test1", "test2")
                                .with(loaderFrom(getActivity()))
                                .parallel()
                                .map(routine)
                                .afterMax(seconds(10))
                                .all()).containsExactly("TEST1", "TEST2");
        assertThat(LoaderStreams.streamOf("test1", "test2")
                                .with(loaderFrom(getActivity()))
                                .sync()
                                .map(routine)
                                .all()).containsExactly("TEST1", "TEST2");
        assertThat(LoaderStreams.streamOf("test1", "test2")
                                .with(loaderFrom(getActivity()))
                                .serial()
                                .map(routine)
                                .afterMax(seconds(10))
                                .all()).containsExactly("TEST1", "TEST2");
    }

    @SuppressWarnings("ConstantConditions")
    public void testMapRoutineNullPointerError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .async()
                         .map((Routine<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .parallel()
                         .map((Routine<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .sync()
                         .map((Routine<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .serial()
                         .map((Routine<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }
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
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .orElseGet((Consumer<ResultChannel<?>>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .orElseGet(1, (Consumer<ResultChannel<?>>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf().with(loaderFrom(getActivity())).orElseGet((Supplier<?>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .orElseGet(1, (Supplier<?>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @SuppressWarnings("unchecked")
    public void testOutputToSelectable() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        final IOChannel<String> channel = JRoutineCore.io().buildChannel();
        channel.pass("test1", "test2", "test3").close();
        assertThat(LoaderStreams.streamOf(channel)
                                .with(loaderFrom(getActivity()))
                                .toSelectable(33)
                                .afterMax(seconds(10))
                                .all()).containsExactly(
                new ParcelableSelectable<String>("test1", 33),
                new ParcelableSelectable<String>("test2", 33),
                new ParcelableSelectable<String>("test3", 33));
    }

    public void testOutputToSelectableAbort() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        final IOChannel<String> channel = JRoutineCore.io().buildChannel();
        channel.pass("test1", "test2", "test3").abort();
        try {
            LoaderStreams.streamOf(channel)
                         .with(loaderFrom(getActivity()))
                         .toSelectable(33)
                         .afterMax(seconds(10))
                         .all();
            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testPeek() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        testPeek(getActivity());
    }

    @SuppressWarnings("ConstantConditions")
    public void testPeekNullPointerError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            LoaderStreams.streamOf().with(loaderFrom(getActivity())).async().peek(null);
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
            LoaderStreams.streamOf().with(loaderFrom(getActivity())).async().reduce(null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf().with(loaderFrom(getActivity())).sync().reduce(null);
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
            LoaderStreams.streamOf().with(loaderFrom(getActivity())).async().reduce(null, null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf().with(loaderFrom(getActivity())).sync().reduce(null, null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testRepeat() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        final IOChannel<Object> ioChannel = JRoutineCore.io().buildChannel();
        final OutputChannel<Object> channel =
                LoaderStreams.streamOf(ioChannel).with(loaderFrom(getActivity())).repeat();
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

    public void testRepeatAbort() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        final IOChannel<Object> ioChannel = JRoutineCore.io().buildChannel();
        final OutputChannel<Object> channel =
                LoaderStreams.streamOf(ioChannel).with(loaderFrom(getActivity())).repeat();
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

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        final Routine<Object, String> routine =
                JRoutineCore.on(functionOperation(new Function<Object, String>() {

                    public String apply(final Object o) {

                        return o.toString();
                    }
                })).buildRoutine();
        try {
            LoaderStreams.streamOf((Object) null)
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

    public void testSkip() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        assertThat(LoaderStreams.streamOf()
                                .sync()
                                .thenGet(range(1, 10))
                                .with(loaderFrom(getActivity()))
                                .async()
                                .skip(5)
                                .afterMax(seconds(10))
                                .all()).isEqualTo(Arrays.asList(6, 7, 8, 9, 10));
        assertThat(LoaderStreams.streamOf()
                                .sync()
                                .thenGet(range(1, 10))
                                .with(loaderFrom(getActivity()))
                                .async()
                                .skip(15)
                                .afterMax(seconds(10))
                                .all()).isEmpty();
        assertThat(LoaderStreams.streamOf()
                                .sync()
                                .thenGet(range(1, 10))
                                .with(loaderFrom(getActivity()))
                                .async()
                                .skip(0)
                                .afterMax(seconds(10))
                                .all()).isEqualTo(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
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

        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(getActivity()))
                                .sync()
                                .then((String) null)
                                .all()).containsOnly((String) null);
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(getActivity()))
                                .sync()
                                .then((String[]) null)
                                .all()).isEmpty();
        assertThat(
                LoaderStreams.streamOf("test1").with(loaderFrom(getActivity())).sync().then().all())
                .isEmpty();
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(getActivity()))
                                .sync()
                                .then((List<String>) null)
                                .all()).isEmpty();
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(getActivity()))
                                .sync()
                                .then(Collections.<String>emptyList())
                                .all()).isEmpty();
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(getActivity()))
                                .sync()
                                .then("TEST2")
                                .all()).containsOnly("TEST2");
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(getActivity()))
                                .sync()
                                .then("TEST2", "TEST2")
                                .all()).containsOnly("TEST2", "TEST2");
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(getActivity()))
                                .sync()
                                .then(Collections.singletonList("TEST2"))
                                .all()).containsOnly("TEST2");
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(getActivity()))
                                .async()
                                .then((String) null)
                                .afterMax(seconds(10))
                                .all()).containsOnly((String) null);
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(getActivity()))
                                .async()
                                .then((String[]) null)
                                .afterMax(seconds(10))
                                .all()).isEmpty();
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(getActivity()))
                                .async()
                                .then()
                                .afterMax(seconds(10))
                                .all()).isEmpty();
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(getActivity()))
                                .async()
                                .then((List<String>) null)
                                .afterMax(seconds(10))
                                .all()).isEmpty();
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(getActivity()))
                                .async()
                                .then(Collections.<String>emptyList())
                                .afterMax(seconds(10))
                                .all()).isEmpty();
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(getActivity()))
                                .async()
                                .then("TEST2")
                                .afterMax(seconds(10))
                                .all()).containsOnly("TEST2");
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(getActivity()))
                                .async()
                                .then("TEST2", "TEST2")
                                .afterMax(seconds(10))
                                .all()).containsOnly("TEST2", "TEST2");
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(getActivity()))
                                .async()
                                .then(Collections.singletonList("TEST2"))
                                .afterMax(seconds(10))
                                .all()).containsOnly("TEST2");
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(getActivity()))
                                .parallel()
                                .then((String) null)
                                .afterMax(seconds(10))
                                .all()).containsOnly((String) null);
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(getActivity()))
                                .parallel()
                                .then((String[]) null)
                                .afterMax(seconds(10))
                                .all()).isEmpty();
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(getActivity()))
                                .parallel()
                                .then()
                                .afterMax(seconds(10))
                                .all()).isEmpty();
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(getActivity()))
                                .parallel()
                                .then((List<String>) null)
                                .afterMax(seconds(10))
                                .all()).isEmpty();
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(getActivity()))
                                .parallel()
                                .then(Collections.<String>emptyList())
                                .afterMax(seconds(10))
                                .all()).isEmpty();
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(getActivity()))
                                .parallel()
                                .then("TEST2")
                                .afterMax(seconds(10))
                                .all()).containsOnly("TEST2");
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(getActivity()))
                                .parallel()
                                .then("TEST2", "TEST2")
                                .afterMax(seconds(10))
                                .all()).containsOnly("TEST2", "TEST2");
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(getActivity()))
                                .parallel()
                                .then(Collections.singletonList("TEST2"))
                                .afterMax(seconds(10))
                                .all()).containsOnly("TEST2");
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(getActivity()))
                                .serial()
                                .then((String) null)
                                .afterMax(seconds(10))
                                .all()).containsOnly((String) null);
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(getActivity()))
                                .serial()
                                .then((String[]) null)
                                .afterMax(seconds(10))
                                .all()).isEmpty();
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(getActivity()))
                                .serial()
                                .then()
                                .afterMax(seconds(10))
                                .all()).isEmpty();
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(getActivity()))
                                .serial()
                                .then((List<String>) null)
                                .afterMax(seconds(10))
                                .all()).isEmpty();
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(getActivity()))
                                .serial()
                                .then(Collections.<String>emptyList())
                                .afterMax(seconds(10))
                                .all()).isEmpty();
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(getActivity()))
                                .serial()
                                .then("TEST2")
                                .afterMax(seconds(10))
                                .all()).containsOnly("TEST2");
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(getActivity()))
                                .serial()
                                .then("TEST2", "TEST2")
                                .afterMax(seconds(10))
                                .all()).containsOnly("TEST2", "TEST2");
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(getActivity()))
                                .serial()
                                .then(Collections.singletonList("TEST2"))
                                .afterMax(seconds(10))
                                .all()).containsOnly("TEST2");
    }

    public void testThenNegativeCount() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .sync()
                         .thenGet(-1, Functions.constant(null));
            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .async()
                         .thenGet(0, Functions.constant(null));
            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .parallel()
                         .thenGet(-1, Functions.constant(null));
            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .parallel()
                         .thenGet(-1, Functions.constant(null));
            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .parallel()
                         .thenGet(-1, Functions.sink());
            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .serial()
                         .thenGet(-1, Functions.constant(null));
            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .serial()
                         .thenGet(-1, Functions.constant(null));
            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .serial()
                         .thenGet(-1, Functions.sink());
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
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .sync()
                         .thenGet(3, (Consumer<ResultChannel<?>>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .sync()
                         .thenGet((Consumer<ResultChannel<?>>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .sync()
                         .thenGet(3, (Supplier<?>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .sync()
                         .thenGet((Supplier<?>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .async()
                         .thenGet(3, (Consumer<ResultChannel<?>>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .async()
                         .thenGet((Consumer<ResultChannel<?>>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .async()
                         .thenGet((Supplier<?>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .async()
                         .thenGet(3, (Supplier<?>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .parallel()
                         .thenGet(3, (Supplier<?>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .parallel()
                         .thenGet(3, (Consumer<ResultChannel<?>>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .serial()
                         .thenGet(3, (Supplier<?>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .serial()
                         .thenGet(3, (Consumer<ResultChannel<?>>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }
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
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .tryCatch((BiConsumer<RoutineException, InputChannel<?>>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .tryCatch((Function<RoutineException, ?>) null);
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
            LoaderStreams.streamOf().with(loaderFrom(getActivity())).tryFinally(null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    private static class RetryFunction implements Function<Object, StreamChannel<String>> {

        private final Activity mActivity;

        private final Routine<Object, String> mRoutine;

        private RetryFunction(@NotNull final Activity activity,
                @NotNull final Routine<Object, String> routine) {

            mActivity = activity;
            mRoutine = routine;
        }

        private static StreamChannel<String> apply(final Object o, @NotNull final Activity activity,
                @NotNull final Routine<Object, String> routine, @NotNull final int[] count) {

            return LoaderStreams.streamOf(o)
                                .with(loaderFrom(activity))
                                .map(routine)
                                .tryCatch(new BiConsumer<RoutineException, InputChannel<String>>() {

                                    public void accept(final RoutineException e,
                                            final InputChannel<String> channel) {

                                        if (++count[0] < 3) {
                                            LoaderStreams.streamOf(o)
                                                         .with(loaderFrom(activity))
                                                         .map(routine)
                                                         .tryCatch(this)
                                                         .bind(channel);

                                        } else {
                                            throw e;
                                        }
                                    }
                                });

        }

        public StreamChannel<String> apply(final Object o) {

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
