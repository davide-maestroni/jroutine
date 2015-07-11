/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh.bmd.jrt.android.core;

import android.annotation.TargetApi;
import android.os.Build.VERSION_CODES;
import android.support.v4.util.SparseArrayCompat;
import android.test.ActivityInstrumentationTestCase2;

import com.gh.bmd.jrt.android.core.Channels.ParcelableSelectable;
import com.gh.bmd.jrt.android.invocation.FilterContextInvocation;
import com.gh.bmd.jrt.android.invocation.TemplateContextInvocation;
import com.gh.bmd.jrt.builder.InvocationConfiguration.OrderType;
import com.gh.bmd.jrt.builder.TransportChannelBuilder;
import com.gh.bmd.jrt.channel.AbortException;
import com.gh.bmd.jrt.channel.InputChannel;
import com.gh.bmd.jrt.channel.InvocationChannel;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.ResultChannel;
import com.gh.bmd.jrt.channel.TransportChannel;
import com.gh.bmd.jrt.channel.TransportChannel.TransportOutput;
import com.gh.bmd.jrt.invocation.InvocationException;
import com.gh.bmd.jrt.routine.Routine;
import com.gh.bmd.jrt.util.ClassToken;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import static com.gh.bmd.jrt.android.core.ServiceContext.serviceFrom;
import static com.gh.bmd.jrt.invocation.Invocations.factoryOf;
import static com.gh.bmd.jrt.util.ClassToken.tokenOf;
import static com.gh.bmd.jrt.util.TimeDuration.millis;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Routine channels unit tests.
 * <p/>
 * Created by davide-maestroni on 6/18/15.
 */
@TargetApi(VERSION_CODES.FROYO)
public class ChannelsTest extends ActivityInstrumentationTestCase2<TestActivity> {

    public ChannelsTest() {

        super(TestActivity.class);
    }

    public void testCombine() {

        final InvocationChannel<String, String> channel1 =
                JRoutine.on(serviceFrom(getActivity()), tokenOf(PassingString.class))
                        .asyncInvoke()
                        .orderByCall();
        final InvocationChannel<Integer, Integer> channel2 =
                JRoutine.on(serviceFrom(getActivity()), tokenOf(PassingInteger.class))
                        .asyncInvoke()
                        .orderByCall();
        Channels.combine(channel1, channel2)
                .pass(new ParcelableSelectable<String>("test1", 0))
                .pass(new ParcelableSelectable<Integer>(1, 1));
        Channels.combine(3, channel1, channel2)
                .pass(new ParcelableSelectable<String>("test2", 3))
                .pass(new ParcelableSelectable<Integer>(2, 4));
        Channels.combine(Arrays.<InvocationChannel<?, ?>>asList(channel1, channel2))
                .pass(new ParcelableSelectable<String>("test3", 0))
                .pass(new ParcelableSelectable<Integer>(3, 1));
        Channels.combine(-5, Arrays.<InvocationChannel<?, ?>>asList(channel1, channel2))
                .pass(new ParcelableSelectable<String>("test4", -5))
                .pass(new ParcelableSelectable<Integer>(4, -4));
        final HashMap<Integer, InvocationChannel<?, ?>> map =
                new HashMap<Integer, InvocationChannel<?, ?>>(2);
        map.put(31, channel1);
        map.put(17, channel2);
        Channels.combine(map)
                .pass(new ParcelableSelectable<String>("test5", 31))
                .pass(new ParcelableSelectable<Integer>(5, 17));
        assertThat(channel1.result().eventually().all()).containsExactly("test1", "test2", "test3",
                                                                         "test4", "test5");
        assertThat(channel2.result().eventually().all()).containsExactly(1, 2, 3, 4, 5);
    }

