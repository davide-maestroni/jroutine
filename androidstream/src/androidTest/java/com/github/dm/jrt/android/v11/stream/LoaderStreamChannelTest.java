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
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.channel.ParcelableSelectable;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.InvocationConfiguration.OrderType;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel.InputChannel;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.ExecutionDeadlockException;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.common.TimeoutException;
import com.github.dm.jrt.core.invocation.FilterInvocation;
import com.github.dm.jrt.core.invocation.InvocationFactories;
import com.github.dm.jrt.core.invocation.InvocationFactory;
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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.android.v11.core.LoaderContext.loaderFrom;
import static com.github.dm.jrt.core.util.TimeDuration.minutes;
import static com.github.dm.jrt.core.util.TimeDuration.seconds;
import static com.github.dm.jrt.function.Functions.functionFilter;
import static com.github.dm.jrt.stream.Streams.range;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Android stream channel unit tests.
 * <p/>
 * Created by davide-maestroni on 03/10/2016.
 */
@TargetApi(VERSION_CODES.FROYO)
public class LoaderStreamChannelTest extends ActivityInstrumentationTestCase2<TestActivity> {

    private static final Runner sSingleThreadRunner = Runners.poolRunner(1);

    public LoaderStreamChannelTest() {

        super(TestActivity.class);
    }

    private static void testCollectBiFunction(@NotNull final Activity activity) {

        assertThat(LoaderStreams.streamOf("test1", "test2", "test3")
                                .with(loaderFrom(activity))
                                .async()
                                .collect(new BiFunction<String, String, String>() {

                                    public String apply(final String s, final String s2) {

                                        return s + s2;
                                    }
                                })
                                .afterMax(seconds(10))
                                .all()).containsExactly("test1test2test3");
        assertThat(LoaderStreams.streamOf("test1", "test2", "test3")
                                .with(loaderFrom(activity))
                                .sync()
                                .collect(new BiFunction<String, String, String>() {

                                    public String apply(final String s, final String s2) {

                                        return s + s2;
                                    }
                                })
                                .all()).containsExactly("test1test2test3");
    }

