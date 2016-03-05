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
import android.support.v4.util.SparseArrayCompat;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.builder.LoaderConfiguration.CacheStrategyType;
import com.github.dm.jrt.android.core.channel.ParcelableSelectable;
import com.github.dm.jrt.android.invocation.FunctionContextInvocationFactory;
import com.github.dm.jrt.android.invocation.MissingInvocationException;
import com.github.dm.jrt.android.invocation.PassingFunctionContextInvocation;
import com.github.dm.jrt.android.v4.core.JRoutineLoaderCompat;
import com.github.dm.jrt.android.v4.core.LoaderContextCompat;
import com.github.dm.jrt.android.v4.core.channel.SparseChannelsCompat;
import com.github.dm.jrt.core.DelegatingInvocation.DelegationType;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.IOChannelBuilder;
import com.github.dm.jrt.core.builder.InvocationConfiguration.OrderType;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.channel.InvocationChannel;
import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.invocation.FilterInvocation;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.InvocationInterruptedException;
import com.github.dm.jrt.core.invocation.TemplateInvocation;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Functions;
import com.github.dm.jrt.function.Supplier;
import com.github.dm.jrt.log.Log.Level;
import com.github.dm.jrt.routine.Routine;
import com.github.dm.jrt.stream.StreamChannel;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.dm.jrt.android.core.DelegatingContextInvocation.factoryFrom;
import static com.github.dm.jrt.core.invocation.InvocationFactories.factoryOf;
import static com.github.dm.jrt.core.util.TimeDuration.ZERO;
import static com.github.dm.jrt.core.util.TimeDuration.millis;
import static com.github.dm.jrt.core.util.TimeDuration.seconds;
import static com.github.dm.jrt.stream.Streams.range;
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

        final LoaderContextCompat context = loaderFrom(getActivity());
        StreamChannel<String> channel1 =
                LoaderStreamsCompat.streamOf("test1", "test2", "test3").with(context).runOnShared();
        StreamChannel<String> channel2 =
                LoaderStreamsCompat.streamOf("test4", "test5", "test6").with(context).runOnShared();
        assertThat(LoaderStreamsCompat.blend(channel2, channel1).build().afterMax(seconds(1)).all())
                .containsOnly("test1", "test2", "test3", "test4", "test5", "test6");
        channel1 =
                LoaderStreamsCompat.streamOf("test1", "test2", "test3").with(context).runOnShared();
        channel2 =
                LoaderStreamsCompat.streamOf("test4", "test5", "test6").with(context).runOnShared();
        assertThat(LoaderStreamsCompat.blend(Arrays.<StreamChannel<?>>asList(channel1, channel2))
                                      .build()
                                      .afterMax(seconds(1))
                                      .all()).containsOnly("test1", "test2", "test3", "test4",
                                                           "test5", "test6");
    }

    public void testBlendAbort() {

        final IOChannelBuilder builder = JRoutineCore.io();
        final FunctionContextInvocationFactory<Object, Object> factory =
                PassingFunctionContextInvocation.factoryOf();
        final Routine<Object, Object> routine =
                JRoutineLoaderCompat.with(loaderFrom(getActivity())).on(factory).buildRoutine();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().abort();

        try {

            routine.asyncCall(LoaderStreamsCompat.blend(channel1, channel2).build())
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
                    LoaderStreamsCompat.blend(Arrays.<OutputChannel<?>>asList(channel1, channel2))
                                       .build()).afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testBlendError() {

        try {

            LoaderStreamsCompat.blend();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            LoaderStreamsCompat.blend((OutputChannel<?>[]) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            LoaderStreamsCompat.blend(new OutputChannel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            LoaderStreamsCompat.blend(Collections.<OutputChannel<?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            LoaderStreamsCompat.blend((List<OutputChannel<?>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            LoaderStreamsCompat.blend(Collections.<OutputChannel<?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testBuilder() {

        final LoaderContextCompat context = loaderFrom(getActivity());
        assertThat(LoaderStreamsCompat.streamOf("test")
                                      .with(context)
                                      .runOnShared()
                                      .afterMax(seconds(1))
                                      .all()).containsExactly("test");
        assertThat(LoaderStreamsCompat.streamOf("test1", "test2", "test3")
                                      .with(context)
                                      .runOnShared()
                                      .afterMax(seconds(1))
                                      .all()).containsExactly("test1", "test2", "test3");
        assertThat(LoaderStreamsCompat.streamOf(Arrays.asList("test1", "test2", "test3"))
                                      .with(context)
                                      .runOnShared()
                                      .afterMax(seconds(1))
                                      .all()).containsExactly("test1", "test2", "test3");
        assertThat(LoaderStreamsCompat.streamOf(JRoutineCore.io().of("test1", "test2", "test3"))
                                      .with(context)
                                      .runOnShared()
                                      .afterMax(seconds(1))
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

    public void testConcat() {

        final LoaderContextCompat context = loaderFrom(getActivity());
        StreamChannel<String> channel1 =
                LoaderStreamsCompat.streamOf("test1", "test2", "test3").with(context).runOnShared();
        StreamChannel<String> channel2 =
                LoaderStreamsCompat.streamOf("test4", "test5", "test6").with(context).runOnShared();
        assertThat(LoaderStreamsCompat.concat(channel2, channel1)
                                      .build()
                                      .afterMax(seconds(1))
                                      .all()).containsExactly("test4", "test5", "test6", "test1",
                                                              "test2", "test3");
        channel1 =
                LoaderStreamsCompat.streamOf("test1", "test2", "test3").with(context).runOnShared();
        channel2 =
                LoaderStreamsCompat.streamOf("test4", "test5", "test6").with(context).runOnShared();
        assertThat(LoaderStreamsCompat.concat(Arrays.<StreamChannel<?>>asList(channel1, channel2))
                                      .build()
                                      .afterMax(seconds(1))
                                      .all()).containsExactly("test1", "test2", "test3", "test4",
                                                              "test5", "test6");
    }

    public void testConcatAbort() {

        final IOChannelBuilder builder = JRoutineCore.io();
        final FunctionContextInvocationFactory<Object, Object> factory =
                PassingFunctionContextInvocation.factoryOf();
        final Routine<Object, Object> routine =
                JRoutineLoaderCompat.with(loaderFrom(getActivity())).on(factory).buildRoutine();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().abort();

        try {

            routine.asyncCall(LoaderStreamsCompat.concat(channel1, channel2).build())
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
                    LoaderStreamsCompat.concat(Arrays.<OutputChannel<?>>asList(channel1, channel2))
                                       .build()).afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testConcatError() {

        try {

            LoaderStreamsCompat.concat();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            LoaderStreamsCompat.concat((OutputChannel<?>[]) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            LoaderStreamsCompat.concat(new OutputChannel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            LoaderStreamsCompat.concat(Collections.<OutputChannel<?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            LoaderStreamsCompat.concat((List<OutputChannel<?>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            LoaderStreamsCompat.concat(Collections.<OutputChannel<?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testFactory() {

        assertThat(LoaderStreamsCompat.onStream(toUpperCaseChannel())
                                      .asyncCall("test1", "test2", "test3")
                                      .afterMax(seconds(3))
                                      .all()).containsExactly("TEST1", "TEST2", "TEST3");

        try {

            final InvocationChannel<String, String> channel =
                    LoaderStreamsCompat.onStream(toUpperCaseChannel()).asyncInvoke();
            channel.abort(new IllegalArgumentException());
            channel.result().afterMax(seconds(3)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        final FunctionContextInvocationFactory<String, String> factory =
                LoaderStreamsCompat.contextFactory(toUpperCaseChannel());
        assertThat(JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                       .on(factory)
                                       .asyncCall("test1", "test2", "test3")
                                       .afterMax(seconds(3))
                                       .all()).containsExactly("TEST1", "TEST2", "TEST3");

        try {

            final InvocationChannel<String, String> channel =
                    JRoutineLoaderCompat.with(loaderFrom(getActivity())).on(factory).asyncInvoke();
            channel.abort(new IllegalArgumentException());
            channel.result().afterMax(seconds(3)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        assertThat(LoaderStreamsCompat.onStreamWith(loaderFrom(getActivity()), toUpperCaseChannel())
                                      .asyncCall("test1", "test2", "test3")
                                      .afterMax(seconds(3))
                                      .all()).containsExactly("TEST1", "TEST2", "TEST3");

        try {

            final InvocationChannel<String, String> channel =
                    LoaderStreamsCompat.onStreamWith(loaderFrom(getActivity()),
                                                     toUpperCaseChannel()).asyncInvoke();
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
                LoaderStreamsCompat.contextFactory(function);
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(null);
        assertThat(factory).isNotEqualTo("test");
        assertThat(factory).isNotEqualTo(
                LoaderStreamsCompat.contextFactory(Functions.<StreamChannel<?>>identity()));
        assertThat(factory).isEqualTo(LoaderStreamsCompat.contextFactory(function));
        assertThat(factory.hashCode()).isEqualTo(
                LoaderStreamsCompat.contextFactory(function).hashCode());
    }

    @SuppressWarnings("ConstantConditions")
    public void testFactoryError() {

        try {

            LoaderStreamsCompat.contextFactory(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            LoaderStreamsCompat.onStreamWith(null, Functions.<StreamChannel<?>>identity());

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            LoaderStreamsCompat.onStreamWith(loaderFrom(getActivity()), null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @SuppressWarnings("unchecked")
    public void testGroupBy() {

        final LoaderContextCompat context = loaderFrom(getActivity());
        assertThat(LoaderStreamsCompat.streamOf()
                                      .with(context)
                                      .sync()
                                      .then(range(1, 10))
                                      .async()
                                      .map(LoaderStreamsCompat.<Number>groupBy(3))
                                      .afterMax(seconds(3))
                                      .all()).containsExactly(Arrays.<Number>asList(1, 2, 3),
                                                              Arrays.<Number>asList(4, 5, 6),
                                                              Arrays.<Number>asList(7, 8, 9),
                                                              Collections.<Number>singletonList(
                                                                      10));
        assertThat(LoaderStreamsCompat.streamOf()
                                      .with(context)
                                      .sync()
                                      .then(range(1, 10))
                                      .async()
                                      .map(LoaderStreamsCompat.<Number>groupBy(13))
                                      .afterMax(seconds(3))
                                      .all()).containsExactly(
                Arrays.<Number>asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        assertThat(LoaderStreamsCompat.streamOf()
                                      .sync()
                                      .then(range(1, 10))
                                      .with(context)
                                      .async()
                                      .map(LoaderStreamsCompat.<Number>groupBy(3))
                                      .afterMax(seconds(3))
                                      .all()).containsExactly(Arrays.<Number>asList(1, 2, 3),
                                                              Arrays.<Number>asList(4, 5, 6),
                                                              Arrays.<Number>asList(7, 8, 9),
                                                              Collections.<Number>singletonList(
                                                                      10));
        assertThat(LoaderStreamsCompat.streamOf()
                                      .sync()
                                      .then(range(1, 10))
                                      .with(context)
                                      .async()
                                      .map(LoaderStreamsCompat.<Number>groupBy(13))
                                      .afterMax(seconds(3))
                                      .all()).containsExactly(
                Arrays.<Number>asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
    }

    public void testGroupByEquals() {

        final InvocationFactory<Object, List<Object>> factory = LoaderStreamsCompat.groupBy(2);
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(null);
        assertThat(factory).isNotEqualTo("test");
        assertThat(factory).isNotEqualTo(LoaderStreamsCompat.groupBy(3));
        assertThat(factory).isEqualTo(LoaderStreamsCompat.groupBy(2));
        assertThat(factory.hashCode()).isEqualTo(LoaderStreamsCompat.groupBy(2).hashCode());
    }

    public void testGroupByError() {

        try {

            LoaderStreamsCompat.groupBy(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            LoaderStreamsCompat.groupBy(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @SuppressWarnings("unchecked")
    public void testGroupByPlaceholder() {

        final LoaderContextCompat context = loaderFrom(getActivity());
        assertThat(LoaderStreamsCompat.streamOf()
                                      .with(context)
                                      .sync()
                                      .then(range(1, 10))
                                      .async()
                                      .map(LoaderStreamsCompat.<Number>groupBy(3, 0))
                                      .afterMax(seconds(3))
                                      .all()).containsExactly(Arrays.<Number>asList(1, 2, 3),
                                                              Arrays.<Number>asList(4, 5, 6),
                                                              Arrays.<Number>asList(7, 8, 9),
                                                              Arrays.<Number>asList(10, 0, 0));
        assertThat(LoaderStreamsCompat.streamOf()
                                      .with(context)
                                      .sync()
                                      .then(range(1, 10))
                                      .async()
                                      .map(LoaderStreamsCompat.<Number>groupBy(13, -1))
                                      .afterMax(seconds(3))
                                      .all()).containsExactly(
                Arrays.<Number>asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, -1, -1, -1));
        assertThat(LoaderStreamsCompat.streamOf()
                                      .sync()
                                      .then(range(1, 10))
                                      .with(context)
                                      .async()
                                      .map(LoaderStreamsCompat.<Number>groupBy(3, -31))
                                      .afterMax(seconds(3))
                                      .all()).containsExactly(Arrays.<Number>asList(1, 2, 3),
                                                              Arrays.<Number>asList(4, 5, 6),
                                                              Arrays.<Number>asList(7, 8, 9),
                                                              Arrays.<Number>asList(10, -31, -31));
        assertThat(LoaderStreamsCompat.streamOf()
                                      .sync()
                                      .then(range(1, 10))
                                      .with(context)
                                      .async()
                                      .map(LoaderStreamsCompat.<Number>groupBy(13, 71))
                                      .afterMax(seconds(3))
                                      .all()).containsExactly(
                Arrays.<Number>asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 71, 71, 71));
    }

    public void testGroupByPlaceholderEquals() {

        final Object placeholder = -11;
        final InvocationFactory<Object, List<Object>> factory =
                LoaderStreamsCompat.groupBy(2, placeholder);
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(null);
        assertThat(factory).isNotEqualTo("test");
        assertThat(factory).isNotEqualTo(LoaderStreamsCompat.groupBy(3, -11));
        assertThat(factory).isEqualTo(LoaderStreamsCompat.groupBy(2, -11));
        assertThat(factory.hashCode()).isEqualTo(LoaderStreamsCompat.groupBy(2, -11).hashCode());
    }

    public void testGroupByPlaceholderError() {

        try {

            LoaderStreamsCompat.groupBy(-1, 77);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            LoaderStreamsCompat.groupBy(0, null);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testJoin() {

        final IOChannelBuilder builder = JRoutineCore.io();
        final FunctionContextInvocationFactory<List<?>, Character> factory =
                factoryFrom(JRoutineCore.on(new CharAt()).buildRoutine(), 1, DelegationType.SYNC);
        final Routine<List<?>, Character> routine =
                JRoutineLoaderCompat.with(loaderFrom(getActivity())).on(factory).buildRoutine();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.asyncCall(LoaderStreamsCompat.join(channel1, channel2).build())
                          .afterMax(seconds(10))
                          .all()).containsExactly('s', '2');
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.asyncCall(
                LoaderStreamsCompat.join(Arrays.<OutputChannel<?>>asList(channel1, channel2))
                                   .build()).afterMax(seconds(10)).all()).containsExactly('s', '2');
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall()
                .after(millis(100))
                .pass("testtest")
                .pass("test2")
                .pass("test3")
                .close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.asyncCall(LoaderStreamsCompat.join(channel1, channel2).build())
                          .afterMax(seconds(10))
                          .all()).containsExactly('s', '2');
    }

    public void testJoinAbort() {

        final IOChannelBuilder builder = JRoutineCore.io();
        final FunctionContextInvocationFactory<List<?>, Character> factory =
                factoryFrom(JRoutineCore.on(new CharAt()).buildRoutine(), 1, DelegationType.SYNC);
        final Routine<List<?>, Character> routine =
                JRoutineLoaderCompat.with(loaderFrom(getActivity())).on(factory).buildRoutine();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().abort();

        try {

            routine.asyncCall(LoaderStreamsCompat.join(channel1, channel2).build())
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
                    LoaderStreamsCompat.join(Arrays.<OutputChannel<?>>asList(channel1, channel2))
                                       .build()).afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testJoinError() {

        try {

            LoaderStreamsCompat.join();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            LoaderStreamsCompat.join(Collections.<OutputChannel<?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            LoaderStreamsCompat.join(new OutputChannel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            LoaderStreamsCompat.join(Collections.<OutputChannel<?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testJoinPlaceholder() {

        final IOChannelBuilder builder = JRoutineCore.io();
        final FunctionContextInvocationFactory<List<?>, Character> factory =
                factoryFrom(JRoutineCore.on(new CharAt()).buildRoutine(), 1, DelegationType.SYNC);
        final Routine<List<?>, Character> routine =
                JRoutineLoaderCompat.with(loaderFrom(getActivity())).on(factory).buildRoutine();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.asyncCall(
                LoaderStreamsCompat.join(new Object(), channel1, channel2).build())
                          .afterMax(seconds(10))
                          .all()).containsExactly('s', '2');
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.asyncCall(
                LoaderStreamsCompat.join(null, Arrays.<OutputChannel<?>>asList(channel1, channel2))
                                   .build()).afterMax(seconds(10)).all()).containsExactly('s', '2');
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

            routine.asyncCall(LoaderStreamsCompat.join(new Object(), channel1, channel2).build())
                   .afterMax(seconds(10))
                   .all();

            fail();

        } catch (final InvocationException ignored) {

        }
    }

    public void testJoinPlaceholderAbort() {

        final IOChannelBuilder builder = JRoutineCore.io();
        final FunctionContextInvocationFactory<List<?>, Character> factory =
                factoryFrom(JRoutineCore.on(new CharAt()).buildRoutine(), 1, DelegationType.SYNC);
        final Routine<List<?>, Character> routine =
                JRoutineLoaderCompat.with(loaderFrom(getActivity())).on(factory).buildRoutine();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().abort();

        try {

            routine.asyncCall(LoaderStreamsCompat.join((Object) null, channel1, channel2).build())
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

            routine.asyncCall(LoaderStreamsCompat.join(new Object(),
                                                       Arrays.<OutputChannel<?>>asList(channel1,
                                                                                       channel2))
                                                 .build()).afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testJoinPlaceholderError() {

        try {

            LoaderStreamsCompat.join(new Object());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            LoaderStreamsCompat.join(null, Collections.<OutputChannel<?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            LoaderStreamsCompat.join(new Object(), new OutputChannel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            LoaderStreamsCompat.join(new Object(),
                                     Collections.<OutputChannel<?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testLazyBuilder() {

        assertThat(LoaderStreamsCompat.lazyStreamOf().afterMax(seconds(1)).all()).isEmpty();
        assertThat(LoaderStreamsCompat.lazyStreamOf("test")
                                      .afterMax(seconds(1))
                                      .all()).containsExactly("test");
        assertThat(LoaderStreamsCompat.lazyStreamOf("test1", "test2", "test3")
                                      .afterMax(seconds(1))
                                      .all()).containsExactly("test1", "test2", "test3");
        assertThat(LoaderStreamsCompat.lazyStreamOf(Arrays.asList("test1", "test2", "test3"))
                                      .afterMax(seconds(1))
                                      .all()).containsExactly("test1", "test2", "test3");
        assertThat(LoaderStreamsCompat.lazyStreamOf(JRoutineCore.io().of("test1", "test2", "test3"))
                                      .afterMax(seconds(1))
                                      .all()).containsExactly("test1", "test2", "test3");
        final LoaderContextCompat context = loaderFrom(getActivity());
        assertThat(LoaderStreamsCompat.lazyStreamOf()
                                      .with(context)
                                      .runOnShared()
                                      .afterMax(seconds(1))
                                      .all()).isEmpty();
        assertThat(LoaderStreamsCompat.lazyStreamOf("test")
                                      .with(context)
                                      .runOnShared()
                                      .afterMax(seconds(1))
                                      .all()).containsExactly("test");
        assertThat(LoaderStreamsCompat.lazyStreamOf("test1", "test2", "test3")
                                      .with(context)
                                      .runOnShared()
                                      .afterMax(seconds(1))
                                      .all()).containsExactly("test1", "test2", "test3");
        assertThat(LoaderStreamsCompat.lazyStreamOf(Arrays.asList("test1", "test2", "test3"))
                                      .with(context)
                                      .runOnShared()
                                      .afterMax(seconds(1))
                                      .all()).containsExactly("test1", "test2", "test3");
        assertThat(LoaderStreamsCompat.lazyStreamOf(JRoutineCore.io().of("test1", "test2", "test3"))
                                      .with(context)
                                      .runOnShared()
                                      .afterMax(seconds(1))
                                      .all()).containsExactly("test1", "test2", "test3");
    }

    @SuppressWarnings("ConstantConditions")
    public void testLazyBuilderNullPointerError() {

        try {

            LoaderStreamsCompat.lazyStreamOf((OutputChannel<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testLimit() {

        final LoaderContextCompat context = loaderFrom(getActivity());
        assertThat(LoaderStreamsCompat.streamOf()
                                      .with(context)
                                      .sync()
                                      .then(range(1, 10))
                                      .async()
                                      .map(LoaderStreamsCompat.limit(5))
                                      .afterMax(seconds(3))
                                      .all()).containsExactly(1, 2, 3, 4, 5);
        assertThat(LoaderStreamsCompat.streamOf()
                                      .with(context)
                                      .sync()
                                      .then(range(1, 10))
                                      .async()
                                      .map(LoaderStreamsCompat.limit(0))
                                      .afterMax(seconds(3))
                                      .all()).isEmpty();
        assertThat(LoaderStreamsCompat.streamOf()
                                      .with(context)
                                      .sync()
                                      .then(range(1, 10))
                                      .async()
                                      .map(LoaderStreamsCompat.limit(15))
                                      .afterMax(seconds(3))
                                      .all()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(LoaderStreamsCompat.streamOf()
                                      .sync()
                                      .then(range(1, 10))
                                      .with(context)
                                      .async()
                                      .map(LoaderStreamsCompat.limit(5))
                                      .afterMax(seconds(3))
                                      .all()).containsExactly(1, 2, 3, 4, 5);
        assertThat(LoaderStreamsCompat.streamOf()
                                      .sync()
                                      .then(range(1, 10))
                                      .with(context)
                                      .async()
                                      .map(LoaderStreamsCompat.limit(0))
                                      .afterMax(seconds(3))
                                      .all()).isEmpty();
        assertThat(LoaderStreamsCompat.streamOf()
                                      .sync()
                                      .then(range(1, 10))
                                      .with(context)
                                      .async()
                                      .map(LoaderStreamsCompat.limit(15))
                                      .afterMax(seconds(3))
                                      .all()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    public void testLimitEquals() {

        final InvocationFactory<Object, Object> factory = LoaderStreamsCompat.limit(2);
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(null);
        assertThat(factory).isNotEqualTo("test");
        assertThat(factory).isNotEqualTo(LoaderStreamsCompat.limit(3));
        assertThat(factory).isEqualTo(LoaderStreamsCompat.limit(2));
        assertThat(factory.hashCode()).isEqualTo(LoaderStreamsCompat.limit(2).hashCode());
    }

    public void testLimitError() {

        try {

            LoaderStreamsCompat.limit(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testLoaderId() {

        final LoaderContextCompat context = loaderFrom(getActivity());
        LoaderStreamsCompat.streamOf("test1").with(context).loaderId(11).async().map(toUpperCase());
        assertThat(JRoutineLoaderCompat.with(context)
                                       .onId(11)
                                       .buildChannel()
                                       .afterMax(seconds(10))
                                       .next()).isEqualTo("TEST1");
        LoaderStreamsCompat.streamOf("test2")
                           .with(context)
                           .withLoaders()
                           .withLoaderId(21)
                           .getConfigured()
                           .async()
                           .map(toUpperCase());
        assertThat(JRoutineLoaderCompat.with(context)
                                       .onId(21)
                                       .buildChannel()
                                       .afterMax(seconds(10))
                                       .next()).isEqualTo("TEST2");
        LoaderStreamsCompat.streamOf("test3")
                           .with(context)
                           .withStreamLoaders()
                           .withLoaderId(31)
                           .getConfigured()
                           .async()
                           .map(toUpperCase());
        assertThat(JRoutineLoaderCompat.with(context)
                                       .onId(31)
                                       .buildChannel()
                                       .afterMax(seconds(10))
                                       .next()).isEqualTo("TEST3");
    }

    public void testMap() {

        final IOChannelBuilder builder = JRoutineCore.io()
                                                     .withChannels()
                                                     .withChannelOrder(OrderType.BY_CALL)
                                                     .getConfigured();
        final IOChannel<String> channel1 = builder.buildChannel();
        final IOChannel<Integer> channel2 = builder.buildChannel();

        final OutputChannel<? extends ParcelableSelectable<Object>> channel =
                LoaderStreamsCompat.merge(Arrays.<OutputChannel<?>>asList(channel1, channel2))
                                   .build();
        final OutputChannel<ParcelableSelectable<Object>> output =
                JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                    .on(factoryFrom(JRoutineCore.on(new Sort()).buildRoutine(), 1,
                                                    DelegationType.SYNC))
                                    .withInvocations()
                                    .withInputOrder(OrderType.BY_CALL)
                                    .getConfigured()
                                    .asyncCall(channel);
        final SparseArrayCompat<OutputChannel<Object>> channelMap =
                SparseChannelsCompat.selectParcelable(output, Sort.INTEGER, Sort.STRING).build();

        for (int i = 0; i < 4; i++) {

            final String input = Integer.toString(i);
            channel1.after(millis(20)).pass(input);
            channel2.after(millis(20)).pass(i);
        }

        channel1.close();
        channel2.close();

        final LoaderContextCompat context = loaderFrom(getActivity());
        assertThat(LoaderStreamsCompat.streamOf(channelMap.get(Sort.STRING))
                                      .with(context)
                                      .runOnShared()
                                      .afterMax(seconds(1))
                                      .all()).containsExactly("0", "1", "2", "3");
        assertThat(LoaderStreamsCompat.streamOf(channelMap.get(Sort.INTEGER))
                                      .with(context)
                                      .runOnShared()
                                      .afterMax(seconds(1))
                                      .all()).containsExactly(0, 1, 2, 3);
    }

    public void testMerge() {

        final IOChannelBuilder builder = JRoutineCore.io()
                                                     .withChannels()
                                                     .withChannelOrder(OrderType.BY_CALL)
                                                     .getConfigured();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        OutputChannel<? extends ParcelableSelectable<?>> outputChannel;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = LoaderStreamsCompat.merge(-7, channel1, channel2).build();
        channel1.pass("test1").close();
        channel2.pass(13).close();
        assertThat(outputChannel.afterMax(seconds(1)).all()).containsOnly(
                new ParcelableSelectable<String>("test1", -7),
                new ParcelableSelectable<Integer>(13, -6));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel =
                LoaderStreamsCompat.merge(11, Arrays.<OutputChannel<?>>asList(channel1, channel2))
                                   .build();
        channel2.pass(13).close();
        channel1.pass("test1").close();
        assertThat(outputChannel.afterMax(seconds(1)).all()).containsOnly(
                new ParcelableSelectable<String>("test1", 11),
                new ParcelableSelectable<Integer>(13, 12));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = LoaderStreamsCompat.merge(channel1, channel2).build();
        channel1.pass("test2").close();
        channel2.pass(-17).close();
        assertThat(outputChannel.afterMax(seconds(1)).all()).containsOnly(
                new ParcelableSelectable<String>("test2", 0),
                new ParcelableSelectable<Integer>(-17, 1));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel =
                LoaderStreamsCompat.merge(Arrays.<OutputChannel<?>>asList(channel1, channel2))
                                   .build();
        channel1.pass("test2").close();
        channel2.pass(-17).close();
        assertThat(outputChannel.afterMax(seconds(1)).all()).containsOnly(
                new ParcelableSelectable<String>("test2", 0),
                new ParcelableSelectable<Integer>(-17, 1));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        final SparseArrayCompat<OutputChannel<?>> channelMap =
                new SparseArrayCompat<OutputChannel<?>>(2);
        channelMap.put(7, channel1);
        channelMap.put(-3, channel2);
        outputChannel = LoaderStreamsCompat.merge(channelMap).build();
        channel1.pass("test3").close();
        channel2.pass(111).close();
        assertThat(outputChannel.afterMax(seconds(1)).all()).containsOnly(
                new ParcelableSelectable<String>("test3", 7),
                new ParcelableSelectable<Integer>(111, -3));
    }

    @SuppressWarnings("unchecked")
    public void testMerge4() {

        final IOChannelBuilder builder = JRoutineCore.io()
                                                     .withChannels()
                                                     .withChannelOrder(OrderType.BY_CALL)
                                                     .getConfigured();
        final IOChannel<String> channel1 = builder.buildChannel();
        final IOChannel<String> channel2 = builder.buildChannel();
        final IOChannel<String> channel3 = builder.buildChannel();
        final IOChannel<String> channel4 = builder.buildChannel();

        final Routine<ParcelableSelectable<String>, String> routine =
                JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                    .on(factoryFrom(JRoutineCore.on(factoryOf(
                                                            new ClassToken<Amb<String>>() {}))
                                                                .buildRoutine(), 1,
                                                    DelegationType.SYNC))
                                    .buildRoutine();
        final OutputChannel<String> outputChannel = routine.asyncCall(
                LoaderStreamsCompat.merge(Arrays.asList(channel1, channel2, channel3, channel4))
                                   .build());

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

        final IOChannelBuilder builder = JRoutineCore.io()
                                                     .withChannels()
                                                     .withChannelOrder(OrderType.BY_CALL)
                                                     .getConfigured();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        OutputChannel<? extends ParcelableSelectable<?>> outputChannel;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = LoaderStreamsCompat.merge(-7, channel1, channel2).build();
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
                LoaderStreamsCompat.merge(11, Arrays.<OutputChannel<?>>asList(channel1, channel2))
                                   .build();
        channel2.abort();
        channel1.pass("test1").close();

        try {

            outputChannel.afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = LoaderStreamsCompat.merge(channel1, channel2).build();
        channel1.abort();
        channel2.pass(-17).close();

        try {

            outputChannel.afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel =
                LoaderStreamsCompat.merge(Arrays.<OutputChannel<?>>asList(channel1, channel2))
                                   .build();
        channel1.pass("test2").close();
        channel2.abort();

        try {

            outputChannel.afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        final SparseArrayCompat<OutputChannel<?>> channelMap =
                new SparseArrayCompat<OutputChannel<?>>(2);
        channelMap.append(7, channel1);
        channelMap.append(-3, channel2);
        outputChannel = LoaderStreamsCompat.merge(channelMap).build();
        channel1.abort();
        channel2.pass(111).close();

        try {

            outputChannel.afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testMergeError() {

        try {

            LoaderStreamsCompat.merge(0, Collections.<OutputChannel<Object>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            LoaderStreamsCompat.merge(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            LoaderStreamsCompat.merge(Collections.<OutputChannel<Object>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            LoaderStreamsCompat.merge(Collections.<Integer, OutputChannel<Object>>emptyMap());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            LoaderStreamsCompat.merge();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            LoaderStreamsCompat.merge(new OutputChannel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            LoaderStreamsCompat.merge(Collections.<OutputChannel<?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            LoaderStreamsCompat.merge(0, new OutputChannel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            LoaderStreamsCompat.merge(0, Collections.<OutputChannel<?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            LoaderStreamsCompat.merge(Collections.<Integer, OutputChannel<?>>singletonMap(1, null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @SuppressWarnings("unchecked")
    public void testOutputToSelectable() {

        final IOChannel<String> channel = JRoutineCore.io().buildChannel();
        channel.pass("test1", "test2", "test3").close();
        assertThat(LoaderStreamsCompat.toSelectable(channel.asOutput(), 33)
                                      .build()
                                      .afterMax(seconds(1))
                                      .all()).containsExactly(
                new ParcelableSelectable<String>("test1", 33),
                new ParcelableSelectable<String>("test2", 33),
                new ParcelableSelectable<String>("test3", 33));
    }

    public void testOutputToSelectableAbort() {

        final IOChannel<String> channel = JRoutineCore.io().buildChannel();
        channel.pass("test1", "test2", "test3").abort();

        try {
            LoaderStreamsCompat.toSelectable(channel.asOutput(), 33)
                               .build()
                               .afterMax(seconds(1))
                               .all();
            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testRepeat() {

        final IOChannel<Object> ioChannel = JRoutineCore.io().buildChannel();
        final OutputChannel<Object> channel = LoaderStreamsCompat.repeat(ioChannel).build();
        ioChannel.pass("test1", "test2");
        final IOChannel<Object> output1 = JRoutineCore.io().buildChannel();
        channel.bindTo(output1).close();
        assertThat(output1.next()).isEqualTo("test1");
        final IOChannel<Object> output2 = JRoutineCore.io().buildChannel();
        channel.bindTo(output2).close();
        ioChannel.pass("test3").close();
        assertThat(output2.all()).containsExactly("test1", "test2", "test3");
        assertThat(output1.all()).containsExactly("test2", "test3");
    }

    public void testRepeatAbort() {

        final IOChannel<Object> ioChannel = JRoutineCore.io().buildChannel();
        final OutputChannel<Object> channel = LoaderStreamsCompat.repeat(ioChannel).build();
        ioChannel.pass("test1", "test2");
        final IOChannel<Object> output1 = JRoutineCore.io().buildChannel();
        channel.bindTo(output1).close();
        assertThat(output1.next()).isEqualTo("test1");
        final IOChannel<Object> output2 = JRoutineCore.io().buildChannel();
        channel.bindTo(output2).close();
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

        final LoaderContextCompat context = loaderFrom(getActivity());
        LoaderStreamsCompat.streamOf("test1")
                           .with(context)
                           .routineId(11)
                           .async()
                           .map(toUpperCase());

        try {
            JRoutineLoaderCompat.with(context).onId(11).buildChannel().afterMax(seconds(10)).next();
            fail();

        } catch (final MissingInvocationException ignored) {

        }

        assertThat(LoaderStreamsCompat.streamOf("test2")
                                      .with(context)
                                      .withLoaders()
                                      .withRoutineId(11)
                                      .getConfigured()
                                      .async()
                                      .map(toUpperCase())
                                      .afterMax(seconds(10))
                                      .next()).isEqualTo("TEST2");
        final AtomicInteger count = new AtomicInteger();
        LoaderStreamsCompat.streamOf().with(context).routineId(11).then(delayedIncrement(count));
        assertThat(LoaderStreamsCompat.streamOf()
                                      .with(context)
                                      .routineId(11)
                                      .then(increment(count))
                                      .afterMax(seconds(10))
                                      .next()).isEqualTo(1);
    }

    @SuppressWarnings("unchecked")
    public void testSelectMap() {

        final Routine<ParcelableSelectable<Object>, ParcelableSelectable<Object>> routine =
                JRoutineCore.on(new Sort()).buildRoutine();
        final IOChannel<ParcelableSelectable<Object>> inputChannel =
                JRoutineCore.io().buildChannel();
        final OutputChannel<ParcelableSelectable<Object>> outputChannel =
                routine.asyncCall(inputChannel);
        final StreamChannel<Object> intChannel =
                LoaderStreamsCompat.selectParcelable(outputChannel, Sort.INTEGER, Sort.STRING)
                                   .withChannels()
                                   .withLogLevel(Level.WARNING)
                                   .getConfigured()
                                   .build()
                                   .get(Sort.INTEGER);
        final StreamChannel<Object> strChannel = LoaderStreamsCompat.selectParcelable(outputChannel,
                                                                                      Arrays.asList(
                                                                                              Sort.STRING,
                                                                                              Sort.INTEGER))
                                                                    .withChannels()
                                                                    .withLogLevel(Level.WARNING)
                                                                    .getConfigured()
                                                                    .build()
                                                                    .get(Sort.STRING);
        inputChannel.pass(new ParcelableSelectable<Object>("test21", Sort.STRING),
                          new ParcelableSelectable<Object>(-11, Sort.INTEGER));
        assertThat(intChannel.afterMax(seconds(10)).next()).isEqualTo(-11);
        assertThat(strChannel.afterMax(seconds(10)).next()).isEqualTo("test21");
        inputChannel.pass(new ParcelableSelectable<Object>(-11, Sort.INTEGER),
                          new ParcelableSelectable<Object>("test21", Sort.STRING));
        assertThat(intChannel.afterMax(seconds(10)).next()).isEqualTo(-11);
        assertThat(strChannel.afterMax(seconds(10)).next()).isEqualTo("test21");
        inputChannel.pass(new ParcelableSelectable<Object>("test21", Sort.STRING),
                          new ParcelableSelectable<Object>(-11, Sort.INTEGER));
        assertThat(intChannel.afterMax(seconds(10)).next()).isEqualTo(-11);
        assertThat(strChannel.afterMax(seconds(10)).next()).isEqualTo("test21");
    }

    @SuppressWarnings("unchecked")
    public void testSelectMapAbort() {

        final Routine<ParcelableSelectable<Object>, ParcelableSelectable<Object>> routine =
                JRoutineCore.on(new Sort()).buildRoutine();
        IOChannel<ParcelableSelectable<Object>> inputChannel = JRoutineCore.io().buildChannel();
        OutputChannel<ParcelableSelectable<Object>> outputChannel = routine.asyncCall(inputChannel);
        LoaderStreamsCompat.selectParcelable(Sort.STRING, 2, outputChannel).build();
        inputChannel.after(millis(100))
                    .pass(new ParcelableSelectable<Object>("test21", Sort.STRING),
                          new ParcelableSelectable<Object>(-11, Sort.INTEGER))
                    .abort();

        try {

            LoaderStreamsCompat.selectParcelable(outputChannel, Sort.STRING, Sort.INTEGER)
                               .build()
                               .get(Sort.STRING)
                               .afterMax(seconds(1))
                               .all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            LoaderStreamsCompat.selectParcelable(outputChannel, Sort.INTEGER, Sort.STRING)
                               .build()
                               .get(Sort.INTEGER)
                               .afterMax(seconds(1))
                               .all();

            fail();

        } catch (final AbortException ignored) {

        }

        inputChannel = JRoutineCore.io().buildChannel();
        outputChannel = routine.asyncCall(inputChannel);
        LoaderStreamsCompat.selectParcelable(outputChannel, Sort.INTEGER, Sort.STRING).build();
        inputChannel.after(millis(100))
                    .pass(new ParcelableSelectable<Object>(-11, Sort.INTEGER),
                          new ParcelableSelectable<Object>("test21", Sort.STRING))
                    .abort();

        try {

            LoaderStreamsCompat.selectParcelable(outputChannel, Sort.STRING, Sort.INTEGER)
                               .build()
                               .get(Sort.STRING)
                               .afterMax(seconds(1))
                               .all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            LoaderStreamsCompat.selectParcelable(outputChannel, Sort.STRING, Sort.INTEGER)
                               .build()
                               .get(Sort.INTEGER)
                               .afterMax(seconds(1))
                               .all();

            fail();

        } catch (final AbortException ignored) {

        }

        inputChannel = JRoutineCore.io().buildChannel();
        outputChannel = routine.asyncCall(inputChannel);
        LoaderStreamsCompat.selectParcelable(outputChannel,
                                             Arrays.asList(Sort.STRING, Sort.INTEGER)).build();
        inputChannel.after(millis(100))
                    .pass(new ParcelableSelectable<Object>("test21", Sort.STRING),
                          new ParcelableSelectable<Object>(-11, Sort.INTEGER))
                    .abort();

        try {

            LoaderStreamsCompat.selectParcelable(outputChannel, Sort.INTEGER, Sort.STRING)
                               .build()
                               .get(Sort.STRING)
                               .afterMax(seconds(1))
                               .all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            LoaderStreamsCompat.selectParcelable(outputChannel, Sort.INTEGER, Sort.STRING)
                               .build()
                               .get(Sort.INTEGER)
                               .afterMax(seconds(1))
                               .all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testSkip() {

        final LoaderContextCompat context = loaderFrom(getActivity());
        assertThat(LoaderStreamsCompat.streamOf()
                                      .with(context)
                                      .sync()
                                      .then(range(1, 10))
                                      .async()
                                      .map(LoaderStreamsCompat.skip(5))
                                      .afterMax(seconds(3))
                                      .all()).containsExactly(6, 7, 8, 9, 10);
        assertThat(LoaderStreamsCompat.streamOf()
                                      .with(context)
                                      .sync()
                                      .then(range(1, 10))
                                      .async()
                                      .map(LoaderStreamsCompat.skip(15))
                                      .afterMax(seconds(3))
                                      .all()).isEmpty();
        assertThat(LoaderStreamsCompat.streamOf()
                                      .with(context)
                                      .sync()
                                      .then(range(1, 10))
                                      .async()
                                      .map(LoaderStreamsCompat.skip(0))
                                      .afterMax(seconds(3))
                                      .all()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(LoaderStreamsCompat.streamOf()
                                      .sync()
                                      .then(range(1, 10))
                                      .with(context)
                                      .async()
                                      .map(LoaderStreamsCompat.skip(5))
                                      .afterMax(seconds(3))
                                      .all()).containsExactly(6, 7, 8, 9, 10);
        assertThat(LoaderStreamsCompat.streamOf()
                                      .sync()
                                      .then(range(1, 10))
                                      .with(context)
                                      .async()
                                      .map(LoaderStreamsCompat.skip(15))
                                      .afterMax(seconds(3))
                                      .all()).isEmpty();
        assertThat(LoaderStreamsCompat.streamOf()
                                      .sync()
                                      .then(range(1, 10))
                                      .with(context)
                                      .async()
                                      .map(LoaderStreamsCompat.skip(0))
                                      .afterMax(seconds(3))
                                      .all()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    public void testSkipEquals() {

        final InvocationFactory<Object, Object> factory = LoaderStreamsCompat.skip(2);
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(null);
        assertThat(factory).isNotEqualTo("test");
        assertThat(factory).isNotEqualTo(LoaderStreamsCompat.skip(3));
        assertThat(factory).isEqualTo(LoaderStreamsCompat.skip(2));
        assertThat(factory.hashCode()).isEqualTo(LoaderStreamsCompat.skip(2).hashCode());
    }

    public void testSkipError() {

        try {

            LoaderStreamsCompat.skip(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testStaleTime() throws InterruptedException {

        final LoaderContextCompat context = loaderFrom(getActivity());
        final AtomicInteger count = new AtomicInteger();
        final Function<String, String> function = stringIncrement(count);
        LoaderStreamsCompat.streamOf("test")
                           .with(context)
                           .async()
                           .cache(CacheStrategyType.CACHE)
                           .map(function);
        assertThat(LoaderStreamsCompat.streamOf("test")
                                      .with(context)
                                      .staleAfter(2000, TimeUnit.MILLISECONDS)
                                      .async()
                                      .map(function)
                                      .afterMax(seconds(10))
                                      .next()).isEqualTo("test1");
        seconds(5).sleepAtLeast();
        assertThat(LoaderStreamsCompat.streamOf("test")
                                      .with(context)
                                      .staleAfter(ZERO)
                                      .async()
                                      .map(function)
                                      .afterMax(seconds(10))
                                      .next()).isEqualTo("test2");
        seconds(5).sleepAtLeast();
        LoaderStreamsCompat.streamOf("test")
                           .with(context)
                           .cache(CacheStrategyType.CACHE_IF_SUCCESS)
                           .async()
                           .map(function);
        seconds(5).sleepAtLeast();
        assertThat(LoaderStreamsCompat.streamOf("test")
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
                    SparseChannelsCompat.<Object, Integer>selectParcelable(result, INTEGER)
                                        .build()
                                        .pass(selectable.<Integer>data())
                                        .close();
                    break;

                case STRING:
                    SparseChannelsCompat.<Object, String>selectParcelable(result, STRING)
                                        .build()
                                        .pass(selectable.<String>data())
                                        .close();
                    break;
            }
        }
    }
}