    public void testCombineAbort() {

        InvocationChannel<String, String> channel1;
        InvocationChannel<Integer, Integer> channel2;
        channel1 = JRoutine.on(serviceFrom(getActivity()), tokenOf(PassingString.class))
                           .asyncInvoke()
                           .orderByCall();
        channel2 = JRoutine.on(serviceFrom(getActivity()), tokenOf(PassingInteger.class))
                           .asyncInvoke()
                           .orderByCall();
        Channels.combine(channel1, channel2).abort();

        try {

            channel1.result().eventually().next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.result().eventually().next();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = JRoutine.on(serviceFrom(getActivity()), tokenOf(PassingString.class))
                           .asyncInvoke()
                           .orderByCall();
        channel2 = JRoutine.on(serviceFrom(getActivity()), tokenOf(PassingInteger.class))
                           .asyncInvoke()
                           .orderByCall();
        Channels.combine(3, channel1, channel2).abort();

        try {

            channel1.result().eventually().next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.result().eventually().next();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = JRoutine.on(serviceFrom(getActivity()), tokenOf(PassingString.class))
                           .asyncInvoke()
                           .orderByCall();
        channel2 = JRoutine.on(serviceFrom(getActivity()), tokenOf(PassingInteger.class))
                           .asyncInvoke()
                           .orderByCall();
        Channels.combine(Arrays.<InvocationChannel<?, ?>>asList(channel1, channel2)).abort();

        try {

            channel1.result().eventually().next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.result().eventually().next();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = JRoutine.on(serviceFrom(getActivity()), tokenOf(PassingString.class))
                           .asyncInvoke()
                           .orderByCall();
        channel2 = JRoutine.on(serviceFrom(getActivity()), tokenOf(PassingInteger.class))
                           .asyncInvoke()
                           .orderByCall();
        Channels.combine(-5, Arrays.<InvocationChannel<?, ?>>asList(channel1, channel2)).abort();

        try {

            channel1.result().eventually().next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.result().eventually().next();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = JRoutine.on(serviceFrom(getActivity()), tokenOf(PassingString.class))
                           .asyncInvoke()
                           .orderByCall();
        channel2 = JRoutine.on(serviceFrom(getActivity()), tokenOf(PassingInteger.class))
                           .asyncInvoke()
                           .orderByCall();
        final HashMap<Integer, InvocationChannel<?, ?>> map =
                new HashMap<Integer, InvocationChannel<?, ?>>(2);
        map.put(31, channel1);
        map.put(17, channel2);
        Channels.combine(map).abort();

        try {

            channel1.result().eventually().next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.result().eventually().next();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testCombineError() {

        try {

            Channels.combine();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Channels.combine(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Channels.combine(Collections.<InputChannel<?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Channels.combine(0, Collections.<InputChannel<?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Channels.combine(Collections.<Integer, InputChannel<?>>emptyMap());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testDistribute() {

        final InvocationChannel<String, String> channel1 =
                JRoutine.on(serviceFrom(getActivity()), tokenOf(PassingString.class))
                        .asyncInvoke()
                        .orderByCall();
        final InvocationChannel<String, String> channel2 =
                JRoutine.on(serviceFrom(getActivity()), tokenOf(PassingString.class))
                        .asyncInvoke()
                        .orderByCall();
        Channels.distribute(channel1, channel2).pass(Arrays.<Object>asList("test1-1", "test1-2"));
        Channels.distribute(Arrays.<InputChannel<?>>asList(channel1, channel2))
                .pass(Arrays.<Object>asList("test2-1", "test2-2"));
        Channels.distribute(channel1, channel2).pass(Collections.<Object>singletonList("test3-1"));
        assertThat(channel1.result().eventually().all()).containsExactly("test1-1", "test2-1",
                                                                         "test3-1");
        assertThat(channel2.result().eventually().all()).containsExactly("test1-2", "test2-2");
    }

    public void testDistributeAbort() {

        InvocationChannel<String, String> channel1;
        InvocationChannel<String, String> channel2;
        channel1 = JRoutine.on(serviceFrom(getActivity()), tokenOf(PassingString.class))
                           .asyncInvoke()
                           .orderByCall();
        channel2 = JRoutine.on(serviceFrom(getActivity()), tokenOf(PassingString.class))
                           .asyncInvoke()
                           .orderByCall();
        Channels.distribute(channel1, channel2).abort();

        try {

            channel1.result().eventually().next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.result().eventually().next();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = JRoutine.on(serviceFrom(getActivity()), tokenOf(PassingString.class))
                           .asyncInvoke()
                           .orderByCall();
        channel2 = JRoutine.on(serviceFrom(getActivity()), tokenOf(PassingString.class))
                           .asyncInvoke()
                           .orderByCall();
        Channels.distribute(Arrays.<InputChannel<?>>asList(channel1, channel2)).abort();

        try {

            channel1.result().eventually().next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.result().eventually().next();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testDistributeAndFlush() {

        final InvocationChannel<String, String> channel1 =
                JRoutine.on(serviceFrom(getActivity()), tokenOf(PassingString.class))
                        .asyncInvoke()
                        .orderByCall();
        final InvocationChannel<String, String> channel2 =
                JRoutine.on(serviceFrom(getActivity()), tokenOf(PassingString.class))
                        .asyncInvoke()
                        .orderByCall();
        Channels.distributeAndFlush(channel1, channel2)
                .pass(Arrays.<Object>asList("test1-1", "test1-2"));
        Channels.distributeAndFlush(Arrays.<InputChannel<?>>asList(channel1, channel2))
                .pass(Arrays.<Object>asList("test2-1", "test2-2"));
        Channels.distributeAndFlush(channel1, channel2)
                .pass(Collections.<Object>singletonList("test3-1"));
        assertThat(channel1.result().eventually().all()).containsExactly("test1-1", "test2-1",
                                                                         "test3-1");
        assertThat(channel2.result().eventually().all()).containsExactly("test1-2", "test2-2",
                                                                         null);
    }

    public void testDistributeAndFlushAbort() {

        InvocationChannel<String, String> channel1;
        InvocationChannel<String, String> channel2;
        channel1 = JRoutine.on(serviceFrom(getActivity()), tokenOf(PassingString.class))
                           .asyncInvoke()
                           .orderByCall();
        channel2 = JRoutine.on(serviceFrom(getActivity()), tokenOf(PassingString.class))
                           .asyncInvoke()
                           .orderByCall();
        Channels.distributeAndFlush(channel1, channel2).abort();

        try {

            channel1.result().eventually().next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.result().eventually().next();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = JRoutine.on(serviceFrom(getActivity()), tokenOf(PassingString.class))
                           .asyncInvoke()
                           .orderByCall();
        channel2 = JRoutine.on(serviceFrom(getActivity()), tokenOf(PassingString.class))
                           .asyncInvoke()
                           .orderByCall();
        Channels.distributeAndFlush(Arrays.<InputChannel<?>>asList(channel1, channel2)).abort();

        try {

            channel1.result().eventually().next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.result().eventually().next();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testDistributeAndFlushError() {

        final InvocationChannel<String, String> channel1 =
                JRoutine.on(serviceFrom(getActivity()), tokenOf(PassingString.class))
                        .asyncInvoke()
                        .orderByCall();
        Channels.distributeAndFlush(channel1).pass(Arrays.<Object>asList("test1-1", "test1-2"));

        try {

            channel1.result().eventually().all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            Channels.distributeAndFlush();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Channels.distributeAndFlush(Collections.<InputChannel<?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testDistributeError() {

        final InvocationChannel<String, String> channel1 =
                JRoutine.on(serviceFrom(getActivity()), tokenOf(PassingString.class))
                        .asyncInvoke()
                        .orderByCall();
        Channels.distribute(channel1).pass(Arrays.<Object>asList("test1-1", "test1-2"));

        try {

            channel1.result().eventually().all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            Channels.distribute();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Channels.distribute(Collections.<InputChannel<?>>emptyList());

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
                JRoutine.on(serviceFrom(getActivity()), tokenOf(Sort.class)).buildRoutine();
        SparseArrayCompat<InputChannel<Object>> channelMap;
        InvocationChannel<ParcelableSelectable<Object>, ParcelableSelectable<Object>> channel;
        channel = routine.asyncInvoke();
        channelMap = Channels.mapParcelable(channel, Arrays.asList(Sort.INTEGER, Sort.STRING));
        channelMap.get(Sort.INTEGER).pass(-11);
        channelMap.get(Sort.STRING).pass("test21");
        assertThat(channel.result().eventually().all()).containsOnlyElementsOf(outputs);
        channel = routine.asyncInvoke();
        channelMap = Channels.mapParcelable(channel, Sort.INTEGER, Sort.STRING);
        channelMap.get(Sort.INTEGER).pass(-11);
        channelMap.get(Sort.STRING).pass("test21");
        assertThat(channel.result().eventually().all()).containsOnlyElementsOf(outputs);
        channel = routine.asyncInvoke();
        channelMap = Channels.mapParcelable(Math.min(Sort.INTEGER, Sort.STRING), 2, channel);
        channelMap.get(Sort.INTEGER).pass(-11);
        channelMap.get(Sort.STRING).pass("test21");
        assertThat(channel.result().eventually().all()).containsOnlyElementsOf(outputs);
    }

    public void testInputMapAbort() {

        final Routine<ParcelableSelectable<Object>, ParcelableSelectable<Object>> routine =
                JRoutine.on(serviceFrom(getActivity()), tokenOf(Sort.class)).buildRoutine();
        SparseArrayCompat<InputChannel<Object>> channelMap;
        InvocationChannel<ParcelableSelectable<Object>, ParcelableSelectable<Object>> channel;
        channel = routine.asyncInvoke();
        channelMap = Channels.mapParcelable(channel, Arrays.asList(Sort.INTEGER, Sort.STRING));
        channelMap.get(Sort.INTEGER).pass(-11);
        channelMap.get(Sort.STRING).abort();

        try {

            channel.result().eventually().all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel = routine.asyncInvoke();
        channelMap = Channels.mapParcelable(channel, Sort.INTEGER, Sort.STRING);
        channelMap.get(Sort.INTEGER).abort();
        channelMap.get(Sort.STRING).pass("test21");

        try {

            channel.result().eventually().all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel = routine.asyncInvoke();
        channelMap = Channels.mapParcelable(Math.min(Sort.INTEGER, Sort.STRING), 2, channel);
        channelMap.get(Sort.INTEGER).abort();
        channelMap.get(Sort.STRING).abort();

        try {

            channel.result().eventually().all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testInputMapError() {

        try {

            Channels.mapParcelable(0, 0,
                                   JRoutine.on(serviceFrom(getActivity()), tokenOf(Sort.class))
                                           .asyncInvoke());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @SuppressWarnings("unchecked")
    public void testInputSelect() {

        final TransportChannel<ParcelableSelectable<String>> channel =
                JRoutine.transport().buildChannel();
        Channels.selectParcelable(channel.input(), 33).pass("test1", "test2", "test3");
        channel.input().close();
        assertThat(channel.output().all()).containsExactly(
                new ParcelableSelectable<String>("test1", 33),
                new ParcelableSelectable<String>("test2", 33),
                new ParcelableSelectable<String>("test3", 33));
    }

    public void testInputSelectAbort() {

        final TransportChannel<ParcelableSelectable<String>> channel =
                JRoutine.transport().buildChannel();
        Channels.selectParcelable(channel.input(), 33).pass("test1", "test2", "test3").abort();
        channel.input().close();

        try {

            channel.output().eventually().all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @SuppressWarnings("unchecked")
    public void testInputToSelectable() {

        final TransportChannel<String> channel = JRoutine.transport().buildChannel();
        Channels.toSelectable(channel.input(), 33)
                .pass(new ParcelableSelectable<String>("test1", 33),
                      new ParcelableSelectable<String>("test2", -33),
                      new ParcelableSelectable<String>("test3", 33),
                      new ParcelableSelectable<String>("test4", 333));
        channel.input().close();
        assertThat(channel.output().eventually().all()).containsExactly("test1", "test3");
    }

    public void testInputToSelectableAbort() {

        final TransportChannel<String> channel = JRoutine.transport().buildChannel();
        Channels.toSelectable(channel.input(), 33).abort();
        channel.input().close();

        try {

            channel.output().eventually().all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testJoin() {

        final TransportChannelBuilder builder = JRoutine.transport();
        final Routine<List<?>, Character> routine =
                JRoutine.on(serviceFrom(getActivity()), tokenOf(CharAt.class)).buildRoutine();
        TransportChannel<String> channel1;
        TransportChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.input().orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.input().orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.asyncCall(Channels.join(channel1.output(), channel2.output()))
                          .eventually()
                          .all()).containsExactly('s', '2');
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.input().orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.input().orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.asyncCall(Channels.join(
                Arrays.<OutputChannel<?>>asList(channel1.output(), channel2.output())))
                          .eventually()
                          .all()).containsExactly('s', '2');
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.input()
                .orderByCall()
                .after(millis(100))
                .pass("testtest")
                .pass("test2")
                .pass("test3")
                .close();
        channel2.input().orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.asyncCall(Channels.join(channel1.output(), channel2.output()))
                          .eventually()
                          .all()).containsExactly('s', '2');
    }

    public void testJoinAbort() {

        final TransportChannelBuilder builder = JRoutine.transport();
        final Routine<List<?>, Character> routine =
                JRoutine.on(serviceFrom(getActivity()), tokenOf(CharAt.class)).buildRoutine();
        TransportChannel<String> channel1;
        TransportChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.input().orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.input().orderByCall().abort();

        try {

            routine.asyncCall(Channels.join(channel1.output(), channel2.output()))
                   .eventually()
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.input().orderByCall().abort();
        channel2.input().orderByCall().after(millis(110)).pass(6).pass(4).close();

        try {

            routine.asyncCall(Channels.join(
                    Arrays.<OutputChannel<?>>asList(channel1.output(), channel2.output())))
                   .eventually()
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testJoinAndFlush() {

        final TransportChannelBuilder builder = JRoutine.transport();
        final Routine<List<?>, Character> routine =
                JRoutine.on(serviceFrom(getActivity()), tokenOf(CharAt.class)).buildRoutine();
        TransportChannel<String> channel1;
        TransportChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.input().orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.input().orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.asyncCall(Channels.joinAndFlush(channel1.output(), channel2.output()))
                          .eventually()
                          .all()).containsExactly('s', '2');
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.input().orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.input().orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.asyncCall(Channels.joinAndFlush(
                Arrays.<OutputChannel<?>>asList(channel1.output(), channel2.output())))
                          .eventually()
                          .all()).containsExactly('s', '2');
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.input()
                .orderByCall()
                .after(millis(100))
                .pass("testtest")
                .pass("test2")
                .pass("test3")
                .close();
        channel2.input().orderByCall().after(millis(110)).pass(6).pass(4).close();

        try {

            routine.asyncCall(Channels.joinAndFlush(channel1.output(), channel2.output()))
                   .eventually()
                   .all();

            fail();

        } catch (final InvocationException ignored) {

        }
    }

    public void testJoinAndFlushAbort() {

        final TransportChannelBuilder builder = JRoutine.transport();
        final Routine<List<?>, Character> routine =
                JRoutine.on(serviceFrom(getActivity()), tokenOf(CharAt.class)).buildRoutine();
        TransportChannel<String> channel1;
        TransportChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.input().orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.input().orderByCall().abort();

        try {

            routine.asyncCall(Channels.joinAndFlush(channel1.output(), channel2.output()))
                   .eventually()
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.input().orderByCall().abort();
        channel2.input().orderByCall().after(millis(110)).pass(6).pass(4).close();

        try {

            routine.asyncCall(Channels.joinAndFlush(
                    Arrays.<OutputChannel<?>>asList(channel1.output(), channel2.output())))
                   .eventually()
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testJoinAndFlushError() {

        try {

            Channels.joinAndFlush();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Channels.joinAndFlush(Collections.<OutputChannel<?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testJoinError() {

        try {

            Channels.join();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Channels.join(Collections.<OutputChannel<?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testMap() {

        final TransportChannelBuilder builder =
                JRoutine.transport().channels().withChannelOrder(OrderType.BY_CALL).set();
        final TransportChannel<String> channel1 = builder.buildChannel();
        final TransportChannel<Integer> channel2 = builder.buildChannel();

        final OutputChannel<? extends ParcelableSelectable<Object>> channel =
                Channels.mergeParcelable(
                        Arrays.<TransportOutput<?>>asList(channel1.output(), channel2.output()));
        final OutputChannel<ParcelableSelectable<Object>> output =
                JRoutine.on(serviceFrom(getActivity()), tokenOf(Sort.class))
                        .invocations()
                        .withInputOrder(OrderType.BY_CALL)
                        .set()
                        .asyncCall(channel);
        final Map<Integer, OutputChannel<Object>> channelMap =
                Channels.map(output, Sort.INTEGER, Sort.STRING);

        for (int i = 0; i < 4; i++) {

            final String input = Integer.toString(i);
            channel1.input().after(millis(20)).pass(input);
            channel2.input().after(millis(20)).pass(i);
        }

        channel1.input().close();
        channel2.input().close();

        assertThat(channelMap.get(Sort.STRING).eventually().all()).containsExactly("0", "1", "2",
                                                                                   "3");
        assertThat(channelMap.get(Sort.INTEGER).eventually().all()).containsExactly(0, 1, 2, 3);
    }

    @SuppressWarnings("unchecked")
    public void testMerge() {

        final TransportChannelBuilder builder =
                JRoutine.transport().channels().withChannelOrder(OrderType.BY_CALL).set();
        TransportChannel<String> channel1;
        TransportChannel<Integer> channel2;
        OutputChannel<? extends ParcelableSelectable<?>> outputChannel;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = Channels.mergeParcelable(-7, channel1.output(), channel2.output());
        channel1.input().pass("test1").close();
        channel2.input().pass(13).close();
        assertThat(outputChannel.eventually().all()).containsOnly(
                new ParcelableSelectable<String>("test1", -7),
                new ParcelableSelectable<Integer>(13, -6));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel =
                Channels.mergeParcelable(11, Arrays.asList(channel1.output(), channel2.output()));
        channel2.input().pass(13).close();
        channel1.input().pass("test1").close();
        assertThat(outputChannel.eventually().all()).containsOnly(
                new ParcelableSelectable<String>("test1", 11),
                new ParcelableSelectable<Integer>(13, 12));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = Channels.mergeParcelable(channel1.output(), channel2.output());
        channel1.input().pass("test2").close();
        channel2.input().pass(-17).close();
        assertThat(outputChannel.eventually().all()).containsOnly(
                new ParcelableSelectable<String>("test2", 0),
                new ParcelableSelectable<Integer>(-17, 1));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel =
                Channels.mergeParcelable(Arrays.asList(channel1.output(), channel2.output()));
        channel1.input().pass("test2").close();
        channel2.input().pass(-17).close();
        assertThat(outputChannel.eventually().all()).containsOnly(
                new ParcelableSelectable<String>("test2", 0),
                new ParcelableSelectable<Integer>(-17, 1));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        final SparseArrayCompat<OutputChannel<?>> channelMap =
                new SparseArrayCompat<OutputChannel<?>>(2);
        channelMap.put(7, channel1.output());
        channelMap.put(-3, channel2.output());
        outputChannel = Channels.mergeParcelable(channelMap);
        channel1.input().pass("test3").close();
        channel2.input().pass(111).close();
        assertThat(outputChannel.eventually().all()).containsOnly(
                new ParcelableSelectable<String>("test3", 7),
                new ParcelableSelectable<Integer>(111, -3));
    }

    @SuppressWarnings("unchecked")
    public void testMerge4() {

        final TransportChannelBuilder builder =
                JRoutine.transport().channels().withChannelOrder(OrderType.BY_CALL).set();
        final TransportChannel<String> channel1 = builder.buildChannel();
        final TransportChannel<String> channel2 = builder.buildChannel();
        final TransportChannel<String> channel3 = builder.buildChannel();
        final TransportChannel<String> channel4 = builder.buildChannel();

        final Routine<ParcelableSelectable<String>, String> routine =
                JRoutine.on(factoryOf(new ClassToken<Amb<String>>() {})).buildRoutine();
        final OutputChannel<String> outputChannel = routine.asyncCall(Channels.mergeParcelable(
                Arrays.asList(channel1.output(), channel2.output(), channel3.output(),
                              channel4.output())));

        for (int i = 0; i < 4; i++) {

            final String input = Integer.toString(i);
            channel1.input().after(millis(20)).pass(input);
            channel2.input().after(millis(20)).pass(input);
            channel3.input().after(millis(20)).pass(input);
            channel4.input().after(millis(20)).pass(input);
        }

        channel1.input().close();
        channel2.input().close();
        channel3.input().close();
        channel4.input().close();

        assertThat(outputChannel.eventually().all()).containsExactly("0", "1", "2", "3");
    }

    @SuppressWarnings("unchecked")
    public void testMergeAbort() {

        final TransportChannelBuilder builder =
                JRoutine.transport().channels().withChannelOrder(OrderType.BY_CALL).set();
        TransportChannel<String> channel1;
        TransportChannel<Integer> channel2;
        OutputChannel<? extends ParcelableSelectable<?>> outputChannel;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = Channels.mergeParcelable(-7, channel1.output(), channel2.output());
        channel1.input().pass("test1").close();
        channel2.input().abort();

        try {

            outputChannel.eventually().all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel =
                Channels.mergeParcelable(11, Arrays.asList(channel1.output(), channel2.output()));
        channel2.input().abort();
        channel1.input().pass("test1").close();

        try {

            outputChannel.eventually().all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = Channels.mergeParcelable(channel1.output(), channel2.output());
        channel1.input().abort();
        channel2.input().pass(-17).close();

        try {

            outputChannel.eventually().all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel =
                Channels.mergeParcelable(Arrays.asList(channel1.output(), channel2.output()));
        channel1.input().pass("test2").close();
        channel2.input().abort();

        try {

            outputChannel.eventually().all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        final SparseArrayCompat<OutputChannel<?>> channelMap =
                new SparseArrayCompat<OutputChannel<?>>(2);
        channelMap.put(7, channel1.output());
        channelMap.put(-3, channel2.output());
        outputChannel = Channels.mergeParcelable(channelMap);
        channel1.input().abort();
        channel2.input().pass(111).close();

        try {

            outputChannel.eventually().all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testMergeError() {

        try {

            Channels.merge(0, Collections.<OutputChannel<Object>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Channels.merge(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Channels.merge(Collections.<OutputChannel<Object>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Channels.merge(Collections.<Integer, OutputChannel<Object>>emptyMap());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Channels.merge();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @SuppressWarnings("unchecked")
    public void testOutputMap() {

        final Routine<ParcelableSelectable<Object>, ParcelableSelectable<Object>> routine =
                JRoutine.on(serviceFrom(getActivity()), tokenOf(Sort.class)).buildRoutine();
        Map<Integer, OutputChannel<Object>> channelMap;
        OutputChannel<ParcelableSelectable<Object>> channel;
        channel = routine.asyncCall(new ParcelableSelectable<Object>("test21", Sort.STRING),
                                    new ParcelableSelectable<Object>(-11, Sort.INTEGER));
        channelMap = Channels.map(channel, Arrays.asList(Sort.INTEGER, Sort.STRING));
        assertThat(channelMap.get(Sort.INTEGER).eventually().all()).containsOnly(-11);
        assertThat(channelMap.get(Sort.STRING).eventually().all()).containsOnly("test21");
        channel = routine.asyncCall(new ParcelableSelectable<Object>(-11, Sort.INTEGER),
                                    new ParcelableSelectable<Object>("test21", Sort.STRING));
        channelMap = Channels.map(channel, Sort.INTEGER, Sort.STRING);
        assertThat(channelMap.get(Sort.INTEGER).eventually().all()).containsOnly(-11);
        assertThat(channelMap.get(Sort.STRING).eventually().all()).containsOnly("test21");
        channel = routine.asyncCall(new ParcelableSelectable<Object>("test21", Sort.STRING),
                                    new ParcelableSelectable<Object>(-11, Sort.INTEGER));
        channelMap = Channels.map(Math.min(Sort.INTEGER, Sort.STRING), 2, channel);
        assertThat(channelMap.get(Sort.INTEGER).eventually().all()).containsOnly(-11);
        assertThat(channelMap.get(Sort.STRING).eventually().all()).containsOnly("test21");
    }

    @SuppressWarnings("unchecked")
    public void testOutputMapAbort() {

        final Routine<ParcelableSelectable<Object>, ParcelableSelectable<Object>> routine =
                JRoutine.on(serviceFrom(getActivity()), tokenOf(Sort.class)).buildRoutine();
        Map<Integer, OutputChannel<Object>> channelMap;
        OutputChannel<ParcelableSelectable<Object>> channel;
        channel = routine.asyncInvoke()
                         .after(millis(100))
                         .pass(new ParcelableSelectable<Object>("test21", Sort.STRING),
                               new ParcelableSelectable<Object>(-11, Sort.INTEGER))
                         .result();
        channelMap = Channels.map(channel, Arrays.asList(Sort.INTEGER, Sort.STRING));
        channel.abort();

        try {

            channelMap.get(Sort.STRING).eventually().all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channelMap.get(Sort.INTEGER).eventually().all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel = routine.asyncInvoke()
                         .after(millis(100))
                         .pass(new ParcelableSelectable<Object>(-11, Sort.INTEGER),
                               new ParcelableSelectable<Object>("test21", Sort.STRING))
                         .result();
        channelMap = Channels.map(channel, Sort.INTEGER, Sort.STRING);
        channel.abort();

        try {

            channelMap.get(Sort.STRING).eventually().all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channelMap.get(Sort.INTEGER).eventually().all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel = routine.asyncInvoke()
                         .after(millis(100))
                         .pass(new ParcelableSelectable<Object>("test21", Sort.STRING),
                               new ParcelableSelectable<Object>(-11, Sort.INTEGER))
                         .result();
        channelMap = Channels.map(Math.min(Sort.INTEGER, Sort.STRING), 2, channel);
        channel.abort();

        try {

            channelMap.get(Sort.STRING).eventually().all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channelMap.get(Sort.INTEGER).eventually().all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public void testOutputMapError() {

        try {

            Channels.map(0, 0,
                         JRoutine.on(serviceFrom(getActivity()), tokenOf(Sort.class)).asyncCall());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @SuppressWarnings("unchecked")
    public void testOutputSelect() {

        final TransportChannel<ParcelableSelectable<String>> channel =
                JRoutine.transport().buildChannel();
        final OutputChannel<String> outputChannel = Channels.select(channel.output(), 33);
        channel.input()
               .pass(new ParcelableSelectable<String>("test1", 33),
                     new ParcelableSelectable<String>("test2", -33),
                     new ParcelableSelectable<String>("test3", 33),
                     new ParcelableSelectable<String>("test4", 333));
        channel.input().close();
        assertThat(outputChannel.all()).containsExactly("test1", "test3");
    }

    public void testOutputSelectAbort() {

        final TransportChannel<ParcelableSelectable<String>> channel =
                JRoutine.transport().buildChannel();
        final OutputChannel<String> outputChannel = Channels.select(channel.output(), 33);
        channel.input().abort();

        try {

            outputChannel.eventually().all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @SuppressWarnings("unchecked")
    public void testOutputToSelectable() {

        final TransportChannel<String> channel = JRoutine.transport().buildChannel();
        channel.input().pass("test1", "test2", "test3").close();
        assertThat(Channels.toSelectable(channel.output(), 33).eventually().all()).containsExactly(
                new ParcelableSelectable<String>("test1", 33),
                new ParcelableSelectable<String>("test2", 33),
                new ParcelableSelectable<String>("test3", 33));
    }

    public void testOutputToSelectableAbort() {

        final TransportChannel<String> channel = JRoutine.transport().buildChannel();
        channel.input().pass("test1", "test2", "test3").abort();

        try {

            Channels.toSelectable(channel.output(), 33).eventually().all();

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
                @Nonnull final ResultChannel<DATA> result) {

            if (mFirstIndex == NO_INDEX) {

                mFirstIndex = input.index;
                result.pass(input.data);

            } else if (mFirstIndex == input.index) {

                result.pass(input.data);
            }
        }
    }

    private static class CharAt extends FilterContextInvocation<List<?>, Character> {

        public void onInput(final List<?> objects, @Nonnull final ResultChannel<Character> result) {

            final String text = (String) objects.get(0);
            final int index = ((Integer) objects.get(1));
            result.pass(text.charAt(index));
        }
    }

    private static class PassingInteger extends FilterContextInvocation<Integer, Integer> {

        public void onInput(final Integer i, @Nonnull final ResultChannel<Integer> result) {

            result.pass(i);
        }
    }

    private static class PassingString extends FilterContextInvocation<String, String> {

        public void onInput(final String s, @Nonnull final ResultChannel<String> result) {

            result.pass(s);
        }
    }

    private static class Sort extends
            FilterContextInvocation<ParcelableSelectable<Object>, ParcelableSelectable<Object>> {

        private static final int INTEGER = 1;

        private static final int STRING = 0;

        public void onInput(final ParcelableSelectable<Object> selectable,
                @Nonnull final ResultChannel<ParcelableSelectable<Object>> result) {

            switch (selectable.index) {

                case INTEGER:
                    Channels.<Object, Integer>selectParcelable(result, INTEGER)
                            .pass((Integer) selectable.data);
                    break;

                case STRING:
                    Channels.<Object, String>selectParcelable(result, STRING)
                            .pass((String) selectable.data);
                    break;
            }
        }
    }
}
