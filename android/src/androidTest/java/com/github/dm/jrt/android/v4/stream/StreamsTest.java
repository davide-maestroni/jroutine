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
import com.github.dm.jrt.android.core.Channels.ParcelableSelectable;
import com.github.dm.jrt.android.invocation.FunctionContextInvocationFactory;
import com.github.dm.jrt.android.invocation.MissingInvocationException;
import com.github.dm.jrt.android.invocation.PassingFunctionContextInvocation;
import com.github.dm.jrt.android.v4.TestActivity;
import com.github.dm.jrt.android.v4.core.ChannelsCompat;
import com.github.dm.jrt.android.v4.core.JRoutineCompat;
import com.github.dm.jrt.android.v4.core.LoaderContextCompat;
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
import static com.github.dm.jrt.android.v4.core.LoaderContextCompat.loaderFrom;
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

        final LoaderContextCompat context = loaderFrom(getActivity());
        StreamChannel<String> channel1 =
                StreamsCompat.streamOf("test1", "test2", "test3").with(context).runOnShared();
        StreamChannel<String> channel2 =
                StreamsCompat.streamOf("test4", "test5", "test6").with(context).runOnShared();
        assertThat(StreamsCompat.blend(channel2, channel1).afterMax(seconds(1)).all()).containsOnly(
                "test1", "test2", "test3", "test4", "test5", "test6");
        channel1 = StreamsCompat.streamOf("test1", "test2", "test3").with(context).runOnShared();
        channel2 = StreamsCompat.streamOf("test4", "test5", "test6").with(context).runOnShared();
        assertThat(StreamsCompat.blend(Arrays.<StreamChannel<?>>asList(channel1, channel2))
                                .afterMax(seconds(1))
                                .all()).containsOnly("test1", "test2", "test3", "test4", "test5",
                                                     "test6");
    }

    public void testBlendAbort() {

        final IOChannelBuilder builder = JRoutineCompat.io();
        final FunctionContextInvocationFactory<Object, Object> factory =
                PassingFunctionContextInvocation.factoryOf();
        final Routine<Object, Object> routine =
                JRoutineCompat.with(loaderFrom(getActivity())).on(factory).buildRoutine();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().abort();

        try {

            routine.asyncCall(StreamsCompat.blend(channel1, channel2)).afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().abort();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();

        try {

            routine.asyncCall(
                    StreamsCompat.blend(Arrays.<OutputChannel<?>>asList(channel1, channel2)))
                   .afterMax(seconds(1))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testBlendError() {

        try {

            StreamsCompat.blend();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            StreamsCompat.blend((OutputChannel<?>[]) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            StreamsCompat.blend(new OutputChannel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            StreamsCompat.blend(Collections.<OutputChannel<?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            StreamsCompat.blend((List<OutputChannel<?>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            StreamsCompat.blend(Collections.<OutputChannel<?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testBuilder() {

        final LoaderContextCompat context = loaderFrom(getActivity());
        assertThat(StreamsCompat.streamOf("test")
                                .with(context)
                                .runOnShared()
                                .afterMax(seconds(1))
                                .all()).containsExactly("test");
        assertThat(StreamsCompat.streamOf("test1", "test2", "test3")
                                .with(context)
                                .runOnShared()
                                .afterMax(seconds(1))
                                .all()).containsExactly("test1", "test2", "test3");
        assertThat(StreamsCompat.streamOf(Arrays.asList("test1", "test2", "test3"))
                                .with(context)
                                .runOnShared()
                                .afterMax(seconds(1))
                                .all()).containsExactly("test1", "test2", "test3");
        assertThat(StreamsCompat.streamOf(JRoutineCompat.io().of("test1", "test2", "test3"))
                                .with(context)
                                .runOnShared()
                                .afterMax(seconds(1))
                                .all()).containsExactly("test1", "test2", "test3");
    }

    @SuppressWarnings("ConstantConditions")
    public void testBuilderNullPointerError() {

        try {

            StreamsCompat.streamOf((OutputChannel<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testConcat() {

        final LoaderContextCompat context = loaderFrom(getActivity());
        StreamChannel<String> channel1 =
                StreamsCompat.streamOf("test1", "test2", "test3").with(context).runOnShared();
        StreamChannel<String> channel2 =
                StreamsCompat.streamOf("test4", "test5", "test6").with(context).runOnShared();
        assertThat(StreamsCompat.concat(channel2, channel1)
                                .afterMax(seconds(1))
                                .all()).containsExactly("test4", "test5", "test6", "test1", "test2",
                                                        "test3");
        channel1 = StreamsCompat.streamOf("test1", "test2", "test3").with(context).runOnShared();
        channel2 = StreamsCompat.streamOf("test4", "test5", "test6").with(context).runOnShared();
        assertThat(StreamsCompat.concat(Arrays.<StreamChannel<?>>asList(channel1, channel2))
                                .afterMax(seconds(1))
                                .all()).containsExactly("test1", "test2", "test3", "test4", "test5",
                                                        "test6");
    }

    public void testConcatAbort() {

        final IOChannelBuilder builder = JRoutineCompat.io();
        final FunctionContextInvocationFactory<Object, Object> factory =
                PassingFunctionContextInvocation.factoryOf();
        final Routine<Object, Object> routine =
                JRoutineCompat.with(loaderFrom(getActivity())).on(factory).buildRoutine();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().abort();

        try {

            routine.asyncCall(StreamsCompat.concat(channel1, channel2)).afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().abort();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();

        try {

            routine.asyncCall(
                    StreamsCompat.concat(Arrays.<OutputChannel<?>>asList(channel1, channel2)))
                   .afterMax(seconds(1))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testConcatError() {

        try {

            StreamsCompat.concat();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            StreamsCompat.concat((OutputChannel<?>[]) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            StreamsCompat.concat(new OutputChannel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            StreamsCompat.concat(Collections.<OutputChannel<?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            StreamsCompat.concat((List<OutputChannel<?>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            StreamsCompat.concat(Collections.<OutputChannel<?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testFactory() {

        assertThat(StreamsCompat.onStream(toUpperCaseChannel())
                                .asyncCall("test1", "test2", "test3")
                                .afterMax(seconds(3))
                                .all()).containsExactly("TEST1", "TEST2", "TEST3");

        try {

            final InvocationChannel<String, String> channel =
                    StreamsCompat.onStream(toUpperCaseChannel()).asyncInvoke();
            channel.abort(new IllegalArgumentException());
            channel.result().afterMax(seconds(3)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        final FunctionContextInvocationFactory<String, String> factory =
                StreamsCompat.contextFactory(toUpperCaseChannel());
        assertThat(JRoutineCompat.with(loaderFrom(getActivity()))
                                 .on(factory)
                                 .asyncCall("test1", "test2", "test3")
                                 .afterMax(seconds(3))
                                 .all()).containsExactly("TEST1", "TEST2", "TEST3");

        try {

            final InvocationChannel<String, String> channel =
                    JRoutineCompat.with(loaderFrom(getActivity())).on(factory).asyncInvoke();
            channel.abort(new IllegalArgumentException());
            channel.result().afterMax(seconds(3)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        assertThat(StreamsCompat.onStreamWith(loaderFrom(getActivity()), toUpperCaseChannel())
                                .asyncCall("test1", "test2", "test3")
                                .afterMax(seconds(3))
                                .all()).containsExactly("TEST1", "TEST2", "TEST3");

        try {

            final InvocationChannel<String, String> channel =
                    StreamsCompat.onStreamWith(loaderFrom(getActivity()), toUpperCaseChannel())
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
                StreamsCompat.contextFactory(function);
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(null);
        assertThat(factory).isNotEqualTo("test");
        assertThat(factory).isNotEqualTo(
                StreamsCompat.contextFactory(Functions.<StreamChannel<?>>identity()));
        assertThat(factory).isEqualTo(StreamsCompat.contextFactory(function));
        assertThat(factory.hashCode()).isEqualTo(StreamsCompat.contextFactory(function).hashCode());
    }

    @SuppressWarnings("ConstantConditions")
    public void testFactoryError() {

        try {

            StreamsCompat.contextFactory(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            StreamsCompat.onStreamWith(null, Functions.<StreamChannel<?>>identity());

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            StreamsCompat.onStreamWith(loaderFrom(getActivity()), null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @SuppressWarnings("unchecked")
    public void testGroupBy() {

        final LoaderContextCompat context = loaderFrom(getActivity());
        assertThat(StreamsCompat.streamOf()
                                .with(context)
                                .sync()
                                .range(1, 10)
                                .async()
                                .map(StreamsCompat.<Number>groupBy(3))
                                .afterMax(seconds(3))
                                .all()).containsExactly(Arrays.<Number>asList(1, 2, 3),
                                                        Arrays.<Number>asList(4, 5, 6),
                                                        Arrays.<Number>asList(7, 8, 9),
                                                        Collections.<Number>singletonList(10));
        assertThat(StreamsCompat.streamOf()
                                .with(context)
                                .sync()
                                .range(1, 10)
                                .async()
                                .map(StreamsCompat.<Number>groupBy(13))
                                .afterMax(seconds(3))
                                .all()).containsExactly(
                Arrays.<Number>asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        assertThat(StreamsCompat.streamOf()
                                .sync()
                                .range(1, 10)
                                .with(context)
                                .async()
                                .map(StreamsCompat.<Number>groupBy(3))
                                .afterMax(seconds(3))
                                .all()).containsExactly(Arrays.<Number>asList(1, 2, 3),
                                                        Arrays.<Number>asList(4, 5, 6),
                                                        Arrays.<Number>asList(7, 8, 9),
                                                        Collections.<Number>singletonList(10));
        assertThat(StreamsCompat.streamOf()
                                .sync()
                                .range(1, 10)
                                .with(context)
                                .async()
                                .map(StreamsCompat.<Number>groupBy(13))
                                .afterMax(seconds(3))
                                .all()).containsExactly(
                Arrays.<Number>asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
    }

    public void testGroupByEquals() {

        final InvocationFactory<Object, List<Object>> factory = StreamsCompat.groupBy(2);
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(null);
        assertThat(factory).isNotEqualTo("test");
        assertThat(factory).isNotEqualTo(StreamsCompat.groupBy(3));
        assertThat(factory).isEqualTo(StreamsCompat.groupBy(2));
        assertThat(factory.hashCode()).isEqualTo(StreamsCompat.groupBy(2).hashCode());
    }

    public void testGroupByError() {

        try {

            StreamsCompat.groupBy(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            StreamsCompat.groupBy(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @SuppressWarnings("unchecked")
    public void testGroupByPlaceholder() {

        final LoaderContextCompat context = loaderFrom(getActivity());
        assertThat(StreamsCompat.streamOf()
                                .with(context)
                                .sync()
                                .range(1, 10)
                                .async()
                                .map(StreamsCompat.<Number>groupBy(3, 0))
                                .afterMax(seconds(3))
                                .all()).containsExactly(Arrays.<Number>asList(1, 2, 3),
                                                        Arrays.<Number>asList(4, 5, 6),
                                                        Arrays.<Number>asList(7, 8, 9),
                                                        Arrays.<Number>asList(10, 0, 0));
        assertThat(StreamsCompat.streamOf()
                                .with(context)
                                .sync()
                                .range(1, 10)
                                .async()
                                .map(StreamsCompat.<Number>groupBy(13, -1))
                                .afterMax(seconds(3))
                                .all()).containsExactly(
                Arrays.<Number>asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, -1, -1, -1));
        assertThat(StreamsCompat.streamOf()
                                .sync()
                                .range(1, 10)
                                .with(context)
                                .async()
                                .map(StreamsCompat.<Number>groupBy(3, -31))
                                .afterMax(seconds(3))
                                .all()).containsExactly(Arrays.<Number>asList(1, 2, 3),
                                                        Arrays.<Number>asList(4, 5, 6),
                                                        Arrays.<Number>asList(7, 8, 9),
                                                        Arrays.<Number>asList(10, -31, -31));
        assertThat(StreamsCompat.streamOf()
                                .sync()
                                .range(1, 10)
                                .with(context)
                                .async()
                                .map(StreamsCompat.<Number>groupBy(13, 71))
                                .afterMax(seconds(3))
                                .all()).containsExactly(
                Arrays.<Number>asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 71, 71, 71));
    }

    public void testGroupByPlaceholderEquals() {

        final Object placeholder = -11;
        final InvocationFactory<Object, List<Object>> factory =
                StreamsCompat.groupBy(2, placeholder);
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(null);
        assertThat(factory).isNotEqualTo("test");
        assertThat(factory).isNotEqualTo(StreamsCompat.groupBy(3, -11));
        assertThat(factory).isEqualTo(StreamsCompat.groupBy(2, -11));
        assertThat(factory.hashCode()).isEqualTo(StreamsCompat.groupBy(2, -11).hashCode());
    }

    public void testGroupByPlaceholderError() {

        try {

            StreamsCompat.groupBy(-1, 77);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            StreamsCompat.groupBy(0, null);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testJoin() {

        final IOChannelBuilder builder = JRoutineCompat.io();
        final FunctionContextInvocationFactory<List<?>, Character> factory =
                factoryFrom(JRoutineCompat.on(new CharAt()).buildRoutine(), 1, DelegationType.SYNC);
        final Routine<List<?>, Character> routine =
                JRoutineCompat.with(loaderFrom(getActivity())).on(factory).buildRoutine();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.asyncCall(StreamsCompat.join(channel1, channel2))
                          .afterMax(seconds(10))
                          .all()).containsExactly('s', '2');
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.asyncCall(
                StreamsCompat.join(Arrays.<OutputChannel<?>>asList(channel1, channel2)))
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
        assertThat(routine.asyncCall(StreamsCompat.join(channel1, channel2))
                          .afterMax(seconds(10))
                          .all()).containsExactly('s', '2');
    }

    public void testJoinAbort() {

        final IOChannelBuilder builder = JRoutineCompat.io();
        final FunctionContextInvocationFactory<List<?>, Character> factory =
                factoryFrom(JRoutineCompat.on(new CharAt()).buildRoutine(), 1, DelegationType.SYNC);
        final Routine<List<?>, Character> routine =
                JRoutineCompat.with(loaderFrom(getActivity())).on(factory).buildRoutine();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().abort();

        try {

            routine.asyncCall(StreamsCompat.join(channel1, channel2)).afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().abort();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();

        try {

            routine.asyncCall(
                    StreamsCompat.join(Arrays.<OutputChannel<?>>asList(channel1, channel2)))
                   .afterMax(seconds(1))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testJoinError() {

        try {

            StreamsCompat.join();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            StreamsCompat.join(Collections.<OutputChannel<?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            StreamsCompat.join(new OutputChannel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            StreamsCompat.join(Collections.<OutputChannel<?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testJoinPlaceholder() {

        final IOChannelBuilder builder = JRoutineCompat.io();
        final FunctionContextInvocationFactory<List<?>, Character> factory =
                factoryFrom(JRoutineCompat.on(new CharAt()).buildRoutine(), 1, DelegationType.SYNC);
        final Routine<List<?>, Character> routine =
                JRoutineCompat.with(loaderFrom(getActivity())).on(factory).buildRoutine();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.asyncCall(StreamsCompat.join(new Object(), channel1, channel2))
                          .afterMax(seconds(10))
                          .all()).containsExactly('s', '2');
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.asyncCall(
                StreamsCompat.join(null, Arrays.<OutputChannel<?>>asList(channel1, channel2)))
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

            routine.asyncCall(StreamsCompat.join(new Object(), channel1, channel2))
                   .afterMax(seconds(10))
                   .all();

            fail();

        } catch (final InvocationException ignored) {

        }
    }

    public void testJoinPlaceholderAbort() {

        final IOChannelBuilder builder = JRoutineCompat.io();
        final FunctionContextInvocationFactory<List<?>, Character> factory =
                factoryFrom(JRoutineCompat.on(new CharAt()).buildRoutine(), 1, DelegationType.SYNC);
        final Routine<List<?>, Character> routine =
                JRoutineCompat.with(loaderFrom(getActivity())).on(factory).buildRoutine();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().abort();

        try {

            routine.asyncCall(StreamsCompat.join((Object) null, channel1, channel2))
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

            routine.asyncCall(StreamsCompat.join(new Object(),
                                                 Arrays.<OutputChannel<?>>asList(channel1,
                                                                                 channel2)))
                   .afterMax(seconds(1))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testJoinPlaceholderError() {

        try {

            StreamsCompat.join(new Object());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            StreamsCompat.join(null, Collections.<OutputChannel<?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            StreamsCompat.join(new Object(), new OutputChannel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            StreamsCompat.join(new Object(), Collections.<OutputChannel<?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testLazyBuilder() {

        assertThat(StreamsCompat.lazyStreamOf().afterMax(seconds(1)).all()).isEmpty();
        assertThat(StreamsCompat.lazyStreamOf("test").afterMax(seconds(1)).all()).containsExactly(
                "test");
        assertThat(StreamsCompat.lazyStreamOf("test1", "test2", "test3").afterMax(seconds(1)).all())
                .containsExactly("test1", "test2", "test3");
        assertThat(StreamsCompat.lazyStreamOf(Arrays.asList("test1", "test2", "test3"))
                                .afterMax(seconds(1))
                                .all()).containsExactly("test1", "test2", "test3");
        assertThat(StreamsCompat.lazyStreamOf(JRoutineCompat.io().of("test1", "test2", "test3"))
                                .afterMax(seconds(1))
                                .all()).containsExactly("test1", "test2", "test3");
        final LoaderContextCompat context = loaderFrom(getActivity());
        assertThat(
                StreamsCompat.lazyStreamOf().with(context).runOnShared().afterMax(seconds(1)).all())
                .isEmpty();
        assertThat(StreamsCompat.lazyStreamOf("test")
                                .with(context)
                                .runOnShared()
                                .afterMax(seconds(1))
                                .all()).containsExactly("test");
        assertThat(StreamsCompat.lazyStreamOf("test1", "test2", "test3")
                                .with(context)
                                .runOnShared()
                                .afterMax(seconds(1))
                                .all()).containsExactly("test1", "test2", "test3");
        assertThat(StreamsCompat.lazyStreamOf(Arrays.asList("test1", "test2", "test3"))
                                .with(context)
                                .runOnShared()
                                .afterMax(seconds(1))
                                .all()).containsExactly("test1", "test2", "test3");
        assertThat(StreamsCompat.lazyStreamOf(JRoutineCompat.io().of("test1", "test2", "test3"))
                                .with(context)
                                .runOnShared()
                                .afterMax(seconds(1))
                                .all()).containsExactly("test1", "test2", "test3");
    }

    @SuppressWarnings("ConstantConditions")
    public void testLazyBuilderNullPointerError() {

        try {

            StreamsCompat.lazyStreamOf((OutputChannel<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testLimit() {

        final LoaderContextCompat context = loaderFrom(getActivity());
        assertThat(StreamsCompat.streamOf()
                                .with(context)
                                .sync()
                                .range(1, 10)
                                .async()
                                .map(StreamsCompat.limit(5))
                                .afterMax(seconds(3))
                                .all()).containsExactly(1, 2, 3, 4, 5);
        assertThat(StreamsCompat.streamOf()
                                .with(context)
                                .sync()
                                .range(1, 10)
                                .async()
                                .map(StreamsCompat.limit(0))
                                .afterMax(seconds(3))
                                .all()).isEmpty();
        assertThat(StreamsCompat.streamOf()
                                .with(context)
                                .sync()
                                .range(1, 10)
                                .async()
                                .map(StreamsCompat.limit(15))
                                .afterMax(seconds(3))
                                .all()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(StreamsCompat.streamOf()
                                .sync()
                                .range(1, 10)
                                .with(context)
                                .async()
                                .map(StreamsCompat.limit(5))
                                .afterMax(seconds(3))
                                .all()).containsExactly(1, 2, 3, 4, 5);
        assertThat(StreamsCompat.streamOf()
                                .sync()
                                .range(1, 10)
                                .with(context)
                                .async()
                                .map(StreamsCompat.limit(0))
                                .afterMax(seconds(3))
                                .all()).isEmpty();
        assertThat(StreamsCompat.streamOf()
                                .sync()
                                .range(1, 10)
                                .with(context)
                                .async()
                                .map(StreamsCompat.limit(15))
                                .afterMax(seconds(3))
                                .all()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    public void testLimitEquals() {

        final InvocationFactory<Object, Object> factory = StreamsCompat.limit(2);
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(null);
        assertThat(factory).isNotEqualTo("test");
        assertThat(factory).isNotEqualTo(StreamsCompat.limit(3));
        assertThat(factory).isEqualTo(StreamsCompat.limit(2));
        assertThat(factory.hashCode()).isEqualTo(StreamsCompat.limit(2).hashCode());
    }

    public void testLimitError() {

        try {

            StreamsCompat.limit(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testLoaderId() {

        final LoaderContextCompat context = loaderFrom(getActivity());
        StreamsCompat.streamOf("test1").with(context).loaderId(11).async().map(toUpperCase());
        assertThat(JRoutineCompat.with(context)
                                 .onId(11)
                                 .buildChannel()
                                 .afterMax(seconds(10))
                                 .next()).isEqualTo("TEST1");
        StreamsCompat.streamOf("test2")
                     .with(context)
                     .withLoaders()
                     .withLoaderId(21)
                     .set()
                     .async()
                     .map(toUpperCase());
        assertThat(JRoutineCompat.with(context)
                                 .onId(21)
                                 .buildChannel()
                                 .afterMax(seconds(10))
                                 .next()).isEqualTo("TEST2");
        StreamsCompat.streamOf("test3")
                     .with(context)
                     .withStreamLoaders()
                     .withLoaderId(31)
                     .set()
                     .async()
                     .map(toUpperCase());
        assertThat(JRoutineCompat.with(context)
                                 .onId(31)
                                 .buildChannel()
                                 .afterMax(seconds(10))
                                 .next()).isEqualTo("TEST3");
    }

    public void testMap() {

        final IOChannelBuilder builder =
                JRoutineCompat.io().withChannels().withChannelOrder(OrderType.BY_CALL).set();
        final IOChannel<String> channel1 = builder.buildChannel();
        final IOChannel<Integer> channel2 = builder.buildChannel();

        final OutputChannel<? extends ParcelableSelectable<Object>> channel =
                StreamsCompat.merge(Arrays.<OutputChannel<?>>asList(channel1, channel2));
        final OutputChannel<ParcelableSelectable<Object>> output =
                JRoutineCompat.with(loaderFrom(getActivity()))
                              .on(factoryFrom(JRoutineCompat.on(new Sort()).buildRoutine(), 1,
                                              DelegationType.SYNC))
                              .withInvocations()
                              .withInputOrder(OrderType.BY_CALL)
                              .set()
                              .asyncCall(channel);
        final SparseArrayCompat<OutputChannel<Object>> channelMap =
                ChannelsCompat.selectParcelable(output, Sort.INTEGER, Sort.STRING);

        for (int i = 0; i < 4; i++) {

            final String input = Integer.toString(i);
            channel1.after(millis(20)).pass(input);
            channel2.after(millis(20)).pass(i);
        }

        channel1.close();
        channel2.close();

        final LoaderContextCompat context = loaderFrom(getActivity());
        assertThat(StreamsCompat.streamOf(channelMap.get(Sort.STRING))
                                .with(context)
                                .runOnShared()
                                .afterMax(seconds(1))
                                .all()).containsExactly("0", "1", "2", "3");
        assertThat(StreamsCompat.streamOf(channelMap.get(Sort.INTEGER))
                                .with(context)
                                .runOnShared()
                                .afterMax(seconds(1))
                                .all()).containsExactly(0, 1, 2, 3);
    }

    public void testMerge() {

        final IOChannelBuilder builder =
                JRoutineCompat.io().withChannels().withChannelOrder(OrderType.BY_CALL).set();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        OutputChannel<? extends ParcelableSelectable<?>> outputChannel;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = StreamsCompat.<Object>merge(-7, channel1, channel2);
        channel1.pass("test1").close();
        channel2.pass(13).close();
        assertThat(outputChannel.afterMax(seconds(1)).all()).containsOnly(
                new ParcelableSelectable<String>("test1", -7),
                new ParcelableSelectable<Integer>(13, -6));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = StreamsCompat.<Object>merge(11, Arrays.<OutputChannel<?>>asList(channel1,
                                                                                        channel2));
        channel2.pass(13).close();
        channel1.pass("test1").close();
        assertThat(outputChannel.afterMax(seconds(1)).all()).containsOnly(
                new ParcelableSelectable<String>("test1", 11),
                new ParcelableSelectable<Integer>(13, 12));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = StreamsCompat.<Object>merge(channel1, channel2);
        channel1.pass("test2").close();
        channel2.pass(-17).close();
        assertThat(outputChannel.afterMax(seconds(1)).all()).containsOnly(
                new ParcelableSelectable<String>("test2", 0),
                new ParcelableSelectable<Integer>(-17, 1));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel =
                StreamsCompat.<Object>merge(Arrays.<OutputChannel<?>>asList(channel1, channel2));
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
        outputChannel = StreamsCompat.<Object>merge(channelMap);
        channel1.pass("test3").close();
        channel2.pass(111).close();
        assertThat(outputChannel.afterMax(seconds(1)).all()).containsOnly(
                new ParcelableSelectable<String>("test3", 7),
                new ParcelableSelectable<Integer>(111, -3));
    }

    @SuppressWarnings("unchecked")
    public void testMerge4() {

        final IOChannelBuilder builder =
                JRoutineCompat.io().withChannels().withChannelOrder(OrderType.BY_CALL).set();
        final IOChannel<String> channel1 = builder.buildChannel();
        final IOChannel<String> channel2 = builder.buildChannel();
        final IOChannel<String> channel3 = builder.buildChannel();
        final IOChannel<String> channel4 = builder.buildChannel();

        final Routine<ParcelableSelectable<String>, String> routine =
                JRoutineCompat.with(loaderFrom(getActivity()))
                              .on(factoryFrom(
                                      JRoutineCompat.on(factoryOf(new ClassToken<Amb<String>>() {}))
                                                    .buildRoutine(), 1, DelegationType.SYNC))
                              .buildRoutine();
        final OutputChannel<String> outputChannel = routine.asyncCall(
                StreamsCompat.merge(Arrays.asList(channel1, channel2, channel3, channel4)));

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

        final IOChannelBuilder builder =
                JRoutineCompat.io().withChannels().withChannelOrder(OrderType.BY_CALL).set();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        OutputChannel<? extends ParcelableSelectable<?>> outputChannel;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = StreamsCompat.<Object>merge(-7, channel1, channel2);
        channel1.pass("test1").close();
        channel2.abort();

        try {

            outputChannel.afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = StreamsCompat.<Object>merge(11, Arrays.<OutputChannel<?>>asList(channel1,
                                                                                        channel2));
        channel2.abort();
        channel1.pass("test1").close();

        try {

            outputChannel.afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = StreamsCompat.<Object>merge(channel1, channel2);
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
                StreamsCompat.<Object>merge(Arrays.<OutputChannel<?>>asList(channel1, channel2));
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
        outputChannel = StreamsCompat.<Object>merge(channelMap);
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

            StreamsCompat.merge(0, Collections.<OutputChannel<Object>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            StreamsCompat.merge(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            StreamsCompat.merge(Collections.<OutputChannel<Object>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            StreamsCompat.merge(Collections.<Integer, OutputChannel<Object>>emptyMap());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            StreamsCompat.merge();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            StreamsCompat.merge(new OutputChannel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            StreamsCompat.merge(Collections.<OutputChannel<?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            StreamsCompat.merge(0, new OutputChannel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            StreamsCompat.merge(0, Collections.<OutputChannel<?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            StreamsCompat.merge(Collections.<Integer, OutputChannel<?>>singletonMap(1, null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @SuppressWarnings("unchecked")
    public void testOutputToSelectable() {

        final IOChannel<String> channel = JRoutineCompat.io().buildChannel();
        channel.pass("test1", "test2", "test3").close();
        assertThat(StreamsCompat.toSelectable(channel.asOutput(), 33)
                                .afterMax(seconds(1))
                                .all()).containsExactly(
                new ParcelableSelectable<String>("test1", 33),
                new ParcelableSelectable<String>("test2", 33),
                new ParcelableSelectable<String>("test3", 33));
    }

    public void testOutputToSelectableAbort() {

        final IOChannel<String> channel = JRoutineCompat.io().buildChannel();
        channel.pass("test1", "test2", "test3").abort();

        try {
            StreamsCompat.toSelectable(channel.asOutput(), 33).afterMax(seconds(1)).all();
            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testRepeat() {

        final IOChannel<Object> ioChannel = JRoutineCompat.io().buildChannel();
        final OutputChannel<Object> channel = StreamsCompat.repeat(ioChannel);
        ioChannel.pass("test1", "test2");
        final IOChannel<Object> output1 = JRoutineCompat.io().buildChannel();
        channel.passTo(output1).close();
        assertThat(output1.next()).isEqualTo("test1");
        final IOChannel<Object> output2 = JRoutineCompat.io().buildChannel();
        channel.passTo(output2).close();
        ioChannel.pass("test3").close();
        assertThat(output2.all()).containsExactly("test1", "test2", "test3");
        assertThat(output1.all()).containsExactly("test2", "test3");
    }

    public void testRepeatAbort() {

        final IOChannel<Object> ioChannel = JRoutineCompat.io().buildChannel();
        final OutputChannel<Object> channel = StreamsCompat.repeat(ioChannel);
        ioChannel.pass("test1", "test2");
        final IOChannel<Object> output1 = JRoutineCompat.io().buildChannel();
        channel.passTo(output1).close();
        assertThat(output1.next()).isEqualTo("test1");
        final IOChannel<Object> output2 = JRoutineCompat.io().buildChannel();
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

        final LoaderContextCompat context = loaderFrom(getActivity());
        StreamsCompat.streamOf("test1").with(context).routineId(11).async().map(toUpperCase());

        try {
            JRoutineCompat.with(context).onId(11).buildChannel().afterMax(seconds(10)).next();
            fail();

        } catch (final MissingInvocationException ignored) {

        }

        assertThat(StreamsCompat.streamOf("test2")
                                .with(context)
                                .withLoaders()
                                .withRoutineId(11)
                                .set()
                                .async()
                                .map(toUpperCase())
                                .afterMax(seconds(10))
                                .next()).isEqualTo("TEST2");
        final AtomicInteger count = new AtomicInteger();
        StreamsCompat.streamOf().with(context).routineId(11).then(delayedIncrement(count));
        assertThat(StreamsCompat.streamOf()
                                .with(context)
                                .routineId(11)
                                .then(increment(count))
                                .afterMax(seconds(10))
                                .next()).isEqualTo(1);
    }

    public void testSkip() {

        final LoaderContextCompat context = loaderFrom(getActivity());
        assertThat(StreamsCompat.streamOf()
                                .with(context)
                                .sync()
                                .range(1, 10)
                                .async()
                                .map(StreamsCompat.skip(5))
                                .afterMax(seconds(3))
                                .all()).containsExactly(6, 7, 8, 9, 10);
        assertThat(StreamsCompat.streamOf()
                                .with(context)
                                .sync()
                                .range(1, 10)
                                .async()
                                .map(StreamsCompat.skip(15))
                                .afterMax(seconds(3))
                                .all()).isEmpty();
        assertThat(StreamsCompat.streamOf()
                                .with(context)
                                .sync()
                                .range(1, 10)
                                .async()
                                .map(StreamsCompat.skip(0))
                                .afterMax(seconds(3))
                                .all()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(StreamsCompat.streamOf()
                                .sync()
                                .range(1, 10)
                                .with(context)
                                .async()
                                .map(StreamsCompat.skip(5))
                                .afterMax(seconds(3))
                                .all()).containsExactly(6, 7, 8, 9, 10);
        assertThat(StreamsCompat.streamOf()
                                .sync()
                                .range(1, 10)
                                .with(context)
                                .async()
                                .map(StreamsCompat.skip(15))
                                .afterMax(seconds(3))
                                .all()).isEmpty();
        assertThat(StreamsCompat.streamOf()
                                .sync()
                                .range(1, 10)
                                .with(context)
                                .async()
                                .map(StreamsCompat.skip(0))
                                .afterMax(seconds(3))
                                .all()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    public void testSkipEquals() {

        final InvocationFactory<Object, Object> factory = StreamsCompat.skip(2);
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(null);
        assertThat(factory).isNotEqualTo("test");
        assertThat(factory).isNotEqualTo(StreamsCompat.skip(3));
        assertThat(factory).isEqualTo(StreamsCompat.skip(2));
        assertThat(factory.hashCode()).isEqualTo(StreamsCompat.skip(2).hashCode());
    }

    public void testSkipError() {

        try {

            StreamsCompat.skip(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testStaleTime() throws InterruptedException {


        final LoaderContextCompat context = loaderFrom(getActivity());
        final AtomicInteger count = new AtomicInteger();
        final Function<String, String> function = stringIncrement(count);
        StreamsCompat.streamOf("test")
                     .with(context)
                     .async()
                     .cache(CacheStrategyType.CACHE)
                     .map(function);
        assertThat(StreamsCompat.streamOf("test")
                                .with(context)
                                .staleAfter(2000, TimeUnit.MILLISECONDS)
                                .async()
                                .map(function)
                                .afterMax(seconds(10))
                                .next()).isEqualTo("test1");
        seconds(5).sleepAtLeast();
        assertThat(StreamsCompat.streamOf("test")
                                .with(context)
                                .staleAfter(ZERO)
                                .async()
                                .map(function)
                                .afterMax(seconds(10))
                                .next()).isEqualTo("test2");
        seconds(5).sleepAtLeast();
        StreamsCompat.streamOf("test")
                     .with(context)
                     .cache(CacheStrategyType.CACHE_IF_SUCCESS)
                     .async()
                     .map(function);
        seconds(5).sleepAtLeast();
        assertThat(StreamsCompat.streamOf("test")
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
                    ChannelsCompat.<Object, Integer>selectParcelable(result, INTEGER)
                                  .pass(selectable.<Integer>data())
                                  .close();
                    break;

                case STRING:
                    ChannelsCompat.<Object, String>selectParcelable(result, STRING)
                                  .pass(selectable.<String>data())
                                  .close();
                    break;
            }
        }
    }
}
