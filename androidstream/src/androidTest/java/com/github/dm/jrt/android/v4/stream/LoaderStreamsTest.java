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

import com.github.dm.jrt.android.channel.ParcelableSelectable;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.CacheStrategyType;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.core.invocation.IdentityContextInvocation;
import com.github.dm.jrt.android.core.invocation.MissingLoaderException;
import com.github.dm.jrt.android.v4.channel.SparseChannelsCompat;
import com.github.dm.jrt.android.v4.core.JRoutineLoaderCompat;
import com.github.dm.jrt.android.v4.core.LoaderContextCompat;
import com.github.dm.jrt.channel.Selectable;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.ChannelBuilder;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel.InputChannel;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.config.InvocationConfiguration.OrderType;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.InvocationInterruptedException;
import com.github.dm.jrt.core.invocation.MappingInvocation;
import com.github.dm.jrt.core.invocation.TemplateInvocation;
import com.github.dm.jrt.core.log.Log.Level;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Functions;
import com.github.dm.jrt.function.Supplier;
import com.github.dm.jrt.stream.StreamChannel;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.dm.jrt.android.core.invocation.ContextInvocationFactory.factoryFrom;
import static com.github.dm.jrt.android.core.invocation.ContextInvocationFactory.factoryOf;
import static com.github.dm.jrt.android.v4.core.LoaderContextCompat.loaderFrom;
import static com.github.dm.jrt.core.util.UnitDuration.millis;
import static com.github.dm.jrt.core.util.UnitDuration.seconds;
import static com.github.dm.jrt.core.util.UnitDuration.zero;
import static com.github.dm.jrt.stream.Streams.range;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Streams unit tests.
 * <p>
 * Created by davide-maestroni on 01/04/2016.
 */
@TargetApi(VERSION_CODES.FROYO)
public class LoaderStreamsTest extends ActivityInstrumentationTestCase2<TestActivity> {

