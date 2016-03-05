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

package com.github.dm.jrt.android.core.channel;

import android.annotation.TargetApi;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.core.JRoutineService;
import com.github.dm.jrt.android.invocation.FilterContextInvocation;
import com.github.dm.jrt.android.invocation.TemplateContextInvocation;
import com.github.dm.jrt.builder.IOChannelBuilder;
import com.github.dm.jrt.builder.InvocationConfiguration.OrderType;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel.InputChannel;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.channel.InvocationChannel;
import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.invocation.InvocationException;
import com.github.dm.jrt.invocation.InvocationFactories;
import com.github.dm.jrt.routine.Routine;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.dm.jrt.android.core.ServiceContext.serviceFrom;
import static com.github.dm.jrt.android.core.TargetInvocationFactory.factoryOf;
import static com.github.dm.jrt.core.util.TimeDuration.millis;
import static com.github.dm.jrt.core.util.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Routine channels unit tests.
 * <p/>
 * Created by davide-maestroni on 06/18/2015.
 */
@TargetApi(VERSION_CODES.FROYO)
public class AndroidChannelsTest extends ActivityInstrumentationTestCase2<TestActivity> {

    public AndroidChannelsTest() {

        super(TestActivity.class);
    }

    public void testCombine() {

        final InvocationChannel<String, String> channel1 =
                JRoutineService.with(serviceFrom(getActivity()))
                               .on(factoryOf(PassingString.class))
                               .asyncInvoke()
                               .orderByCall();
        final InvocationChannel<Integer, Integer> channel2 =
                JRoutineService.with(serviceFrom(getActivity()))
                               .on(factoryOf(PassingInteger.class))
                               .asyncInvoke()
                               .orderByCall();
        AndroidChannels.combine(channel1, channel2)
                       .build()
                       .pass(new ParcelableSelectable<String>("test1", 0))
                       .pass(new ParcelableSelectable<Integer>(1, 1))
                       .close();
        AndroidChannels.combine(3, channel1, channel2)
                       .build()
                       .pass(new ParcelableSelectable<String>("test2", 3))
                       .pass(new ParcelableSelectable<Integer>(2, 4))
                       .close();
        AndroidChannels.combine(Arrays.<InvocationChannel<?, ?>>asList(channel1, channel2))
                       .build()
                       .pass(new ParcelableSelectable<String>("test3", 0))
                       .pass(new ParcelableSelectable<Integer>(3, 1))
                       .close();
        AndroidChannels.combine(-5, Arrays.<InvocationChannel<?, ?>>asList(channel1, channel2))
                       .build()
                       .pass(new ParcelableSelectable<String>("test4", -5))
                       .pass(new ParcelableSelectable<Integer>(4, -4))
                       .close();
        final HashMap<Integer, InvocationChannel<?, ?>> map =
                new HashMap<Integer, InvocationChannel<?, ?>>(2);
        map.put(31, channel1);
        map.put(17, channel2);
        AndroidChannels.combine(map)
                       .build()
                       .pass(new ParcelableSelectable<String>("test5", 31))
                       .pass(new ParcelableSelectable<Integer>(5, 17))
                       .close();
        assertThat(channel1.result().afterMax(seconds(10)).all()).containsExactly("test1", "test2",
                                                                                  "test3", "test4",
                                                                                  "test5");
        assertThat(channel2.result().afterMax(seconds(10)).all()).containsExactly(1, 2, 3, 4, 5);
    }

