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
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;
import android.util.SparseArray;

import com.github.dm.jrt.android.builder.LoaderConfiguration.CacheStrategyType;
import com.github.dm.jrt.android.core.Channels.ParcelableSelectable;
import com.github.dm.jrt.android.invocation.FunctionContextInvocationFactory;
import com.github.dm.jrt.android.invocation.MissingInvocationException;
import com.github.dm.jrt.android.invocation.PassingFunctionContextInvocation;
import com.github.dm.jrt.android.v11.TestActivity;
import com.github.dm.jrt.android.v11.core.Channels;
import com.github.dm.jrt.android.v11.core.JRoutine;
import com.github.dm.jrt.android.v11.core.LoaderContext;
import com.github.dm.jrt.builder.IOChannelBuilder;
import com.github.dm.jrt.builder.InvocationConfiguration.OrderType;
import com.github.dm.jrt.channel.AbortException;
import com.github.dm.jrt.channel.Channel.OutputChannel;
import com.github.dm.jrt.channel.IOChannel;
import com.github.dm.jrt.channel.InvocationChannel;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.core.DelegatingInvocation.DelegationType;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Functions;
import com.github.dm.jrt.function.Supplier;
import com.github.dm.jrt.invocation.FilterInvocation;
import com.github.dm.jrt.invocation.InvocationException;
import com.github.dm.jrt.invocation.InvocationFactory;
import com.github.dm.jrt.invocation.InvocationInterruptedException;
import com.github.dm.jrt.invocation.TemplateInvocation;
import com.github.dm.jrt.routine.Routine;
import com.github.dm.jrt.stream.StreamChannel;
import com.github.dm.jrt.util.ClassToken;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.dm.jrt.android.core.DelegatingContextInvocation.factoryFrom;
import static com.github.dm.jrt.android.v11.core.LoaderContext.loaderFrom;
import static com.github.dm.jrt.invocation.Invocations.factoryOf;
import static com.github.dm.jrt.util.TimeDuration.ZERO;
import static com.github.dm.jrt.util.TimeDuration.millis;
import static com.github.dm.jrt.util.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Streams unit tests.
 * <p/>
 * Created by davide-maestroni on 01/04/2016.
 */
@TargetApi(VERSION_CODES.FROYO)
public class StreamsTest extends ActivityInstrumentationTestCase2<TestActivity> {

    public StreamsTest() {

        super(TestActivity.class);
    }

    @NotNull
    private static Supplier<Integer> delayedIncrement(@NotNull final AtomicInteger count) {

        return new Supplier<Integer>() {

            public Integer get() {

                try {
                    seconds(1).sleepAtLeast();
                } catch (final InterruptedException e) {
                    throw InvocationInterruptedException.wrapIfNeeded(e);
                }
                return count.incrementAndGet();
            }
        };
    }

    @NotNull
    private static Supplier<Integer> increment(@NotNull final AtomicInteger count) {

        return new Supplier<Integer>() {

            public Integer get() {

                return count.incrementAndGet();
            }
        };
    }

    @NotNull
    private static Function<String, String> stringIncrement(@NotNull final AtomicInteger count) {

        return new Function<String, String>() {

            public String apply(final String s) {

                return s + count.incrementAndGet();
            }
        };
    }

    @NotNull
    private static Function<String, String> toUpperCase() {

        return new Function<String, String>() {

            public String apply(final String s) {

                try {
                    seconds(1).sleepAtLeast();
                } catch (final InterruptedException e) {
                    throw InvocationInterruptedException.wrapIfNeeded(e);
                }
                return s.toUpperCase();
            }
        };
    }

    @NotNull
    private static Function<StreamChannel<String>, StreamChannel<String>> toUpperCaseChannel() {

        return new Function<StreamChannel<String>, StreamChannel<String>>() {

            public StreamChannel<String> apply(final StreamChannel<String> channel) {

                return channel.sync().map(new Function<String, String>() {

                    public String apply(final String s) {

                        return s.toUpperCase();
                    }
                });
            }
        };
    }