    public LoaderStreamsTest() {

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
    private static Function<StreamChannel<String, String>, StreamChannel<String, String>>
    toUpperCaseChannel() {

        return new Function<StreamChannel<String, String>, StreamChannel<String, String>>() {

            public StreamChannel<String, String> apply(
                    final StreamChannel<String, String> channel) {

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
        StreamChannel<String, String> channel1 =
                LoaderStreamsCompat.streamOf("test1", "test2", "test3")
                                   .with(context)
                                   .asyncMap(null);
        StreamChannel<String, String> channel2 =
                LoaderStreamsCompat.streamOf("test4", "test5", "test6")
                                   .with(context)
                                   .asyncMap(null);
        assertThat(LoaderStreamsCompat.blend(channel2, channel1)
                                      .buildChannels()
                                      .afterMax(seconds(10))
                                      .all()).containsOnly("test1", "test2", "test3", "test4",
                "test5", "test6");
        channel1 = LoaderStreamsCompat.streamOf("test1", "test2", "test3")
                                      .with(context)
                                      .asyncMap(null);
        channel2 = LoaderStreamsCompat.streamOf("test4", "test5", "test6")
                                      .with(context)
                                      .asyncMap(null);
        assertThat(LoaderStreamsCompat.blend(Arrays.<StreamChannel<?, ?>>asList(channel1, channel2))
                                      .buildChannels()
                                      .afterMax(seconds(10))
                                      .all()).containsOnly("test1", "test2", "test3", "test4",
                "test5", "test6");
    }

    public void testBlendAbort() {

        final ChannelBuilder builder = JRoutineCore.io();
        final ContextInvocationFactory<Object, Object> factory =
                IdentityContextInvocation.factoryOf();
        final Routine<Object, Object> routine =
                JRoutineLoaderCompat.with(loaderFrom(getActivity())).on(factory).buildRoutine();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().abort();

        try {

            routine.async(LoaderStreamsCompat.blend(channel1, channel2).buildChannels())
                   .afterMax(seconds(10))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().abort();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();

        try {

            routine.async(
                    LoaderStreamsCompat.blend(Arrays.<OutputChannel<?>>asList(channel1, channel2))
                                       .buildChannels()).afterMax(seconds(10)).all();

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
                                      .asyncMap(null)
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("test");
        assertThat(LoaderStreamsCompat.streamOf("test1", "test2", "test3")
                                      .with(context)
                                      .asyncMap(null)
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("test1", "test2", "test3");
        assertThat(LoaderStreamsCompat.streamOf(Arrays.asList("test1", "test2", "test3"))
                                      .with(context)
                                      .asyncMap(null)
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("test1", "test2", "test3");
        assertThat(LoaderStreamsCompat.streamOf(JRoutineCore.io().of("test1", "test2", "test3"))
                                      .with(context)
                                      .asyncMap(null)
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

    public void testCombine() {

        final InvocationChannel<String, String> channel1 =
                JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                    .on(IdentityContextInvocation.<String>factoryOf())
                                    .asyncInvoke()
                                    .orderByCall();
        final InvocationChannel<Integer, Integer> channel2 =
                JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                    .on(IdentityContextInvocation.<Integer>factoryOf())
                                    .asyncInvoke()
                                    .orderByCall();
        LoaderStreamsCompat.combine(channel1, channel2)
                           .buildChannels()
                           .pass(new ParcelableSelectable<Object>("test1", 0))
                           .pass(new ParcelableSelectable<Integer>(1, 1))
                           .close();
        LoaderStreamsCompat.combine(3, channel1, channel2)
                           .buildChannels()
                           .pass(new ParcelableSelectable<String>("test2", 3))
                           .pass(new ParcelableSelectable<Integer>(2, 4))
                           .close();
        LoaderStreamsCompat.combine(Arrays.<InvocationChannel<?, ?>>asList(channel1, channel2))
                           .buildChannels()
                           .pass(new ParcelableSelectable<String>("test3", 0))
                           .pass(new ParcelableSelectable<Integer>(3, 1))
                           .close();
        LoaderStreamsCompat.combine(-5, Arrays.<InvocationChannel<?, ?>>asList(channel1, channel2))
                           .buildChannels()
                           .pass(new ParcelableSelectable<String>("test4", -5))
                           .pass(new ParcelableSelectable<Integer>(4, -4))
                           .close();
        final HashMap<Integer, InvocationChannel<?, ?>> map =
                new HashMap<Integer, InvocationChannel<?, ?>>(2);
        map.put(31, channel1);
        map.put(17, channel2);
        LoaderStreamsCompat.combine(map)
                           .buildChannels()
                           .pass(new ParcelableSelectable<String>("test5", 31))
                           .pass(new ParcelableSelectable<Integer>(5, 17))
                           .close();
        final SparseArrayCompat<InvocationChannel<?, ?>> sparseArray =
                new SparseArrayCompat<InvocationChannel<?, ?>>(2);
        sparseArray.put(31, channel1);
        sparseArray.put(17, channel2);
        LoaderStreamsCompat.combine(sparseArray)
                           .buildChannels()
                           .pass(new ParcelableSelectable<String>("test6", 31))
                           .pass(new ParcelableSelectable<Integer>(6, 17))
                           .close();
        assertThat(channel1.result().afterMax(seconds(10)).all()).containsExactly("test1", "test2",
                "test3", "test4", "test5", "test6");
        assertThat(channel2.result().afterMax(seconds(10)).all()).containsExactly(1, 2, 3, 4, 5, 6);
    }

    public void testConcat() {

        final LoaderContextCompat context = loaderFrom(getActivity());
        StreamChannel<String, String> channel1 =
                LoaderStreamsCompat.streamOf("test1", "test2", "test3")
                                   .with(context)
                                   .asyncMap(null);
        StreamChannel<String, String> channel2 =
                LoaderStreamsCompat.streamOf("test4", "test5", "test6")
                                   .with(context)
                                   .asyncMap(null);
        assertThat(LoaderStreamsCompat.concat(channel2, channel1)
                                      .buildChannels()
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("test4", "test5", "test6", "test1",
                "test2", "test3");
        channel1 = LoaderStreamsCompat.streamOf("test1", "test2", "test3")
                                      .with(context)
                                      .asyncMap(null);
        channel2 = LoaderStreamsCompat.streamOf("test4", "test5", "test6")
                                      .with(context)
                                      .asyncMap(null);
        assertThat(
                LoaderStreamsCompat.concat(Arrays.<StreamChannel<?, ?>>asList(channel1, channel2))
                                   .buildChannels()
                                   .afterMax(seconds(10))
                                   .all()).containsExactly("test1", "test2", "test3", "test4",
                "test5", "test6");
    }

    public void testConcatAbort() {

        final ChannelBuilder builder = JRoutineCore.io();
        final ContextInvocationFactory<Object, Object> factory =
                IdentityContextInvocation.factoryOf();
        final Routine<Object, Object> routine =
                JRoutineLoaderCompat.with(loaderFrom(getActivity())).on(factory).buildRoutine();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().abort();

        try {

            routine.async(LoaderStreamsCompat.concat(channel1, channel2).buildChannels())
                   .afterMax(seconds(10))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().abort();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();

        try {

            routine.async(
                    LoaderStreamsCompat.concat(Arrays.<OutputChannel<?>>asList(channel1, channel2))
                                       .buildChannels()).afterMax(seconds(10)).all();

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

    public void testConfiguration() {

        final LoaderStreamChannelCompat<String, String> channel1 =
                LoaderStreamsCompat.streamOf("test1", "test2", "test3");
        final LoaderStreamChannelCompat<String, String> channel2 =
                LoaderStreamsCompat.streamOf("test4", "test5", "test6");
        assertThat(LoaderStreamsCompat.blend(channel2, channel1)
                                      .channelConfiguration()
                                      .withOrder(OrderType.BY_CALL)
                                      .withOutputTimeout(seconds(10))
                                      .apply()
                                      .buildChannels()
                                      .all()).containsExactly("test4", "test5", "test6", "test1",
                "test2", "test3");
    }

    public void testConstructor() {

        boolean failed = false;
        try {
            new LoaderStreamsCompat();
            failed = true;

        } catch (final Throwable ignored) {

        }

        assertThat(failed).isFalse();
    }

    public void testDistribute() {

        final InvocationChannel<String, String> channel1 =
                JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                    .on(IdentityContextInvocation.<String>factoryOf())
                                    .asyncInvoke()
                                    .orderByCall();
        final InvocationChannel<String, String> channel2 =
                JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                    .on(IdentityContextInvocation.<String>factoryOf())
                                    .asyncInvoke()
                                    .orderByCall();
        LoaderStreamsCompat.distribute(channel1, channel2)
                           .buildChannels()
                           .pass(Arrays.asList("test1-1", "test1-2"))
                           .close();
        LoaderStreamsCompat.distribute(Arrays.<InputChannel<?>>asList(channel1, channel2))
                           .buildChannels()
                           .pass(Arrays.asList("test2-1", "test2-2"))
                           .close();
        LoaderStreamsCompat.distribute(channel1, channel2)
                           .buildChannels()
                           .pass(Collections.singletonList("test3-1"))
                           .close();
        assertThat(channel1.result().afterMax(seconds(10)).all()).containsExactly("test1-1",
                "test2-1", "test3-1");
        assertThat(channel2.result().afterMax(seconds(10)).all()).containsExactly("test1-2",
                "test2-2");
    }

    public void testDistributePlaceholder() {

        final InvocationChannel<String, String> channel1 =
                JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                    .on(IdentityContextInvocation.<String>factoryOf())
                                    .asyncInvoke()
                                    .orderByCall();
        final InvocationChannel<String, String> channel2 =
                JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                    .on(IdentityContextInvocation.<String>factoryOf())
                                    .asyncInvoke()
                                    .orderByCall();
        LoaderStreamsCompat.distribute((Object) null, channel1, channel2)
                           .buildChannels()
                           .pass(Arrays.asList("test1-1", "test1-2"))
                           .close();
        final String placeholder = "placeholder";
        LoaderStreamsCompat.distribute((Object) placeholder,
                Arrays.<InputChannel<?>>asList(channel1, channel2))
                           .buildChannels()
                           .pass(Arrays.asList("test2-1", "test2-2"))
                           .close();
        LoaderStreamsCompat.distribute(placeholder, channel1, channel2)
                           .buildChannels()
                           .pass(Collections.singletonList("test3-1"))
                           .close();
        assertThat(channel1.result().afterMax(seconds(10)).all()).containsExactly("test1-1",
                "test2-1", "test3-1");
        assertThat(channel2.result().afterMax(seconds(10)).all()).containsExactly("test1-2",
                "test2-2", placeholder);
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

        final ContextInvocationFactory<String, String> factory =
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

        final Function<StreamChannel<String, String>, StreamChannel<String, String>> function =
                toUpperCaseChannel();
        final ContextInvocationFactory<String, String> factory =
                LoaderStreamsCompat.contextFactory(function);
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(null);
        assertThat(factory).isNotEqualTo("test");
        assertThat(factory).isNotEqualTo(LoaderStreamsCompat.contextFactory(
                Functions.<StreamChannel<Object, Object>>identity()));
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

            LoaderStreamsCompat.onStreamWith(null,
                    Functions.<StreamChannel<Object, Object>>identity());

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            LoaderStreamsCompat.onStreamWith(loaderFrom(getActivity()), null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testFactoryId() {

        final LoaderContextCompat context = loaderFrom(getActivity());
        LoaderStreamsCompat.streamOf("test1")
                           .with(context)
                           .factoryId(11)
                           .async()
                           .map(toUpperCase());

        try {
            JRoutineLoaderCompat.with(context).onId(11).buildChannel().afterMax(seconds(10)).next();
            fail();

        } catch (final MissingLoaderException ignored) {

        }

        assertThat(LoaderStreamsCompat.streamOf("test2")
                                      .with(context)
                                      .loaderConfiguration()
                                      .withFactoryId(11)
                                      .apply()
                                      .async()
                                      .map(toUpperCase())
                                      .afterMax(seconds(10))
                                      .next()).isEqualTo("TEST2");
        final AtomicInteger count = new AtomicInteger();
        LoaderStreamsCompat.streamOf().with(context).factoryId(11).thenGet(delayedIncrement(count));
        assertThat(LoaderStreamsCompat.streamOf()
                                      .with(context)
                                      .factoryId(11)
                                      .thenGet(increment(count))
                                      .afterMax(seconds(10))
                                      .next()).isEqualTo(1);
    }

    @SuppressWarnings("unchecked")
    public void testGroupBy() {

        final LoaderContextCompat context = loaderFrom(getActivity());
        assertThat(LoaderStreamsCompat.streamOf()
                                      .with(context)
                                      .sync()
                                      .thenGetMore(range(1, 10))
                                      .async()
                                      .map(LoaderStreamsCompat.<Number>groupBy(3))
                                      .afterMax(seconds(3))
                                      .all()).containsExactly(Arrays.<Number>asList(1, 2, 3),
                Arrays.<Number>asList(4, 5, 6), Arrays.<Number>asList(7, 8, 9),
                Collections.<Number>singletonList(10));
        assertThat(LoaderStreamsCompat.streamOf()
                                      .with(context)
                                      .sync()
                                      .thenGetMore(range(1, 10))
                                      .async()
                                      .map(LoaderStreamsCompat.<Number>groupBy(13))
                                      .afterMax(seconds(3))
                                      .all()).containsExactly(
                Arrays.<Number>asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        assertThat(LoaderStreamsCompat.streamOf()
                                      .sync()
                                      .thenGetMore(range(1, 10))
                                      .with(context)
                                      .async()
                                      .map(LoaderStreamsCompat.<Number>groupBy(3))
                                      .afterMax(seconds(3))
                                      .all()).containsExactly(Arrays.<Number>asList(1, 2, 3),
                Arrays.<Number>asList(4, 5, 6), Arrays.<Number>asList(7, 8, 9),
                Collections.<Number>singletonList(10));
        assertThat(LoaderStreamsCompat.streamOf()
                                      .sync()
                                      .thenGetMore(range(1, 10))
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
                                      .thenGetMore(range(1, 10))
                                      .async()
                                      .map(LoaderStreamsCompat.<Number>groupBy(3, 0))
                                      .afterMax(seconds(3))
                                      .all()).containsExactly(Arrays.<Number>asList(1, 2, 3),
                Arrays.<Number>asList(4, 5, 6), Arrays.<Number>asList(7, 8, 9),
                Arrays.<Number>asList(10, 0, 0));
        assertThat(LoaderStreamsCompat.streamOf()
                                      .with(context)
                                      .sync()
                                      .thenGetMore(range(1, 10))
                                      .async()
                                      .map(LoaderStreamsCompat.<Number>groupBy(13, -1))
                                      .afterMax(seconds(3))
                                      .all()).containsExactly(
                Arrays.<Number>asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, -1, -1, -1));
        assertThat(LoaderStreamsCompat.streamOf()
                                      .sync()
                                      .thenGetMore(range(1, 10))
                                      .with(context)
                                      .async()
                                      .map(LoaderStreamsCompat.<Number>groupBy(3, -31))
                                      .afterMax(seconds(3))
                                      .all()).containsExactly(Arrays.<Number>asList(1, 2, 3),
                Arrays.<Number>asList(4, 5, 6), Arrays.<Number>asList(7, 8, 9),
                Arrays.<Number>asList(10, -31, -31));
        assertThat(LoaderStreamsCompat.streamOf()
                                      .sync()
                                      .thenGetMore(range(1, 10))
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

    public void testInputMap() {

        final ArrayList<ParcelableSelectable<Object>> outputs =
                new ArrayList<ParcelableSelectable<Object>>();
        outputs.add(new ParcelableSelectable<Object>("test21", Sort.STRING));
        outputs.add(new ParcelableSelectable<Object>(-11, Sort.INTEGER));
        final Routine<ParcelableSelectable<Object>, ParcelableSelectable<Object>> routine =
                JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                    .on(factoryOf(Sort.class))
                                    .buildRoutine();
        SparseArrayCompat<IOChannel<Object>> channelMap;
        InvocationChannel<ParcelableSelectable<Object>, ParcelableSelectable<Object>> channel;
        channel = routine.asyncInvoke();
        channelMap = LoaderStreamsCompat.selectParcelable(channel,
                Arrays.asList(Sort.INTEGER, Sort.STRING)).buildChannels();
        channelMap.get(Sort.INTEGER).pass(-11).close();
        channelMap.get(Sort.STRING).pass("test21").close();
        assertThat(channel.result().afterMax(seconds(10)).all()).containsOnlyElementsOf(outputs);
        channel = routine.asyncInvoke();
        channelMap = LoaderStreamsCompat.selectParcelable(channel, Sort.INTEGER, Sort.STRING)
                                        .buildChannels();
        channelMap.get(Sort.INTEGER).pass(-11).close();
        channelMap.get(Sort.STRING).pass("test21").close();
        assertThat(channel.result().afterMax(seconds(10)).all()).containsOnlyElementsOf(outputs);
        channel = routine.asyncInvoke();
        channelMap = LoaderStreamsCompat.selectParcelable(Math.min(Sort.INTEGER, Sort.STRING), 2,
                channel).buildChannels();
        channelMap.get(Sort.INTEGER).pass(-11).close();
        channelMap.get(Sort.STRING).pass("test21").close();
        assertThat(channel.result().afterMax(seconds(10)).all()).containsOnlyElementsOf(outputs);
    }

    @SuppressWarnings("unchecked")
    public void testInputSelect() {

        IOChannel<Selectable<String>> channel = JRoutineCore.io().buildChannel();
        LoaderStreamsCompat.select(channel.asInput(), 33)
                           .buildChannels()
                           .pass("test1", "test2", "test3")
                           .close();
        assertThat(channel.close().afterMax(seconds(10)).all()).containsExactly(
                new ParcelableSelectable<String>("test1", 33),
                new ParcelableSelectable<String>("test2", 33),
                new ParcelableSelectable<String>("test3", 33));
        channel = JRoutineCore.io().buildChannel();
        Map<Integer, IOChannel<String>> channelMap =
                LoaderStreamsCompat.select(channel.asInput(), Arrays.asList(1, 2, 3))
                                   .buildChannels();
        channelMap.get(1).pass("test1").close();
        channelMap.get(2).pass("test2").close();
        channelMap.get(3).pass("test3").close();
        assertThat(channel.close().afterMax(seconds(10)).all()).containsExactly(
                new ParcelableSelectable<String>("test1", 1),
                new ParcelableSelectable<String>("test2", 2),
                new ParcelableSelectable<String>("test3", 3));
        channel = JRoutineCore.io().buildChannel();
        channelMap = LoaderStreamsCompat.select(channel.asInput(), 1, 2, 3).buildChannels();
        channelMap.get(1).pass("test1").close();
        channelMap.get(2).pass("test2").close();
        channelMap.get(3).pass("test3").close();
        assertThat(channel.close().afterMax(seconds(10)).all()).containsExactly(
                new ParcelableSelectable<String>("test1", 1),
                new ParcelableSelectable<String>("test2", 2),
                new ParcelableSelectable<String>("test3", 3));
        channel = JRoutineCore.io().buildChannel();
        channelMap = LoaderStreamsCompat.select(1, 3, channel.asInput()).buildChannels();
        channelMap.get(1).pass("test1").close();
        channelMap.get(2).pass("test2").close();
        channelMap.get(3).pass("test3").close();
        assertThat(channel.close().afterMax(seconds(10)).all()).containsExactly(
                new ParcelableSelectable<String>("test1", 1),
                new ParcelableSelectable<String>("test2", 2),
                new ParcelableSelectable<String>("test3", 3));
        channel = JRoutineCore.io().buildChannel();
        LoaderStreamsCompat.selectParcelable(channel, 33)
                           .buildChannels()
                           .pass("test1", "test2", "test3")
                           .close();
        channel.close();
        assertThat(channel.afterMax(seconds(10)).all()).containsExactly(
                new ParcelableSelectable<String>("test1", 33),
                new ParcelableSelectable<String>("test2", 33),
                new ParcelableSelectable<String>("test3", 33));
    }

    @SuppressWarnings("unchecked")
    public void testInputToSelectable() {

        final IOChannel<String> channel = JRoutineCore.io().buildChannel();
        LoaderStreamsCompat.toSelectable(channel.asInput(), 33)
                           .buildChannels()
                           .pass(new Selectable<String>("test1", 33),
                                   new Selectable<String>("test2", -33),
                                   new Selectable<String>("test3", 33),
                                   new Selectable<String>("test4", 333))
                           .close();
        assertThat(channel.close().afterMax(seconds(10)).all()).containsExactly("test1", "test3");
    }

    public void testJoin() {

        final ChannelBuilder builder = JRoutineCore.io();
        final ContextInvocationFactory<List<?>, Character> factory = factoryFrom(new CharAt());
        final Routine<List<?>, Character> routine =
                JRoutineLoaderCompat.with(loaderFrom(getActivity())).on(factory).buildRoutine();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.asyncCall(LoaderStreamsCompat.join(channel1, channel2).buildChannels())
                          .afterMax(seconds(10))
                          .all()).containsExactly('s', '2');
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.async(
                LoaderStreamsCompat.join(Arrays.<OutputChannel<?>>asList(channel1, channel2))
                                   .buildChannels()).afterMax(seconds(10)).all()).containsExactly(
                's', '2');
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall()
                .after(millis(100))
                .pass("testtest")
                .pass("test2")
                .pass("test3")
                .close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.asyncCall(LoaderStreamsCompat.join(channel1, channel2).buildChannels())
                          .afterMax(seconds(10))
                          .all()).containsExactly('s', '2');
    }

    public void testJoinAbort() {

        final ChannelBuilder builder = JRoutineCore.io();
        final ContextInvocationFactory<List<?>, Character> factory = factoryFrom(new CharAt());
        final Routine<List<?>, Character> routine =
                JRoutineLoaderCompat.with(loaderFrom(getActivity())).on(factory).buildRoutine();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().abort();

        try {

            routine.asyncCall(LoaderStreamsCompat.join(channel1, channel2).buildChannels())
                   .afterMax(seconds(10))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().abort();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();

        try {

            routine.async(
                    LoaderStreamsCompat.join(Arrays.<OutputChannel<?>>asList(channel1, channel2))
                                       .buildChannels()).afterMax(seconds(10)).all();

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

        final ChannelBuilder builder = JRoutineCore.io();
        final ContextInvocationFactory<List<?>, Character> factory = factoryFrom(new CharAt());
        final Routine<List<?>, Character> routine =
                JRoutineLoaderCompat.with(loaderFrom(getActivity())).on(factory).buildRoutine();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.async(
                LoaderStreamsCompat.join(new Object(), channel1, channel2).buildChannels())
                          .afterMax(seconds(10))
                          .all()).containsExactly('s', '2');
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.async(
                LoaderStreamsCompat.join(null, Arrays.<OutputChannel<?>>asList(channel1, channel2))
                                   .buildChannels()).afterMax(seconds(10)).all()).containsExactly(
                's', '2');
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

            routine.async(
                    LoaderStreamsCompat.join(new Object(), channel1, channel2).buildChannels())
                   .afterMax(seconds(10))
                   .all();

            fail();

        } catch (final InvocationException ignored) {

        }
    }

    public void testJoinPlaceholderAbort() {

        final ChannelBuilder builder = JRoutineCore.io();
        final ContextInvocationFactory<List<?>, Character> factory = factoryFrom(new CharAt());
        final Routine<List<?>, Character> routine =
                JRoutineLoaderCompat.with(loaderFrom(getActivity())).on(factory).buildRoutine();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().abort();

        try {

            routine.async(
                    LoaderStreamsCompat.join((Object) null, channel1, channel2).buildChannels())
                   .afterMax(seconds(10))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().abort();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();

        try {

            routine.async(LoaderStreamsCompat.join(new Object(),
                    Arrays.<OutputChannel<?>>asList(channel1, channel2)).buildChannels())
                   .afterMax(seconds(10))
                   .all();

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

    public void testLimit() {

        final LoaderContextCompat context = loaderFrom(getActivity());
        assertThat(LoaderStreamsCompat.streamOf()
                                      .with(context)
                                      .sync()
                                      .thenGetMore(range(1, 10))
                                      .async()
                                      .map(LoaderStreamsCompat.limit(5))
                                      .afterMax(seconds(3))
                                      .all()).containsExactly(1, 2, 3, 4, 5);
        assertThat(LoaderStreamsCompat.streamOf()
                                      .with(context)
                                      .sync()
                                      .thenGetMore(range(1, 10))
                                      .async()
                                      .map(LoaderStreamsCompat.limit(0))
                                      .afterMax(seconds(3))
                                      .all()).isEmpty();
        assertThat(LoaderStreamsCompat.streamOf()
                                      .with(context)
                                      .sync()
                                      .thenGetMore(range(1, 10))
                                      .async()
                                      .map(LoaderStreamsCompat.limit(15))
                                      .afterMax(seconds(3))
                                      .all()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(LoaderStreamsCompat.streamOf()
                                      .sync()
                                      .thenGetMore(range(1, 10))
                                      .with(context)
                                      .async()
                                      .map(LoaderStreamsCompat.limit(5))
                                      .afterMax(seconds(3))
                                      .all()).containsExactly(1, 2, 3, 4, 5);
        assertThat(LoaderStreamsCompat.streamOf()
                                      .sync()
                                      .thenGetMore(range(1, 10))
                                      .with(context)
                                      .async()
                                      .map(LoaderStreamsCompat.limit(0))
                                      .afterMax(seconds(3))
                                      .all()).isEmpty();
        assertThat(LoaderStreamsCompat.streamOf()
                                      .sync()
                                      .thenGetMore(range(1, 10))
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
        LoaderStreamsCompat.streamOf("test1")
                           .with(context)
                           .loaderId(11)
                           .async()
                           .map(toUpperCase())
                           .bind();
        assertThat(JRoutineLoaderCompat.with(context)
                                       .onId(11)
                                       .buildChannel()
                                       .afterMax(seconds(10))
                                       .next()).isEqualTo("TEST1");
        LoaderStreamsCompat.streamOf("test2")
                           .with(context)
                           .loaderConfiguration()
                           .withLoaderId(21)
                           .apply()
                           .async()
                           .map(toUpperCase())
                           .bind();
        assertThat(JRoutineLoaderCompat.with(context)
                                       .onId(21)
                                       .buildChannel()
                                       .afterMax(seconds(10))
                                       .next()).isEqualTo("TEST2");
        LoaderStreamsCompat.streamOf("test3")
                           .with(context)
                           .streamLoaderConfiguration()
                           .withLoaderId(31)
                           .apply()
                           .async()
                           .map(toUpperCase())
                           .bind();
        assertThat(JRoutineLoaderCompat.with(context)
                                       .onId(31)
                                       .buildChannel()
                                       .afterMax(seconds(10))
                                       .next()).isEqualTo("TEST3");
    }

    public void testMap() {

        final ChannelBuilder builder =
                JRoutineCore.io().channelConfiguration().withOrder(OrderType.BY_CALL).apply();
        final IOChannel<String> channel1 = builder.buildChannel();
        final IOChannel<Integer> channel2 = builder.buildChannel();

        final OutputChannel<? extends ParcelableSelectable<Object>> channel =
                LoaderStreamsCompat.merge(Arrays.<OutputChannel<?>>asList(channel1, channel2))
                                   .buildChannels();
        final OutputChannel<ParcelableSelectable<Object>> output =
                JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                    .on(factoryFrom(new Sort()))
                                    .invocationConfiguration()
                                    .withInputOrder(OrderType.BY_CALL)
                                    .apply()
                                    .async(channel);
        final SparseArrayCompat<OutputChannel<Object>> channelMap =
                SparseChannelsCompat.selectParcelableOutput(output, Sort.INTEGER, Sort.STRING)
                                    .buildChannels();

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
                                      .asyncMap(null)
                                      .afterMax(seconds(10))
                                      .all()).containsExactly("0", "1", "2", "3");
        assertThat(LoaderStreamsCompat.streamOf(channelMap.get(Sort.INTEGER))
                                      .with(context)
                                      .asyncMap(null)
                                      .afterMax(seconds(10))
                                      .all()).containsExactly(0, 1, 2, 3);
    }

    public void testMerge() {

        final ChannelBuilder builder =
                JRoutineCore.io().channelConfiguration().withOrder(OrderType.BY_CALL).apply();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        OutputChannel<? extends ParcelableSelectable<?>> outputChannel;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = LoaderStreamsCompat.merge(-7, channel1, channel2).buildChannels();
        channel1.pass("test1").close();
        channel2.pass(13).close();
        assertThat(outputChannel.afterMax(seconds(10)).all()).containsOnly(
                new ParcelableSelectable<String>("test1", -7),
                new ParcelableSelectable<Integer>(13, -6));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel =
                LoaderStreamsCompat.merge(11, Arrays.<OutputChannel<?>>asList(channel1, channel2))
                                   .buildChannels();
        channel2.pass(13).close();
        channel1.pass("test1").close();
        assertThat(outputChannel.afterMax(seconds(10)).all()).containsOnly(
                new ParcelableSelectable<String>("test1", 11),
                new ParcelableSelectable<Integer>(13, 12));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = LoaderStreamsCompat.merge(channel1, channel2).buildChannels();
        channel1.pass("test2").close();
        channel2.pass(-17).close();
        assertThat(outputChannel.afterMax(seconds(10)).all()).containsOnly(
                new ParcelableSelectable<String>("test2", 0),
                new ParcelableSelectable<Integer>(-17, 1));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel =
                LoaderStreamsCompat.merge(Arrays.<OutputChannel<?>>asList(channel1, channel2))
                                   .buildChannels();
        channel1.pass("test2").close();
        channel2.pass(-17).close();
        assertThat(outputChannel.afterMax(seconds(10)).all()).containsOnly(
                new ParcelableSelectable<String>("test2", 0),
                new ParcelableSelectable<Integer>(-17, 1));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        final SparseArrayCompat<OutputChannel<?>> channelMap =
                new SparseArrayCompat<OutputChannel<?>>(2);
        channelMap.put(7, channel1);
        channelMap.put(-3, channel2);
        outputChannel = LoaderStreamsCompat.merge(channelMap).buildChannels();
        channel1.pass("test3").close();
        channel2.pass(111).close();
        assertThat(outputChannel.afterMax(seconds(10)).all()).containsOnly(
                new ParcelableSelectable<String>("test3", 7),
                new ParcelableSelectable<Integer>(111, -3));
    }

    @SuppressWarnings("unchecked")
    public void testMerge4() {

        final ChannelBuilder builder =
                JRoutineCore.io().channelConfiguration().withOrder(OrderType.BY_CALL).apply();
        final IOChannel<String> channel1 = builder.buildChannel();
        final IOChannel<String> channel2 = builder.buildChannel();
        final IOChannel<String> channel3 = builder.buildChannel();
        final IOChannel<String> channel4 = builder.buildChannel();

        final Routine<ParcelableSelectable<String>, String> routine =
                JRoutineLoaderCompat.with(loaderFrom(getActivity()))
                                    .on(factoryOf(new ClassToken<Amb<String>>() {}))
                                    .buildRoutine();
        final OutputChannel<String> outputChannel = routine.asyncCall(
                LoaderStreamsCompat.merge(Arrays.asList(channel1, channel2, channel3, channel4))
                                   .buildChannels());

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

        final ChannelBuilder builder =
                JRoutineCore.io().channelConfiguration().withOrder(OrderType.BY_CALL).apply();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        OutputChannel<? extends ParcelableSelectable<?>> outputChannel;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = LoaderStreamsCompat.merge(-7, channel1, channel2).buildChannels();
        channel1.pass("test1").close();
        channel2.abort();

        try {

            outputChannel.afterMax(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel =
                LoaderStreamsCompat.merge(11, Arrays.<OutputChannel<?>>asList(channel1, channel2))
                                   .buildChannels();
        channel2.abort();
        channel1.pass("test1").close();

        try {

            outputChannel.afterMax(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = LoaderStreamsCompat.merge(channel1, channel2).buildChannels();
        channel1.abort();
        channel2.pass(-17).close();

        try {

            outputChannel.afterMax(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel =
                LoaderStreamsCompat.merge(Arrays.<OutputChannel<?>>asList(channel1, channel2))
                                   .buildChannels();
        channel1.pass("test2").close();
        channel2.abort();

        try {

            outputChannel.afterMax(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        final SparseArrayCompat<OutputChannel<?>> channelMap =
                new SparseArrayCompat<OutputChannel<?>>(2);
        channelMap.append(7, channel1);
        channelMap.append(-3, channel2);
        outputChannel = LoaderStreamsCompat.merge(channelMap).buildChannels();
        channel1.abort();
        channel2.pass(111).close();

        try {

            outputChannel.afterMax(seconds(10)).all();

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
                                      .buildChannels()
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
            LoaderStreamsCompat.toSelectable(channel.asOutput(), 33)
                               .buildChannels()
                               .afterMax(seconds(10))
                               .all();
            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testReplay() {

        final IOChannel<Object> ioChannel = JRoutineCore.io().buildChannel();
        final OutputChannel<Object> channel = LoaderStreamsCompat.replay(ioChannel).buildChannels();
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
        final OutputChannel<Object> channel = LoaderStreamsCompat.replay(ioChannel).buildChannels();
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

    @SuppressWarnings("unchecked")
    public void testSelectMap() {

        final Routine<ParcelableSelectable<Object>, ParcelableSelectable<Object>> routine =
                JRoutineCore.on(new Sort()).buildRoutine();
        final IOChannel<ParcelableSelectable<Object>> inputChannel =
                JRoutineCore.io().buildChannel();
        final OutputChannel<ParcelableSelectable<Object>> outputChannel =
                routine.asyncCall(inputChannel);
        final StreamChannel<Object, Object> intChannel =
                LoaderStreamsCompat.selectParcelable(outputChannel, Sort.INTEGER, Sort.STRING)
                                   .channelConfiguration()
                                   .withLogLevel(Level.WARNING)
                                   .apply()
                                   .buildChannels()
                                   .get(Sort.INTEGER);
        final StreamChannel<Object, Object> strChannel =
                LoaderStreamsCompat.selectParcelable(outputChannel,
                        Arrays.asList(Sort.STRING, Sort.INTEGER))
                                   .channelConfiguration()
                                   .withLogLevel(Level.WARNING)
                                   .apply()
                                   .buildChannels()
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
        LoaderStreamsCompat.selectParcelable(Sort.STRING, 2, outputChannel).buildChannels();
        inputChannel.after(millis(100))
                    .pass(new ParcelableSelectable<Object>("test21", Sort.STRING),
                            new ParcelableSelectable<Object>(-11, Sort.INTEGER))
                    .abort();

        try {

            LoaderStreamsCompat.selectParcelable(outputChannel, Sort.STRING, Sort.INTEGER)
                               .buildChannels()
                               .get(Sort.STRING)
                               .afterMax(seconds(10))
                               .all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            LoaderStreamsCompat.selectParcelable(outputChannel, Sort.INTEGER, Sort.STRING)
                               .buildChannels()
                               .get(Sort.INTEGER)
                               .afterMax(seconds(10))
                               .all();

            fail();

        } catch (final AbortException ignored) {

        }

        inputChannel = JRoutineCore.io().buildChannel();
        outputChannel = routine.asyncCall(inputChannel);
        LoaderStreamsCompat.selectParcelable(outputChannel, Sort.INTEGER, Sort.STRING)
                           .buildChannels();
        inputChannel.after(millis(100))
                    .pass(new ParcelableSelectable<Object>(-11, Sort.INTEGER),
                            new ParcelableSelectable<Object>("test21", Sort.STRING))
                    .abort();

        try {

            LoaderStreamsCompat.selectParcelable(outputChannel, Sort.STRING, Sort.INTEGER)
                               .buildChannels()
                               .get(Sort.STRING)
                               .afterMax(seconds(10))
                               .all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            LoaderStreamsCompat.selectParcelable(outputChannel, Sort.STRING, Sort.INTEGER)
                               .buildChannels()
                               .get(Sort.INTEGER)
                               .afterMax(seconds(10))
                               .all();

            fail();

        } catch (final AbortException ignored) {

        }

        inputChannel = JRoutineCore.io().buildChannel();
        outputChannel = routine.asyncCall(inputChannel);
        LoaderStreamsCompat.selectParcelable(outputChannel,
                Arrays.asList(Sort.STRING, Sort.INTEGER)).buildChannels();
        inputChannel.after(millis(100))
                    .pass(new ParcelableSelectable<Object>("test21", Sort.STRING),
                            new ParcelableSelectable<Object>(-11, Sort.INTEGER))
                    .abort();

        try {

            LoaderStreamsCompat.selectParcelable(outputChannel, Sort.INTEGER, Sort.STRING)
                               .buildChannels()
                               .get(Sort.STRING)
                               .afterMax(seconds(10))
                               .all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            LoaderStreamsCompat.selectParcelable(outputChannel, Sort.INTEGER, Sort.STRING)
                               .buildChannels()
                               .get(Sort.INTEGER)
                               .afterMax(seconds(10))
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
                                      .thenGetMore(range(1, 10))
                                      .async()
                                      .map(LoaderStreamsCompat.skip(5))
                                      .afterMax(seconds(3))
                                      .all()).containsExactly(6, 7, 8, 9, 10);
        assertThat(LoaderStreamsCompat.streamOf()
                                      .with(context)
                                      .sync()
                                      .thenGetMore(range(1, 10))
                                      .async()
                                      .map(LoaderStreamsCompat.skip(15))
                                      .afterMax(seconds(3))
                                      .all()).isEmpty();
        assertThat(LoaderStreamsCompat.streamOf()
                                      .with(context)
                                      .sync()
                                      .thenGetMore(range(1, 10))
                                      .async()
                                      .map(LoaderStreamsCompat.skip(0))
                                      .afterMax(seconds(3))
                                      .all()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(LoaderStreamsCompat.streamOf()
                                      .sync()
                                      .thenGetMore(range(1, 10))
                                      .with(context)
                                      .async()
                                      .map(LoaderStreamsCompat.skip(5))
                                      .afterMax(seconds(3))
                                      .all()).containsExactly(6, 7, 8, 9, 10);
        assertThat(LoaderStreamsCompat.streamOf()
                                      .sync()
                                      .thenGetMore(range(1, 10))
                                      .with(context)
                                      .async()
                                      .map(LoaderStreamsCompat.skip(15))
                                      .afterMax(seconds(3))
                                      .all()).isEmpty();
        assertThat(LoaderStreamsCompat.streamOf()
                                      .sync()
                                      .thenGetMore(range(1, 10))
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
                           .map(function)
                           .immediately();
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
                                      .staleAfter(zero())
                                      .async()
                                      .map(function)
                                      .afterMax(seconds(10))
                                      .next()).isEqualTo("test2");
        seconds(5).sleepAtLeast();
        LoaderStreamsCompat.streamOf("test")
                           .with(context)
                           .cache(CacheStrategyType.CACHE_IF_SUCCESS)
                           .async()
                           .map(function)
                           .immediately();
        seconds(5).sleepAtLeast();
        assertThat(LoaderStreamsCompat.streamOf("test")
                                      .with(context)
                                      .staleAfter(zero())
                                      .async()
                                      .map(function)
                                      .afterMax(seconds(10))
                                      .next()).isEqualTo("test4");
    }

    private static class Amb<DATA> extends TemplateInvocation<ParcelableSelectable<DATA>, DATA> {

        private static final int NO_INDEX = Integer.MIN_VALUE;

        private int mFirstIndex;

        @Override
        public void onRecycle() {

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

    private static class CharAt extends MappingInvocation<List<?>, Character> {

        /**
         * Constructor.
         */
        protected CharAt() {

            super(null);
        }

        public void onInput(final List<?> objects, @NotNull final ResultChannel<Character> result) {

            final String text = (String) objects.get(0);
            final int index = ((Integer) objects.get(1));
            result.pass(text.charAt(index));
        }
    }

    private static class Sort
            extends MappingInvocation<ParcelableSelectable<Object>, ParcelableSelectable<Object>> {

        private static final int INTEGER = 1;

        private static final int STRING = 0;

        /**
         * Constructor.
         */
        protected Sort() {

            super(null);
        }

        public void onInput(final ParcelableSelectable<Object> selectable,
                @NotNull final ResultChannel<ParcelableSelectable<Object>> result) {

            switch (selectable.index) {

                case INTEGER:
                    SparseChannelsCompat.<Object, Integer>selectParcelableOutput(result,
                            INTEGER).buildChannels().pass(selectable.<Integer>data()).close();
                    break;

                case STRING:
                    SparseChannelsCompat.<Object, String>selectParcelableOutput(result,
                            STRING).buildChannels().pass(selectable.<String>data()).close();
                    break;
            }
        }
    }
}
