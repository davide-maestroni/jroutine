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

import com.github.dm.jrt.android.channel.ParcelableSelectable;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.CacheStrategyType;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.core.invocation.IdentityContextInvocation;
import com.github.dm.jrt.android.core.invocation.MissingLoaderException;
import com.github.dm.jrt.android.v11.channel.SparseChannels;
import com.github.dm.jrt.android.v11.core.JRoutineLoader;
import com.github.dm.jrt.android.v11.core.LoaderContext;
import com.github.dm.jrt.channel.Selectable;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.ChannelBuilder;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
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
import static com.github.dm.jrt.android.v11.core.LoaderContext.loaderFrom;
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

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final LoaderContext context = loaderFrom(getActivity());
        StreamChannel<String, String> channel1 =
                LoaderStreams.streamOf("test1", "test2", "test3").with(context).asyncMap(null);
        StreamChannel<String, String> channel2 =
                LoaderStreams.streamOf("test4", "test5", "test6").with(context).asyncMap(null);
        assertThat(LoaderStreams.blend(channel2, channel1).buildChannels().after(seconds(10)).all())
                .containsOnly("test1", "test2", "test3", "test4", "test5", "test6");
        channel1 = LoaderStreams.streamOf("test1", "test2", "test3").with(context).asyncMap(null);
        channel2 = LoaderStreams.streamOf("test4", "test5", "test6").with(context).asyncMap(null);
        assertThat(LoaderStreams.blend(Arrays.<StreamChannel<?, ?>>asList(channel1, channel2))
                                .buildChannels()
                                .after(seconds(10))
                                .all()).containsOnly("test1", "test2", "test3", "test4", "test5",
                "test6");
    }

    public void testBlendAbort() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final ChannelBuilder builder = JRoutineCore.io();
        final Routine<Object, Object> routine = JRoutineLoader.with(loaderFrom(getActivity()))
                                                              .on(IdentityContextInvocation
                                                                      .factoryOf())
                                                              .buildRoutine();
        Channel<String, String> channel1;
        Channel<Integer, Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().abort();

        try {

            routine.async(LoaderStreams.blend(channel1, channel2).buildChannels())
                   .after(seconds(10))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().abort();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();

        try {

            routine.async(LoaderStreams.blend(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                                       .buildChannels()).after(seconds(10)).all();

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

            LoaderStreams.blend();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            LoaderStreams.blend((Channel<?, ?>[]) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            LoaderStreams.blend(new Channel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            LoaderStreams.blend(Collections.<Channel<?, ?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            LoaderStreams.blend((List<Channel<?, ?>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            LoaderStreams.blend(Collections.<Channel<?, ?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testBuilder() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final LoaderContext context = loaderFrom(getActivity());
        assertThat(LoaderStreams.streamOf("test")
                                .with(context)
                                .asyncMap(null)
                                .after(seconds(10))
                                .all()).containsExactly("test");
        assertThat(LoaderStreams.streamOf("test1", "test2", "test3")
                                .with(context)
                                .asyncMap(null)
                                .after(seconds(10))
                                .all()).containsExactly("test1", "test2", "test3");
        assertThat(LoaderStreams.streamOf(Arrays.asList("test1", "test2", "test3"))
                                .with(context)
                                .asyncMap(null)
                                .after(seconds(10))
                                .all()).containsExactly("test1", "test2", "test3");
        assertThat(LoaderStreams.streamOf(JRoutineCore.io().of("test1", "test2", "test3"))
                                .with(context)
                                .asyncMap(null)
                                .after(seconds(10))
                                .all()).containsExactly("test1", "test2", "test3");
    }

    public void testCombine() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        final Channel<String, String> channel1 = JRoutineLoader.with(loaderFrom(getActivity()))
                                                               .on(IdentityContextInvocation
                                                                       .<String>factoryOf())
                                                               .async()
                                                               .orderByCall();
        final Channel<Integer, Integer> channel2 = JRoutineLoader.with(loaderFrom(getActivity()))
                                                                 .on(IdentityContextInvocation
                                                                         .<Integer>factoryOf())
                                                                 .async()
                                                                 .orderByCall();
        LoaderStreams.combine(channel1, channel2)
                     .buildChannels()
                     .pass(new ParcelableSelectable<Object>("test1", 0))
                     .pass(new ParcelableSelectable<Integer>(1, 1))
                     .close();
        LoaderStreams.combine(3, channel1, channel2)
                     .buildChannels()
                     .pass(new ParcelableSelectable<String>("test2", 3))
                     .pass(new ParcelableSelectable<Integer>(2, 4))
                     .close();
        LoaderStreams.combine(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                     .buildChannels()
                     .pass(new ParcelableSelectable<String>("test3", 0))
                     .pass(new ParcelableSelectable<Integer>(3, 1))
                     .close();
        LoaderStreams.combine(-5, Arrays.<Channel<?, ?>>asList(channel1, channel2))
                     .buildChannels()
                     .pass(new ParcelableSelectable<String>("test4", -5))
                     .pass(new ParcelableSelectable<Integer>(4, -4))
                     .close();
        final HashMap<Integer, Channel<?, ?>> map = new HashMap<Integer, Channel<?, ?>>(2);
        map.put(31, channel1);
        map.put(17, channel2);
        LoaderStreams.combine(map)
                     .buildChannels()
                     .pass(new ParcelableSelectable<String>("test5", 31))
                     .pass(new ParcelableSelectable<Integer>(5, 17))
                     .close();
        final SparseArray<Channel<?, ?>> sparseArray = new SparseArray<Channel<?, ?>>(2);
        sparseArray.put(31, channel1);
        sparseArray.put(17, channel2);
        LoaderStreams.combine(sparseArray)
                     .buildChannels()
                     .pass(new ParcelableSelectable<String>("test6", 31))
                     .pass(new ParcelableSelectable<Integer>(6, 17))
                     .close();
        assertThat(channel1.close().after(seconds(10)).all()).containsExactly("test1", "test2",
                "test3", "test4", "test5", "test6");
        assertThat(channel2.close().after(seconds(10)).all()).containsExactly(1, 2, 3, 4, 5, 6);
    }

    public void testConcat() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final LoaderContext context = loaderFrom(getActivity());
        StreamChannel<String, String> channel1 =
                LoaderStreams.streamOf("test1", "test2", "test3").with(context).asyncMap(null);
        StreamChannel<String, String> channel2 =
                LoaderStreams.streamOf("test4", "test5", "test6").with(context).asyncMap(null);
        assertThat(LoaderStreams.concat(channel2, channel1)
                                .buildChannels()
                                .after(seconds(10))
                                .all()).containsExactly("test4", "test5", "test6", "test1", "test2",
                "test3");
        channel1 = LoaderStreams.streamOf("test1", "test2", "test3").with(context).asyncMap(null);
        channel2 = LoaderStreams.streamOf("test4", "test5", "test6").with(context).asyncMap(null);
        assertThat(LoaderStreams.concat(Arrays.<StreamChannel<?, ?>>asList(channel1, channel2))
                                .buildChannels()
                                .after(seconds(10))
                                .all()).containsExactly("test1", "test2", "test3", "test4", "test5",
                "test6");
    }

    public void testConcatAbort() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final ChannelBuilder builder = JRoutineCore.io();
        final Routine<Object, Object> routine = JRoutineLoader.with(loaderFrom(getActivity()))
                                                              .on(IdentityContextInvocation
                                                                      .factoryOf())
                                                              .buildRoutine();
        Channel<String, String> channel1;
        Channel<Integer, Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().abort();

        try {

            routine.async(LoaderStreams.concat(channel1, channel2).buildChannels())
                   .after(seconds(10))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().abort();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();

        try {

            routine.async(LoaderStreams.concat(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                                       .buildChannels()).after(seconds(10)).all();

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

            LoaderStreams.concat();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            LoaderStreams.concat((Channel<?, ?>[]) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            LoaderStreams.concat(new Channel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            LoaderStreams.concat(Collections.<Channel<?, ?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            LoaderStreams.concat((List<Channel<?, ?>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            LoaderStreams.concat(Collections.<Channel<?, ?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testConfiguration() {

        final LoaderStreamChannel<String, String> channel1 =
                LoaderStreams.streamOf("test1", "test2", "test3");
        final LoaderStreamChannel<String, String> channel2 =
                LoaderStreams.streamOf("test4", "test5", "test6");
        assertThat(LoaderStreams.blend(channel2, channel1)
                                .channelConfiguration()
                                .withOrder(OrderType.BY_CALL)
                                .withOutputTimeout(seconds(10))
                                .apply()
                                .buildChannels()
                                .all()).containsExactly("test4", "test5", "test6", "test1", "test2",
                "test3");
    }

    public void testConstructor() {

        boolean failed = false;
        try {
            new LoaderStreams();
            failed = true;

        } catch (final Throwable ignored) {

        }

        assertThat(failed).isFalse();
    }

    public void testDistribute() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        final Channel<String, String> channel1 = JRoutineLoader.with(loaderFrom(getActivity()))
                                                               .on(IdentityContextInvocation
                                                                       .<String>factoryOf())
                                                               .async()
                                                               .orderByCall();
        final Channel<String, String> channel2 = JRoutineLoader.with(loaderFrom(getActivity()))
                                                               .on(IdentityContextInvocation
                                                                       .<String>factoryOf())
                                                               .async()
                                                               .orderByCall();
        LoaderStreams.distribute(channel1, channel2)
                     .buildChannels()
                     .pass(Arrays.asList("test1-1", "test1-2"))
                     .close();
        LoaderStreams.distribute(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                     .buildChannels()
                     .pass(Arrays.asList("test2-1", "test2-2"))
                     .close();
        LoaderStreams.distribute(channel1, channel2)
                     .buildChannels()
                     .pass(Collections.singletonList("test3-1"))
                     .close();
        assertThat(channel1.close().after(seconds(10)).all()).containsExactly("test1-1", "test2-1",
                "test3-1");
        assertThat(channel2.close().after(seconds(10)).all()).containsExactly("test1-2", "test2-2");
    }

    public void testDistributePlaceholder() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        final Channel<String, String> channel1 = JRoutineLoader.with(loaderFrom(getActivity()))
                                                               .on(IdentityContextInvocation
                                                                       .<String>factoryOf())
                                                               .async()
                                                               .orderByCall();
        final Channel<String, String> channel2 = JRoutineLoader.with(loaderFrom(getActivity()))
                                                               .on(IdentityContextInvocation
                                                                       .<String>factoryOf())
                                                               .async()
                                                               .orderByCall();
        LoaderStreams.distribute((Object) null, channel1, channel2)
                     .buildChannels()
                     .pass(Arrays.asList("test1-1", "test1-2"))
                     .close();
        final String placeholder = "placeholder";
        LoaderStreams.distribute((Object) placeholder,
                Arrays.<Channel<?, ?>>asList(channel1, channel2))
                     .buildChannels()
                     .pass(Arrays.asList("test2-1", "test2-2"))
                     .close();
        LoaderStreams.distribute(placeholder, channel1, channel2)
                     .buildChannels()
                     .pass(Collections.singletonList("test3-1"))
                     .close();
        assertThat(channel1.close().after(seconds(10)).all()).containsExactly("test1-1", "test2-1",
                "test3-1");
        assertThat(channel2.close().after(seconds(10)).all()).containsExactly("test1-2", "test2-2",
                placeholder);
    }

    public void testFactory() {

        assertThat(LoaderStreams.onStream(toUpperCaseChannel())
                                .async("test1", "test2", "test3")
                                .after(seconds(3))
                                .all()).containsExactly("TEST1", "TEST2", "TEST3");

        try {

            final Channel<String, String> channel =
                    LoaderStreams.onStream(toUpperCaseChannel()).async();
            channel.abort(new IllegalArgumentException());
            channel.close().after(seconds(3)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final ContextInvocationFactory<String, String> factory =
                LoaderStreams.contextFactory(toUpperCaseChannel());
        assertThat(JRoutineLoader.with(loaderFrom(getActivity()))
                                 .on(factory)
                                 .async("test1", "test2", "test3")
                                 .after(seconds(3))
                                 .all()).containsExactly("TEST1", "TEST2", "TEST3");

        try {

            final Channel<String, String> channel =
                    JRoutineLoader.with(loaderFrom(getActivity())).on(factory).async();
            channel.abort(new IllegalArgumentException());
            channel.close().after(seconds(3)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        assertThat(LoaderStreams.onStreamWith(loaderFrom(getActivity()), toUpperCaseChannel())
                                .async("test1", "test2", "test3")
                                .after(seconds(3))
                                .all()).containsExactly("TEST1", "TEST2", "TEST3");

        try {

            final Channel<String, String> channel =
                    LoaderStreams.onStreamWith(loaderFrom(getActivity()), toUpperCaseChannel())
                                 .async();
            channel.abort(new IllegalArgumentException());
            channel.close().after(seconds(3)).next();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testFactoryEquals() {

        final Function<StreamChannel<String, String>, StreamChannel<String, String>> function =
                toUpperCaseChannel();
        final ContextInvocationFactory<String, String> factory =
                LoaderStreams.contextFactory(function);
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(null);
        assertThat(factory).isNotEqualTo("test");
        assertThat(factory).isNotEqualTo(
                LoaderStreams.contextFactory(Functions.<StreamChannel<Object, Object>>identity()));
        assertThat(factory).isEqualTo(LoaderStreams.contextFactory(function));
        assertThat(factory.hashCode()).isEqualTo(LoaderStreams.contextFactory(function).hashCode());
    }

    @SuppressWarnings("ConstantConditions")
    public void testFactoryError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        try {

            LoaderStreams.contextFactory(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            LoaderStreams.onStreamWith(null, Functions.<StreamChannel<Object, Object>>identity());

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            LoaderStreams.onStreamWith(loaderFrom(getActivity()), null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testFactoryId() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final LoaderContext context = loaderFrom(getActivity());
        LoaderStreams.streamOf("test1").with(context).factoryId(11).async().map(toUpperCase());

        try {
            JRoutineLoader.with(context).onId(11).buildChannel().after(seconds(10)).next();
            fail();

        } catch (final MissingLoaderException ignored) {

        }

        assertThat(LoaderStreams.streamOf("test2")
                                .with(context)
                                .loaderConfiguration()
                                .withFactoryId(11)
                                .apply()
                                .async()
                                .map(toUpperCase())
                                .after(seconds(10))
                                .next()).isEqualTo("TEST2");
        final AtomicInteger count = new AtomicInteger();
        LoaderStreams.streamOf().with(context).factoryId(11).thenGet(delayedIncrement(count));
        assertThat(LoaderStreams.streamOf()
                                .with(context)
                                .factoryId(11)
                                .thenGet(increment(count))
                                .after(seconds(10))
                                .next()).isEqualTo(1);
    }

    @SuppressWarnings("unchecked")
    public void testGroupBy() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final LoaderContext context = loaderFrom(getActivity());
        assertThat(LoaderStreams.streamOf()
                                .with(context)
                                .sync()
                                .thenGetMore(range(1, 10))
                                .async()
                                .map(LoaderStreams.<Number>groupBy(3))
                                .after(seconds(3))
                                .all()).containsExactly(Arrays.<Number>asList(1, 2, 3),
                Arrays.<Number>asList(4, 5, 6), Arrays.<Number>asList(7, 8, 9),
                Collections.<Number>singletonList(10));
        assertThat(LoaderStreams.streamOf()
                                .with(context)
                                .sync()
                                .thenGetMore(range(1, 10))
                                .async()
                                .map(LoaderStreams.<Number>groupBy(13))
                                .after(seconds(3))
                                .all()).containsExactly(
                Arrays.<Number>asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        assertThat(LoaderStreams.streamOf()
                                .sync()
                                .thenGetMore(range(1, 10))
                                .with(context)
                                .async()
                                .map(LoaderStreams.<Number>groupBy(3))
                                .after(seconds(3))
                                .all()).containsExactly(Arrays.<Number>asList(1, 2, 3),
                Arrays.<Number>asList(4, 5, 6), Arrays.<Number>asList(7, 8, 9),
                Collections.<Number>singletonList(10));
        assertThat(LoaderStreams.streamOf()
                                .sync()
                                .thenGetMore(range(1, 10))
                                .with(context)
                                .async()
                                .map(LoaderStreams.<Number>groupBy(13))
                                .after(seconds(3))
                                .all()).containsExactly(
                Arrays.<Number>asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
    }

    public void testGroupByEquals() {

        final InvocationFactory<Object, List<Object>> factory = LoaderStreams.groupBy(2);
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(null);
        assertThat(factory).isNotEqualTo("test");
        assertThat(factory).isNotEqualTo(LoaderStreams.groupBy(3));
        assertThat(factory).isEqualTo(LoaderStreams.groupBy(2));
        assertThat(factory.hashCode()).isEqualTo(LoaderStreams.groupBy(2).hashCode());
    }

    public void testGroupByError() {

        try {

            LoaderStreams.groupBy(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            LoaderStreams.groupBy(0);

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
        assertThat(LoaderStreams.streamOf()
                                .with(context)
                                .sync()
                                .thenGetMore(range(1, 10))
                                .async()
                                .map(LoaderStreams.<Number>groupBy(3, 0))
                                .after(seconds(3))
                                .all()).containsExactly(Arrays.<Number>asList(1, 2, 3),
                Arrays.<Number>asList(4, 5, 6), Arrays.<Number>asList(7, 8, 9),
                Arrays.<Number>asList(10, 0, 0));
        assertThat(LoaderStreams.streamOf()
                                .with(context)
                                .sync()
                                .thenGetMore(range(1, 10))
                                .async()
                                .map(LoaderStreams.<Number>groupBy(13, -1))
                                .after(seconds(3))
                                .all()).containsExactly(
                Arrays.<Number>asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, -1, -1, -1));
        assertThat(LoaderStreams.streamOf()
                                .sync()
                                .thenGetMore(range(1, 10))
                                .with(context)
                                .async()
                                .map(LoaderStreams.<Number>groupBy(3, -31))
                                .after(seconds(3))
                                .all()).containsExactly(Arrays.<Number>asList(1, 2, 3),
                Arrays.<Number>asList(4, 5, 6), Arrays.<Number>asList(7, 8, 9),
                Arrays.<Number>asList(10, -31, -31));
        assertThat(LoaderStreams.streamOf()
                                .sync()
                                .thenGetMore(range(1, 10))
                                .with(context)
                                .async()
                                .map(LoaderStreams.<Number>groupBy(13, 71))
                                .after(seconds(3))
                                .all()).containsExactly(
                Arrays.<Number>asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 71, 71, 71));
    }

    public void testGroupByPlaceholderEquals() {

        final Object placeholder = -11;
        final InvocationFactory<Object, List<Object>> factory =
                LoaderStreams.groupBy(2, placeholder);
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(null);
        assertThat(factory).isNotEqualTo("test");
        assertThat(factory).isNotEqualTo(LoaderStreams.groupBy(3, -11));
        assertThat(factory).isEqualTo(LoaderStreams.groupBy(2, -11));
        assertThat(factory.hashCode()).isEqualTo(LoaderStreams.groupBy(2, -11).hashCode());
    }

    public void testGroupByPlaceholderError() {

        try {

            LoaderStreams.groupBy(-1, 77);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            LoaderStreams.groupBy(0, null);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testInputMap() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        final ArrayList<ParcelableSelectable<Object>> outputs =
                new ArrayList<ParcelableSelectable<Object>>();
        outputs.add(new ParcelableSelectable<Object>("test21", Sort.STRING));
        outputs.add(new ParcelableSelectable<Object>(-11, Sort.INTEGER));
        final Routine<ParcelableSelectable<Object>, ParcelableSelectable<Object>> routine =
                JRoutineLoader.with(loaderFrom(getActivity()))
                              .on(factoryOf(Sort.class))
                              .buildRoutine();
        SparseArray<Channel<Object, ?>> channelMap;
        Channel<ParcelableSelectable<Object>, ParcelableSelectable<Object>> channel;
        channel = routine.async();
        channelMap = LoaderStreams.selectParcelableInput(channel,
                Arrays.asList(Sort.INTEGER, Sort.STRING)).buildChannels();
        channelMap.get(Sort.INTEGER).pass(-11).close();
        channelMap.get(Sort.STRING).pass("test21").close();
        assertThat(channel.close().after(seconds(10)).all()).containsOnlyElementsOf(outputs);
        channel = routine.async();
        channelMap = LoaderStreams.selectParcelableInput(channel, Sort.INTEGER, Sort.STRING)
                                  .buildChannels();
        channelMap.get(Sort.INTEGER).pass(-11).close();
        channelMap.get(Sort.STRING).pass("test21").close();
        assertThat(channel.close().after(seconds(10)).all()).containsOnlyElementsOf(outputs);
        channel = routine.async();
        channelMap =
                LoaderStreams.selectParcelableInput(Math.min(Sort.INTEGER, Sort.STRING), 2, channel)
                             .buildChannels();
        channelMap.get(Sort.INTEGER).pass(-11).close();
        channelMap.get(Sort.STRING).pass("test21").close();
        assertThat(channel.close().after(seconds(10)).all()).containsOnlyElementsOf(outputs);
    }

    @SuppressWarnings("unchecked")
    public void testInputSelect() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        Channel<Selectable<String>, Selectable<String>> channel = JRoutineCore.io().buildChannel();
        LoaderStreams.selectInput(channel, 33)
                     .buildChannels()
                     .pass("test1", "test2", "test3")
                     .close();
        assertThat(channel.close().after(seconds(10)).all()).containsExactly(
                new ParcelableSelectable<String>("test1", 33),
                new ParcelableSelectable<String>("test2", 33),
                new ParcelableSelectable<String>("test3", 33));
        channel = JRoutineCore.io().buildChannel();
        Map<Integer, Channel<String, ?>> channelMap =
                LoaderStreams.selectInput(channel, Arrays.asList(1, 2, 3)).buildChannels();
        channelMap.get(1).pass("test1").close();
        channelMap.get(2).pass("test2").close();
        channelMap.get(3).pass("test3").close();
        assertThat(channel.close().after(seconds(10)).all()).containsExactly(
                new ParcelableSelectable<String>("test1", 1),
                new ParcelableSelectable<String>("test2", 2),
                new ParcelableSelectable<String>("test3", 3));
        channel = JRoutineCore.io().buildChannel();
        channelMap = LoaderStreams.selectInput(channel, 1, 2, 3).buildChannels();
        channelMap.get(1).pass("test1").close();
        channelMap.get(2).pass("test2").close();
        channelMap.get(3).pass("test3").close();
        assertThat(channel.close().after(seconds(10)).all()).containsExactly(
                new ParcelableSelectable<String>("test1", 1),
                new ParcelableSelectable<String>("test2", 2),
                new ParcelableSelectable<String>("test3", 3));
        channel = JRoutineCore.io().buildChannel();
        channelMap = LoaderStreams.selectInput(1, 3, channel).buildChannels();
        channelMap.get(1).pass("test1").close();
        channelMap.get(2).pass("test2").close();
        channelMap.get(3).pass("test3").close();
        assertThat(channel.close().after(seconds(10)).all()).containsExactly(
                new ParcelableSelectable<String>("test1", 1),
                new ParcelableSelectable<String>("test2", 2),
                new ParcelableSelectable<String>("test3", 3));
        channel = JRoutineCore.io().buildChannel();
        LoaderStreams.selectInput(channel, 33)
                     .buildChannels()
                     .pass("test1", "test2", "test3")
                     .close();
        channel.close();
        assertThat(channel.after(seconds(10)).all()).containsExactly(
                new ParcelableSelectable<String>("test1", 33),
                new ParcelableSelectable<String>("test2", 33),
                new ParcelableSelectable<String>("test3", 33));
    }

    @SuppressWarnings("unchecked")
    public void testInputToSelectable() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        final Channel<String, String> channel = JRoutineCore.io().buildChannel();
        LoaderStreams.selectableInput(channel, 33)
                     .buildChannels()
                     .pass(new Selectable<String>("test1", 33),
                             new Selectable<String>("test2", -33),
                             new Selectable<String>("test3", 33),
                             new Selectable<String>("test4", 333))
                     .close();
        assertThat(channel.close().after(seconds(10)).all()).containsExactly("test1", "test3");
    }

    public void testJoin() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final ChannelBuilder builder = JRoutineCore.io();
        final Routine<List<?>, Character> routine = JRoutineLoader.with(loaderFrom(getActivity()))
                                                                  .on(factoryFrom(new CharAt()))
                                                                  .buildRoutine();
        Channel<String, String> channel1;
        Channel<Integer, Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.async(LoaderStreams.join(channel1, channel2).buildChannels())
                          .after(seconds(10))
                          .all()).containsExactly('s', '2');
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.async(
                LoaderStreams.join(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                             .buildChannels()).after(seconds(10)).all()).containsExactly('s', '2');
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall()
                .after(millis(100))
                .pass("testtest")
                .pass("test2")
                .pass("test3")
                .close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.async(LoaderStreams.join(channel1, channel2).buildChannels())
                          .after(seconds(10))
                          .all()).containsExactly('s', '2');
    }

    public void testJoinAbort() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final ChannelBuilder builder = JRoutineCore.io();
        final Routine<List<?>, Character> routine = JRoutineLoader.with(loaderFrom(getActivity()))
                                                                  .on(factoryFrom(new CharAt()))
                                                                  .buildRoutine();
        Channel<String, String> channel1;
        Channel<Integer, Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().abort();

        try {

            routine.async(LoaderStreams.join(channel1, channel2).buildChannels())
                   .after(seconds(10))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().abort();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();

        try {

            routine.async(LoaderStreams.join(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                                       .buildChannels()).after(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testJoinError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        try {

            LoaderStreams.join();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            LoaderStreams.join(Collections.<Channel<?, ?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            LoaderStreams.join(new Channel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            LoaderStreams.join(Collections.<Channel<?, ?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testJoinPlaceholder() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final ChannelBuilder builder = JRoutineCore.io();
        final Routine<List<?>, Character> routine = JRoutineLoader.with(loaderFrom(getActivity()))
                                                                  .on(factoryFrom(new CharAt()))
                                                                  .buildRoutine();
        Channel<String, String> channel1;
        Channel<Integer, Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(
                routine.async(LoaderStreams.join(new Object(), channel1, channel2).buildChannels())
                       .after(seconds(10))
                       .all()).containsExactly('s', '2');
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.async(
                LoaderStreams.join(null, Arrays.<Channel<?, ?>>asList(channel1, channel2))
                             .buildChannels()).after(seconds(10)).all()).containsExactly('s', '2');
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

            routine.async(LoaderStreams.join(new Object(), channel1, channel2).buildChannels())
                   .after(seconds(10))
                   .all();

            fail();

        } catch (final InvocationException ignored) {

        }
    }

    public void testJoinPlaceholderAbort() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final ChannelBuilder builder = JRoutineCore.io();
        final Routine<List<?>, Character> routine = JRoutineLoader.with(loaderFrom(getActivity()))
                                                                  .on(factoryFrom(new CharAt()))
                                                                  .buildRoutine();
        Channel<String, String> channel1;
        Channel<Integer, Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().abort();

        try {

            routine.async(LoaderStreams.join((Object) null, channel1, channel2).buildChannels())
                   .after(seconds(10))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().abort();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();

        try {

            routine.async(LoaderStreams.join(new Object(),
                    Arrays.<Channel<?, ?>>asList(channel1, channel2)).buildChannels())
                   .after(seconds(10))
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

            LoaderStreams.join(new Object());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            LoaderStreams.join(null, Collections.<Channel<?, ?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            LoaderStreams.join(new Object(), new Channel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            LoaderStreams.join(new Object(), Collections.<Channel<?, ?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testLimit() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final LoaderContext context = loaderFrom(getActivity());
        assertThat(LoaderStreams.streamOf()
                                .with(context)
                                .sync()
                                .thenGetMore(range(1, 10))
                                .async()
                                .map(LoaderStreams.limit(5))
                                .after(seconds(3))
                                .all()).containsExactly(1, 2, 3, 4, 5);
        assertThat(LoaderStreams.streamOf()
                                .with(context)
                                .sync()
                                .thenGetMore(range(1, 10))
                                .async()
                                .map(LoaderStreams.limit(0))
                                .after(seconds(3))
                                .all()).isEmpty();
        assertThat(LoaderStreams.streamOf()
                                .with(context)
                                .sync()
                                .thenGetMore(range(1, 10))
                                .async()
                                .map(LoaderStreams.limit(15))
                                .after(seconds(3))
                                .all()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(LoaderStreams.streamOf()
                                .sync()
                                .thenGetMore(range(1, 10))
                                .with(context)
                                .async()
                                .map(LoaderStreams.limit(5))
                                .after(seconds(3))
                                .all()).containsExactly(1, 2, 3, 4, 5);
        assertThat(LoaderStreams.streamOf()
                                .sync()
                                .thenGetMore(range(1, 10))
                                .with(context)
                                .async()
                                .map(LoaderStreams.limit(0))
                                .after(seconds(3))
                                .all()).isEmpty();
        assertThat(LoaderStreams.streamOf()
                                .sync()
                                .thenGetMore(range(1, 10))
                                .with(context)
                                .async()
                                .map(LoaderStreams.limit(15))
                                .after(seconds(3))
                                .all()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    public void testLimitEquals() {

        final InvocationFactory<Object, Object> factory = LoaderStreams.limit(2);
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(null);
        assertThat(factory).isNotEqualTo("test");
        assertThat(factory).isNotEqualTo(LoaderStreams.limit(3));
        assertThat(factory).isEqualTo(LoaderStreams.limit(2));
        assertThat(factory.hashCode()).isEqualTo(LoaderStreams.limit(2).hashCode());
    }

    public void testLimitError() {

        try {

            LoaderStreams.limit(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testLoaderId() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final LoaderContext context = loaderFrom(getActivity());
        LoaderStreams.streamOf("test1")
                     .with(context)
                     .loaderId(11)
                     .async()
                     .map(toUpperCase())
                     .bind();
        assertThat(JRoutineLoader.with(context)
                                 .onId(11)
                                 .buildChannel()
                                 .after(seconds(10))
                                 .next()).isEqualTo("TEST1");
        LoaderStreams.streamOf("test2")
                     .with(context)
                     .loaderConfiguration()
                     .withLoaderId(21)
                     .apply()
                     .async()
                     .map(toUpperCase())
                     .bind();
        assertThat(JRoutineLoader.with(context)
                                 .onId(21)
                                 .buildChannel()
                                 .after(seconds(10))
                                 .next()).isEqualTo("TEST2");
        LoaderStreams.streamOf("test3")
                     .with(context)
                     .streamLoaderConfiguration()
                     .withLoaderId(31)
                     .apply()
                     .async()
                     .map(toUpperCase())
                     .bind();
        assertThat(JRoutineLoader.with(context)
                                 .onId(31)
                                 .buildChannel()
                                 .after(seconds(10))
                                 .next()).isEqualTo("TEST3");
    }

    public void testMap() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final ChannelBuilder builder =
                JRoutineCore.io().channelConfiguration().withOrder(OrderType.BY_CALL).apply();
        final Channel<String, String> channel1 = builder.buildChannel();
        final Channel<Integer, Integer> channel2 = builder.buildChannel();

        final Channel<?, ? extends ParcelableSelectable<Object>> channel =
                LoaderStreams.merge(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                             .buildChannels();
        final Channel<?, ParcelableSelectable<Object>> output =
                JRoutineLoader.with(loaderFrom(getActivity()))
                              .on(factoryFrom(new Sort()))
                              .invocationConfiguration()
                              .withInputOrder(OrderType.BY_CALL)
                              .apply()
                              .async(channel);
        final SparseArray<Channel<?, Object>> channelMap =
                SparseChannels.selectParcelableOutput(output, Sort.INTEGER, Sort.STRING)
                              .buildChannels();

        for (int i = 0; i < 4; i++) {

            final String input = Integer.toString(i);
            channel1.after(millis(20)).pass(input);
            channel2.after(millis(20)).pass(i);
        }

        channel1.close();
        channel2.close();

        final LoaderContext context = loaderFrom(getActivity());
        assertThat(LoaderStreams.streamOf(channelMap.get(Sort.STRING))
                                .with(context)
                                .asyncMap(null)
                                .after(seconds(10))
                                .all()).containsExactly("0", "1", "2", "3");
        assertThat(LoaderStreams.streamOf(channelMap.get(Sort.INTEGER))
                                .with(context)
                                .asyncMap(null)
                                .after(seconds(10))
                                .all()).containsExactly(0, 1, 2, 3);
    }

    public void testMerge() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final ChannelBuilder builder =
                JRoutineCore.io().channelConfiguration().withOrder(OrderType.BY_CALL).apply();
        Channel<String, String> channel1;
        Channel<Integer, Integer> channel2;
        Channel<?, ? extends ParcelableSelectable<?>> outputChannel;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = LoaderStreams.merge(-7, channel1, channel2).buildChannels();
        channel1.pass("test1").close();
        channel2.pass(13).close();
        assertThat(outputChannel.after(seconds(10)).all()).containsOnly(
                new ParcelableSelectable<String>("test1", -7),
                new ParcelableSelectable<Integer>(13, -6));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = LoaderStreams.merge(11, Arrays.<Channel<?, ?>>asList(channel1, channel2))
                                     .buildChannels();
        channel2.pass(13).close();
        channel1.pass("test1").close();
        assertThat(outputChannel.after(seconds(10)).all()).containsOnly(
                new ParcelableSelectable<String>("test1", 11),
                new ParcelableSelectable<Integer>(13, 12));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = LoaderStreams.merge(channel1, channel2).buildChannels();
        channel1.pass("test2").close();
        channel2.pass(-17).close();
        assertThat(outputChannel.after(seconds(10)).all()).containsOnly(
                new ParcelableSelectable<String>("test2", 0),
                new ParcelableSelectable<Integer>(-17, 1));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = LoaderStreams.merge(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                                     .buildChannels();
        channel1.pass("test2").close();
        channel2.pass(-17).close();
        assertThat(outputChannel.after(seconds(10)).all()).containsOnly(
                new ParcelableSelectable<String>("test2", 0),
                new ParcelableSelectable<Integer>(-17, 1));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        final SparseArray<Channel<?, ?>> channelMap = new SparseArray<Channel<?, ?>>(2);
        channelMap.put(7, channel1);
        channelMap.put(-3, channel2);
        outputChannel = LoaderStreams.merge(channelMap).buildChannels();
        channel1.pass("test3").close();
        channel2.pass(111).close();
        assertThat(outputChannel.after(seconds(10)).all()).containsOnly(
                new ParcelableSelectable<String>("test3", 7),
                new ParcelableSelectable<Integer>(111, -3));
    }

    @SuppressWarnings("unchecked")
    public void testMerge4() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final ChannelBuilder builder =
                JRoutineCore.io().channelConfiguration().withOrder(OrderType.BY_CALL).apply();
        final Channel<String, String> channel1 = builder.buildChannel();
        final Channel<String, String> channel2 = builder.buildChannel();
        final Channel<String, String> channel3 = builder.buildChannel();
        final Channel<String, String> channel4 = builder.buildChannel();

        final Routine<ParcelableSelectable<String>, String> routine =
                JRoutineLoader.with(loaderFrom(getActivity()))
                              .on(factoryOf(new ClassToken<Amb<String>>() {}))
                              .buildRoutine();
        final Channel<?, String> outputChannel = routine.async(
                LoaderStreams.merge(Arrays.asList(channel1, channel2, channel3, channel4))
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

        assertThat(outputChannel.after(seconds(10)).all()).containsExactly("0", "1", "2", "3");
    }

    public void testMergeAbort() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final ChannelBuilder builder =
                JRoutineCore.io().channelConfiguration().withOrder(OrderType.BY_CALL).apply();
        Channel<String, String> channel1;
        Channel<Integer, Integer> channel2;
        Channel<?, ? extends ParcelableSelectable<?>> outputChannel;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = LoaderStreams.merge(-7, channel1, channel2).buildChannels();
        channel1.pass("test1").close();
        channel2.abort();

        try {

            outputChannel.after(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = LoaderStreams.merge(11, Arrays.<Channel<?, ?>>asList(channel1, channel2))
                                     .buildChannels();
        channel2.abort();
        channel1.pass("test1").close();

        try {

            outputChannel.after(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = LoaderStreams.merge(channel1, channel2).buildChannels();
        channel1.abort();
        channel2.pass(-17).close();

        try {

            outputChannel.after(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = LoaderStreams.merge(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                                     .buildChannels();
        channel1.pass("test2").close();
        channel2.abort();

        try {

            outputChannel.after(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        final SparseArray<Channel<?, ?>> channelMap = new SparseArray<Channel<?, ?>>(2);
        channelMap.append(7, channel1);
        channelMap.append(-3, channel2);
        outputChannel = LoaderStreams.merge(channelMap).buildChannels();
        channel1.abort();
        channel2.pass(111).close();

        try {

            outputChannel.after(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testMergeError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        try {

            LoaderStreams.merge(0, Collections.<Channel<?, Object>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            LoaderStreams.merge(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            LoaderStreams.merge(Collections.<Channel<?, Object>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            LoaderStreams.merge(Collections.<Integer, Channel<?, Object>>emptyMap());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            LoaderStreams.merge();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            LoaderStreams.merge(new Channel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            LoaderStreams.merge(Collections.<Channel<?, ?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            LoaderStreams.merge(0, new Channel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            LoaderStreams.merge(0, Collections.<Channel<?, ?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            LoaderStreams.merge(Collections.<Integer, Channel<?, ?>>singletonMap(1, null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @SuppressWarnings("unchecked")
    public void testOutputToSelectable() {

        final Channel<String, String> channel = JRoutineCore.io().buildChannel();
        channel.pass("test1", "test2", "test3").close();
        assertThat(LoaderStreams.selectableOutput(channel, 33)
                                .buildChannels()
                                .after(seconds(10))
                                .all()).containsExactly(
                new ParcelableSelectable<String>("test1", 33),
                new ParcelableSelectable<String>("test2", 33),
                new ParcelableSelectable<String>("test3", 33));
    }

    public void testOutputToSelectableAbort() {

        final Channel<String, String> channel = JRoutineCore.io().buildChannel();
        channel.pass("test1", "test2", "test3").abort();

        try {
            LoaderStreams.selectableOutput(channel, 33).buildChannels().after(seconds(10)).all();
            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testReplay() {

        final Channel<Object, Object> inputChannel = JRoutineCore.io().buildChannel();
        final Channel<?, Object> channel = LoaderStreams.replay(inputChannel).buildChannels();
        inputChannel.pass("test1", "test2");
        final Channel<Object, Object> output1 = JRoutineCore.io().buildChannel();
        channel.bind(output1).close();
        assertThat(output1.next()).isEqualTo("test1");
        final Channel<Object, Object> output2 = JRoutineCore.io().buildChannel();
        channel.bind(output2).close();
        inputChannel.pass("test3").close();
        assertThat(output2.all()).containsExactly("test1", "test2", "test3");
        assertThat(output1.all()).containsExactly("test2", "test3");
    }

    public void testReplayAbort() {

        final Channel<Object, Object> inputChannel = JRoutineCore.io().buildChannel();
        final Channel<?, Object> channel = LoaderStreams.replay(inputChannel).buildChannels();
        inputChannel.pass("test1", "test2");
        final Channel<Object, Object> output1 = JRoutineCore.io().buildChannel();
        channel.bind(output1).close();
        assertThat(output1.next()).isEqualTo("test1");
        final Channel<Object, Object> output2 = JRoutineCore.io().buildChannel();
        channel.bind(output2).close();
        inputChannel.abort();

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

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final Routine<ParcelableSelectable<Object>, ParcelableSelectable<Object>> routine =
                JRoutineCore.on(new Sort()).buildRoutine();
        final Channel<ParcelableSelectable<Object>, ParcelableSelectable<Object>> inputChannel =
                JRoutineCore.io().buildChannel();
        final Channel<?, ParcelableSelectable<Object>> outputChannel = routine.async(inputChannel);
        final StreamChannel<Object, Object> intChannel =
                LoaderStreams.selectParcelableOutput(outputChannel, Sort.INTEGER, Sort.STRING)
                             .channelConfiguration()
                             .withLogLevel(Level.WARNING)
                             .apply()
                             .buildChannels()
                             .get(Sort.INTEGER);
        final StreamChannel<Object, Object> strChannel =
                LoaderStreams.selectParcelableOutput(outputChannel,
                        Arrays.asList(Sort.STRING, Sort.INTEGER))
                             .channelConfiguration()
                             .withLogLevel(Level.WARNING)
                             .apply()
                             .buildChannels()
                             .get(Sort.STRING);
        inputChannel.pass(new ParcelableSelectable<Object>("test21", Sort.STRING),
                new ParcelableSelectable<Object>(-11, Sort.INTEGER));
        assertThat(intChannel.after(seconds(10)).next()).isEqualTo(-11);
        assertThat(strChannel.after(seconds(10)).next()).isEqualTo("test21");
        inputChannel.pass(new ParcelableSelectable<Object>(-11, Sort.INTEGER),
                new ParcelableSelectable<Object>("test21", Sort.STRING));
        assertThat(intChannel.after(seconds(10)).next()).isEqualTo(-11);
        assertThat(strChannel.after(seconds(10)).next()).isEqualTo("test21");
        inputChannel.pass(new ParcelableSelectable<Object>("test21", Sort.STRING),
                new ParcelableSelectable<Object>(-11, Sort.INTEGER));
        assertThat(intChannel.after(seconds(10)).next()).isEqualTo(-11);
        assertThat(strChannel.after(seconds(10)).next()).isEqualTo("test21");
    }

    @SuppressWarnings("unchecked")
    public void testSelectMapAbort() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final Routine<ParcelableSelectable<Object>, ParcelableSelectable<Object>> routine =
                JRoutineCore.on(new Sort()).buildRoutine();
        Channel<ParcelableSelectable<Object>, ParcelableSelectable<Object>> inputChannel =
                JRoutineCore.io().buildChannel();
        Channel<?, ParcelableSelectable<Object>> outputChannel = routine.async(inputChannel);
        LoaderStreams.selectParcelableOutput(Sort.STRING, 2, outputChannel).buildChannels();
        inputChannel.after(millis(100))
                    .pass(new ParcelableSelectable<Object>("test21", Sort.STRING),
                            new ParcelableSelectable<Object>(-11, Sort.INTEGER))
                    .abort();

        try {

            LoaderStreams.selectParcelableOutput(outputChannel, Sort.STRING, Sort.INTEGER)
                         .buildChannels()
                         .get(Sort.STRING)
                         .after(seconds(10))
                         .all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            LoaderStreams.selectParcelableOutput(outputChannel, Sort.INTEGER, Sort.STRING)
                         .buildChannels()
                         .get(Sort.INTEGER)
                         .after(seconds(10))
                         .all();

            fail();

        } catch (final AbortException ignored) {

        }

        inputChannel = JRoutineCore.io().buildChannel();
        outputChannel = routine.async(inputChannel);
        LoaderStreams.selectParcelableOutput(outputChannel, Sort.INTEGER, Sort.STRING)
                     .buildChannels();
        inputChannel.after(millis(100))
                    .pass(new ParcelableSelectable<Object>(-11, Sort.INTEGER),
                            new ParcelableSelectable<Object>("test21", Sort.STRING))
                    .abort();

        try {

            LoaderStreams.selectParcelableOutput(outputChannel, Sort.STRING, Sort.INTEGER)
                         .buildChannels()
                         .get(Sort.STRING)
                         .after(seconds(10))
                         .all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            LoaderStreams.selectParcelableOutput(outputChannel, Sort.STRING, Sort.INTEGER)
                         .buildChannels()
                         .get(Sort.INTEGER)
                         .after(seconds(10))
                         .all();

            fail();

        } catch (final AbortException ignored) {

        }

        inputChannel = JRoutineCore.io().buildChannel();
        outputChannel = routine.async(inputChannel);
        LoaderStreams.selectParcelableOutput(outputChannel,
                Arrays.asList(Sort.STRING, Sort.INTEGER)).buildChannels();
        inputChannel.after(millis(100))
                    .pass(new ParcelableSelectable<Object>("test21", Sort.STRING),
                            new ParcelableSelectable<Object>(-11, Sort.INTEGER))
                    .abort();

        try {

            LoaderStreams.selectParcelableOutput(outputChannel, Sort.INTEGER, Sort.STRING)
                         .buildChannels()
                         .get(Sort.STRING)
                         .after(seconds(10))
                         .all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            LoaderStreams.selectParcelableOutput(outputChannel, Sort.INTEGER, Sort.STRING)
                         .buildChannels()
                         .get(Sort.INTEGER)
                         .after(seconds(10))
                         .all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testSkip() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final LoaderContext context = loaderFrom(getActivity());
        assertThat(LoaderStreams.streamOf()
                                .with(context)
                                .sync()
                                .thenGetMore(range(1, 10))
                                .async()
                                .map(LoaderStreams.skip(5))
                                .after(seconds(3))
                                .all()).containsExactly(6, 7, 8, 9, 10);
        assertThat(LoaderStreams.streamOf()
                                .with(context)
                                .sync()
                                .thenGetMore(range(1, 10))
                                .async()
                                .map(LoaderStreams.skip(15))
                                .after(seconds(3))
                                .all()).isEmpty();
        assertThat(LoaderStreams.streamOf()
                                .with(context)
                                .sync()
                                .thenGetMore(range(1, 10))
                                .async()
                                .map(LoaderStreams.skip(0))
                                .after(seconds(3))
                                .all()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(LoaderStreams.streamOf()
                                .sync()
                                .thenGetMore(range(1, 10))
                                .with(context)
                                .async()
                                .map(LoaderStreams.skip(5))
                                .after(seconds(3))
                                .all()).containsExactly(6, 7, 8, 9, 10);
        assertThat(LoaderStreams.streamOf()
                                .sync()
                                .thenGetMore(range(1, 10))
                                .with(context)
                                .async()
                                .map(LoaderStreams.skip(15))
                                .after(seconds(3))
                                .all()).isEmpty();
        assertThat(LoaderStreams.streamOf()
                                .sync()
                                .thenGetMore(range(1, 10))
                                .with(context)
                                .async()
                                .map(LoaderStreams.skip(0))
                                .after(seconds(3))
                                .all()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    public void testSkipEquals() {

        final InvocationFactory<Object, Object> factory = LoaderStreams.skip(2);
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(null);
        assertThat(factory).isNotEqualTo("test");
        assertThat(factory).isNotEqualTo(LoaderStreams.skip(3));
        assertThat(factory).isEqualTo(LoaderStreams.skip(2));
        assertThat(factory.hashCode()).isEqualTo(LoaderStreams.skip(2).hashCode());
    }

    public void testSkipError() {

        try {

            LoaderStreams.skip(-1);

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
        LoaderStreams.streamOf("test")
                     .with(context)
                     .async()
                     .cache(CacheStrategyType.CACHE)
                     .map(function)
                     .immediately();
        assertThat(LoaderStreams.streamOf("test")
                                .with(context)
                                .staleAfter(2000, TimeUnit.MILLISECONDS)
                                .async()
                                .map(function)
                                .after(seconds(10))
                                .next()).isEqualTo("test1");
        seconds(5).sleepAtLeast();
        assertThat(LoaderStreams.streamOf("test")
                                .with(context)
                                .staleAfter(zero())
                                .async()
                                .map(function)
                                .after(seconds(10))
                                .next()).isEqualTo("test2");
        seconds(5).sleepAtLeast();
        LoaderStreams.streamOf("test")
                     .with(context)
                     .cache(CacheStrategyType.CACHE_IF_SUCCESS)
                     .async()
                     .map(function)
                     .immediately();
        seconds(5).sleepAtLeast();
        assertThat(LoaderStreams.streamOf("test")
                                .with(context)
                                .staleAfter(zero())
                                .async()
                                .map(function)
                                .after(seconds(10))
                                .next()).isEqualTo("test4");
    }

    private static class Amb<DATA> extends TemplateInvocation<ParcelableSelectable<DATA>, DATA> {

        private static final int NO_INDEX = Integer.MIN_VALUE;

        private int mFirstIndex;

        @Override
        public void onInput(final ParcelableSelectable<DATA> input,
                @NotNull final Channel<DATA, ?> result) {

            if (mFirstIndex == NO_INDEX) {

                mFirstIndex = input.index;
                result.pass(input.data);

            } else if (mFirstIndex == input.index) {

                result.pass(input.data);
            }
        }

        @Override
        public void onRecycle() {

            mFirstIndex = NO_INDEX;
        }
    }

    private static class CharAt extends MappingInvocation<List<?>, Character> {

        /**
         * Constructor.
         */
        protected CharAt() {

            super(null);
        }

        public void onInput(final List<?> objects, @NotNull final Channel<Character, ?> result) {

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
                @NotNull final Channel<ParcelableSelectable<Object>, ?> result) {

            switch (selectable.index) {

                case INTEGER:
                    LoaderStreams.<Object, Integer>selectParcelableInput(result,
                            INTEGER).buildChannels().pass(selectable.<Integer>data()).close();
                    break;

                case STRING:
                    LoaderStreams.<Object, String>selectParcelableInput(result,
                            STRING).buildChannels().pass(selectable.<String>data()).close();
                    break;
            }
        }
    }
}
