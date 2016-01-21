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
package com.github.dm.jrt.core;

import com.github.dm.jrt.builder.IOChannelBuilder;
import com.github.dm.jrt.builder.InvocationConfiguration.OrderType;
import com.github.dm.jrt.channel.AbortException;
import com.github.dm.jrt.channel.Channel.InputChannel;
import com.github.dm.jrt.channel.Channel.OutputChannel;
import com.github.dm.jrt.channel.IOChannel;
import com.github.dm.jrt.channel.InvocationChannel;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.core.Channels.Selectable;
import com.github.dm.jrt.invocation.FilterInvocation;
import com.github.dm.jrt.invocation.InvocationException;
import com.github.dm.jrt.invocation.PassingInvocation;
import com.github.dm.jrt.invocation.TemplateInvocation;
import com.github.dm.jrt.routine.Routine;
import com.github.dm.jrt.stream.StreamChannel;
import com.github.dm.jrt.stream.Streams;
import com.github.dm.jrt.util.ClassToken;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.dm.jrt.invocation.Invocations.factoryOf;
import static com.github.dm.jrt.util.TimeDuration.millis;
import static com.github.dm.jrt.util.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Routine channels unit tests.
 * <p/>
 * Created by davide-maestroni on 03/18/2015.
 */
public class ChannelsTest {

    @Test
    public void testBlend() {

        IOChannel<String> ioChannel1 = JRoutine.io().of("test1", "test2", "test3");
        IOChannel<String> ioChannel2 = JRoutine.io().of("test4", "test5", "test6");
        assertThat(Channels.blend(ioChannel2, ioChannel1).afterMax(seconds(1)).all()).containsOnly(
                "test1", "test2", "test3", "test4", "test5", "test6");
        ioChannel1 = JRoutine.io().of("test1", "test2", "test3");
        ioChannel2 = JRoutine.io().of("test4", "test5", "test6");
        assertThat(Channels.blend(Arrays.<OutputChannel<?>>asList(ioChannel1, ioChannel2))
                           .afterMax(seconds(1))
                           .all()).containsOnly("test1", "test2", "test3", "test4", "test5",
                                                "test6");
    }