    public void testCombineAbort() {

        InvocationChannel<String, String> channel1;
        InvocationChannel<Integer, Integer> channel2;
        channel1 = JRoutineService.with(serviceFrom(getActivity()))
                                  .on(factoryOf(PassingString.class))
                                  .asyncInvoke()
                                  .orderByCall();
        channel2 = JRoutineService.with(serviceFrom(getActivity()))
                                  .on(factoryOf(PassingInteger.class))
                                  .asyncInvoke()
                                  .orderByCall();
        AndroidChannels.combine(channel1, channel2).build().abort();

        try {

            channel1.result().afterMax(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.result().afterMax(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = JRoutineService.with(serviceFrom(getActivity()))
                                  .on(factoryOf(PassingString.class))
                                  .asyncInvoke()
                                  .orderByCall();
        channel2 = JRoutineService.with(serviceFrom(getActivity()))
                                  .on(factoryOf(PassingInteger.class))
                                  .asyncInvoke()
                                  .orderByCall();
        AndroidChannels.combine(3, channel1, channel2).build().abort();

        try {

            channel1.result().afterMax(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.result().afterMax(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = JRoutineService.with(serviceFrom(getActivity()))
                                  .on(factoryOf(PassingString.class))
                                  .asyncInvoke()
                                  .orderByCall();
        channel2 = JRoutineService.with(serviceFrom(getActivity()))
                                  .on(factoryOf(PassingInteger.class))
                                  .asyncInvoke()
                                  .orderByCall();
        AndroidChannels.combine(Arrays.<InvocationChannel<?, ?>>asList(channel1, channel2))
                       .build()
                       .abort();

        try {

            channel1.result().afterMax(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.result().afterMax(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = JRoutineService.with(serviceFrom(getActivity()))
                                  .on(factoryOf(PassingString.class))
                                  .asyncInvoke()
                                  .orderByCall();
        channel2 = JRoutineService.with(serviceFrom(getActivity()))
                                  .on(factoryOf(PassingInteger.class))
                                  .asyncInvoke()
                                  .orderByCall();
        AndroidChannels.combine(-5, Arrays.<InvocationChannel<?, ?>>asList(channel1, channel2))
                       .build()
                       .abort();

        try {

            channel1.result().afterMax(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.result().afterMax(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = JRoutineService.with(serviceFrom(getActivity()))
                                  .on(factoryOf(PassingString.class))
                                  .asyncInvoke()
                                  .orderByCall();
        channel2 = JRoutineService.with(serviceFrom(getActivity()))
                                  .on(factoryOf(PassingInteger.class))
                                  .asyncInvoke()
                                  .orderByCall();
        final HashMap<Integer, InvocationChannel<?, ?>> map =
                new HashMap<Integer, InvocationChannel<?, ?>>(2);
        map.put(31, channel1);
        map.put(17, channel2);
        AndroidChannels.combine(map).build().abort();

        try {

            channel1.result().afterMax(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.result().afterMax(seconds(10)).next();

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

            AndroidChannels.combine(Collections.<InputChannel<?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            AndroidChannels.combine(0, Collections.<InputChannel<?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            AndroidChannels.combine(Collections.<Integer, InputChannel<?>>emptyMap());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testDistribute() {

        final InvocationChannel<String, String> channel1 =
                JRoutineService.with(serviceFrom(getActivity()))
                               .on(factoryOf(PassingString.class))
                               .asyncInvoke()
                               .orderByCall();
        final InvocationChannel<String, String> channel2 =
                JRoutineService.with(serviceFrom(getActivity()))
                               .on(factoryOf(PassingString.class))
                               .asyncInvoke()
                               .orderByCall();
        AndroidChannels.distribute(channel1, channel2)
                       .build()
                       .pass(Arrays.asList("test1-1", "test1-2"))
                       .close();
        AndroidChannels.distribute(Arrays.<InputChannel<?>>asList(channel1, channel2))
                       .build()
                       .pass(Arrays.asList("test2-1", "test2-2"))
                       .close();
        AndroidChannels.distribute(channel1, channel2)
                       .build()
                       .pass(Collections.singletonList("test3-1"))
                       .close();
        assertThat(channel1.result().afterMax(seconds(10)).all()).containsExactly("test1-1",
                                                                                  "test2-1",
                                                                                  "test3-1");
        assertThat(channel2.result().afterMax(seconds(10)).all()).containsExactly("test1-2",
                                                                                  "test2-2");
    }

    public void testDistributeAbort() {

        InvocationChannel<String, String> channel1;
        InvocationChannel<String, String> channel2;
        channel1 = JRoutineService.with(serviceFrom(getActivity()))
                                  .on(factoryOf(PassingString.class))
                                  .asyncInvoke()
                                  .orderByCall();
        channel2 = JRoutineService.with(serviceFrom(getActivity()))
                                  .on(factoryOf(PassingString.class))
                                  .asyncInvoke()
                                  .orderByCall();
        AndroidChannels.distribute(channel1, channel2).build().abort();

        try {

            channel1.result().afterMax(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.result().afterMax(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = JRoutineService.with(serviceFrom(getActivity()))
                                  .on(factoryOf(PassingString.class))
                                  .asyncInvoke()
                                  .orderByCall();
        channel2 = JRoutineService.with(serviceFrom(getActivity()))
                                  .on(factoryOf(PassingString.class))
                                  .asyncInvoke()
                                  .orderByCall();
        AndroidChannels.distribute(Arrays.<InputChannel<?>>asList(channel1, channel2))
                       .build()
                       .abort();

        try {

            channel1.result().afterMax(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.result().afterMax(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testDistributeError() {

        final InvocationChannel<String, String> channel1 =
                JRoutineService.with(serviceFrom(getActivity()))
                               .on(factoryOf(PassingString.class))
                               .asyncInvoke()
                               .orderByCall();
        AndroidChannels.distribute(channel1)
                       .build()
                       .pass(Arrays.asList("test1-1", "test1-2"))
                       .close();

        try {

            channel1.result().afterMax(seconds(10)).all();

            fail();

        } catch (final InvocationException ignored) {

        }

        try {

            AndroidChannels.distribute();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            AndroidChannels.distribute(Collections.<InputChannel<?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testDistributePlaceholder() {

        final InvocationChannel<String, String> channel1 =
                JRoutineService.with(serviceFrom(getActivity()))
                               .on(factoryOf(PassingString.class))
                               .asyncInvoke()
                               .orderByCall();
        final InvocationChannel<String, String> channel2 =
                JRoutineService.with(serviceFrom(getActivity()))
                               .on(factoryOf(PassingString.class))
                               .asyncInvoke()
                               .orderByCall();
        AndroidChannels.distribute((Object) null, channel1, channel2)
                       .build()
                       .pass(Arrays.asList("test1-1", "test1-2"))
                       .close();
        final String placeholder = "placeholder";
        AndroidChannels.distribute((Object) placeholder,
                                   Arrays.<InputChannel<?>>asList(channel1, channel2))
                       .build()
                       .pass(Arrays.asList("test2-1", "test2-2"))
                       .close();
        AndroidChannels.distribute(placeholder, channel1, channel2)
                       .build()
                       .pass(Collections.singletonList("test3-1"))
                       .close();
        assertThat(channel1.result().afterMax(seconds(10)).all()).containsExactly("test1-1",
                                                                                  "test2-1",
                                                                                  "test3-1");
        assertThat(channel2.result().afterMax(seconds(10)).all()).containsExactly("test1-2",
                                                                                  "test2-2",
                                                                                  placeholder);
    }

    public void testDistributePlaceholderAbort() {

        InvocationChannel<String, String> channel1;
        InvocationChannel<String, String> channel2;
        channel1 = JRoutineService.with(serviceFrom(getActivity()))
                                  .on(factoryOf(PassingString.class))
                                  .asyncInvoke()
                                  .orderByCall();
        channel2 = JRoutineService.with(serviceFrom(getActivity()))
                                  .on(factoryOf(PassingString.class))
                                  .asyncInvoke()
                                  .orderByCall();
        AndroidChannels.distribute((Object) null, channel1, channel2).build().abort();

        try {

            channel1.result().afterMax(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.result().afterMax(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = JRoutineService.with(serviceFrom(getActivity()))
                                  .on(factoryOf(PassingString.class))
                                  .asyncInvoke()
                                  .orderByCall();
        channel2 = JRoutineService.with(serviceFrom(getActivity()))
                                  .on(factoryOf(PassingString.class))
                                  .asyncInvoke()
                                  .orderByCall();
        AndroidChannels.distribute(null, Arrays.<InputChannel<?>>asList(channel1, channel2))
                       .build()
                       .abort();

        try {

            channel1.result().afterMax(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.result().afterMax(seconds(10)).next();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testDistributePlaceholderError() {

        final InvocationChannel<String, String> channel1 =
                JRoutineService.with(serviceFrom(getActivity()))
                               .on(factoryOf(PassingString.class))
                               .asyncInvoke()
                               .orderByCall();
        AndroidChannels.distribute((Object) null, channel1)
                       .build()
                       .pass(Arrays.asList("test1-1", "test1-2"))
                       .close();

        try {

            channel1.result().afterMax(seconds(10)).all();

            fail();

        } catch (final InvocationException ignored) {

        }

        try {

            AndroidChannels.distribute(new Object());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            AndroidChannels.distribute(null, Collections.<InputChannel<?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @SuppressWarnings("unchecked")
    public void testInputSelect() {

        final IOChannel<ParcelableSelectable<String>> channel = JRoutineCore.io().buildChannel();
        AndroidChannels.selectParcelable(channel, 33)
                       .build()
                       .pass("test1", "test2", "test3")
                       .close();
        channel.close();
        assertThat(channel.afterMax(seconds(10)).all()).containsExactly(
                new ParcelableSelectable<String>("test1", 33),
                new ParcelableSelectable<String>("test2", 33),
                new ParcelableSelectable<String>("test3", 33));
    }

    public void testInputSelectAbort() {

        final IOChannel<ParcelableSelectable<String>> channel = JRoutineCore.io().buildChannel();
        AndroidChannels.selectParcelable(channel, 33)
                       .build()
                       .pass("test1", "test2", "test3")
                       .abort();
        channel.close();

        try {

            channel.afterMax(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @SuppressWarnings("unchecked")
    public void testInputToSelectable() {

        final IOChannel<String> channel = JRoutineCore.io().buildChannel();
        AndroidChannels.toSelectable(channel.asInput(), 33)
                       .build()
                       .pass(new ParcelableSelectable<String>("test1", 33),
                             new ParcelableSelectable<String>("test2", -33),
                             new ParcelableSelectable<String>("test3", 33),
                             new ParcelableSelectable<String>("test4", 333))
                       .close();
        channel.close();
        assertThat(channel.afterMax(seconds(10)).all()).containsExactly("test1", "test3");
    }

    public void testInputToSelectableAbort() {

        final IOChannel<String> channel = JRoutineCore.io().buildChannel();
        AndroidChannels.toSelectable(channel.asInput(), 33).build().abort();
        channel.close();

        try {

            channel.afterMax(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testJoin() {

        final IOChannelBuilder builder = JRoutineCore.io();
        final Routine<List<?>, Character> routine = JRoutineService.with(serviceFrom(getActivity()))
                                                                   .on(factoryOf(CharAt.class))
                                                                   .buildRoutine();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.asyncCall(AndroidChannels.join(channel1, channel2).build())
                          .afterMax(seconds(10))
                          .all()).containsExactly('s', '2');
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.asyncCall(
                AndroidChannels.join(Arrays.<OutputChannel<?>>asList(channel1, channel2)).build())
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
        assertThat(routine.asyncCall(AndroidChannels.join(channel1, channel2).build())
                          .afterMax(seconds(10))
                          .all()).containsExactly('s', '2');
    }

    public void testJoinAbort() {

        final IOChannelBuilder builder = JRoutineCore.io();
        final Routine<List<?>, Character> routine = JRoutineService.with(serviceFrom(getActivity()))
                                                                   .on(factoryOf(CharAt.class))
                                                                   .buildRoutine();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().abort();

        try {

            routine.asyncCall(AndroidChannels.join(channel1, channel2).build())
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
                    AndroidChannels.join(Arrays.<OutputChannel<?>>asList(channel1, channel2))
                                   .build()).afterMax(seconds(10)).all();

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

            AndroidChannels.join(Collections.<OutputChannel<?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testJoinPlaceholder() {

        final IOChannelBuilder builder = JRoutineCore.io();
        final Routine<List<?>, Character> routine = JRoutineService.with(serviceFrom(getActivity()))
                                                                   .on(factoryOf(CharAt.class))
                                                                   .buildRoutine();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.asyncCall(AndroidChannels.join(new Object(), channel1, channel2).build())
                          .afterMax(seconds(10))
                          .all()).containsExactly('s', '2');
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.asyncCall(
                AndroidChannels.join(null, Arrays.<OutputChannel<?>>asList(channel1, channel2))
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

            routine.asyncCall(AndroidChannels.join(new Object(), channel1, channel2).build())
                   .afterMax(seconds(10))
                   .all();

            fail();

        } catch (final InvocationException ignored) {

        }
    }

    public void testJoinPlaceholderAbort() {

        final IOChannelBuilder builder = JRoutineCore.io();
        final Routine<List<?>, Character> routine = JRoutineService.with(serviceFrom(getActivity()))
                                                                   .on(factoryOf(CharAt.class))
                                                                   .buildRoutine();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().abort();

        try {

            routine.asyncCall(AndroidChannels.join((Object) null, channel1, channel2).build())
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

            routine.asyncCall(AndroidChannels.join(new Object(),
                                                   Arrays.<OutputChannel<?>>asList(channel1,
                                                                                   channel2))
                                             .build()).afterMax(seconds(10)).all();

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

            AndroidChannels.join(null, Collections.<OutputChannel<?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testMap() {

        final IOChannelBuilder builder = JRoutineCore.io()
                                                     .withChannels()
                                                     .withChannelOrder(OrderType.BY_CALL)
                                                     .getConfigured();
        final IOChannel<String> channel1 = builder.buildChannel();
        final IOChannel<Integer> channel2 = builder.buildChannel();

        final OutputChannel<? extends ParcelableSelectable<Object>> channel =
                AndroidChannels.merge(Arrays.<IOChannel<?>>asList(channel1, channel2)).build();
        final OutputChannel<ParcelableSelectable<Object>> output =
                JRoutineService.with(serviceFrom(getActivity()))
                               .on(factoryOf(Sort.class))
                               .withInvocations()
                               .withInputOrder(OrderType.BY_CALL)
                               .getConfigured()
                               .asyncCall(channel);
        final Map<Integer, OutputChannel<Object>> channelMap =
                AndroidChannels.select(output, Sort.INTEGER, Sort.STRING).build();

        for (int i = 0; i < 4; i++) {

            final String input = Integer.toString(i);
            channel1.after(millis(20)).pass(input);
            channel2.after(millis(20)).pass(i);
        }

        channel1.close();
        channel2.close();

        assertThat(channelMap.get(Sort.STRING).afterMax(seconds(10)).all()).containsExactly("0",
                                                                                            "1",
                                                                                            "2",
                                                                                            "3");
        assertThat(channelMap.get(Sort.INTEGER).afterMax(seconds(10)).all()).containsExactly(0, 1,
                                                                                             2, 3);
    }

    @SuppressWarnings("unchecked")
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
        outputChannel = AndroidChannels.merge(-7, channel1, channel2).build();
        channel1.pass("test1").close();
        channel2.pass(13).close();
        assertThat(outputChannel.afterMax(seconds(10)).all()).containsOnly(
                new ParcelableSelectable<String>("test1", -7),
                new ParcelableSelectable<Integer>(13, -6));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = AndroidChannels.merge(11, Arrays.asList(channel1, channel2)).build();
        channel2.pass(13).close();
        channel1.pass("test1").close();
        assertThat(outputChannel.afterMax(seconds(10)).all()).containsOnly(
                new ParcelableSelectable<String>("test1", 11),
                new ParcelableSelectable<Integer>(13, 12));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = AndroidChannels.merge(channel1, channel2).build();
        channel1.pass("test2").close();
        channel2.pass(-17).close();
        assertThat(outputChannel.afterMax(seconds(10)).all()).containsOnly(
                new ParcelableSelectable<String>("test2", 0),
                new ParcelableSelectable<Integer>(-17, 1));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = AndroidChannels.merge(Arrays.asList(channel1, channel2)).build();
        channel1.pass("test2").close();
        channel2.pass(-17).close();
        assertThat(outputChannel.afterMax(seconds(10)).all()).containsOnly(
                new ParcelableSelectable<String>("test2", 0),
                new ParcelableSelectable<Integer>(-17, 1));
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
                JRoutineCore.on(InvocationFactories.factoryOf(new ClassToken<Amb<String>>() {}))
                            .buildRoutine();
        final OutputChannel<String> outputChannel = routine.asyncCall(
                AndroidChannels.merge(Arrays.asList(channel1, channel2, channel3, channel4))
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

    @SuppressWarnings("unchecked")
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
        outputChannel = AndroidChannels.merge(-7, channel1, channel2).build();
        channel1.pass("test1").close();
        channel2.abort();

        try {

            outputChannel.afterMax(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = AndroidChannels.merge(11, Arrays.asList(channel1, channel2)).build();
        channel2.abort();
        channel1.pass("test1").close();

        try {

            outputChannel.afterMax(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = AndroidChannels.merge(channel1, channel2).build();
        channel1.abort();
        channel2.pass(-17).close();

        try {

            outputChannel.afterMax(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = AndroidChannels.merge(Arrays.asList(channel1, channel2)).build();
        channel1.pass("test2").close();
        channel2.abort();

        try {

            outputChannel.afterMax(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testMergeError() {

        try {

            AndroidChannels.merge(0, Collections.<OutputChannel<Object>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            AndroidChannels.merge(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            AndroidChannels.merge(Collections.<OutputChannel<Object>>emptyList());

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
                JRoutineService.with(serviceFrom(getActivity()))
                               .on(factoryOf(Sort.class))
                               .buildRoutine();
        Map<Integer, OutputChannel<Object>> channelMap;
        OutputChannel<ParcelableSelectable<Object>> channel;
        channel = routine.asyncCall(new ParcelableSelectable<Object>("test21", Sort.STRING),
                                    new ParcelableSelectable<Object>(-11, Sort.INTEGER));
        channelMap =
                AndroidChannels.select(channel, Arrays.asList(Sort.INTEGER, Sort.STRING)).build();
        assertThat(channelMap.get(Sort.INTEGER).afterMax(seconds(10)).all()).containsOnly(-11);
        assertThat(channelMap.get(Sort.STRING).afterMax(seconds(10)).all()).containsOnly("test21");
        channel = routine.asyncCall(new ParcelableSelectable<Object>(-11, Sort.INTEGER),
                                    new ParcelableSelectable<Object>("test21", Sort.STRING));
        channelMap = AndroidChannels.select(channel, Sort.INTEGER, Sort.STRING).build();
        assertThat(channelMap.get(Sort.INTEGER).afterMax(seconds(10)).all()).containsOnly(-11);
        assertThat(channelMap.get(Sort.STRING).afterMax(seconds(10)).all()).containsOnly("test21");
        channel = routine.asyncCall(new ParcelableSelectable<Object>("test21", Sort.STRING),
                                    new ParcelableSelectable<Object>(-11, Sort.INTEGER));
        channelMap =
                AndroidChannels.select(Math.min(Sort.INTEGER, Sort.STRING), 2, channel).build();
        assertThat(channelMap.get(Sort.INTEGER).afterMax(seconds(10)).all()).containsOnly(-11);
        assertThat(channelMap.get(Sort.STRING).afterMax(seconds(10)).all()).containsOnly("test21");
    }

    @SuppressWarnings("unchecked")
    public void testOutputMapAbort() {

        final Routine<ParcelableSelectable<Object>, ParcelableSelectable<Object>> routine =
                JRoutineService.with(serviceFrom(getActivity()))
                               .on(factoryOf(Sort.class))
                               .buildRoutine();
        Map<Integer, OutputChannel<Object>> channelMap;
        OutputChannel<ParcelableSelectable<Object>> channel;
        channel = routine.asyncInvoke()
                         .after(millis(100))
                         .pass(new ParcelableSelectable<Object>("test21", Sort.STRING),
                               new ParcelableSelectable<Object>(-11, Sort.INTEGER))
                         .result();
        channelMap =
                AndroidChannels.select(channel, Arrays.asList(Sort.INTEGER, Sort.STRING)).build();
        channel.abort();

        try {

            channelMap.get(Sort.STRING).afterMax(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channelMap.get(Sort.INTEGER).afterMax(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel = routine.asyncInvoke()
                         .after(millis(100))
                         .pass(new ParcelableSelectable<Object>(-11, Sort.INTEGER),
                               new ParcelableSelectable<Object>("test21", Sort.STRING))
                         .result();
        channelMap = AndroidChannels.select(channel, Sort.INTEGER, Sort.STRING).build();
        channel.abort();

        try {

            channelMap.get(Sort.STRING).afterMax(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channelMap.get(Sort.INTEGER).afterMax(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel = routine.asyncInvoke()
                         .after(millis(100))
                         .pass(new ParcelableSelectable<Object>("test21", Sort.STRING),
                               new ParcelableSelectable<Object>(-11, Sort.INTEGER))
                         .result();
        channelMap =
                AndroidChannels.select(Math.min(Sort.INTEGER, Sort.STRING), 2, channel).build();
        channel.abort();

        try {

            channelMap.get(Sort.STRING).afterMax(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channelMap.get(Sort.INTEGER).afterMax(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testOutputMapError() {

        try {

            AndroidChannels.select(0, 0, JRoutineService.with(serviceFrom(getActivity()))
                                                        .on(factoryOf(Sort.class))
                                                        .asyncCall());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @SuppressWarnings("unchecked")
    public void testOutputSelect() {

        final IOChannel<ParcelableSelectable<String>> channel = JRoutineCore.io().buildChannel();
        final OutputChannel<String> outputChannel =
                AndroidChannels.select(channel, 33).build().get(33);
        channel.pass(new ParcelableSelectable<String>("test1", 33),
                     new ParcelableSelectable<String>("test2", -33),
                     new ParcelableSelectable<String>("test3", 33),
                     new ParcelableSelectable<String>("test4", 333));
        channel.close();
        assertThat(outputChannel.afterMax(seconds(10)).all()).containsExactly("test1", "test3");
    }

    public void testOutputSelectAbort() {

        final IOChannel<ParcelableSelectable<String>> channel = JRoutineCore.io().buildChannel();
        final OutputChannel<String> outputChannel =
                AndroidChannels.select(channel, 33).build().get(33);
        channel.abort();

        try {

            outputChannel.afterMax(seconds(10)).all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @SuppressWarnings("unchecked")
    public void testOutputToSelectable() {

        final IOChannel<String> channel = JRoutineCore.io().buildChannel();
        channel.pass("test1", "test2", "test3").close();
        assertThat(AndroidChannels.toSelectable(channel.asOutput(), 33)
                                  .build()
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

            AndroidChannels.toSelectable(channel.asOutput(), 33)
                           .build()
                           .afterMax(seconds(10))
                           .all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    private static class Amb<DATA>
            extends TemplateContextInvocation<ParcelableSelectable<DATA>, DATA> {

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

    private static class CharAt extends FilterContextInvocation<List<?>, Character> {

        private CharAt() {

            super(null);
        }

        public void onInput(final List<?> objects, @NotNull final ResultChannel<Character> result) {

            final String text = (String) objects.get(0);
            final int index = ((Integer) objects.get(1));
            result.pass(text.charAt(index));
        }
    }

    private static class PassingInteger extends FilterContextInvocation<Integer, Integer> {

        private PassingInteger() {

            super(null);
        }

        public void onInput(final Integer i, @NotNull final ResultChannel<Integer> result) {

            result.pass(i);
        }
    }

    private static class PassingString extends FilterContextInvocation<String, String> {

        private PassingString() {

            super(null);
        }

        public void onInput(final String s, @NotNull final ResultChannel<String> result) {

            result.pass(s);
        }
    }

    private static class Sort extends
            FilterContextInvocation<ParcelableSelectable<Object>, ParcelableSelectable<Object>> {

        private static final int INTEGER = 1;

        private static final int STRING = 0;

        private Sort() {

            super(null);
        }

        public void onInput(final ParcelableSelectable<Object> selectable,
                @NotNull final ResultChannel<ParcelableSelectable<Object>> result) {

            switch (selectable.index) {

                case INTEGER:
                    AndroidChannels.<Object, Integer>selectParcelable(result, INTEGER)
                                   .build()
                                   .pass((Integer) selectable.data)
                                   .close();
                    break;

                case STRING:
                    AndroidChannels.<Object, String>selectParcelable(result, STRING)
                                   .build()
                                   .pass((String) selectable.data)
                                   .close();
                    break;
            }
        }
    }
}