    private static void testCollectConsumer(@NotNull final Activity activity) {

        assertThat(LoaderStreams.streamOf("test1", "test2", "test3")
                                .with(loaderFrom(activity))
                                .async()
                                .collect(new BiConsumer<List<?
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
                                .collect(
                                        new BiConsumer<List<? extends String>,
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

    private static void testCollectFunction(@NotNull final Activity activity) {

        assertThat(LoaderStreams.streamOf("test1", "test2", "test3")
                                .with(loaderFrom(activity))
                                .async()
                                .collect(new Function<List<? extends String>, String>() {

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
                                .collect(new Function<List<? extends String>, String>() {

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
        assertThat(LoaderStreams.streamOf()
                                .with(loaderFrom(activity))
                                .async()
                                .then(range(1, 1000))
                                .backPressureOn(sSingleThreadRunner, 2, 10, TimeUnit.SECONDS)
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
                                .collect(new BiFunction<SumData, SumData, SumData>() {

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
                                .consume(new Consumer<String>() {

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
                                .consume(new Consumer<String>() {

                                    public void accept(final String s) {

                                        list.add(s);
                                    }
                                })
                                .afterMax(seconds(10))
                                .all()).isEmpty();
        assertThat(list).containsOnly("test1", "test2", "test3");
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
                                                            .filter(Functions.<String>notNull());
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
                                                            .filter(Functions.<String>notNull());
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
                                                            .filter(Functions.<String>notNull());
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
                         .withInvocations()
                         .withRunner(runner1)
                         .getConfigured()
                         .map(new Function<String, Object>() {

                             public Object apply(final String s) {

                                 return LoaderStreams.streamOf(s)
                                                     .with(loaderFrom(activity))
                                                     .withInvocations()
                                                     .withRunner(runner1)
                                                     .getConfigured()
                                                     .map(Functions.identity())
                                                     .withInvocations()
                                                     .withRunner(runner2)
                                                     .getConfigured()
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
    }

    private static void testThen(@NotNull final Activity activity) {

        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(activity))
                                .sync()
                                .then(new Consumer<ResultChannel<String>>() {

                                    public void accept(final ResultChannel<String> resultChannel) {

                                        resultChannel.pass("TEST2");
                                    }
                                })
                                .all()).containsOnly("TEST2");
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(activity))
                                .sync()
                                .then(new Supplier<String>() {

                                    public String get() {

                                        return "TEST2";
                                    }
                                })
                                .all()).containsOnly("TEST2");
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(activity))
                                .sync()
                                .then(3, new Supplier<String>() {

                                    public String get() {

                                        return "TEST2";
                                    }
                                })
                                .afterMax(seconds(10))
                                .all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(activity))
                                .async()
                                .then(new Consumer<ResultChannel<String>>() {

                                    public void accept(final ResultChannel<String> resultChannel) {

                                        resultChannel.pass("TEST2");
                                    }
                                })
                                .afterMax(seconds(10))
                                .all()).containsOnly("TEST2");
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(activity))
                                .async()
                                .then(new Supplier<String>() {

                                    public String get() {

                                        return "TEST2";
                                    }
                                })
                                .afterMax(seconds(10))
                                .all()).containsOnly("TEST2");
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(activity))
                                .async()
                                .then(3, new Supplier<String>() {

                                    public String get() {

                                        return "TEST2";
                                    }
                                })
                                .afterMax(seconds(10))
                                .all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(activity))
                                .parallel()
                                .then(3, new Consumer<ResultChannel<String>>() {

                                    public void accept(final ResultChannel<String> resultChannel) {

                                        resultChannel.pass("TEST2");
                                    }
                                })
                                .afterMax(seconds(10))
                                .all()).containsExactly("TEST2", "TEST2", "TEST2");
        assertThat(LoaderStreams.streamOf("test1")
                                .with(loaderFrom(activity))
                                .parallel()
                                .then(3, new Supplier<String>() {

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

        try {
            LoaderStreams.streamOf("test")
                         .with(loaderFrom(activity))
                         .sync()
                         .map(new Function<Object, Object>() {

                             public Object apply(final Object o) {

                                 throw new NullPointerException();
                             }
                         })
                         .tryCatch(new Consumer<RoutineException>() {

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
                                .tryCatch(new Consumer<RoutineException>() {

                                    public void accept(final RoutineException e) {

                                    }
                                })
                                .all()).isEmpty();
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

        assertThat(channel.skip(1).next(1)).containsExactly("test2");
        assertThat(channel.eventuallyExit().next(4)).containsExactly("test3");
        assertThat(channel.eventuallyExit().nextOr("test4")).isEqualTo("test4");
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

    public void testCollectBiFunction() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        testCollectBiFunction(getActivity());
    }

    @SuppressWarnings("ConstantConditions")
    public void testCollectBiFunctionNullPointerError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .async()
                         .collect((BiFunction<Object, Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .sync()
                         .collect((BiFunction<Object, Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testCollectConsumer() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        testCollectConsumer(getActivity());
    }

    @SuppressWarnings("ConstantConditions")
    public void testCollectConsumerNullPointerError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .async()
                         .collect((BiConsumer<List<?>, ResultChannel<Object>>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .sync()
                         .collect((BiConsumer<List<?>, ResultChannel<Object>>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testCollectFunction() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        testCollectFunction(getActivity());
    }

    @SuppressWarnings("ConstantConditions")
    public void testCollectFunctionNullPointerError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .async()
                         .collect((Function<List<?>, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .sync()
                         .collect((Function<List<?>, Object>) null);
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

    @SuppressWarnings("ConstantConditions")
    public void testConsumeNullPointerError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        final Consumer<Object> consumer = null;
        try {
            LoaderStreams.streamOf().with(loaderFrom(getActivity())).sync().consume(consumer);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf().with(loaderFrom(getActivity())).async().consume(consumer);
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
                                .filter(Functions.notNull())
                                .afterMax(seconds(10))
                                .all()).containsExactly("test");
        assertThat(LoaderStreams.streamOf(null, "test")
                                .with(loaderFrom(getActivity()))
                                .parallel()
                                .filter(Functions.notNull())
                                .afterMax(seconds(10))
                                .all()).containsExactly("test");
        assertThat(LoaderStreams.streamOf(null, "test")
                                .with(loaderFrom(getActivity()))
                                .sync()
                                .filter(Functions.notNull())
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
    }

    public void testMapFactory() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        final InvocationFactory<String, String> factory =
                InvocationFactories.factoryOf(UpperCase.class);
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
                         .map((FilterInvocation<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .parallel()
                         .map((FilterInvocation<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .sync()
                         .map((FilterInvocation<Object, Object>) null);
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
    }

    public void testMapRoutine() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        final Routine<String, String> routine = JRoutineCore.on(new UpperCase())
                                                            .withInvocations()
                                                            .withOutputOrder(OrderType.BY_CALL)
                                                            .getConfigured()
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
                JRoutineCore.on(functionFilter(new Function<Object, String>() {

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
    }

    public void testThenNegativeCount() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .sync()
                         .then(-1, Functions.constant(null));
            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .async()
                         .then(0, Functions.constant(null));
            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .parallel()
                         .then(-1, Functions.constant(null));
            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .parallel()
                         .then(-1, Functions.constant(null));
            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .parallel()
                         .then(-1, Functions.sink());
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
                         .then(3, (Consumer<ResultChannel<?>>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .sync()
                         .then((Consumer<ResultChannel<?>>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .sync()
                         .then(3, (Supplier<?>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .sync()
                         .then((Supplier<?>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .async()
                         .then(3, (Consumer<ResultChannel<?>>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .async()
                         .then((Consumer<ResultChannel<?>>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .async()
                         .then((Supplier<?>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .async()
                         .then(3, (Supplier<?>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .parallel()
                         .then(3, (Supplier<?>) null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            LoaderStreams.streamOf()
                         .with(loaderFrom(getActivity()))
                         .parallel()
                         .then(3, (Consumer<ResultChannel<?>>) null);
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
                         .tryCatch((Consumer<RoutineException>) null);
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

    private static class UpperCase extends FilterInvocation<String, String> {

        public void onInput(final String input, @NotNull final ResultChannel<String> result) {

            result.pass(input.toUpperCase());
        }
    }
}