    @Test
    public void testBlendAbort() {

        final IOChannelBuilder builder = JRoutine.io();
        final Routine<Object, Object> routine =
                JRoutine.on(PassingInvocation.factoryOf()).buildRoutine();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().abort();

        try {

            routine.asyncCall(Channels.blend(channel1, channel2)).afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().abort();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();

        try {

            routine.asyncCall(Channels.blend(Arrays.<OutputChannel<?>>asList(channel1, channel2)))
                   .afterMax(seconds(1))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testBlendError() {

        try {

            Channels.blend();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Channels.blend((OutputChannel<?>[]) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Channels.blend(new OutputChannel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Channels.blend(Collections.<OutputChannel<?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Channels.blend((List<OutputChannel<?>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Channels.blend(Collections.<OutputChannel<?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testCombine() {

        final InvocationChannel<String, String> channel1 =
                JRoutine.on(PassingInvocation.<String>factoryOf()).asyncInvoke().orderByCall();
        final InvocationChannel<Integer, Integer> channel2 =
                JRoutine.on(PassingInvocation.<Integer>factoryOf()).asyncInvoke().orderByCall();
        Channels.combine(channel1, channel2)
                .pass(new Selectable<String>("test1", 0))
                .pass(new Selectable<Integer>(1, 1))
                .close();
        Channels.combine(3, channel1, channel2)
                .pass(new Selectable<String>("test2", 3))
                .pass(new Selectable<Integer>(2, 4))
                .close();
        Channels.combine(Arrays.<InvocationChannel<?, ?>>asList(channel1, channel2))
                .pass(new Selectable<String>("test3", 0))
                .pass(new Selectable<Integer>(3, 1))
                .close();
        Channels.combine(-5, Arrays.<InvocationChannel<?, ?>>asList(channel1, channel2))
                .pass(new Selectable<String>("test4", -5))
                .pass(new Selectable<Integer>(4, -4))
                .close();
        final HashMap<Integer, InvocationChannel<?, ?>> map =
                new HashMap<Integer, InvocationChannel<?, ?>>(2);
        map.put(31, channel1);
        map.put(17, channel2);
        Channels.combine(map)
                .pass(new Selectable<String>("test5", 31))
                .pass(new Selectable<Integer>(5, 17))
                .close();
        assertThat(channel1.result().afterMax(seconds(1)).all()).containsExactly("test1", "test2",
                                                                                 "test3", "test4",
                                                                                 "test5");
        assertThat(channel2.result().afterMax(seconds(1)).all()).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    public void testCombineAbort() {

        InvocationChannel<String, String> channel1;
        InvocationChannel<Integer, Integer> channel2;
        channel1 = JRoutine.on(PassingInvocation.<String>factoryOf()).asyncInvoke().orderByCall();
        channel2 = JRoutine.on(PassingInvocation.<Integer>factoryOf()).asyncInvoke().orderByCall();
        Channels.<Object>combine(channel1, channel2).abort();

        try {

            channel1.result().afterMax(seconds(1)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.result().afterMax(seconds(1)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = JRoutine.on(PassingInvocation.<String>factoryOf()).asyncInvoke().orderByCall();
        channel2 = JRoutine.on(PassingInvocation.<Integer>factoryOf()).asyncInvoke().orderByCall();
        Channels.<Object>combine(3, channel1, channel2).abort();

        try {

            channel1.result().afterMax(seconds(1)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.result().afterMax(seconds(1)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = JRoutine.on(PassingInvocation.<String>factoryOf()).asyncInvoke().orderByCall();
        channel2 = JRoutine.on(PassingInvocation.<Integer>factoryOf()).asyncInvoke().orderByCall();
        Channels.combine(Arrays.<InvocationChannel<?, ?>>asList(channel1, channel2)).abort();

        try {

            channel1.result().afterMax(seconds(1)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.result().afterMax(seconds(1)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = JRoutine.on(PassingInvocation.<String>factoryOf()).asyncInvoke().orderByCall();
        channel2 = JRoutine.on(PassingInvocation.<Integer>factoryOf()).asyncInvoke().orderByCall();
        Channels.combine(-5, Arrays.<InvocationChannel<?, ?>>asList(channel1, channel2)).abort();

        try {

            channel1.result().afterMax(seconds(1)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.result().afterMax(seconds(1)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = JRoutine.on(PassingInvocation.<String>factoryOf()).asyncInvoke().orderByCall();
        channel2 = JRoutine.on(PassingInvocation.<Integer>factoryOf()).asyncInvoke().orderByCall();
        final HashMap<Integer, InvocationChannel<?, ?>> map =
                new HashMap<Integer, InvocationChannel<?, ?>>(2);
        map.put(31, channel1);
        map.put(17, channel2);
        Channels.combine(map).abort();

        try {

            channel1.result().afterMax(seconds(1)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.result().afterMax(seconds(1)).next();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @Test
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

        try {

            Channels.combine(new InvocationChannel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Channels.combine(0, new InvocationChannel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Channels.combine(Collections.<InvocationChannel<?, ?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Channels.combine(0, Collections.<InvocationChannel<?, ?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testConcat() {

        IOChannel<String> ioChannel1 = JRoutine.io().of("test1", "test2", "test3");
        IOChannel<String> ioChannel2 = JRoutine.io().of("test4", "test5", "test6");
        assertThat(
                Channels.concat(ioChannel2, ioChannel1).afterMax(seconds(1)).all()).containsExactly(
                "test4", "test5", "test6", "test1", "test2", "test3");
        ioChannel1 = JRoutine.io().of("test1", "test2", "test3");
        ioChannel2 = JRoutine.io().of("test4", "test5", "test6");
        assertThat(Channels.concat(Arrays.<OutputChannel<?>>asList(ioChannel1, ioChannel2))
                           .afterMax(seconds(1))
                           .all()).containsExactly("test1", "test2", "test3", "test4", "test5",
                                                   "test6");
    }

    @Test
    public void testConcatAbort() {

        final IOChannelBuilder builder = JRoutine.io();
        final Routine<Object, Object> routine =
                JRoutine.on(PassingInvocation.factoryOf()).buildRoutine();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().abort();

        try {

            routine.asyncCall(Channels.concat(channel1, channel2)).afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().abort();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();

        try {

            routine.asyncCall(Channels.concat(Arrays.<OutputChannel<?>>asList(channel1, channel2)))
                   .afterMax(seconds(1))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testConcatError() {

        try {

            Channels.concat();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Channels.concat((OutputChannel<?>[]) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Channels.concat(new OutputChannel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Channels.concat(Collections.<OutputChannel<?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Channels.concat((List<OutputChannel<?>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Channels.concat(Collections.<OutputChannel<?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testDistribute() {

        final InvocationChannel<String, String> channel1 =
                JRoutine.on(PassingInvocation.<String>factoryOf()).asyncInvoke().orderByCall();
        final InvocationChannel<String, String> channel2 =
                JRoutine.on(PassingInvocation.<String>factoryOf()).asyncInvoke().orderByCall();
        Channels.distribute(channel1, channel2).pass(Arrays.asList("test1-1", "test1-2")).close();
        Channels.distribute(Arrays.<InputChannel<?>>asList(channel1, channel2))
                .pass(Arrays.asList("test2-1", "test2-2"))
                .close();
        Channels.distribute(channel1, channel2).pass(Collections.singletonList("test3-1")).close();
        assertThat(channel1.result().afterMax(seconds(1)).all()).containsExactly("test1-1",
                                                                                 "test2-1",
                                                                                 "test3-1");
        assertThat(channel2.result().afterMax(seconds(1)).all()).containsExactly("test1-2",
                                                                                 "test2-2");
    }

    @Test
    public void testDistributeAbort() {

        InvocationChannel<String, String> channel1;
        InvocationChannel<String, String> channel2;
        channel1 = JRoutine.on(PassingInvocation.<String>factoryOf()).asyncInvoke().orderByCall();
        channel2 = JRoutine.on(PassingInvocation.<String>factoryOf()).asyncInvoke().orderByCall();
        Channels.distribute(channel1, channel2).abort();

        try {

            channel1.result().afterMax(seconds(1)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.result().afterMax(seconds(1)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = JRoutine.on(PassingInvocation.<String>factoryOf()).asyncInvoke().orderByCall();
        channel2 = JRoutine.on(PassingInvocation.<String>factoryOf()).asyncInvoke().orderByCall();
        Channels.distribute(Arrays.<InputChannel<?>>asList(channel1, channel2)).abort();

        try {

            channel1.result().afterMax(seconds(1)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.result().afterMax(seconds(1)).next();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @Test
    public void testDistributeAndFlush() {

        final InvocationChannel<String, String> channel1 =
                JRoutine.on(PassingInvocation.<String>factoryOf()).asyncInvoke().orderByCall();
        final InvocationChannel<String, String> channel2 =
                JRoutine.on(PassingInvocation.<String>factoryOf()).asyncInvoke().orderByCall();
        Channels.distributeAndFlush(null, channel1, channel2)
                .pass(Arrays.asList("test1-1", "test1-2"))
                .close();
        final String placeholder = "placeholder";
        Channels.distributeAndFlush((Object) placeholder,
                                    Arrays.<InputChannel<?>>asList(channel1, channel2))
                .pass(Arrays.asList("test2-1", "test2-2"))
                .close();
        Channels.distributeAndFlush(placeholder, channel1, channel2)
                .pass(Collections.singletonList("test3-1"))
                .close();
        assertThat(channel1.result().afterMax(seconds(1)).all()).containsExactly("test1-1",
                                                                                 "test2-1",
                                                                                 "test3-1");
        assertThat(channel2.result().afterMax(seconds(1)).all()).containsExactly("test1-2",
                                                                                 "test2-2",
                                                                                 placeholder);
    }

    @Test
    public void testDistributeAndFlushAbort() {

        InvocationChannel<String, String> channel1;
        InvocationChannel<String, String> channel2;
        channel1 = JRoutine.on(PassingInvocation.<String>factoryOf()).asyncInvoke().orderByCall();
        channel2 = JRoutine.on(PassingInvocation.<String>factoryOf()).asyncInvoke().orderByCall();
        Channels.distributeAndFlush(null, channel1, channel2).abort();

        try {

            channel1.result().afterMax(seconds(1)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.result().afterMax(seconds(1)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = JRoutine.on(PassingInvocation.<String>factoryOf()).asyncInvoke().orderByCall();
        channel2 = JRoutine.on(PassingInvocation.<String>factoryOf()).asyncInvoke().orderByCall();
        Channels.distributeAndFlush(null, Arrays.<InputChannel<?>>asList(channel1, channel2))
                .abort();

        try {

            channel1.result().afterMax(seconds(1)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            channel2.result().afterMax(seconds(1)).next();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @Test
    public void testDistributeAndFlushError() {

        final InvocationChannel<String, String> channel1 =
                JRoutine.on(PassingInvocation.<String>factoryOf()).asyncInvoke().orderByCall();
        Channels.distributeAndFlush(null, channel1)
                .pass(Arrays.asList("test1-1", "test1-2"))
                .close();

        try {

            channel1.result().afterMax(seconds(1)).all();

            fail();

        } catch (final InvocationException ignored) {

        }

        try {

            Channels.distributeAndFlush(new Object());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Channels.distributeAndFlush(null, Collections.<InputChannel<?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Channels.distributeAndFlush(new Object(), new InputChannel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Channels.distributeAndFlush(new Object(),
                                        Collections.<InputChannel<?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testDistributeError() {

        final InvocationChannel<String, String> channel1 =
                JRoutine.on(PassingInvocation.<String>factoryOf()).asyncInvoke().orderByCall();
        Channels.distribute(channel1).pass(Arrays.asList("test1-1", "test1-2")).close();

        try {

            channel1.result().afterMax(seconds(1)).all();

            fail();

        } catch (final InvocationException ignored) {

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

        try {

            Channels.distribute(new InputChannel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Channels.distribute(Collections.<InputChannel<?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testInputMap() {

        final ArrayList<Selectable<Object>> outputs = new ArrayList<Selectable<Object>>();
        outputs.add(new Selectable<Object>("test21", Sort.STRING));
        outputs.add(new Selectable<Object>(-11, Sort.INTEGER));
        final Routine<Selectable<Object>, Selectable<Object>> routine =
                JRoutine.on(new Sort()).buildRoutine();
        Map<Integer, IOChannel<Object>> channelMap;
        InvocationChannel<Selectable<Object>, Selectable<Object>> channel;
        channel = routine.asyncInvoke();
        channelMap = Channels.select(channel, Arrays.asList(Sort.INTEGER, Sort.STRING));
        channelMap.get(Sort.INTEGER).pass(-11).close();
        channelMap.get(Sort.STRING).pass("test21").close();
        assertThat(channel.result().afterMax(seconds(10)).all()).containsOnlyElementsOf(outputs);
        channel = routine.asyncInvoke();
        channelMap = Channels.select(channel, Sort.INTEGER, Sort.STRING);
        channelMap.get(Sort.INTEGER).pass(-11).close();
        channelMap.get(Sort.STRING).pass("test21").close();
        assertThat(channel.result().afterMax(seconds(10)).all()).containsOnlyElementsOf(outputs);
        channel = routine.asyncInvoke();
        channelMap = Channels.select(Math.min(Sort.INTEGER, Sort.STRING), 2, channel);
        channelMap.get(Sort.INTEGER).pass(-11).close();
        channelMap.get(Sort.STRING).pass("test21").close();
        assertThat(channel.result().afterMax(seconds(10)).all()).containsOnlyElementsOf(outputs);
    }

    @Test
    public void testInputMapAbort() {

        final Routine<Selectable<Object>, Selectable<Object>> routine =
                JRoutine.on(new Sort()).buildRoutine();
        Map<Integer, IOChannel<Object>> channelMap;
        InvocationChannel<Selectable<Object>, Selectable<Object>> channel;
        channel = routine.asyncInvoke();
        channelMap = Channels.select(channel, Arrays.asList(Sort.INTEGER, Sort.STRING));
        channelMap.get(Sort.INTEGER).pass(-11).close();
        channelMap.get(Sort.STRING).abort();

        try {

            channel.result().afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel = routine.asyncInvoke();
        channelMap = Channels.select(channel, Sort.INTEGER, Sort.STRING);
        channelMap.get(Sort.INTEGER).abort();
        channelMap.get(Sort.STRING).pass("test21").close();

        try {

            channel.result().afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel = routine.asyncInvoke();
        channelMap = Channels.select(Math.min(Sort.INTEGER, Sort.STRING), 2, channel);
        channelMap.get(Sort.INTEGER).abort();
        channelMap.get(Sort.STRING).abort();

        try {

            channel.result().afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @Test
    public void testInputMapError() {

        try {

            Channels.select(0, 0, JRoutine.on(new Sort()).asyncInvoke());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInputSelect() {

        final IOChannel<Selectable<String>> channel = JRoutine.io().buildChannel();
        Channels.select(channel.asInput(), 33).pass("test1", "test2", "test3").close();
        assertThat(channel.close().afterMax(seconds(1)).all()).containsExactly(
                new Selectable<String>("test1", 33), new Selectable<String>("test2", 33),
                new Selectable<String>("test3", 33));
    }

    @Test
    public void testInputSelectAbort() {

        final IOChannel<Selectable<String>> channel = JRoutine.io().buildChannel();
        Channels.select(channel.asInput(), 33).pass("test1", "test2", "test3").abort();

        try {

            channel.close().afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInputToSelectable() {

        final IOChannel<String> channel = JRoutine.io().buildChannel();
        Channels.toSelectable(channel.asInput(), 33)
                .pass(new Selectable<String>("test1", 33), new Selectable<String>("test2", -33),
                      new Selectable<String>("test3", 33), new Selectable<String>("test4", 333))
                .close();
        assertThat(channel.close().afterMax(seconds(1)).all()).containsExactly("test1", "test3");
    }

    @Test
    public void testInputToSelectableAbort() {

        final IOChannel<String> channel = JRoutine.io().buildChannel();
        Channels.toSelectable(channel.asInput(), 33).abort();

        try {

            channel.close().afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @Test
    public void testJoin() {

        final IOChannelBuilder builder = JRoutine.io();
        final Routine<List<?>, Character> routine = JRoutine.on(new CharAt()).buildRoutine();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.asyncCall(Channels.join(channel1, channel2)).afterMax(seconds(10)).all())
                .containsExactly('s', '2');
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.asyncCall(
                Channels.join(Arrays.<OutputChannel<?>>asList(channel1, channel2)))
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
        assertThat(routine.asyncCall(Channels.join(channel1, channel2)).afterMax(seconds(10)).all())
                .containsExactly('s', '2');
    }

    @Test
    public void testJoinAbort() {

        final IOChannelBuilder builder = JRoutine.io();
        final Routine<List<?>, Character> routine = JRoutine.on(new CharAt()).buildRoutine();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().abort();

        try {

            routine.asyncCall(Channels.join(channel1, channel2)).afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().abort();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();

        try {

            routine.asyncCall(Channels.join(Arrays.<OutputChannel<?>>asList(channel1, channel2)))
                   .afterMax(seconds(1))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @Test
    public void testJoinAndFlush() {

        final IOChannelBuilder builder = JRoutine.io();
        final Routine<List<?>, Character> routine = JRoutine.on(new CharAt()).buildRoutine();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.asyncCall(Channels.joinAndFlush(new Object(), channel1, channel2))
                          .afterMax(seconds(10))
                          .all()).containsExactly('s', '2');
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.asyncCall(
                Channels.joinAndFlush(null, Arrays.<OutputChannel<?>>asList(channel1, channel2)))
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

            routine.asyncCall(Channels.joinAndFlush(new Object(), channel1, channel2))
                   .afterMax(seconds(10))
                   .all();

            fail();

        } catch (final InvocationException ignored) {

        }
    }

    @Test
    public void testJoinAndFlushAbort() {

        final IOChannelBuilder builder = JRoutine.io();
        final Routine<List<?>, Character> routine = JRoutine.on(new CharAt()).buildRoutine();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().abort();

        try {

            routine.asyncCall(Channels.joinAndFlush(null, channel1, channel2))
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

            routine.asyncCall(Channels.joinAndFlush(new Object(),
                                                    Arrays.<OutputChannel<?>>asList(channel1,
                                                                                    channel2)))
                   .afterMax(seconds(1))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @Test
    public void testJoinAndFlushError() {

        try {

            Channels.joinAndFlush(new Object());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Channels.joinAndFlush(null, Collections.<OutputChannel<?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Channels.joinAndFlush(new Object(), new OutputChannel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Channels.joinAndFlush(new Object(), Collections.<OutputChannel<?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
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

        try {

            Channels.join(new OutputChannel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Channels.join(Collections.<OutputChannel<?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testMap() {

        final IOChannelBuilder builder =
                JRoutine.io().withChannels().withChannelOrder(OrderType.BY_CALL).set();
        final IOChannel<String> channel1 = builder.buildChannel();
        final IOChannel<Integer> channel2 = builder.buildChannel();

        final OutputChannel<? extends Selectable<Object>> channel =
                Channels.merge(Arrays.<OutputChannel<?>>asList(channel1, channel2));
        final OutputChannel<Selectable<Object>> output = JRoutine.on(new Sort())
                                                                 .withInvocations()
                                                                 .withInputOrder(OrderType.BY_CALL)
                                                                 .set()
                                                                 .asyncCall(channel);
        final Map<Integer, OutputChannel<Object>> channelMap =
                Channels.select(output, Sort.INTEGER, Sort.STRING);

        for (int i = 0; i < 4; i++) {

            final String input = Integer.toString(i);
            channel1.after(millis(20)).pass(input);
            channel2.after(millis(20)).pass(i);
        }

        channel1.close();
        channel2.close();

        assertThat(Streams.streamOf(channelMap.get(Sort.STRING))
                          .runOnShared()
                          .afterMax(seconds(1))
                          .all()).containsExactly("0", "1", "2", "3");
        assertThat(Streams.streamOf(channelMap.get(Sort.INTEGER))
                          .runOnShared()
                          .afterMax(seconds(1))
                          .all()).containsExactly(0, 1, 2, 3);
    }

    @Test
    public void testMerge() {

        final IOChannelBuilder builder =
                JRoutine.io().withChannels().withChannelOrder(OrderType.BY_CALL).set();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        OutputChannel<? extends Selectable<?>> outputChannel;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = Channels.<Object>merge(-7, channel1, channel2);
        channel1.pass("test1").close();
        channel2.pass(13).close();
        assertThat(outputChannel.afterMax(seconds(1)).all()).containsOnly(
                new Selectable<String>("test1", -7), new Selectable<Integer>(13, -6));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel =
                Channels.<Object>merge(11, Arrays.<OutputChannel<?>>asList(channel1, channel2));
        channel2.pass(13).close();
        channel1.pass("test1").close();
        assertThat(outputChannel.afterMax(seconds(1)).all()).containsOnly(
                new Selectable<String>("test1", 11), new Selectable<Integer>(13, 12));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = Channels.<Object>merge(channel1, channel2);
        channel1.pass("test2").close();
        channel2.pass(-17).close();
        assertThat(outputChannel.afterMax(seconds(1)).all()).containsOnly(
                new Selectable<String>("test2", 0), new Selectable<Integer>(-17, 1));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = Channels.<Object>merge(Arrays.<OutputChannel<?>>asList(channel1, channel2));
        channel1.pass("test2").close();
        channel2.pass(-17).close();
        assertThat(outputChannel.afterMax(seconds(1)).all()).containsOnly(
                new Selectable<String>("test2", 0), new Selectable<Integer>(-17, 1));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        final HashMap<Integer, OutputChannel<?>> channelMap =
                new HashMap<Integer, OutputChannel<?>>(2);
        channelMap.put(7, channel1);
        channelMap.put(-3, channel2);
        outputChannel = Channels.<Object>merge(channelMap);
        channel1.pass("test3").close();
        channel2.pass(111).close();
        assertThat(outputChannel.afterMax(seconds(1)).all()).containsOnly(
                new Selectable<String>("test3", 7), new Selectable<Integer>(111, -3));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMerge4() {

        final IOChannelBuilder builder =
                JRoutine.io().withChannels().withChannelOrder(OrderType.BY_CALL).set();
        final IOChannel<String> channel1 = builder.buildChannel();
        final IOChannel<String> channel2 = builder.buildChannel();
        final IOChannel<String> channel3 = builder.buildChannel();
        final IOChannel<String> channel4 = builder.buildChannel();

        final Routine<Selectable<String>, String> routine =
                JRoutine.on(factoryOf(new ClassToken<Amb<String>>() {})).buildRoutine();
        final OutputChannel<String> outputChannel = routine.asyncCall(
                Channels.merge(Arrays.asList(channel1, channel2, channel3, channel4)));

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

    @Test
    public void testMergeAbort() {

        final IOChannelBuilder builder =
                JRoutine.io().withChannels().withChannelOrder(OrderType.BY_CALL).set();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        OutputChannel<? extends Selectable<?>> outputChannel;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = Channels.<Object>merge(-7, channel1, channel2);
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
                Channels.<Object>merge(11, Arrays.<OutputChannel<?>>asList(channel1, channel2));
        channel2.abort();
        channel1.pass("test1").close();

        try {

            outputChannel.afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = Channels.<Object>merge(channel1, channel2);
        channel1.abort();
        channel2.pass(-17).close();

        try {

            outputChannel.afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = Channels.<Object>merge(Arrays.<OutputChannel<?>>asList(channel1, channel2));
        channel1.pass("test2").close();
        channel2.abort();

        try {

            outputChannel.afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        final HashMap<Integer, OutputChannel<?>> channelMap =
                new HashMap<Integer, OutputChannel<?>>(2);
        channelMap.put(7, channel1);
        channelMap.put(-3, channel2);
        outputChannel = Channels.<Object>merge(channelMap);
        channel1.abort();
        channel2.pass(111).close();

        try {

            outputChannel.afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @Test
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

        try {

            Channels.merge(new OutputChannel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Channels.merge(Collections.<OutputChannel<?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Channels.merge(0, new OutputChannel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Channels.merge(0, Collections.<OutputChannel<?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Channels.merge(Collections.<Integer, OutputChannel<?>>singletonMap(1, null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testOutputMap() {

        final Routine<Selectable<Object>, Selectable<Object>> routine =
                JRoutine.on(new Sort()).buildRoutine();
        Map<Integer, OutputChannel<Object>> channelMap;
        OutputChannel<Selectable<Object>> channel;
        channel = routine.asyncCall(new Selectable<Object>("test21", Sort.STRING),
                                    new Selectable<Object>(-11, Sort.INTEGER));
        channelMap = Channels.select(channel, Arrays.asList(Sort.INTEGER, Sort.STRING));
        assertThat(Streams.streamOf(channelMap.get(Sort.INTEGER))
                          .runOnShared()
                          .afterMax(seconds(1))
                          .all()).containsOnly(-11);
        assertThat(Streams.streamOf(channelMap.get(Sort.STRING))
                          .runOnShared()
                          .afterMax(seconds(1))
                          .all()).containsOnly("test21");
        channel = routine.asyncCall(new Selectable<Object>(-11, Sort.INTEGER),
                                    new Selectable<Object>("test21", Sort.STRING));
        channelMap = Channels.select(channel, Sort.INTEGER, Sort.STRING);
        assertThat(Streams.streamOf(channelMap.get(Sort.INTEGER))
                          .runOnShared()
                          .afterMax(seconds(1))
                          .all()).containsOnly(-11);
        assertThat(Streams.streamOf(channelMap.get(Sort.STRING))
                          .runOnShared()
                          .afterMax(seconds(1))
                          .all()).containsOnly("test21");
        channel = routine.asyncCall(new Selectable<Object>("test21", Sort.STRING),
                                    new Selectable<Object>(-11, Sort.INTEGER));
        channelMap = Channels.select(Math.min(Sort.INTEGER, Sort.STRING), 2, channel);
        assertThat(Streams.streamOf(channelMap.get(Sort.INTEGER))
                          .runOnShared()
                          .afterMax(seconds(1))
                          .all()).containsOnly(-11);
        assertThat(Streams.streamOf(channelMap.get(Sort.STRING))
                          .runOnShared()
                          .afterMax(seconds(1))
                          .all()).containsOnly("test21");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testOutputMapAbort() {

        final Routine<Selectable<Object>, Selectable<Object>> routine =
                JRoutine.on(new Sort()).buildRoutine();
        Map<Integer, OutputChannel<Object>> channelMap;
        OutputChannel<Selectable<Object>> channel;
        channel = routine.asyncInvoke()
                         .after(millis(100))
                         .pass(new Selectable<Object>("test21", Sort.STRING),
                               new Selectable<Object>(-11, Sort.INTEGER))
                         .result();
        channelMap = Channels.select(channel, Arrays.asList(Sort.INTEGER, Sort.STRING));
        channel.abort();

        try {

            Streams.streamOf(channelMap.get(Sort.STRING)).runOnShared().afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            Streams.streamOf(channelMap.get(Sort.INTEGER)).runOnShared().afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel = routine.asyncInvoke()
                         .after(millis(100))
                         .pass(new Selectable<Object>(-11, Sort.INTEGER),
                               new Selectable<Object>("test21", Sort.STRING))
                         .result();
        channelMap = Channels.select(channel, Sort.INTEGER, Sort.STRING);
        channel.abort();

        try {

            Streams.streamOf(channelMap.get(Sort.STRING)).runOnShared().afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            Streams.streamOf(channelMap.get(Sort.INTEGER)).runOnShared().afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel = routine.asyncInvoke()
                         .after(millis(100))
                         .pass(new Selectable<Object>("test21", Sort.STRING),
                               new Selectable<Object>(-11, Sort.INTEGER))
                         .result();
        channelMap = Channels.select(Math.min(Sort.INTEGER, Sort.STRING), 2, channel);
        channel.abort();

        try {

            Streams.streamOf(channelMap.get(Sort.STRING)).runOnShared().afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            Streams.streamOf(channelMap.get(Sort.INTEGER)).runOnShared().afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @Test
    public void testOutputMapError() {

        try {

            Channels.select(0, 0, JRoutine.on(new Sort()).asyncCall());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testOutputSelect() {

        final IOChannel<Selectable<String>> channel = JRoutine.io().buildChannel();
        final OutputChannel<String> outputChannel = Channels.select(channel.asOutput(), 33).get(33);
        channel.pass(new Selectable<String>("test1", 33), new Selectable<String>("test2", -33),
                     new Selectable<String>("test3", 33), new Selectable<String>("test4", 333));
        channel.close();
        assertThat(outputChannel.afterMax(seconds(1)).all()).containsExactly("test1", "test3");
    }

    @Test
    public void testOutputSelectAbort() {

        final IOChannel<Selectable<String>> channel = JRoutine.io().buildChannel();
        final OutputChannel<String> outputChannel = Channels.select(channel.asOutput(), 33).get(33);
        channel.abort();

        try {

            outputChannel.afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testOutputToSelectable() {

        final IOChannel<String> channel = JRoutine.io().buildChannel();
        channel.pass("test1", "test2", "test3").close();
        assertThat(Channels.toSelectable(channel.asOutput(), 33)
                           .afterMax(seconds(1))
                           .all()).containsExactly(new Selectable<String>("test1", 33),
                                                   new Selectable<String>("test2", 33),
                                                   new Selectable<String>("test3", 33));
    }

    @Test
    public void testOutputToSelectableAbort() {

        final IOChannel<String> channel = JRoutine.io().buildChannel();
        channel.pass("test1", "test2", "test3").abort();

        try {

            Channels.toSelectable(channel.asOutput(), 33).afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @Test
    public void testRepeat() {

        final IOChannel<Object> ioChannel = JRoutine.io().buildChannel();
        final OutputChannel<Object> channel = Channels.repeat(ioChannel);
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

    @Test
    public void testRepeatAbort() {

        final IOChannel<Object> ioChannel = JRoutine.io().buildChannel();
        final OutputChannel<Object> channel = Channels.repeat(ioChannel);
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

    @Test
    @SuppressWarnings("unchecked")
    public void testSelectableOutput() {

        final Routine<Selectable<Object>, Selectable<Object>> routine =
                JRoutine.on(new Sort()).buildRoutine();
        final IOChannel<Selectable<Object>> inputChannel = JRoutine.io().buildChannel();
        final OutputChannel<Selectable<Object>> outputChannel = routine.asyncCall(inputChannel);
        final StreamChannel<Object> intChannel =
                Streams.streamOf(Channels.select(outputChannel).index(Sort.INTEGER)).runOnShared();
        final StreamChannel<Object> strChannel =
                Streams.streamOf(Channels.select(outputChannel).index(Sort.STRING)).runOnShared();
        inputChannel.pass(new Selectable<Object>("test21", Sort.STRING),
                          new Selectable<Object>(-11, Sort.INTEGER));
        assertThat(intChannel.afterMax(seconds(10)).next()).isEqualTo(-11);
        assertThat(strChannel.afterMax(seconds(10)).next()).isEqualTo("test21");
        inputChannel.pass(new Selectable<Object>(-11, Sort.INTEGER),
                          new Selectable<Object>("test21", Sort.STRING));
        assertThat(intChannel.afterMax(seconds(10)).next()).isEqualTo(-11);
        assertThat(strChannel.afterMax(seconds(10)).next()).isEqualTo("test21");
        inputChannel.pass(new Selectable<Object>("test21", Sort.STRING),
                          new Selectable<Object>(-11, Sort.INTEGER));
        assertThat(intChannel.afterMax(seconds(10)).next()).isEqualTo(-11);
        assertThat(strChannel.afterMax(seconds(10)).next()).isEqualTo("test21");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSelectableOutputAbort() {

        final Routine<Selectable<Object>, Selectable<Object>> routine =
                JRoutine.on(new Sort()).buildRoutine();
        IOChannel<Selectable<Object>> inputChannel = JRoutine.io().buildChannel();
        OutputChannel<Selectable<Object>> outputChannel = routine.asyncCall(inputChannel);
        Channels.select(outputChannel).index(Sort.INTEGER);
        Channels.select(outputChannel).index(Sort.STRING);
        inputChannel.after(millis(100))
                    .pass(new Selectable<Object>("test21", Sort.STRING),
                          new Selectable<Object>(-11, Sort.INTEGER))
                    .abort();

        try {

            Streams.streamOf(Channels.select(outputChannel).index(Sort.STRING))
                   .runOnShared()
                   .afterMax(seconds(1))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            Streams.streamOf(Channels.select(outputChannel).index(Sort.INTEGER))
                   .runOnShared()
                   .afterMax(seconds(1))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }

        inputChannel = JRoutine.io().buildChannel();
        outputChannel = routine.asyncCall(inputChannel);
        Channels.select(outputChannel).index(Sort.INTEGER);
        Channels.select(outputChannel).index(Sort.STRING);
        inputChannel.after(millis(100))
                    .pass(new Selectable<Object>(-11, Sort.INTEGER),
                          new Selectable<Object>("test21", Sort.STRING))
                    .abort();

        try {

            Streams.streamOf(Channels.select(outputChannel).index(Sort.STRING))
                   .runOnShared()
                   .afterMax(seconds(1))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            Streams.streamOf(Channels.select(outputChannel).index(Sort.INTEGER))
                   .runOnShared()
                   .afterMax(seconds(1))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }

        inputChannel = JRoutine.io().buildChannel();
        outputChannel = routine.asyncCall(inputChannel);
        Channels.select(outputChannel).index(Sort.INTEGER);
        Channels.select(outputChannel).index(Sort.STRING);
        inputChannel.after(millis(100))
                    .pass(new Selectable<Object>("test21", Sort.STRING),
                          new Selectable<Object>(-11, Sort.INTEGER))
                    .abort();

        try {

            Streams.streamOf(Channels.select(outputChannel).index(Sort.STRING))
                   .runOnShared()
                   .afterMax(seconds(1))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            Streams.streamOf(Channels.select(outputChannel).index(Sort.INTEGER))
                   .runOnShared()
                   .afterMax(seconds(1))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testSelectableOutputError() {

        try {

            Channels.select(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        final IOChannel<Selectable<Object>> channel = JRoutine.io().buildChannel();
        Channels.select(channel).index(0);
        channel.pass(new Selectable<Object>("test", 0));
        assertThat(Channels.select(channel).index(0).afterMax(seconds(1)).next()).isEqualTo("test");

        try {

            Channels.select(channel).index(1);

            fail();

        } catch (final IllegalStateException ignored) {

        }
    }

    private static class Amb<DATA> extends TemplateInvocation<Selectable<DATA>, DATA> {

        private static final int NO_INDEX = Integer.MIN_VALUE;

        private int mFirstIndex;

        @Override
        public void onInitialize() {

            mFirstIndex = NO_INDEX;
        }

        @Override
        public void onInput(final Selectable<DATA> input,
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

    private static class Sort extends FilterInvocation<Selectable<Object>, Selectable<Object>> {

        private static final int INTEGER = 1;

        private static final int STRING = 0;

        public void onInput(final Selectable<Object> selectable,
                @NotNull final ResultChannel<Selectable<Object>> result) {

            switch (selectable.index) {

                case INTEGER:
                    Channels.<Object, Integer>select(result, INTEGER)
                            .pass(selectable.<Integer>data())
                            .close();
                    break;

                case STRING:
                    Channels.<Object, String>select(result, STRING)
                            .pass(selectable.<String>data())
                            .close();
                    break;
            }
        }
    }
}
