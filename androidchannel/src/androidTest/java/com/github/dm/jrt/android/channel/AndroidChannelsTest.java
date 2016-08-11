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

package com.github.dm.jrt.android.channel;

import android.annotation.TargetApi;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.core.JRoutineService;
import com.github.dm.jrt.android.core.invocation.TemplateContextInvocation;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.ChannelBuilder;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ClassToken;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.dm.jrt.android.core.ServiceContext.serviceFrom;
import static com.github.dm.jrt.android.core.invocation.TargetInvocationFactory.factoryOf;
import static com.github.dm.jrt.core.config.ChannelConfiguration.builder;
import static com.github.dm.jrt.core.util.UnitDuration.millis;
import static com.github.dm.jrt.core.util.UnitDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Routine channels unit tests.
 * <p>
 * Created by davide-maestroni on 06/18/2015.
 */
@TargetApi(VERSION_CODES.FROYO)
public class AndroidChannelsTest extends ActivityInstrumentationTestCase2<TestActivity> {

    public AndroidChannelsTest() {

        super(TestActivity.class);
    }

    public void testCombine() {

        final Channel<String, String> channel1 = JRoutineService.on(serviceFrom(getActivity()))
                                                                .with(factoryOf(
                                                                        PassingString.class))
                                                                .asyncCall()
                                                                .sortedByCall();
        final Channel<Integer, Integer> channel2 = JRoutineService.on(serviceFrom(getActivity()))
                                                                  .with(factoryOf(
                                                                          PassingInteger.class))
                                                                  .asyncCall()
                                                                  .sortedByCall();
        AndroidChannels.combine(channel1, channel2)
                       .buildChannels()
                       .pass(new ParcelableSelectable<String>("test1", 0))
                       .pass(new ParcelableSelectable<Integer>(1, 1))
                       .close();
        AndroidChannels.combine(3, channel1, channel2)
                       .buildChannels()
                       .pass(new ParcelableSelectable<String>("test2", 3))
                       .pass(new ParcelableSelectable<Integer>(2, 4))
                       .close();
        AndroidChannels.combine(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                       .buildChannels()
                       .pass(new ParcelableSelectable<String>("test3", 0))
                       .pass(new ParcelableSelectable<Integer>(3, 1))
                       .close();
        AndroidChannels.combine(-5, Arrays.<Channel<?, ?>>asList(channel1, channel2))
                       .buildChannels()
                       .pass(new ParcelableSelectable<String>("test4", -5))
                       .pass(new ParcelableSelectable<Integer>(4, -4))
                       .close();
        final HashMap<Integer, Channel<?, ?>> map = new HashMap<Integer, Channel<?, ?>>(2);
        map.put(31, channel1);
        map.put(17, channel2);
        AndroidChannels.combine(map)
                       .buildChannels()
                       .pass(new ParcelableSelectable<String>("test5", 31))
                       .pass(new ParcelableSelectable<Integer>(5, 17))
                       .close();
        assertThat(channel1.close().after(seconds(10)).all()).containsExactly("test1", "test2",
                "test3", "test4", "test5");
        assertThat(channel2.close().after(seconds(10)).all()).containsExactly(1, 2, 3, 4, 5);
    }

    public void testCombineAbort() {

        Channel<String, String> channel1;
        Channel<Integer, Integer> channel2;
        channel1 = JRoutineService.on(serviceFrom(getActivity()))
                                  .with(factoryOf(PassingString.class))
                                  .asyncCall()
                                  .sortedByCall();
        channel2 = JRoutineService.on(serviceFrom(getActivity()))
                                  .with(factoryOf(PassingInteger.class))
                                  .asyncCall()
                                  .sortedByCall();
        AndroidChannels.combine(channel1, channel2).buildChannels().abort();

        try {

            channel1.close().after(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.close().after(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = JRoutineService.on(serviceFrom(getActivity()))
                                  .with(factoryOf(PassingString.class))
                                  .asyncCall()
                                  .sortedByCall();
        channel2 = JRoutineService.on(serviceFrom(getActivity()))
                                  .with(factoryOf(PassingInteger.class))
                                  .asyncCall()
                                  .sortedByCall();
        AndroidChannels.combine(3, channel1, channel2).buildChannels().abort();

        try {

            channel1.close().after(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.close().after(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = JRoutineService.on(serviceFrom(getActivity()))
                                  .with(factoryOf(PassingString.class))
                                  .asyncCall()
                                  .sortedByCall();
        channel2 = JRoutineService.on(serviceFrom(getActivity()))
                                  .with(factoryOf(PassingInteger.class))
                                  .asyncCall()
                                  .sortedByCall();
        AndroidChannels.combine(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                       .buildChannels()
                       .abort();

        try {

            channel1.close().after(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.close().after(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = JRoutineService.on(serviceFrom(getActivity()))
                                  .with(factoryOf(PassingString.class))
                                  .asyncCall()
                                  .sortedByCall();
        channel2 = JRoutineService.on(serviceFrom(getActivity()))
                                  .with(factoryOf(PassingInteger.class))
                                  .asyncCall()
                                  .sortedByCall();
        AndroidChannels.combine(-5, Arrays.<Channel<?, ?>>asList(channel1, channel2))
                       .buildChannels()
                       .abort();

        try {

            channel1.close().after(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.close().after(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = JRoutineService.on(serviceFrom(getActivity()))
                                  .with(factoryOf(PassingString.class))
                                  .asyncCall()
                                  .sortedByCall();
        channel2 = JRoutineService.on(serviceFrom(getActivity()))
                                  .with(factoryOf(PassingInteger.class))
                                  .asyncCall()
                                  .sortedByCall();
        final HashMap<Integer, Channel<?, ?>> map = new HashMap<Integer, Channel<?, ?>>(2);
        map.put(31, channel1);
        map.put(17, channel2);
        AndroidChannels.combine(map).buildChannels().abort();

        try {

            channel1.close().after(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.close().after(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testCombineError() {

        try {

            AndroidChannels.combine();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            AndroidChannels.combine(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            AndroidChannels.combine(Collections.<Channel<?, ?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            AndroidChannels.combine(0, Collections.<Channel<?, ?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            AndroidChannels.combine(Collections.<Integer, Channel<?, ?>>emptyMap());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testConstructor() {

        boolean failed = false;
        try {
            new AndroidChannels();
            failed = true;

        } catch (final Throwable ignored) {

        }

        assertThat(failed).isFalse();
    }

    public void testDistribute() {

        final Channel<String, String> channel1 = JRoutineService.on(serviceFrom(getActivity()))
                                                                .with(factoryOf(
                                                                        PassingString.class))
                                                                .asyncCall()
                                                                .sortedByCall();
        final Channel<String, String> channel2 = JRoutineService.on(serviceFrom(getActivity()))
                                                                .with(factoryOf(
                                                                        PassingString.class))
                                                                .asyncCall()
                                                                .sortedByCall();
        AndroidChannels.distribute(channel1, channel2)
                       .buildChannels()
                       .pass(Arrays.asList("test1-1", "test1-2"))
                       .close();
        AndroidChannels.distribute(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                       .buildChannels()
                       .pass(Arrays.asList("test2-1", "test2-2"))
                       .close();
        AndroidChannels.distribute(channel1, channel2)
                       .buildChannels()
                       .pass(Collections.singletonList("test3-1"))
                       .close();
        assertThat(channel1.close().after(seconds(10)).all()).containsExactly("test1-1", "test2-1",
                "test3-1");
        assertThat(channel2.close().after(seconds(10)).all()).containsExactly("test1-2", "test2-2");
    }

    public void testDistributeAbort() {

        Channel<String, String> channel1;
        Channel<String, String> channel2;
        channel1 = JRoutineService.on(serviceFrom(getActivity()))
                                  .with(factoryOf(PassingString.class))
                                  .asyncCall()
                                  .sortedByCall();
        channel2 = JRoutineService.on(serviceFrom(getActivity()))
                                  .with(factoryOf(PassingString.class))
                                  .asyncCall()
                                  .sortedByCall();
        AndroidChannels.distribute(channel1, channel2).buildChannels().abort();

        try {

            channel1.close().after(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.close().after(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = JRoutineService.on(serviceFrom(getActivity()))
                                  .with(factoryOf(PassingString.class))
                                  .asyncCall()
                                  .sortedByCall();
        channel2 = JRoutineService.on(serviceFrom(getActivity()))
                                  .with(factoryOf(PassingString.class))
                                  .asyncCall()
                                  .sortedByCall();
        AndroidChannels.distribute(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                       .buildChannels()
                       .abort();

        try {

            channel1.close().after(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.close().after(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testDistributeError() {

        final Channel<String, String> channel1 = JRoutineService.on(serviceFrom(getActivity()))
                                                                .with(factoryOf(
                                                                        PassingString.class))
                                                                .asyncCall()
                                                                .sortedByCall();
        AndroidChannels.distribute(channel1)
                       .buildChannels()
                       .pass(Arrays.asList("test1-1", "test1-2"))
                       .close();

        try {

            channel1.close().after(seconds(10)).all();

            fail();

        } catch (final InvocationException ignored) {

        }

        try {

            AndroidChannels.distribute();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            AndroidChannels.distribute(Collections.<Channel<?, ?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testDistributePlaceholder() {

        final Channel<String, String> channel1 = JRoutineService.on(serviceFrom(getActivity()))
                                                                .with(factoryOf(
                                                                        PassingString.class))
                                                                .asyncCall()
                                                                .sortedByCall();
        final Channel<String, String> channel2 = JRoutineService.on(serviceFrom(getActivity()))
                                                                .with(factoryOf(
                                                                        PassingString.class))
                                                                .asyncCall()
                                                                .sortedByCall();
        AndroidChannels.distribute((Object) null, channel1, channel2)
                       .buildChannels()
                       .pass(Arrays.asList("test1-1", "test1-2"))
                       .close();
        final String placeholder = "placeholder";
        AndroidChannels.distribute((Object) placeholder,
                Arrays.<Channel<?, ?>>asList(channel1, channel2))
                       .buildChannels()
                       .pass(Arrays.asList("test2-1", "test2-2"))
                       .close();
        AndroidChannels.distribute(placeholder, channel1, channel2)
                       .buildChannels()
                       .pass(Collections.singletonList("test3-1"))
                       .close();
        assertThat(channel1.close().after(seconds(10)).all()).containsExactly("test1-1", "test2-1",
                "test3-1");
        assertThat(channel2.close().after(seconds(10)).all()).containsExactly("test1-2", "test2-2",
                placeholder);
    }

    public void testDistributePlaceholderAbort() {

        Channel<String, String> channel1;
        Channel<String, String> channel2;
        channel1 = JRoutineService.on(serviceFrom(getActivity()))
                                  .with(factoryOf(PassingString.class))
                                  .asyncCall()
                                  .sortedByCall();
        channel2 = JRoutineService.on(serviceFrom(getActivity()))
                                  .with(factoryOf(PassingString.class))
                                  .asyncCall()
                                  .sortedByCall();
        AndroidChannels.distribute((Object) null, channel1, channel2).buildChannels().abort();

        try {

            channel1.close().after(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.close().after(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = JRoutineService.on(serviceFrom(getActivity()))
                                  .with(factoryOf(PassingString.class))
                                  .asyncCall()
                                  .sortedByCall();
        channel2 = JRoutineService.on(serviceFrom(getActivity()))
                                  .with(factoryOf(PassingString.class))
                                  .asyncCall()
                                  .sortedByCall();
        AndroidChannels.distribute(null, Arrays.<Channel<?, ?>>asList(channel1, channel2))
                       .buildChannels()
                       .abort();

        try {

            channel1.close().after(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.close().after(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testDistributePlaceholderError() {

        final Channel<String, String> channel1 = JRoutineService.on(serviceFrom(getActivity()))
                                                                .with(factoryOf(
                                                                        PassingString.class))
                                                                .asyncCall()
                                                                .sortedByCall();
        AndroidChannels.distribute((Object) null, channel1)
                       .buildChannels()
                       .pass(Arrays.asList("test1-1", "test1-2"))
                       .close();

        try {

            channel1.close().after(seconds(10)).all();

            fail();

        } catch (final InvocationException ignored) {

        }

        try {

            AndroidChannels.distribute(new Object());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            AndroidChannels.distribute(null, Collections.<Channel<?, ?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @SuppressWarnings("unchecked")
    public void testInputSelect() {

        final Channel<ParcelableSelectable<String>, ParcelableSelectable<String>> channel =
                JRoutineCore.io().buildChannel();
        AndroidChannels.selectParcelableInput(channel, 33)
                       .buildChannels()
                       .pass("test1", "test2", "test3")
                       .close();
        channel.close();
        assertThat(channel.after(seconds(10)).all()).containsExactly(
                new ParcelableSelectable<String>("test1", 33),
                new ParcelableSelectable<String>("test2", 33),
                new ParcelableSelectable<String>("test3", 33));
    }

    public void testInputSelectAbort() {

        final Channel<ParcelableSelectable<String>, ParcelableSelectable<String>> channel =
                JRoutineCore.io().buildChannel();
        AndroidChannels.selectParcelableInput(channel, 33)
                       .buildChannels()
                       .pass("test1", "test2", "test3")
                       .abort();
        channel.close();

        try {

            channel.after(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @SuppressWarnings("unchecked")
    public void testInputToSelectable() {

        final Channel<String, String> channel = JRoutineCore.io().buildChannel();
        AndroidChannels.selectableInput(channel, 33)
                       .buildChannels()
                       .pass(new ParcelableSelectable<String>("test1", 33),
                               new ParcelableSelectable<String>("test2", -33),
                               new ParcelableSelectable<String>("test3", 33),
                               new ParcelableSelectable<String>("test4", 333))
                       .close();
        channel.close();
        assertThat(channel.after(seconds(10)).all()).containsExactly("test1", "test3");
    }

    public void testInputToSelectableAbort() {

        final Channel<String, String> channel = JRoutineCore.io().buildChannel();
        AndroidChannels.selectableInput(channel, 33).buildChannels().abort();
        channel.close();

        try {

            channel.after(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testJoin() {

        final ChannelBuilder builder = JRoutineCore.io();
        final Routine<List<?>, Character> routine = JRoutineService.on(serviceFrom(getActivity()))
                                                                   .with(factoryOf(CharAt.class))
                                                                   .buildRoutine();
        Channel<String, String> channel1;
        Channel<Integer, Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.sortedByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.sortedByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.asyncCall(AndroidChannels.join(channel1, channel2).buildChannels())
                          .after(seconds(10))
                          .all()).containsExactly('s', '2');
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.sortedByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.sortedByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.asyncCall(
                AndroidChannels.join(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                               .buildChannels()).after(seconds(10)).all()).containsExactly('s',
                '2');
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.sortedByCall()
                .after(millis(100))
                .pass("testtest")
                .pass("test2")
                .pass("test3")
                .close();
        channel2.sortedByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.asyncCall(AndroidChannels.join(channel1, channel2).buildChannels())
                          .after(seconds(10))
                          .all()).containsExactly('s', '2');
    }

    public void testJoinAbort() {

        final ChannelBuilder builder = JRoutineCore.io();
        final Routine<List<?>, Character> routine = JRoutineService.on(serviceFrom(getActivity()))
                                                                   .with(factoryOf(CharAt.class))
                                                                   .buildRoutine();
        Channel<String, String> channel1;
        Channel<Integer, Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.sortedByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.sortedByCall().abort();

        try {

            routine.asyncCall(AndroidChannels.join(channel1, channel2).buildChannels())
                   .after(seconds(10))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.sortedByCall().abort();
        channel2.sortedByCall().after(millis(110)).pass(6).pass(4).close();

        try {

            routine.asyncCall(AndroidChannels.join(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                                             .buildChannels()).after(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testJoinError() {

        try {

            AndroidChannels.join();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            AndroidChannels.join(Collections.<Channel<?, ?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testJoinPlaceholder() {

        final ChannelBuilder builder = JRoutineCore.io();
        final Routine<List<?>, Character> routine = JRoutineService.on(serviceFrom(getActivity()))
                                                                   .with(factoryOf(CharAt.class))
                                                                   .buildRoutine();
        Channel<String, String> channel1;
        Channel<Integer, Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.sortedByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.sortedByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.asyncCall(
                AndroidChannels.join(new Object(), channel1, channel2).buildChannels())
                          .after(seconds(10))
                          .all()).containsExactly('s', '2');
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.sortedByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.sortedByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.asyncCall(
                AndroidChannels.join(null, Arrays.<Channel<?, ?>>asList(channel1, channel2))
                               .buildChannels()).after(seconds(10)).all()).containsExactly('s',
                '2');
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.sortedByCall()
                .after(millis(100))
                .pass("testtest")
                .pass("test2")
                .pass("test3")
                .close();
        channel2.sortedByCall().after(millis(110)).pass(6).pass(4).close();

        try {

            routine.asyncCall(
                    AndroidChannels.join(new Object(), channel1, channel2).buildChannels())
                   .after(seconds(10))
                   .all();

            fail();

        } catch (final InvocationException ignored) {

        }
    }

    public void testJoinPlaceholderAbort() {

        final ChannelBuilder builder = JRoutineCore.io();
        final Routine<List<?>, Character> routine = JRoutineService.on(serviceFrom(getActivity()))
                                                                   .with(factoryOf(CharAt.class))
                                                                   .buildRoutine();
        Channel<String, String> channel1;
        Channel<Integer, Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.sortedByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.sortedByCall().abort();

        try {

            routine.asyncCall(
                    AndroidChannels.join((Object) null, channel1, channel2).buildChannels())
                   .after(seconds(10))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.sortedByCall().abort();
        channel2.sortedByCall().after(millis(110)).pass(6).pass(4).close();

        try {

            routine.asyncCall(AndroidChannels.join(new Object(),
                    Arrays.<Channel<?, ?>>asList(channel1, channel2)).buildChannels())
                   .after(seconds(10))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testJoinPlaceholderError() {

        try {

            AndroidChannels.join(new Object());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            AndroidChannels.join(null, Collections.<Channel<?, ?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testMap() {

        final ChannelBuilder builder = JRoutineCore.io()
                                                   .apply(builder().withOrder(OrderType.BY_CALL)
                                                                   .buildConfiguration());
        final Channel<String, String> channel1 = builder.buildChannel();
        final Channel<Integer, Integer> channel2 = builder.buildChannel();

        final Channel<?, ? extends ParcelableSelectable<Object>> channel =
                AndroidChannels.merge(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                               .buildChannels();
        final Channel<?, ParcelableSelectable<Object>> output =
                JRoutineService.on(serviceFrom(getActivity()))
                               .with(factoryOf(Sort.class))
                               .invocationConfiguration()
                               .withInputOrder(OrderType.BY_CALL)
                               .configured()
                               .asyncCall(channel);
        final Map<Integer, Channel<?, Object>> channelMap =
                AndroidChannels.selectOutput(output, Sort.INTEGER, Sort.STRING).buildChannels();

        for (int i = 0; i < 4; i++) {

            final String input = Integer.toString(i);
            channel1.after(millis(20)).pass(input);
            channel2.after(millis(20)).pass(i);
        }

        channel1.close();
        channel2.close();

        assertThat(channelMap.get(Sort.STRING).after(seconds(10)).all()).containsExactly("0", "1",
                "2", "3");
        assertThat(channelMap.get(Sort.INTEGER).after(seconds(10)).all()).containsExactly(0, 1, 2,
                3);
    }

    @SuppressWarnings("unchecked")
    public void testMerge() {

        final ChannelBuilder builder = JRoutineCore.io()
                                                   .apply(builder().withOrder(OrderType.BY_CALL)
                                                                   .buildConfiguration());
        Channel<String, String> channel1;
        Channel<Integer, Integer> channel2;
        Channel<?, ? extends ParcelableSelectable<?>> outputChannel;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = AndroidChannels.merge(-7, channel1, channel2).buildChannels();
        channel1.pass("test1").close();
        channel2.pass(13).close();
        assertThat(outputChannel.after(seconds(10)).all()).containsOnly(
                new ParcelableSelectable<String>("test1", -7),
                new ParcelableSelectable<Integer>(13, -6));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel =
                AndroidChannels.merge(11, Arrays.asList(channel1, channel2)).buildChannels();
        channel2.pass(13).close();
        channel1.pass("test1").close();
        assertThat(outputChannel.after(seconds(10)).all()).containsOnly(
                new ParcelableSelectable<String>("test1", 11),
                new ParcelableSelectable<Integer>(13, 12));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = AndroidChannels.merge(channel1, channel2).buildChannels();
        channel1.pass("test2").close();
        channel2.pass(-17).close();
        assertThat(outputChannel.after(seconds(10)).all()).containsOnly(
                new ParcelableSelectable<String>("test2", 0),
                new ParcelableSelectable<Integer>(-17, 1));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = AndroidChannels.merge(Arrays.asList(channel1, channel2)).buildChannels();
        channel1.pass("test2").close();
        channel2.pass(-17).close();
        assertThat(outputChannel.after(seconds(10)).all()).containsOnly(
                new ParcelableSelectable<String>("test2", 0),
                new ParcelableSelectable<Integer>(-17, 1));
    }

    @SuppressWarnings("unchecked")
    public void testMerge4() {

        final ChannelBuilder builder = JRoutineCore.io()
                                                   .apply(builder().withOrder(OrderType.BY_CALL)
                                                                   .buildConfiguration());
        final Channel<String, String> channel1 = builder.buildChannel();
        final Channel<String, String> channel2 = builder.buildChannel();
        final Channel<String, String> channel3 = builder.buildChannel();
        final Channel<String, String> channel4 = builder.buildChannel();

        final Routine<ParcelableSelectable<String>, String> routine =
                JRoutineCore.with(InvocationFactory.factoryOf(new ClassToken<Amb<String>>() {}))
                            .buildRoutine();
        final Channel<?, String> outputChannel = routine.asyncCall(
                AndroidChannels.merge(Arrays.asList(channel1, channel2, channel3, channel4))
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

    @SuppressWarnings("unchecked")
    public void testMergeAbort() {

        final ChannelBuilder builder = JRoutineCore.io()
                                                   .apply(builder().withOrder(OrderType.BY_CALL)
                                                                   .buildConfiguration());
        Channel<String, String> channel1;
        Channel<Integer, Integer> channel2;
        Channel<?, ? extends ParcelableSelectable<?>> outputChannel;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = AndroidChannels.merge(-7, channel1, channel2).buildChannels();
        channel1.pass("test1").close();
        channel2.abort();

        try {

            outputChannel.after(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel =
                AndroidChannels.merge(11, Arrays.asList(channel1, channel2)).buildChannels();
        channel2.abort();
        channel1.pass("test1").close();

        try {

            outputChannel.after(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = AndroidChannels.merge(channel1, channel2).buildChannels();
        channel1.abort();
        channel2.pass(-17).close();

        try {

            outputChannel.after(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = AndroidChannels.merge(Arrays.asList(channel1, channel2)).buildChannels();
        channel1.pass("test2").close();
        channel2.abort();

        try {

            outputChannel.after(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testMergeError() {

        try {

            AndroidChannels.merge(0, Collections.<Channel<?, Object>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            AndroidChannels.merge(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            AndroidChannels.merge(Collections.<Channel<?, Object>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            AndroidChannels.merge();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @SuppressWarnings("unchecked")
    public void testOutputMap() {

        final Routine<ParcelableSelectable<Object>, ParcelableSelectable<Object>> routine =
                JRoutineService.on(serviceFrom(getActivity()))
                               .with(factoryOf(Sort.class))
                               .buildRoutine();
        Map<Integer, Channel<?, Object>> channelMap;
        Channel<?, ParcelableSelectable<Object>> channel;
        channel = routine.asyncCall(new ParcelableSelectable<Object>("test21", Sort.STRING),
                new ParcelableSelectable<Object>(-11, Sort.INTEGER));
        channelMap = AndroidChannels.selectOutput(channel, Arrays.asList(Sort.INTEGER, Sort.STRING))
                                    .buildChannels();
        assertThat(channelMap.get(Sort.INTEGER).after(seconds(10)).all()).containsOnly(-11);
        assertThat(channelMap.get(Sort.STRING).after(seconds(10)).all()).containsOnly("test21");
        channel = routine.asyncCall(new ParcelableSelectable<Object>(-11, Sort.INTEGER),
                new ParcelableSelectable<Object>("test21", Sort.STRING));
        channelMap =
                AndroidChannels.selectOutput(channel, Sort.INTEGER, Sort.STRING).buildChannels();
        assertThat(channelMap.get(Sort.INTEGER).after(seconds(10)).all()).containsOnly(-11);
        assertThat(channelMap.get(Sort.STRING).after(seconds(10)).all()).containsOnly("test21");
        channel = routine.asyncCall(new ParcelableSelectable<Object>("test21", Sort.STRING),
                new ParcelableSelectable<Object>(-11, Sort.INTEGER));
        channelMap = AndroidChannels.selectOutput(Math.min(Sort.INTEGER, Sort.STRING), 2, channel)
                                    .buildChannels();
        assertThat(channelMap.get(Sort.INTEGER).after(seconds(10)).all()).containsOnly(-11);
        assertThat(channelMap.get(Sort.STRING).after(seconds(10)).all()).containsOnly("test21");
    }

    @SuppressWarnings("unchecked")
    public void testOutputMapAbort() {

        final Routine<ParcelableSelectable<Object>, ParcelableSelectable<Object>> routine =
                JRoutineService.on(serviceFrom(getActivity()))
                               .with(factoryOf(Sort.class))
                               .buildRoutine();
        Map<Integer, Channel<?, Object>> channelMap;
        Channel<?, ParcelableSelectable<Object>> channel;
        channel = routine.asyncCall()
                         .after(millis(100))
                         .pass(new ParcelableSelectable<Object>("test21", Sort.STRING),
                                 new ParcelableSelectable<Object>(-11, Sort.INTEGER));
        channelMap = AndroidChannels.selectOutput(channel, Arrays.asList(Sort.INTEGER, Sort.STRING))
                                    .buildChannels();
        channel.abort();

        try {

            channelMap.get(Sort.STRING).after(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channelMap.get(Sort.INTEGER).after(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel = routine.asyncCall()
                         .after(millis(100))
                         .pass(new ParcelableSelectable<Object>(-11, Sort.INTEGER),
                                 new ParcelableSelectable<Object>("test21", Sort.STRING));
        channelMap =
                AndroidChannels.selectOutput(channel, Sort.INTEGER, Sort.STRING).buildChannels();
        channel.abort();

        try {

            channelMap.get(Sort.STRING).after(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channelMap.get(Sort.INTEGER).after(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel = routine.asyncCall()
                         .after(millis(100))
                         .pass(new ParcelableSelectable<Object>("test21", Sort.STRING),
                                 new ParcelableSelectable<Object>(-11, Sort.INTEGER));
        channelMap = AndroidChannels.selectOutput(Math.min(Sort.INTEGER, Sort.STRING), 2, channel)
                                    .buildChannels();
        channel.abort();

        try {

            channelMap.get(Sort.STRING).after(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channelMap.get(Sort.INTEGER).after(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testOutputMapCache() {

        final Routine<ParcelableSelectable<Object>, ParcelableSelectable<Object>> routine =
                JRoutineService.on(serviceFrom(getActivity()))
                               .with(factoryOf(Sort.class))
                               .buildRoutine();
        final Channel<?, ParcelableSelectable<Object>> channel = routine.asyncCall();
        final Map<Integer, Channel<?, Object>> channelMap =
                AndroidChannels.selectOutput(channel, Arrays.asList(Sort.INTEGER, Sort.STRING))
                               .buildChannels();
        assertThat(channelMap).isEqualTo(
                AndroidChannels.selectOutput(channel, Sort.INTEGER, Sort.STRING).buildChannels());
    }

    public void testOutputMapError() {

        try {

            AndroidChannels.selectOutput(0, 0, JRoutineService.on(serviceFrom(getActivity()))
                                                              .with(factoryOf(Sort.class))
                                                              .asyncCall());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @SuppressWarnings("unchecked")
    public void testOutputSelect() {

        final Channel<ParcelableSelectable<String>, ParcelableSelectable<String>> channel =
                JRoutineCore.io().buildChannel();
        final Channel<?, String> outputChannel =
                AndroidChannels.selectOutput(channel, 33).buildChannels().get(33);
        channel.pass(new ParcelableSelectable<String>("test1", 33),
                new ParcelableSelectable<String>("test2", -33),
                new ParcelableSelectable<String>("test3", 33),
                new ParcelableSelectable<String>("test4", 333));
        channel.close();
        assertThat(outputChannel.after(seconds(10)).all()).containsExactly("test1", "test3");
    }

    public void testOutputSelectAbort() {

        final Channel<ParcelableSelectable<String>, ParcelableSelectable<String>> channel =
                JRoutineCore.io().buildChannel();
        final Channel<?, String> outputChannel =
                AndroidChannels.selectOutput(channel, 33).buildChannels().get(33);
        channel.abort();

        try {

            outputChannel.after(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @SuppressWarnings("unchecked")
    public void testOutputToSelectable() {

        final Channel<String, String> channel = JRoutineCore.io().buildChannel();
        channel.pass("test1", "test2", "test3").close();
        assertThat(AndroidChannels.selectableOutput(channel, 33)
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

            AndroidChannels.selectableOutput(channel, 33).buildChannels().after(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    private static class Amb<DATA>
            extends TemplateContextInvocation<ParcelableSelectable<DATA>, DATA> {

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
        public void onRestart() {

            mFirstIndex = NO_INDEX;
        }
    }

    private static class CharAt extends TemplateContextInvocation<List<?>, Character> {

        public void onInput(final List<?> objects, @NotNull final Channel<Character, ?> result) {

            final String text = (String) objects.get(0);
            final int index = ((Integer) objects.get(1));
            result.pass(text.charAt(index));
        }
    }

    private static class PassingInteger extends TemplateContextInvocation<Integer, Integer> {

        public void onInput(final Integer i, @NotNull final Channel<Integer, ?> result) {

            result.pass(i);
        }
    }

    private static class PassingString extends TemplateContextInvocation<String, String> {

        public void onInput(final String s, @NotNull final Channel<String, ?> result) {

            result.pass(s);
        }
    }

    private static class Sort extends
            TemplateContextInvocation<ParcelableSelectable<Object>, ParcelableSelectable<Object>> {

        private static final int INTEGER = 1;

        private static final int STRING = 0;

        public void onInput(final ParcelableSelectable<Object> selectable,
                @NotNull final Channel<ParcelableSelectable<Object>, ?> result) {

            switch (selectable.index) {

                case INTEGER:
                    AndroidChannels.<Object, Integer>selectParcelableInput(result,
                            INTEGER).buildChannels().pass((Integer) selectable.data).close();
                    break;

                case STRING:
                    AndroidChannels.<Object, String>selectParcelableInput(result,
                            STRING).buildChannels().pass((String) selectable.data).close();
                    break;
            }
        }
    }
}