    public void testBlend() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final LoaderContext context = loaderFrom(getActivity());
        StreamChannel<String> channel1 =
                Streams.streamOf("test1", "test2", "test3").with(context).runOnShared();
        StreamChannel<String> channel2 =
                Streams.streamOf("test4", "test5", "test6").with(context).runOnShared();
        assertThat(Streams.blend(channel2, channel1).afterMax(seconds(1)).all()).containsOnly(
                "test1", "test2", "test3", "test4", "test5", "test6");
        channel1 = Streams.streamOf("test1", "test2", "test3").with(context).runOnShared();
        channel2 = Streams.streamOf("test4", "test5", "test6").with(context).runOnShared();
        assertThat(Streams.blend(Arrays.<StreamChannel<?>>asList(channel1, channel2))
                          .afterMax(seconds(1))
                          .all()).containsOnly("test1", "test2", "test3", "test4", "test5",
                                               "test6");
    }

    public void testBlendAbort() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final IOChannelBuilder builder = JRoutine.io();
        final Routine<Object, Object> routine = JRoutine.with(loaderFrom(getActivity()))
                                                        .on(PassingFunctionContextInvocation
                                                                    .factoryOf())
                                                        .buildRoutine();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().abort();

        try {

            routine.asyncCall(Streams.blend(channel1, channel2)).afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().abort();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();

        try {

            routine.asyncCall(Streams.blend(Arrays.<OutputChannel<?>>asList(channel1, channel2)))
                   .afterMax(seconds(1))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testBlendError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        try {

            Streams.blend();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.blend((OutputChannel<?>[]) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.blend(new OutputChannel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.blend(Collections.<OutputChannel<?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.blend((List<OutputChannel<?>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.blend(Collections.<OutputChannel<?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testBuilder() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final LoaderContext context = loaderFrom(getActivity());
        assertThat(Streams.streamOf("test")
                          .with(context)
                          .runOnShared()
                          .afterMax(seconds(1))
                          .all()).containsExactly("test");
        assertThat(Streams.streamOf("test1", "test2", "test3")
                          .with(context)
                          .runOnShared()
                          .afterMax(seconds(1))
                          .all()).containsExactly("test1", "test2", "test3");
        assertThat(Streams.streamOf(Arrays.asList("test1", "test2", "test3"))
                          .with(context)
                          .runOnShared()
                          .afterMax(seconds(1))
                          .all()).containsExactly("test1", "test2", "test3");
        assertThat(Streams.streamOf(JRoutine.io().of("test1", "test2", "test3"))
                          .with(context)
                          .runOnShared()
                          .afterMax(seconds(1))
                          .all()).containsExactly("test1", "test2", "test3");
    }

    @SuppressWarnings("ConstantConditions")
    public void testBuilderNullPointerError() {

        try {

            Streams.streamOf((OutputChannel<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testConcat() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final LoaderContext context = loaderFrom(getActivity());
        StreamChannel<String> channel1 =
                Streams.streamOf("test1", "test2", "test3").with(context).runOnShared();
        StreamChannel<String> channel2 =
                Streams.streamOf("test4", "test5", "test6").with(context).runOnShared();
        assertThat(Streams.concat(channel2, channel1).afterMax(seconds(1)).all()).containsExactly(
                "test4", "test5", "test6", "test1", "test2", "test3");
        channel1 = Streams.streamOf("test1", "test2", "test3").with(context).runOnShared();
        channel2 = Streams.streamOf("test4", "test5", "test6").with(context).runOnShared();
        assertThat(Streams.concat(Arrays.<StreamChannel<?>>asList(channel1, channel2))
                          .afterMax(seconds(1))
                          .all()).containsExactly("test1", "test2", "test3", "test4", "test5",
                                                  "test6");
    }

    public void testConcatAbort() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final IOChannelBuilder builder = JRoutine.io();
        final Routine<Object, Object> routine = JRoutine.with(loaderFrom(getActivity()))
                                                        .on(PassingFunctionContextInvocation
                                                                    .factoryOf())
                                                        .buildRoutine();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().abort();

        try {

            routine.asyncCall(Streams.concat(channel1, channel2)).afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().abort();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();

        try {

            routine.asyncCall(Streams.concat(Arrays.<OutputChannel<?>>asList(channel1, channel2)))
                   .afterMax(seconds(1))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testConcatError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        try {

            Streams.concat();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.concat((OutputChannel<?>[]) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.concat(new OutputChannel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.concat(Collections.<OutputChannel<?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.concat((List<OutputChannel<?>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.concat(Collections.<OutputChannel<?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testFactory() {

        assertThat(Streams.onStream(toUpperCaseChannel())
                          .asyncCall("test1", "test2", "test3")
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2", "TEST3");

        try {

            final InvocationChannel<String, String> channel =
                    Streams.onStream(toUpperCaseChannel()).asyncInvoke();
            channel.abort(new IllegalArgumentException());
            channel.result().afterMax(seconds(3)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final FunctionContextInvocationFactory<String, String> factory =
                Streams.contextFactory(toUpperCaseChannel());
        assertThat(JRoutine.with(loaderFrom(getActivity()))
                           .on(factory)
                           .asyncCall("test1", "test2", "test3")
                           .afterMax(seconds(3))
                           .all()).containsExactly("TEST1", "TEST2", "TEST3");

        try {

            final InvocationChannel<String, String> channel =
                    JRoutine.with(loaderFrom(getActivity())).on(factory).asyncInvoke();
            channel.abort(new IllegalArgumentException());
            channel.result().afterMax(seconds(3)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        assertThat(Streams.onStreamWith(loaderFrom(getActivity()), toUpperCaseChannel())
                          .asyncCall("test1", "test2", "test3")
                          .afterMax(seconds(3))
                          .all()).containsExactly("TEST1", "TEST2", "TEST3");

        try {

            final InvocationChannel<String, String> channel =
                    Streams.onStreamWith(loaderFrom(getActivity()), toUpperCaseChannel())
                           .asyncInvoke();
            channel.abort(new IllegalArgumentException());
            channel.result().afterMax(seconds(3)).next();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testFactoryEquals() {

        final Function<StreamChannel<String>, StreamChannel<String>> function =
                toUpperCaseChannel();
        final FunctionContextInvocationFactory<String, String> factory =
                Streams.contextFactory(function);
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(null);
        assertThat(factory).isNotEqualTo("test");
        assertThat(factory).isNotEqualTo(
                Streams.contextFactory(Functions.<StreamChannel<?>>identity()));
        assertThat(factory).isEqualTo(Streams.contextFactory(function));
        assertThat(factory.hashCode()).isEqualTo(Streams.contextFactory(function).hashCode());
    }

    @SuppressWarnings("ConstantConditions")
    public void testFactoryError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        try {

            Streams.contextFactory(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.onStreamWith(null, Functions.<StreamChannel<?>>identity());

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.onStreamWith(loaderFrom(getActivity()), null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @SuppressWarnings("unchecked")
    public void testGroupBy() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final LoaderContext context = loaderFrom(getActivity());
        assertThat(Streams.streamOf()
                          .with(context)
                          .sync()
                          .range(1, 10)
                          .async()
                          .map(Streams.<Number>groupBy(3))
                          .afterMax(seconds(3))
                          .all()).containsExactly(Arrays.<Number>asList(1, 2, 3),
                                                  Arrays.<Number>asList(4, 5, 6),
                                                  Arrays.<Number>asList(7, 8, 9),
                                                  Collections.<Number>singletonList(10));
        assertThat(Streams.streamOf()
                          .with(context)
                          .sync()
                          .range(1, 10)
                          .async()
                          .map(Streams.<Number>groupBy(13))
                          .afterMax(seconds(3))
                          .all()).containsExactly(
                Arrays.<Number>asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        assertThat(Streams.streamOf()
                          .sync()
                          .range(1, 10)
                          .with(context)
                          .async()
                          .map(Streams.<Number>groupBy(3))
                          .afterMax(seconds(3))
                          .all()).containsExactly(Arrays.<Number>asList(1, 2, 3),
                                                  Arrays.<Number>asList(4, 5, 6),
                                                  Arrays.<Number>asList(7, 8, 9),
                                                  Collections.<Number>singletonList(10));
        assertThat(Streams.streamOf()
                          .sync()
                          .range(1, 10)
                          .with(context)
                          .async()
                          .map(Streams.<Number>groupBy(13))
                          .afterMax(seconds(3))
                          .all()).containsExactly(
                Arrays.<Number>asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
    }

    public void testGroupByEquals() {

        final InvocationFactory<Object, List<Object>> factory = Streams.groupBy(2);
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(null);
        assertThat(factory).isNotEqualTo("test");
        assertThat(factory).isNotEqualTo(Streams.groupBy(3));
        assertThat(factory).isEqualTo(Streams.groupBy(2));
        assertThat(factory.hashCode()).isEqualTo(Streams.groupBy(2).hashCode());
    }

    public void testGroupByError() {

        try {

            Streams.groupBy(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.groupBy(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @SuppressWarnings("unchecked")
    public void testGroupByPlaceholder() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final LoaderContext context = loaderFrom(getActivity());
        assertThat(Streams.streamOf()
                          .with(context)
                          .sync()
                          .range(1, 10)
                          .async()
                          .map(Streams.<Number>groupBy(3, 0))
                          .afterMax(seconds(3))
                          .all()).containsExactly(Arrays.<Number>asList(1, 2, 3),
                                                  Arrays.<Number>asList(4, 5, 6),
                                                  Arrays.<Number>asList(7, 8, 9),
                                                  Arrays.<Number>asList(10, 0, 0));
        assertThat(Streams.streamOf()
                          .with(context)
                          .sync()
                          .range(1, 10)
                          .async()
                          .map(Streams.<Number>groupBy(13, -1))
                          .afterMax(seconds(3))
                          .all()).containsExactly(
                Arrays.<Number>asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, -1, -1, -1));
        assertThat(Streams.streamOf()
                          .sync()
                          .range(1, 10)
                          .with(context)
                          .async()
                          .map(Streams.<Number>groupBy(3, -31))
                          .afterMax(seconds(3))
                          .all()).containsExactly(Arrays.<Number>asList(1, 2, 3),
                                                  Arrays.<Number>asList(4, 5, 6),
                                                  Arrays.<Number>asList(7, 8, 9),
                                                  Arrays.<Number>asList(10, -31, -31));
        assertThat(Streams.streamOf()
                          .sync()
                          .range(1, 10)
                          .with(context)
                          .async()
                          .map(Streams.<Number>groupBy(13, 71))
                          .afterMax(seconds(3))
                          .all()).containsExactly(
                Arrays.<Number>asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 71, 71, 71));
    }

    public void testGroupByPlaceholderEquals() {

        final Object placeholder = -11;
        final InvocationFactory<Object, List<Object>> factory = Streams.groupBy(2, placeholder);
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(null);
        assertThat(factory).isNotEqualTo("test");
        assertThat(factory).isNotEqualTo(Streams.groupBy(3, -11));
        assertThat(factory).isEqualTo(Streams.groupBy(2, -11));
        assertThat(factory.hashCode()).isEqualTo(Streams.groupBy(2, -11).hashCode());
    }

    public void testGroupByPlaceholderError() {

        try {

            Streams.groupBy(-1, 77);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.groupBy(0, null);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testJoin() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final IOChannelBuilder builder = JRoutine.io();
        final Routine<List<?>, Character> routine = JRoutine.with(loaderFrom(getActivity()))
                                                            .on(factoryFrom(
                                                                    JRoutine.on(new CharAt())
                                                                            .buildRoutine(), 1,
                                                                    DelegationType.SYNC))
                                                            .buildRoutine();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.asyncCall(Streams.join(channel1, channel2))
                          .afterMax(seconds(10))
                          .all()).containsExactly('s', '2');
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(
                routine.asyncCall(Streams.join(Arrays.<OutputChannel<?>>asList(channel1, channel2)))
                       .afterMax(seconds(10))
                       .all()).containsExactly('s', '2');
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall()
                .after(millis(100))
                .pass("testtest")
                .pass("test2")
                .pass("test3")
                .close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.asyncCall(Streams.join(channel1, channel2))
                          .afterMax(seconds(10))
                          .all()).containsExactly('s', '2');
    }

    public void testJoinAbort() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final IOChannelBuilder builder = JRoutine.io();
        final Routine<List<?>, Character> routine = JRoutine.with(loaderFrom(getActivity()))
                                                            .on(factoryFrom(
                                                                    JRoutine.on(new CharAt())
                                                                            .buildRoutine(), 1,
                                                                    DelegationType.SYNC))
                                                            .buildRoutine();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().abort();

        try {

            routine.asyncCall(Streams.join(channel1, channel2)).afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().abort();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();

        try {

            routine.asyncCall(Streams.join(Arrays.<OutputChannel<?>>asList(channel1, channel2)))
                   .afterMax(seconds(1))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testJoinError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        try {

            Streams.join();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.join(Collections.<OutputChannel<?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.join(new OutputChannel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.join(Collections.<OutputChannel<?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testJoinPlaceholder() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final IOChannelBuilder builder = JRoutine.io();
        final Routine<List<?>, Character> routine = JRoutine.with(loaderFrom(getActivity()))
                                                            .on(factoryFrom(
                                                                    JRoutine.on(new CharAt())
                                                                            .buildRoutine(), 1,
                                                                    DelegationType.SYNC))
                                                            .buildRoutine();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.asyncCall(Streams.join(new Object(), channel1, channel2))
                          .afterMax(seconds(10))
                          .all()).containsExactly('s', '2');
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.asyncCall(
                Streams.join(null, Arrays.<OutputChannel<?>>asList(channel1, channel2)))
                          .afterMax(seconds(10))
                          .all()).containsExactly('s', '2');
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall()
                .after(millis(100))
                .pass("testtest")
                .pass("test2")
                .pass("test3")
                .close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();

        try {

            routine.asyncCall(Streams.join(new Object(), channel1, channel2))
                   .afterMax(seconds(10))
                   .all();

            fail();

        } catch (final InvocationException ignored) {

        }
    }

    public void testJoinPlaceholderAbort() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final IOChannelBuilder builder = JRoutine.io();
        final Routine<List<?>, Character> routine = JRoutine.with(loaderFrom(getActivity()))
                                                            .on(factoryFrom(
                                                                    JRoutine.on(new CharAt())
                                                                            .buildRoutine(), 1,
                                                                    DelegationType.SYNC))
                                                            .buildRoutine();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().abort();

        try {

            routine.asyncCall(Streams.join((Object) null, channel1, channel2))
                   .afterMax(seconds(1))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().abort();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();

        try {

            routine.asyncCall(
                    Streams.join(new Object(), Arrays.<OutputChannel<?>>asList(channel1, channel2)))
                   .afterMax(seconds(1))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testJoinPlaceholderError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        try {

            Streams.join(new Object());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.join(null, Collections.<OutputChannel<?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.join(new Object(), new OutputChannel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.join(new Object(), Collections.<OutputChannel<?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testLazyBuilder() {

        assertThat(Streams.lazyStreamOf().afterMax(seconds(1)).all()).isEmpty();
        assertThat(Streams.lazyStreamOf("test").afterMax(seconds(1)).all()).containsExactly("test");
        assertThat(Streams.lazyStreamOf("test1", "test2", "test3")
                          .afterMax(seconds(1))
                          .all()).containsExactly("test1", "test2", "test3");
        assertThat(Streams.lazyStreamOf(Arrays.asList("test1", "test2", "test3"))
                          .afterMax(seconds(1))
                          .all()).containsExactly("test1", "test2", "test3");
        assertThat(Streams.lazyStreamOf(JRoutine.io().of("test1", "test2", "test3"))
                          .afterMax(seconds(1))
                          .all()).containsExactly("test1", "test2", "test3");

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        final LoaderContext context = loaderFrom(getActivity());
        assertThat(Streams.lazyStreamOf()
                          .with(context)
                          .runOnShared()
                          .afterMax(seconds(1))
                          .all()).isEmpty();
        assertThat(
                Streams.lazyStreamOf("test").with(context).runOnShared().afterMax(seconds(1)).all())
                .containsExactly("test");
        assertThat(Streams.lazyStreamOf("test1", "test2", "test3")
                          .with(context)
                          .runOnShared()
                          .afterMax(seconds(1))
                          .all()).containsExactly("test1", "test2", "test3");
        assertThat(Streams.lazyStreamOf(Arrays.asList("test1", "test2", "test3"))
                          .with(context)
                          .runOnShared()
                          .afterMax(seconds(1))
                          .all()).containsExactly("test1", "test2", "test3");
        assertThat(Streams.lazyStreamOf(JRoutine.io().of("test1", "test2", "test3"))
                          .with(context)
                          .runOnShared()
                          .afterMax(seconds(1))
                          .all()).containsExactly("test1", "test2", "test3");
    }

    @SuppressWarnings("ConstantConditions")
    public void testLazyBuilderNullPointerError() {

        try {

            Streams.lazyStreamOf((OutputChannel<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testLimit() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final LoaderContext context = loaderFrom(getActivity());
        assertThat(Streams.streamOf()
                          .with(context)
                          .sync()
                          .range(1, 10)
                          .async()
                          .map(Streams.limit(5))
                          .afterMax(seconds(3))
                          .all()).containsExactly(1, 2, 3, 4, 5);
        assertThat(Streams.streamOf()
                          .with(context)
                          .sync()
                          .range(1, 10)
                          .async()
                          .map(Streams.limit(0))
                          .afterMax(seconds(3))
                          .all()).isEmpty();
        assertThat(Streams.streamOf()
                          .with(context)
                          .sync()
                          .range(1, 10)
                          .async()
                          .map(Streams.limit(15))
                          .afterMax(seconds(3))
                          .all()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(Streams.streamOf()
                          .sync()
                          .range(1, 10)
                          .with(context)
                          .async()
                          .map(Streams.limit(5))
                          .afterMax(seconds(3))
                          .all()).containsExactly(1, 2, 3, 4, 5);
        assertThat(Streams.streamOf()
                          .sync()
                          .range(1, 10)
                          .with(context)
                          .async()
                          .map(Streams.limit(0))
                          .afterMax(seconds(3))
                          .all()).isEmpty();
        assertThat(Streams.streamOf()
                          .sync()
                          .range(1, 10)
                          .with(context)
                          .async()
                          .map(Streams.limit(15))
                          .afterMax(seconds(3))
                          .all()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    public void testLimitEquals() {

        final InvocationFactory<Object, Object> factory = Streams.limit(2);
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(null);
        assertThat(factory).isNotEqualTo("test");
        assertThat(factory).isNotEqualTo(Streams.limit(3));
        assertThat(factory).isEqualTo(Streams.limit(2));
        assertThat(factory.hashCode()).isEqualTo(Streams.limit(2).hashCode());
    }

    public void testLimitError() {

        try {

            Streams.limit(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testLoaderId() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final LoaderContext context = loaderFrom(getActivity());
        Streams.streamOf("test1").with(context).loaderId(11).async().map(toUpperCase());
        assertThat(JRoutine.with(context)
                           .onId(11)
                           .buildChannel()
                           .afterMax(seconds(10))
                           .next()).isEqualTo("TEST1");
        Streams.streamOf("test2")
               .with(context)
               .withLoaders()
               .withLoaderId(21)
               .set()
               .async()
               .map(toUpperCase());
        assertThat(JRoutine.with(context)
                           .onId(21)
                           .buildChannel()
                           .afterMax(seconds(10))
                           .next()).isEqualTo("TEST2");
        Streams.streamOf("test3")
               .with(context)
               .withStreamLoaders()
               .withLoaderId(31)
               .set()
               .async()
               .map(toUpperCase());
        assertThat(JRoutine.with(context)
                           .onId(31)
                           .buildChannel()
                           .afterMax(seconds(10))
                           .next()).isEqualTo("TEST3");
    }

    public void testMap() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final IOChannelBuilder builder =
                JRoutine.io().withChannels().withChannelOrder(OrderType.BY_CALL).set();
        final IOChannel<String> channel1 = builder.buildChannel();
        final IOChannel<Integer> channel2 = builder.buildChannel();

        final OutputChannel<? extends ParcelableSelectable<Object>> channel =
                Streams.merge(Arrays.<OutputChannel<?>>asList(channel1, channel2));
        final OutputChannel<ParcelableSelectable<Object>> output =
                JRoutine.with(loaderFrom(getActivity()))
                        .on(factoryFrom(JRoutine.on(new Sort()).buildRoutine(), 1,
                                        DelegationType.SYNC))
                        .withInvocations()
                        .withInputOrder(OrderType.BY_CALL)
                        .set()
                        .asyncCall(channel);
        final SparseArray<OutputChannel<Object>> channelMap =
                Channels.selectParcelable(output, Sort.INTEGER, Sort.STRING);

        for (int i = 0; i < 4; i++) {

            final String input = Integer.toString(i);
            channel1.after(millis(20)).pass(input);
            channel2.after(millis(20)).pass(i);
        }

        channel1.close();
        channel2.close();

        final LoaderContext context = loaderFrom(getActivity());
        assertThat(Streams.streamOf(channelMap.get(Sort.STRING))
                          .with(context)
                          .runOnShared()
                          .afterMax(seconds(1))
                          .all()).containsExactly("0", "1", "2", "3");
        assertThat(Streams.streamOf(channelMap.get(Sort.INTEGER))
                          .with(context)
                          .runOnShared()
                          .afterMax(seconds(1))
                          .all()).containsExactly(0, 1, 2, 3);
    }

    public void testMerge() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final IOChannelBuilder builder =
                JRoutine.io().withChannels().withChannelOrder(OrderType.BY_CALL).set();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        OutputChannel<? extends ParcelableSelectable<?>> outputChannel;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = Streams.<Object>merge(-7, channel1, channel2);
        channel1.pass("test1").close();
        channel2.pass(13).close();
        assertThat(outputChannel.afterMax(seconds(1)).all()).containsOnly(
                new ParcelableSelectable<String>("test1", -7),
                new ParcelableSelectable<Integer>(13, -6));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel =
                Streams.<Object>merge(11, Arrays.<OutputChannel<?>>asList(channel1, channel2));
        channel2.pass(13).close();
        channel1.pass("test1").close();
        assertThat(outputChannel.afterMax(seconds(1)).all()).containsOnly(
                new ParcelableSelectable<String>("test1", 11),
                new ParcelableSelectable<Integer>(13, 12));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = Streams.<Object>merge(channel1, channel2);
        channel1.pass("test2").close();
        channel2.pass(-17).close();
        assertThat(outputChannel.afterMax(seconds(1)).all()).containsOnly(
                new ParcelableSelectable<String>("test2", 0),
                new ParcelableSelectable<Integer>(-17, 1));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = Streams.<Object>merge(Arrays.<OutputChannel<?>>asList(channel1, channel2));
        channel1.pass("test2").close();
        channel2.pass(-17).close();
        assertThat(outputChannel.afterMax(seconds(1)).all()).containsOnly(
                new ParcelableSelectable<String>("test2", 0),
                new ParcelableSelectable<Integer>(-17, 1));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        final SparseArray<OutputChannel<?>> channelMap = new SparseArray<OutputChannel<?>>(2);
        channelMap.put(7, channel1);
        channelMap.put(-3, channel2);
        outputChannel = Streams.<Object>merge(channelMap);
        channel1.pass("test3").close();
        channel2.pass(111).close();
        assertThat(outputChannel.afterMax(seconds(1)).all()).containsOnly(
                new ParcelableSelectable<String>("test3", 7),
                new ParcelableSelectable<Integer>(111, -3));
    }

    @SuppressWarnings("unchecked")
    public void testMerge4() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final IOChannelBuilder builder =
                JRoutine.io().withChannels().withChannelOrder(OrderType.BY_CALL).set();
        final IOChannel<String> channel1 = builder.buildChannel();
        final IOChannel<String> channel2 = builder.buildChannel();
        final IOChannel<String> channel3 = builder.buildChannel();
        final IOChannel<String> channel4 = builder.buildChannel();

        final Routine<ParcelableSelectable<String>, String> routine =
                JRoutine.with(loaderFrom(getActivity()))
                        .on(factoryFrom(JRoutine.on(factoryOf(new ClassToken<Amb<String>>() {}))
                                                .buildRoutine(), 1, DelegationType.SYNC))
                        .buildRoutine();
        final OutputChannel<String> outputChannel = routine.asyncCall(
                Streams.merge(Arrays.asList(channel1, channel2, channel3, channel4)));

        for (int i = 0; i < 4; i++) {

            final String input = Integer.toString(i);
            channel1.after(millis(20)).pass(input);
            channel2.after(millis(20)).pass(input);
            channel3.after(millis(20)).pass(input);
            channel4.after(millis(20)).pass(input);
        }

        channel1.close();
        channel2.close();
        channel3.close();
        channel4.close();

        assertThat(outputChannel.afterMax(seconds(10)).all()).containsExactly("0", "1", "2", "3");
    }

    public void testMergeAbort() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final IOChannelBuilder builder =
                JRoutine.io().withChannels().withChannelOrder(OrderType.BY_CALL).set();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        OutputChannel<? extends ParcelableSelectable<?>> outputChannel;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = Streams.<Object>merge(-7, channel1, channel2);
        channel1.pass("test1").close();
        channel2.abort();

        try {

            outputChannel.afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel =
                Streams.<Object>merge(11, Arrays.<OutputChannel<?>>asList(channel1, channel2));
        channel2.abort();
        channel1.pass("test1").close();

        try {

            outputChannel.afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = Streams.<Object>merge(channel1, channel2);
        channel1.abort();
        channel2.pass(-17).close();

        try {

            outputChannel.afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = Streams.<Object>merge(Arrays.<OutputChannel<?>>asList(channel1, channel2));
        channel1.pass("test2").close();
        channel2.abort();

        try {

            outputChannel.afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        final SparseArray<OutputChannel<?>> channelMap = new SparseArray<OutputChannel<?>>(2);
        channelMap.append(7, channel1);
        channelMap.append(-3, channel2);
        outputChannel = Streams.<Object>merge(channelMap);
        channel1.abort();
        channel2.pass(111).close();

        try {

            outputChannel.afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testMergeError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        try {

            Streams.merge(0, Collections.<OutputChannel<Object>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.merge(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.merge(Collections.<OutputChannel<Object>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.merge(Collections.<Integer, OutputChannel<Object>>emptyMap());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.merge();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.merge(new OutputChannel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.merge(Collections.<OutputChannel<?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.merge(0, new OutputChannel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.merge(0, Collections.<OutputChannel<?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.merge(Collections.<Integer, OutputChannel<?>>singletonMap(1, null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @SuppressWarnings("unchecked")
    public void testOutputToSelectable() {

        final IOChannel<String> channel = JRoutine.io().buildChannel();
        channel.pass("test1", "test2", "test3").close();
        assertThat(Streams.toSelectable(channel.asOutput(), 33)
                          .afterMax(seconds(1))
                          .all()).containsExactly(new ParcelableSelectable<String>("test1", 33),
                                                  new ParcelableSelectable<String>("test2", 33),
                                                  new ParcelableSelectable<String>("test3", 33));
    }

    public void testOutputToSelectableAbort() {

        final IOChannel<String> channel = JRoutine.io().buildChannel();
        channel.pass("test1", "test2", "test3").abort();

        try {
            Streams.toSelectable(channel.asOutput(), 33).afterMax(seconds(1)).all();
            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testRepeat() {

        final IOChannel<Object> ioChannel = JRoutine.io().buildChannel();
        final OutputChannel<Object> channel = Streams.repeat(ioChannel);
        ioChannel.pass("test1", "test2");
        final IOChannel<Object> output1 = JRoutine.io().buildChannel();
        channel.passTo(output1).close();
        assertThat(output1.next()).isEqualTo("test1");
        final IOChannel<Object> output2 = JRoutine.io().buildChannel();
        channel.passTo(output2).close();
        ioChannel.pass("test3").close();
        assertThat(output2.all()).containsExactly("test1", "test2", "test3");
        assertThat(output1.all()).containsExactly("test2", "test3");
    }

    public void testRepeatAbort() {

        final IOChannel<Object> ioChannel = JRoutine.io().buildChannel();
        final OutputChannel<Object> channel = Streams.repeat(ioChannel);
        ioChannel.pass("test1", "test2");
        final IOChannel<Object> output1 = JRoutine.io().buildChannel();
        channel.passTo(output1).close();
        assertThat(output1.next()).isEqualTo("test1");
        final IOChannel<Object> output2 = JRoutine.io().buildChannel();
        channel.passTo(output2).close();
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

    public void testRoutineId() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final LoaderContext context = loaderFrom(getActivity());
        Streams.streamOf("test1").with(context).routineId(11).async().map(toUpperCase());

        try {
            JRoutine.with(context).onId(11).buildChannel().afterMax(seconds(10)).next();
            fail();

        } catch (final MissingInvocationException ignored) {

        }

        assertThat(Streams.streamOf("test2")
                          .with(context)
                          .withLoaders()
                          .withRoutineId(11)
                          .set()
                          .async()
                          .map(toUpperCase())
                          .afterMax(seconds(10))
                          .next()).isEqualTo("TEST2");
        final AtomicInteger count = new AtomicInteger();
        Streams.streamOf().with(context).routineId(11).then(delayedIncrement(count));
        assertThat(Streams.streamOf()
                          .with(context)
                          .routineId(11)
                          .then(increment(count))
                          .afterMax(seconds(10))
                          .next()).isEqualTo(1);
    }

    public void testSkip() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final LoaderContext context = loaderFrom(getActivity());
        assertThat(Streams.streamOf()
                          .with(context)
                          .sync()
                          .range(1, 10)
                          .async()
                          .map(Streams.skip(5))
                          .afterMax(seconds(3))
                          .all()).containsExactly(6, 7, 8, 9, 10);
        assertThat(Streams.streamOf()
                          .with(context)
                          .sync()
                          .range(1, 10)
                          .async()
                          .map(Streams.skip(15))
                          .afterMax(seconds(3))
                          .all()).isEmpty();
        assertThat(Streams.streamOf()
                          .with(context)
                          .sync()
                          .range(1, 10)
                          .async()
                          .map(Streams.skip(0))
                          .afterMax(seconds(3))
                          .all()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(Streams.streamOf()
                          .sync()
                          .range(1, 10)
                          .with(context)
                          .async()
                          .map(Streams.skip(5))
                          .afterMax(seconds(3))
                          .all()).containsExactly(6, 7, 8, 9, 10);
        assertThat(Streams.streamOf()
                          .sync()
                          .range(1, 10)
                          .with(context)
                          .async()
                          .map(Streams.skip(15))
                          .afterMax(seconds(3))
                          .all()).isEmpty();
        assertThat(Streams.streamOf()
                          .sync()
                          .range(1, 10)
                          .with(context)
                          .async()
                          .map(Streams.skip(0))
                          .afterMax(seconds(3))
                          .all()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    public void testSkipEquals() {

        final InvocationFactory<Object, Object> factory = Streams.skip(2);
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(null);
        assertThat(factory).isNotEqualTo("test");
        assertThat(factory).isNotEqualTo(Streams.skip(3));
        assertThat(factory).isEqualTo(Streams.skip(2));
        assertThat(factory.hashCode()).isEqualTo(Streams.skip(2).hashCode());
    }

    public void testSkipError() {

        try {

            Streams.skip(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testStaleTime() throws InterruptedException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final LoaderContext context = loaderFrom(getActivity());
        final AtomicInteger count = new AtomicInteger();
        final Function<String, String> function = stringIncrement(count);
        Streams.streamOf("test").with(context).async().cache(CacheStrategyType.CACHE).map(function);
        assertThat(Streams.streamOf("test")
                          .with(context)
                          .staleAfter(2000, TimeUnit.MILLISECONDS)
                          .async()
                          .map(function)
                          .afterMax(seconds(10))
                          .next()).isEqualTo("test1");
        seconds(5).sleepAtLeast();
        assertThat(Streams.streamOf("test")
                          .with(context)
                          .staleAfter(ZERO)
                          .async()
                          .map(function)
                          .afterMax(seconds(10))
                          .next()).isEqualTo("test2");
        seconds(5).sleepAtLeast();
        Streams.streamOf("test")
               .with(context)
               .cache(CacheStrategyType.CACHE_IF_SUCCESS)
               .async()
               .map(function);
        seconds(5).sleepAtLeast();
        assertThat(Streams.streamOf("test")
                          .with(context)
                          .staleAfter(ZERO)
                          .async()
                          .map(function)
                          .afterMax(seconds(10))
                          .next()).isEqualTo("test4");
    }

    private static class Amb<DATA> extends TemplateInvocation<ParcelableSelectable<DATA>, DATA> {

        private static final int NO_INDEX = Integer.MIN_VALUE;

        private int mFirstIndex;

        @Override
        public void onInitialize() {

            mFirstIndex = NO_INDEX;
        }

        @Override
        public void onInput(final ParcelableSelectable<DATA> input,
                @NotNull final ResultChannel<DATA> result) {

            if (mFirstIndex == NO_INDEX) {

                mFirstIndex = input.index;
                result.pass(input.data);

            } else if (mFirstIndex == input.index) {

                result.pass(input.data);
            }
        }
    }

    private static class CharAt extends FilterInvocation<List<?>, Character> {

        public void onInput(final List<?> objects, @NotNull final ResultChannel<Character> result) {

            final String text = (String) objects.get(0);
            final int index = ((Integer) objects.get(1));
            result.pass(text.charAt(index));
        }
    }

    private static class Sort
            extends FilterInvocation<ParcelableSelectable<Object>, ParcelableSelectable<Object>> {

        private static final int INTEGER = 1;

        private static final int STRING = 0;

        public void onInput(final ParcelableSelectable<Object> selectable,
                @NotNull final ResultChannel<ParcelableSelectable<Object>> result) {

            switch (selectable.index) {

                case INTEGER:
                    Channels.<Object, Integer>selectParcelable(result, INTEGER)
                            .pass(selectable.<Integer>data())
                            .close();
                    break;

                case STRING:
                    Channels.<Object, String>selectParcelable(result, STRING)
                            .pass(selectable.<String>data())
                            .close();
                    break;
            }
        }
    }
}
