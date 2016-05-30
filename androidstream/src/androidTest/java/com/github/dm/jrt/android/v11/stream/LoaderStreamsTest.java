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
import com.github.dm.jrt.core.builder.IOChannelBuilder;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel.InputChannel;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.channel.InvocationChannel;
import com.github.dm.jrt.core.channel.ResultChannel;
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
                LoaderStreams.streamOf("test1", "test2", "test3").with(context).asyncOn(null);
        StreamChannel<String, String> channel2 =
                LoaderStreams.streamOf("test4", "test5", "test6").with(context).asyncOn(null);
        assertThat(
                LoaderStreams.blend(channel2, channel1).buildChannels().afterMax(seconds(10)).all())
                .containsOnly("test1", "test2", "test3", "test4", "test5", "test6");
        channel1 = LoaderStreams.streamOf("test1", "test2", "test3").with(context).asyncOn(null);
        channel2 = LoaderStreams.streamOf("test4", "test5", "test6").with(context).asyncOn(null);
        assertThat(LoaderStreams.blend(Arrays.<StreamChannel<?, ?>>asList(channel1, channel2))
                                .buildChannels()
                                .afterMax(seconds(10))
                                .all()).containsOnly("test1", "test2", "test3", "test4", "test5",
                "test6");
    }

    public void testBlendAbort() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final IOChannelBuilder builder = JRoutineCore.io();
        final Routine<Object, Object> routine = JRoutineLoader.with(loaderFrom(getActivity()))
                                                              .on(IdentityContextInvocation
                                                                      .factoryOf())
                                                              .buildRoutine();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().abort();

        try {

            routine.asyncCall(LoaderStreams.blend(channel1, channel2).buildChannels())
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

            routine.asyncCall(
                    LoaderStreams.blend(Arrays.<OutputChannel<?>>asList(channel1, channel2))
                                 .buildChannels()).afterMax(seconds(10)).all();

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

            LoaderStreams.blend((OutputChannel<?>[]) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            LoaderStreams.blend(new OutputChannel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            LoaderStreams.blend(Collections.<OutputChannel<?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            LoaderStreams.blend((List<OutputChannel<?>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            LoaderStreams.blend(Collections.<OutputChannel<?>>singletonList(null));

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
                                .asyncOn(null)
                                .afterMax(seconds(10))
                                .all()).containsExactly("test");
        assertThat(LoaderStreams.streamOf("test1", "test2", "test3")
                                .with(context)
                                .asyncOn(null)
                                .afterMax(seconds(10))
                                .all()).containsExactly("test1", "test2", "test3");
        assertThat(LoaderStreams.streamOf(Arrays.asList("test1", "test2", "test3"))
                                .with(context)
                                .asyncOn(null)
                                .afterMax(seconds(10))
                                .all()).containsExactly("test1", "test2", "test3");
        assertThat(LoaderStreams.streamOf(JRoutineCore.io().of("test1", "test2", "test3"))
                                .with(context)
                                .asyncOn(null)
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

    public void testCombine() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        final InvocationChannel<String, String> channel1 =
                JRoutineLoader.with(loaderFrom(getActivity()))
                              .on(IdentityContextInvocation.<String>factoryOf())
                              .asyncInvoke()
                              .orderByCall();
        final InvocationChannel<Integer, Integer> channel2 =
                JRoutineLoader.with(loaderFrom(getActivity()))
                              .on(IdentityContextInvocation.<Integer>factoryOf())
                              .asyncInvoke()
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
        LoaderStreams.combine(Arrays.<InvocationChannel<?, ?>>asList(channel1, channel2))
                     .buildChannels()
                     .pass(new ParcelableSelectable<String>("test3", 0))
                     .pass(new ParcelableSelectable<Integer>(3, 1))
                     .close();
        LoaderStreams.combine(-5, Arrays.<InvocationChannel<?, ?>>asList(channel1, channel2))
                     .buildChannels()
                     .pass(new ParcelableSelectable<String>("test4", -5))
                     .pass(new ParcelableSelectable<Integer>(4, -4))
                     .close();
        final HashMap<Integer, InvocationChannel<?, ?>> map =
                new HashMap<Integer, InvocationChannel<?, ?>>(2);
        map.put(31, channel1);
        map.put(17, channel2);
        LoaderStreams.combine(map)
                     .buildChannels()
                     .pass(new ParcelableSelectable<String>("test5", 31))
                     .pass(new ParcelableSelectable<Integer>(5, 17))
                     .close();
        final SparseArray<InvocationChannel<?, ?>> sparseArray =
                new SparseArray<InvocationChannel<?, ?>>(2);
        sparseArray.put(31, channel1);
        sparseArray.put(17, channel2);
        LoaderStreams.combine(sparseArray)
                     .buildChannels()
                     .pass(new ParcelableSelectable<String>("test6", 31))
                     .pass(new ParcelableSelectable<Integer>(6, 17))
                     .close();
        assertThat(channel1.result().afterMax(seconds(10)).all()).containsExactly("test1", "test2",
                "test3", "test4", "test5", "test6");
        assertThat(channel2.result().afterMax(seconds(10)).all()).containsExactly(1, 2, 3, 4, 5, 6);
    }

    public void testConcat() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final LoaderContext context = loaderFrom(getActivity());
        StreamChannel<String, String> channel1 =
                LoaderStreams.streamOf("test1", "test2", "test3").with(context).asyncOn(null);
        StreamChannel<String, String> channel2 =
                LoaderStreams.streamOf("test4", "test5", "test6").with(context).asyncOn(null);
        assertThat(LoaderStreams.concat(channel2, channel1)
                                .buildChannels()
                                .afterMax(seconds(10))
                                .all()).containsExactly("test4", "test5", "test6", "test1", "test2",
                "test3");
        channel1 = LoaderStreams.streamOf("test1", "test2", "test3").with(context).asyncOn(null);
        channel2 = LoaderStreams.streamOf("test4", "test5", "test6").with(context).asyncOn(null);
        assertThat(LoaderStreams.concat(Arrays.<StreamChannel<?, ?>>asList(channel1, channel2))
                                .buildChannels()
                                .afterMax(seconds(10))
                                .all()).containsExactly("test1", "test2", "test3", "test4", "test5",
                "test6");
    }

    public void testConcatAbort() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final IOChannelBuilder builder = JRoutineCore.io();
        final Routine<Object, Object> routine = JRoutineLoader.with(loaderFrom(getActivity()))
                                                              .on(IdentityContextInvocation
                                                                      .factoryOf())
                                                              .buildRoutine();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().abort();

        try {

            routine.asyncCall(LoaderStreams.concat(channel1, channel2).buildChannels())
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

            routine.asyncCall(
                    LoaderStreams.concat(Arrays.<OutputChannel<?>>asList(channel1, channel2))
                                 .buildChannels()).afterMax(seconds(10)).all();

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

            LoaderStreams.concat((OutputChannel<?>[]) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            LoaderStreams.concat(new OutputChannel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            LoaderStreams.concat(Collections.<OutputChannel<?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            LoaderStreams.concat((List<OutputChannel<?>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            LoaderStreams.concat(Collections.<OutputChannel<?>>singletonList(null));

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

        final InvocationChannel<String, String> channel1 =
                JRoutineLoader.with(loaderFrom(getActivity()))
                              .on(IdentityContextInvocation.<String>factoryOf())
                              .asyncInvoke()
                              .orderByCall();
        final InvocationChannel<String, String> channel2 =
                JRoutineLoader.with(loaderFrom(getActivity()))
                              .on(IdentityContextInvocation.<String>factoryOf())
                              .asyncInvoke()
                              .orderByCall();
        LoaderStreams.distribute(channel1, channel2)
                     .buildChannels()
                     .pass(Arrays.asList("test1-1", "test1-2"))
                     .close();
        LoaderStreams.distribute(Arrays.<InputChannel<?>>asList(channel1, channel2))
                     .buildChannels()
                     .pass(Arrays.asList("test2-1", "test2-2"))
                     .close();
        LoaderStreams.distribute(channel1, channel2)
                     .buildChannels()
                     .pass(Collections.singletonList("test3-1"))
                     .close();
        assertThat(channel1.result().afterMax(seconds(10)).all()).containsExactly("test1-1",
                "test2-1", "test3-1");
        assertThat(channel2.result().afterMax(seconds(10)).all()).containsExactly("test1-2",
                "test2-2");
    }

    public void testDistributePlaceholder() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        final InvocationChannel<String, String> channel1 =
                JRoutineLoader.with(loaderFrom(getActivity()))
                              .on(IdentityContextInvocation.<String>factoryOf())
                              .asyncInvoke()
                              .orderByCall();
        final InvocationChannel<String, String> channel2 =
                JRoutineLoader.with(loaderFrom(getActivity()))
                              .on(IdentityContextInvocation.<String>factoryOf())
                              .asyncInvoke()
                              .orderByCall();
        LoaderStreams.distribute((Object) null, channel1, channel2)
                     .buildChannels()
                     .pass(Arrays.asList("test1-1", "test1-2"))
                     .close();
        final String placeholder = "placeholder";
        LoaderStreams.distribute((Object) placeholder,
                Arrays.<InputChannel<?>>asList(channel1, channel2))
                     .buildChannels()
                     .pass(Arrays.asList("test2-1", "test2-2"))
                     .close();
        LoaderStreams.distribute(placeholder, channel1, channel2)
                     .buildChannels()
                     .pass(Collections.singletonList("test3-1"))
                     .close();
        assertThat(channel1.result().afterMax(seconds(10)).all()).containsExactly("test1-1",
                "test2-1", "test3-1");
        assertThat(channel2.result().afterMax(seconds(10)).all()).containsExactly("test1-2",
                "test2-2", placeholder);
    }

    public void testFactory() {

        assertThat(LoaderStreams.onStream(toUpperCaseChannel())
                                .asyncCall("test1", "test2", "test3")
                                .afterMax(seconds(3))
                                .all()).containsExactly("TEST1", "TEST2", "TEST3");

        try {

            final InvocationChannel<String, String> channel =
                    LoaderStreams.onStream(toUpperCaseChannel()).asyncInvoke();
            channel.abort(new IllegalArgumentException());
            channel.result().afterMax(seconds(3)).next();

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
                                 .asyncCall("test1", "test2", "test3")
                                 .afterMax(seconds(3))
                                 .all()).containsExactly("TEST1", "TEST2", "TEST3");

        try {

            final InvocationChannel<String, String> channel =
                    JRoutineLoader.with(loaderFrom(getActivity())).on(factory).asyncInvoke();
            channel.abort(new IllegalArgumentException());
            channel.result().afterMax(seconds(3)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        assertThat(LoaderStreams.onStreamWith(loaderFrom(getActivity()), toUpperCaseChannel())
                                .asyncCall("test1", "test2", "test3")
                                .afterMax(seconds(3))
                                .all()).containsExactly("TEST1", "TEST2", "TEST3");

        try {

            final InvocationChannel<String, String> channel =
                    LoaderStreams.onStreamWith(loaderFrom(getActivity()), toUpperCaseChannel())
                                 .asyncInvoke();
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
            JRoutineLoader.with(context).onId(11).buildChannel().afterMax(seconds(10)).next();
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
                                .afterMax(seconds(10))
                                .next()).isEqualTo("TEST2");
        final AtomicInteger count = new AtomicInteger();
        LoaderStreams.streamOf().with(context).factoryId(11).thenGet(delayedIncrement(count));
        assertThat(LoaderStreams.streamOf()
                                .with(context)
                                .factoryId(11)
                                .thenGet(increment(count))
                                .afterMax(seconds(10))
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
                                .afterMax(seconds(3))
                                .all()).containsExactly(Arrays.<Number>asList(1, 2, 3),
                Arrays.<Number>asList(4, 5, 6), Arrays.<Number>asList(7, 8, 9),
                Collections.<Number>singletonList(10));
        assertThat(LoaderStreams.streamOf()
                                .with(context)
                                .sync()
                                .thenGetMore(range(1, 10))
                                .async()
                                .map(LoaderStreams.<Number>groupBy(13))
                                .afterMax(seconds(3))
                                .all()).containsExactly(
                Arrays.<Number>asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        assertThat(LoaderStreams.streamOf()
                                .sync()
                                .thenGetMore(range(1, 10))
                                .with(context)
                                .async()
                                .map(LoaderStreams.<Number>groupBy(3))
                                .afterMax(seconds(3))
                                .all()).containsExactly(Arrays.<Number>asList(1, 2, 3),
                Arrays.<Number>asList(4, 5, 6), Arrays.<Number>asList(7, 8, 9),
                Collections.<Number>singletonList(10));
        assertThat(LoaderStreams.streamOf()
                                .sync()
                                .thenGetMore(range(1, 10))
                                .with(context)
                                .async()
                                .map(LoaderStreams.<Number>groupBy(13))
                                .afterMax(seconds(3))
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
                                .afterMax(seconds(3))
                                .all()).containsExactly(Arrays.<Number>asList(1, 2, 3),
                Arrays.<Number>asList(4, 5, 6), Arrays.<Number>asList(7, 8, 9),
                Arrays.<Number>asList(10, 0, 0));
        assertThat(LoaderStreams.streamOf()
                                .with(context)
                                .sync()
                                .thenGetMore(range(1, 10))
                                .async()
                                .map(LoaderStreams.<Number>groupBy(13, -1))
                                .afterMax(seconds(3))
                                .all()).containsExactly(
                Arrays.<Number>asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, -1, -1, -1));
        assertThat(LoaderStreams.streamOf()
                                .sync()
                                .thenGetMore(range(1, 10))
                                .with(context)
                                .async()
                                .map(LoaderStreams.<Number>groupBy(3, -31))
                                .afterMax(seconds(3))
                                .all()).containsExactly(Arrays.<Number>asList(1, 2, 3),
                Arrays.<Number>asList(4, 5, 6), Arrays.<Number>asList(7, 8, 9),
                Arrays.<Number>asList(10, -31, -31));
        assertThat(LoaderStreams.streamOf()
                                .sync()
                                .thenGetMore(range(1, 10))
                                .with(context)
                                .async()
                                .map(LoaderStreams.<Number>groupBy(13, 71))
                                .afterMax(seconds(3))
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
        SparseArray<IOChannel<Object>> channelMap;
        InvocationChannel<ParcelableSelectable<Object>, ParcelableSelectable<Object>> channel;
        channel = routine.asyncInvoke();
        channelMap =
                LoaderStreams.selectParcelable(channel, Arrays.asList(Sort.INTEGER, Sort.STRING))
                             .buildChannels();
        channelMap.get(Sort.INTEGER).pass(-11).close();
        channelMap.get(Sort.STRING).pass("test21").close();
        assertThat(channel.result().afterMax(seconds(10)).all()).containsOnlyElementsOf(outputs);
        channel = routine.asyncInvoke();
        channelMap =
                LoaderStreams.selectParcelable(channel, Sort.INTEGER, Sort.STRING).buildChannels();
        channelMap.get(Sort.INTEGER).pass(-11).close();
        channelMap.get(Sort.STRING).pass("test21").close();
        assertThat(channel.result().afterMax(seconds(10)).all()).containsOnlyElementsOf(outputs);
        channel = routine.asyncInvoke();
        channelMap = LoaderStreams.selectParcelable(Math.min(Sort.INTEGER, Sort.STRING), 2, channel)
                                  .buildChannels();
        channelMap.get(Sort.INTEGER).pass(-11).close();
        channelMap.get(Sort.STRING).pass("test21").close();
        assertThat(channel.result().afterMax(seconds(10)).all()).containsOnlyElementsOf(outputs);
    }

    @SuppressWarnings("unchecked")
    public void testInputSelect() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        IOChannel<Selectable<String>> channel = JRoutineCore.io().buildChannel();
        LoaderStreams.select(channel.asInput(), 33)
                     .buildChannels()
                     .pass("test1", "test2", "test3")
                     .close();
        assertThat(channel.close().afterMax(seconds(10)).all()).containsExactly(
                new ParcelableSelectable<String>("test1", 33),
                new ParcelableSelectable<String>("test2", 33),
                new ParcelableSelectable<String>("test3", 33));
        channel = JRoutineCore.io().buildChannel();
        Map<Integer, IOChannel<String>> channelMap =
                LoaderStreams.select(channel.asInput(), Arrays.asList(1, 2, 3)).buildChannels();
        channelMap.get(1).pass("test1").close();
        channelMap.get(2).pass("test2").close();
        channelMap.get(3).pass("test3").close();
        assertThat(channel.close().afterMax(seconds(10)).all()).containsExactly(
                new ParcelableSelectable<String>("test1", 1),
                new ParcelableSelectable<String>("test2", 2),
                new ParcelableSelectable<String>("test3", 3));
        channel = JRoutineCore.io().buildChannel();
        channelMap = LoaderStreams.select(channel.asInput(), 1, 2, 3).buildChannels();
        channelMap.get(1).pass("test1").close();
        channelMap.get(2).pass("test2").close();
        channelMap.get(3).pass("test3").close();
        assertThat(channel.close().afterMax(seconds(10)).all()).containsExactly(
                new ParcelableSelectable<String>("test1", 1),
                new ParcelableSelectable<String>("test2", 2),
                new ParcelableSelectable<String>("test3", 3));
        channel = JRoutineCore.io().buildChannel();
        channelMap = LoaderStreams.select(1, 3, channel.asInput()).buildChannels();
        channelMap.get(1).pass("test1").close();
        channelMap.get(2).pass("test2").close();
        channelMap.get(3).pass("test3").close();
        assertThat(channel.close().afterMax(seconds(10)).all()).containsExactly(
                new ParcelableSelectable<String>("test1", 1),
                new ParcelableSelectable<String>("test2", 2),
                new ParcelableSelectable<String>("test3", 3));
        channel = JRoutineCore.io().buildChannel();
        LoaderStreams.selectParcelable(channel, 33)
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

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        final IOChannel<String> channel = JRoutineCore.io().buildChannel();
        LoaderStreams.toSelectable(channel.asInput(), 33)
                     .buildChannels()
                     .pass(new Selectable<String>("test1", 33),
                             new Selectable<String>("test2", -33),
                             new Selectable<String>("test3", 33),
                             new Selectable<String>("test4", 333))
                     .close();
        assertThat(channel.close().afterMax(seconds(10)).all()).containsExactly("test1", "test3");
    }

    public void testJoin() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final IOChannelBuilder builder = JRoutineCore.io();
        final Routine<List<?>, Character> routine = JRoutineLoader.with(loaderFrom(getActivity()))
                                                                  .on(factoryFrom(new CharAt()))
                                                                  .buildRoutine();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.asyncCall(LoaderStreams.join(channel1, channel2).buildChannels())
                          .afterMax(seconds(10))
                          .all()).containsExactly('s', '2');
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.asyncCall(
                LoaderStreams.join(Arrays.<OutputChannel<?>>asList(channel1, channel2))
                             .buildChannels()).afterMax(seconds(10)).all()).containsExactly('s',
                '2');
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall()
                .after(millis(100))
                .pass("testtest")
                .pass("test2")
                .pass("test3")
                .close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.asyncCall(LoaderStreams.join(channel1, channel2).buildChannels())
                          .afterMax(seconds(10))
                          .all()).containsExactly('s', '2');
    }

    public void testJoinAbort() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final IOChannelBuilder builder = JRoutineCore.io();
        final Routine<List<?>, Character> routine = JRoutineLoader.with(loaderFrom(getActivity()))
                                                                  .on(factoryFrom(new CharAt()))
                                                                  .buildRoutine();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().abort();

        try {

            routine.asyncCall(LoaderStreams.join(channel1, channel2).buildChannels())
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

            routine.asyncCall(
                    LoaderStreams.join(Arrays.<OutputChannel<?>>asList(channel1, channel2))
                                 .buildChannels()).afterMax(seconds(10)).all();

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

            LoaderStreams.join(Collections.<OutputChannel<?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            LoaderStreams.join(new OutputChannel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            LoaderStreams.join(Collections.<OutputChannel<?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testJoinPlaceholder() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final IOChannelBuilder builder = JRoutineCore.io();
        final Routine<List<?>, Character> routine = JRoutineLoader.with(loaderFrom(getActivity()))
                                                                  .on(factoryFrom(new CharAt()))
                                                                  .buildRoutine();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.asyncCall(
                LoaderStreams.join(new Object(), channel1, channel2).buildChannels())
                          .afterMax(seconds(10))
                          .all()).containsExactly('s', '2');
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.asyncCall(
                LoaderStreams.join(null, Arrays.<OutputChannel<?>>asList(channel1, channel2))
                             .buildChannels()).afterMax(seconds(10)).all()).containsExactly('s',
                '2');
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

            routine.asyncCall(LoaderStreams.join(new Object(), channel1, channel2).buildChannels())
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

        final IOChannelBuilder builder = JRoutineCore.io();
        final Routine<List<?>, Character> routine = JRoutineLoader.with(loaderFrom(getActivity()))
                                                                  .on(factoryFrom(new CharAt()))
                                                                  .buildRoutine();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().abort();

        try {

            routine.asyncCall(LoaderStreams.join((Object) null, channel1, channel2).buildChannels())
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

            routine.asyncCall(LoaderStreams.join(new Object(),
                    Arrays.<OutputChannel<?>>asList(channel1, channel2)).buildChannels())
                   .afterMax(seconds(10))
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

            LoaderStreams.join(null, Collections.<OutputChannel<?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            LoaderStreams.join(new Object(), new OutputChannel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            LoaderStreams.join(new Object(), Collections.<OutputChannel<?>>singletonList(null));

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
                                .afterMax(seconds(3))
                                .all()).containsExactly(1, 2, 3, 4, 5);
        assertThat(LoaderStreams.streamOf()
                                .with(context)
                                .sync()
                                .thenGetMore(range(1, 10))
                                .async()
                                .map(LoaderStreams.limit(0))
                                .afterMax(seconds(3))
                                .all()).isEmpty();
        assertThat(LoaderStreams.streamOf()
                                .with(context)
                                .sync()
                                .thenGetMore(range(1, 10))
                                .async()
                                .map(LoaderStreams.limit(15))
                                .afterMax(seconds(3))
                                .all()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(LoaderStreams.streamOf()
                                .sync()
                                .thenGetMore(range(1, 10))
                                .with(context)
                                .async()
                                .map(LoaderStreams.limit(5))
                                .afterMax(seconds(3))
                                .all()).containsExactly(1, 2, 3, 4, 5);
        assertThat(LoaderStreams.streamOf()
                                .sync()
                                .thenGetMore(range(1, 10))
                                .with(context)
                                .async()
                                .map(LoaderStreams.limit(0))
                                .afterMax(seconds(3))
                                .all()).isEmpty();
        assertThat(LoaderStreams.streamOf()
                                .sync()
                                .thenGetMore(range(1, 10))
                                .with(context)
                                .async()
                                .map(LoaderStreams.limit(15))
                                .afterMax(seconds(3))
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
                     .start();
        assertThat(JRoutineLoader.with(context)
                                 .onId(11)
                                 .buildChannel()
                                 .afterMax(seconds(10))
                                 .next()).isEqualTo("TEST1");
        LoaderStreams.streamOf("test2")
                     .with(context)
                     .loaderConfiguration()
                     .withLoaderId(21)
                     .apply()
                     .async()
                     .map(toUpperCase())
                     .start();
        assertThat(JRoutineLoader.with(context)
                                 .onId(21)
                                 .buildChannel()
                                 .afterMax(seconds(10))
                                 .next()).isEqualTo("TEST2");
        LoaderStreams.streamOf("test3")
                     .with(context)
                     .streamLoaderConfiguration()
                     .withLoaderId(31)
                     .apply()
                     .async()
                     .map(toUpperCase())
                     .start();
        assertThat(JRoutineLoader.with(context)
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
                JRoutineCore.io().channelConfiguration().withOrder(OrderType.BY_CALL).apply();
        final IOChannel<String> channel1 = builder.buildChannel();
        final IOChannel<Integer> channel2 = builder.buildChannel();

        final OutputChannel<? extends ParcelableSelectable<Object>> channel =
                LoaderStreams.merge(Arrays.<OutputChannel<?>>asList(channel1, channel2))
                             .buildChannels();
        final OutputChannel<ParcelableSelectable<Object>> output =
                JRoutineLoader.with(loaderFrom(getActivity()))
                              .on(factoryFrom(new Sort()))
                              .invocationConfiguration()
                              .withInputOrder(OrderType.BY_CALL)
                              .apply()
                              .asyncCall(channel);
        final SparseArray<OutputChannel<Object>> channelMap =
                SparseChannels.selectParcelable(output, Sort.INTEGER, Sort.STRING).buildChannels();

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
                                .asyncOn(null)
                                .afterMax(seconds(10))
                                .all()).containsExactly("0", "1", "2", "3");
        assertThat(LoaderStreams.streamOf(channelMap.get(Sort.INTEGER))
                                .with(context)
                                .asyncOn(null)
                                .afterMax(seconds(10))
                                .all()).containsExactly(0, 1, 2, 3);
    }

    public void testMerge() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final IOChannelBuilder builder =
                JRoutineCore.io().channelConfiguration().withOrder(OrderType.BY_CALL).apply();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        OutputChannel<? extends ParcelableSelectable<?>> outputChannel;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = LoaderStreams.merge(-7, channel1, channel2).buildChannels();
        channel1.pass("test1").close();
        channel2.pass(13).close();
        assertThat(outputChannel.afterMax(seconds(10)).all()).containsOnly(
                new ParcelableSelectable<String>("test1", -7),
                new ParcelableSelectable<Integer>(13, -6));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = LoaderStreams.merge(11, Arrays.<OutputChannel<?>>asList(channel1, channel2))
                                     .buildChannels();
        channel2.pass(13).close();
        channel1.pass("test1").close();
        assertThat(outputChannel.afterMax(seconds(10)).all()).containsOnly(
                new ParcelableSelectable<String>("test1", 11),
                new ParcelableSelectable<Integer>(13, 12));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = LoaderStreams.merge(channel1, channel2).buildChannels();
        channel1.pass("test2").close();
        channel2.pass(-17).close();
        assertThat(outputChannel.afterMax(seconds(10)).all()).containsOnly(
                new ParcelableSelectable<String>("test2", 0),
                new ParcelableSelectable<Integer>(-17, 1));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = LoaderStreams.merge(Arrays.<OutputChannel<?>>asList(channel1, channel2))
                                     .buildChannels();
        channel1.pass("test2").close();
        channel2.pass(-17).close();
        assertThat(outputChannel.afterMax(seconds(10)).all()).containsOnly(
                new ParcelableSelectable<String>("test2", 0),
                new ParcelableSelectable<Integer>(-17, 1));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        final SparseArray<OutputChannel<?>> channelMap = new SparseArray<OutputChannel<?>>(2);
        channelMap.put(7, channel1);
        channelMap.put(-3, channel2);
        outputChannel = LoaderStreams.merge(channelMap).buildChannels();
        channel1.pass("test3").close();
        channel2.pass(111).close();
        assertThat(outputChannel.afterMax(seconds(10)).all()).containsOnly(
                new ParcelableSelectable<String>("test3", 7),
                new ParcelableSelectable<Integer>(111, -3));
    }

    @SuppressWarnings("unchecked")
    public void testMerge4() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final IOChannelBuilder builder =
                JRoutineCore.io().channelConfiguration().withOrder(OrderType.BY_CALL).apply();
        final IOChannel<String> channel1 = builder.buildChannel();
        final IOChannel<String> channel2 = builder.buildChannel();
        final IOChannel<String> channel3 = builder.buildChannel();
        final IOChannel<String> channel4 = builder.buildChannel();

        final Routine<ParcelableSelectable<String>, String> routine =
                JRoutineLoader.with(loaderFrom(getActivity()))
                              .on(factoryOf(new ClassToken<Amb<String>>() {}))
                              .buildRoutine();
        final OutputChannel<String> outputChannel = routine.asyncCall(
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

        assertThat(outputChannel.afterMax(seconds(10)).all()).containsExactly("0", "1", "2", "3");
    }

    public void testMergeAbort() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final IOChannelBuilder builder =
                JRoutineCore.io().channelConfiguration().withOrder(OrderType.BY_CALL).apply();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        OutputChannel<? extends ParcelableSelectable<?>> outputChannel;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = LoaderStreams.merge(-7, channel1, channel2).buildChannels();
        channel1.pass("test1").close();
        channel2.abort();

        try {

            outputChannel.afterMax(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = LoaderStreams.merge(11, Arrays.<OutputChannel<?>>asList(channel1, channel2))
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
        outputChannel = LoaderStreams.merge(channel1, channel2).buildChannels();
        channel1.abort();
        channel2.pass(-17).close();

        try {

            outputChannel.afterMax(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = LoaderStreams.merge(Arrays.<OutputChannel<?>>asList(channel1, channel2))
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
        final SparseArray<OutputChannel<?>> channelMap = new SparseArray<OutputChannel<?>>(2);
        channelMap.append(7, channel1);
        channelMap.append(-3, channel2);
        outputChannel = LoaderStreams.merge(channelMap).buildChannels();
        channel1.abort();
        channel2.pass(111).close();

        try {

            outputChannel.afterMax(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testMergeError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        try {

            LoaderStreams.merge(0, Collections.<OutputChannel<Object>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            LoaderStreams.merge(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            LoaderStreams.merge(Collections.<OutputChannel<Object>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            LoaderStreams.merge(Collections.<Integer, OutputChannel<Object>>emptyMap());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            LoaderStreams.merge();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            LoaderStreams.merge(new OutputChannel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            LoaderStreams.merge(Collections.<OutputChannel<?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            LoaderStreams.merge(0, new OutputChannel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            LoaderStreams.merge(0, Collections.<OutputChannel<?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            LoaderStreams.merge(Collections.<Integer, OutputChannel<?>>singletonMap(1, null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @SuppressWarnings("unchecked")
    public void testOutputToSelectable() {

        final IOChannel<String> channel = JRoutineCore.io().buildChannel();
        channel.pass("test1", "test2", "test3").close();
        assertThat(LoaderStreams.toSelectable(channel.asOutput(), 33)
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
            LoaderStreams.toSelectable(channel.asOutput(), 33)
                         .buildChannels()
                         .afterMax(seconds(10))
                         .all();
            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testReplay() {

        final IOChannel<Object> ioChannel = JRoutineCore.io().buildChannel();
        final OutputChannel<Object> channel = LoaderStreams.replay(ioChannel).buildChannels();
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
        final OutputChannel<Object> channel = LoaderStreams.replay(ioChannel).buildChannels();
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

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final Routine<ParcelableSelectable<Object>, ParcelableSelectable<Object>> routine =
                JRoutineCore.on(new Sort()).buildRoutine();
        final IOChannel<ParcelableSelectable<Object>> inputChannel =
                JRoutineCore.io().buildChannel();
        final OutputChannel<ParcelableSelectable<Object>> outputChannel =
                routine.asyncCall(inputChannel);
        final StreamChannel<Object, Object> intChannel =
                LoaderStreams.selectParcelable(outputChannel, Sort.INTEGER, Sort.STRING)
                             .channelConfiguration()
                             .withLogLevel(Level.WARNING)
                             .apply()
                             .buildChannels()
                             .get(Sort.INTEGER);
        final StreamChannel<Object, Object> strChannel =
                LoaderStreams.selectParcelable(outputChannel,
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

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final Routine<ParcelableSelectable<Object>, ParcelableSelectable<Object>> routine =
                JRoutineCore.on(new Sort()).buildRoutine();
        IOChannel<ParcelableSelectable<Object>> inputChannel = JRoutineCore.io().buildChannel();
        OutputChannel<ParcelableSelectable<Object>> outputChannel = routine.asyncCall(inputChannel);
        LoaderStreams.selectParcelable(Sort.STRING, 2, outputChannel).buildChannels();
        inputChannel.after(millis(100))
                    .pass(new ParcelableSelectable<Object>("test21", Sort.STRING),
                            new ParcelableSelectable<Object>(-11, Sort.INTEGER))
                    .abort();

        try {

            LoaderStreams.selectParcelable(outputChannel, Sort.STRING, Sort.INTEGER)
                         .buildChannels()
                         .get(Sort.STRING)
                         .afterMax(seconds(10))
                         .all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            LoaderStreams.selectParcelable(outputChannel, Sort.INTEGER, Sort.STRING)
                         .buildChannels()
                         .get(Sort.INTEGER)
                         .afterMax(seconds(10))
                         .all();

            fail();

        } catch (final AbortException ignored) {

        }

        inputChannel = JRoutineCore.io().buildChannel();
        outputChannel = routine.asyncCall(inputChannel);
        LoaderStreams.selectParcelable(outputChannel, Sort.INTEGER, Sort.STRING).buildChannels();
        inputChannel.after(millis(100))
                    .pass(new ParcelableSelectable<Object>(-11, Sort.INTEGER),
                            new ParcelableSelectable<Object>("test21", Sort.STRING))
                    .abort();

        try {

            LoaderStreams.selectParcelable(outputChannel, Sort.STRING, Sort.INTEGER)
                         .buildChannels()
                         .get(Sort.STRING)
                         .afterMax(seconds(10))
                         .all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            LoaderStreams.selectParcelable(outputChannel, Sort.STRING, Sort.INTEGER)
                         .buildChannels()
                         .get(Sort.INTEGER)
                         .afterMax(seconds(10))
                         .all();

            fail();

        } catch (final AbortException ignored) {

        }

        inputChannel = JRoutineCore.io().buildChannel();
        outputChannel = routine.asyncCall(inputChannel);
        LoaderStreams.selectParcelable(outputChannel, Arrays.asList(Sort.STRING, Sort.INTEGER))
                     .buildChannels();
        inputChannel.after(millis(100))
                    .pass(new ParcelableSelectable<Object>("test21", Sort.STRING),
                            new ParcelableSelectable<Object>(-11, Sort.INTEGER))
                    .abort();

        try {

            LoaderStreams.selectParcelable(outputChannel, Sort.INTEGER, Sort.STRING)
                         .buildChannels()
                         .get(Sort.STRING)
                         .afterMax(seconds(10))
                         .all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            LoaderStreams.selectParcelable(outputChannel, Sort.INTEGER, Sort.STRING)
                         .buildChannels()
                         .get(Sort.INTEGER)
                         .afterMax(seconds(10))
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
                                .afterMax(seconds(3))
                                .all()).containsExactly(6, 7, 8, 9, 10);
        assertThat(LoaderStreams.streamOf()
                                .with(context)
                                .sync()
                                .thenGetMore(range(1, 10))
                                .async()
                                .map(LoaderStreams.skip(15))
                                .afterMax(seconds(3))
                                .all()).isEmpty();
        assertThat(LoaderStreams.streamOf()
                                .with(context)
                                .sync()
                                .thenGetMore(range(1, 10))
                                .async()
                                .map(LoaderStreams.skip(0))
                                .afterMax(seconds(3))
                                .all()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(LoaderStreams.streamOf()
                                .sync()
                                .thenGetMore(range(1, 10))
                                .with(context)
                                .async()
                                .map(LoaderStreams.skip(5))
                                .afterMax(seconds(3))
                                .all()).containsExactly(6, 7, 8, 9, 10);
        assertThat(LoaderStreams.streamOf()
                                .sync()
                                .thenGetMore(range(1, 10))
                                .with(context)
                                .async()
                                .map(LoaderStreams.skip(15))
                                .afterMax(seconds(3))
                                .all()).isEmpty();
        assertThat(LoaderStreams.streamOf()
                                .sync()
                                .thenGetMore(range(1, 10))
                                .with(context)
                                .async()
                                .map(LoaderStreams.skip(0))
                                .afterMax(seconds(3))
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
                                .afterMax(seconds(10))
                                .next()).isEqualTo("test1");
        seconds(5).sleepAtLeast();
        assertThat(LoaderStreams.streamOf("test")
                                .with(context)
                                .staleAfter(zero())
                                .async()
                                .map(function)
                                .afterMax(seconds(10))
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
                    LoaderStreams.<Object, Integer>selectParcelable(result, INTEGER).buildChannels()
                                                                                    .pass(selectable.<Integer>data())
                                                                                    .close();
                    break;

                case STRING:
                    LoaderStreams.<Object, String>selectParcelable(result, STRING).buildChannels()
                                                                                  .pass(selectable.<String>data())
                                                                                  .close();
                    break;
            }
        }
    }
}
