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

package com.github.dm.jrt.stream;

import com.github.dm.jrt.channel.Channels;
import com.github.dm.jrt.channel.Selectable;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.ChannelBuilder;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.InvocationConfiguration.OrderType;
import com.github.dm.jrt.core.invocation.IdentityInvocation;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.MappingInvocation;
import com.github.dm.jrt.core.invocation.TemplateInvocation;
import com.github.dm.jrt.core.log.Log.Level;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Functions;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.dm.jrt.core.invocation.InvocationFactory.factoryOf;
import static com.github.dm.jrt.core.util.UnitDuration.millis;
import static com.github.dm.jrt.core.util.UnitDuration.seconds;
import static com.github.dm.jrt.stream.Streams.range;
import static com.github.dm.jrt.stream.Streams.sequence;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Streams unit tests.
 * <p>
 * Created by davide-maestroni on 12/24/2015.
 */
public class StreamsTest {

    @Test
    public void testBlend() {

        StreamChannel<String, String> channel1 = Streams.streamOf("test1", "test2", "test3");
        StreamChannel<String, String> channel2 = Streams.streamOf("test4", "test5", "test6");
        assertThat(Streams.blend(channel2, channel1)
                          .buildChannels()
                          .after(seconds(1))
                          .all()).containsOnly("test1", "test2", "test3", "test4", "test5",
                "test6");
        channel1 = Streams.streamOf("test1", "test2", "test3");
        channel2 = Streams.streamOf("test4", "test5", "test6");
        assertThat(Streams.blend(Arrays.<StreamChannel<?, ?>>asList(channel1, channel2))
                          .buildChannels()
                          .after(seconds(1))
                          .all()).containsOnly("test1", "test2", "test3", "test4", "test5",
                "test6");
    }

    @Test
    public void testBlendAbort() {

        final ChannelBuilder builder = JRoutineCore.io();
        final Routine<Object, Object> routine =
                JRoutineCore.on(IdentityInvocation.factoryOf()).buildRoutine();
        Channel<String, String> channel1;
        Channel<Integer, Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().abort();

        try {

            routine.async(Streams.blend(channel1, channel2).buildChannels())
                   .after(seconds(1))
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
                    Streams.blend(Arrays.<Channel<?, ?>>asList(channel1, channel2)).buildChannels())
                   .after(seconds(1))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testBlendError() {

        try {

            Streams.blend();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.blend((Channel<?, ?>[]) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.blend(new Channel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.blend(Collections.<Channel<?, ?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.blend((List<Channel<?, ?>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.blend(Collections.<Channel<?, ?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testCombine() {

        final Channel<String, String> channel1 =
                JRoutineCore.on(IdentityInvocation.<String>factoryOf()).async().orderByCall();
        final Channel<Integer, Integer> channel2 =
                JRoutineCore.on(IdentityInvocation.<Integer>factoryOf()).async().orderByCall();
        Streams.combine(channel1, channel2)
               .buildChannels()
               .pass(new Selectable<String>("test1", 0))
               .pass(new Selectable<Integer>(1, 1))
               .close();
        Streams.combine(3, channel1, channel2)
               .buildChannels()
               .pass(new Selectable<String>("test2", 3))
               .pass(new Selectable<Integer>(2, 4))
               .close();
        Streams.combine(Arrays.<Channel<?, ?>>asList(channel1, channel2))
               .buildChannels()
               .pass(new Selectable<String>("test3", 0))
               .pass(new Selectable<Integer>(3, 1))
               .close();
        Streams.combine(-5, Arrays.<Channel<?, ?>>asList(channel1, channel2))
               .buildChannels()
               .pass(new Selectable<String>("test4", -5))
               .pass(new Selectable<Integer>(4, -4))
               .close();
        final HashMap<Integer, Channel<?, ?>> map = new HashMap<Integer, Channel<?, ?>>(2);
        map.put(31, channel1);
        map.put(17, channel2);
        Streams.combine(map)
               .buildChannels()
               .pass(new Selectable<String>("test5", 31))
               .pass(new Selectable<Integer>(5, 17))
               .close();
        assertThat(channel1.close().after(seconds(1)).all()).containsExactly("test1", "test2",
                "test3", "test4", "test5");
        assertThat(channel2.close().after(seconds(1)).all()).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    public void testConcat() {

        StreamChannel<String, String> channel1 = Streams.streamOf("test1", "test2", "test3");
        StreamChannel<String, String> channel2 = Streams.streamOf("test4", "test5", "test6");
        assertThat(Streams.concat(channel2, channel1)
                          .buildChannels()
                          .after(seconds(1))
                          .all()).containsExactly("test4", "test5", "test6", "test1", "test2",
                "test3");
        channel1 = Streams.streamOf("test1", "test2", "test3");
        channel2 = Streams.streamOf("test4", "test5", "test6");
        assertThat(Streams.concat(Arrays.<StreamChannel<?, ?>>asList(channel1, channel2))
                          .buildChannels()
                          .after(seconds(1))
                          .all()).containsExactly("test1", "test2", "test3", "test4", "test5",
                "test6");
    }

    @Test
    public void testConcatAbort() {

        final ChannelBuilder builder = JRoutineCore.io();
        final Routine<Object, Object> routine =
                JRoutineCore.on(IdentityInvocation.factoryOf()).buildRoutine();
        Channel<String, String> channel1;
        Channel<Integer, Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().abort();

        try {

            routine.async(Streams.concat(channel1, channel2).buildChannels())
                   .after(seconds(1))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().abort();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();

        try {

            routine.async(Streams.concat(Arrays.<Channel<?, ?>>asList(channel1, channel2))
                                 .buildChannels()).after(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testConcatError() {

        try {

            Streams.concat();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.concat((Channel<?, ?>[]) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.concat(new Channel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.concat(Collections.<Channel<?, ?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.concat((List<Channel<?, ?>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.concat(Collections.<Channel<?, ?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testConfiguration() {

        final StreamChannel<String, String> channel1 = Streams.streamOf("test1", "test2", "test3");
        final StreamChannel<String, String> channel2 = Streams.streamOf("test4", "test5", "test6");
        assertThat(Streams.blend(channel2, channel1)
                          .channelConfiguration()
                          .withOrder(OrderType.BY_CALL)
                          .withOutputTimeout(seconds(1))
                          .apply()
                          .buildChannels()
                          .all()).containsExactly("test4", "test5", "test6", "test1", "test2",
                "test3");
    }

    @Test
    public void testConstructor() {

        boolean failed = false;
        try {
            new Streams();
            failed = true;

        } catch (final Throwable ignored) {

        }

        assertThat(failed).isFalse();
    }

    @Test
    public void testDistribute() {

        final Channel<String, String> channel1 =
                JRoutineCore.on(IdentityInvocation.<String>factoryOf()).async().orderByCall();
        final Channel<String, String> channel2 =
                JRoutineCore.on(IdentityInvocation.<String>factoryOf()).async().orderByCall();
        Streams.distribute(channel1, channel2)
               .buildChannels()
               .pass(Arrays.asList("test1-1", "test1-2"))
               .close();
        Streams.distribute(Arrays.<Channel<?, ?>>asList(channel1, channel2))
               .buildChannels()
               .pass(Arrays.asList("test2-1", "test2-2"))
               .close();
        Streams.distribute(channel1, channel2)
               .buildChannels()
               .pass(Collections.singletonList("test3-1"))
               .close();
        assertThat(channel1.close().after(seconds(1)).all()).containsExactly("test1-1", "test2-1",
                "test3-1");
        assertThat(channel2.close().after(seconds(1)).all()).containsExactly("test1-2", "test2-2");
    }

    @Test
    public void testDistributePlaceholder() {

        final Channel<String, String> channel1 =
                JRoutineCore.on(IdentityInvocation.<String>factoryOf()).async().orderByCall();
        final Channel<String, String> channel2 =
                JRoutineCore.on(IdentityInvocation.<String>factoryOf()).async().orderByCall();
        Streams.distribute((Object) null, channel1, channel2)
               .buildChannels()
               .pass(Arrays.asList("test1-1", "test1-2"))
               .close();
        final String placeholder = "placeholder";
        Streams.distribute((Object) placeholder, Arrays.<Channel<?, ?>>asList(channel1, channel2))
               .buildChannels()
               .pass(Arrays.asList("test2-1", "test2-2"))
               .close();
        Streams.distribute(placeholder, channel1, channel2)
               .buildChannels()
               .pass(Collections.singletonList("test3-1"))
               .close();
        assertThat(channel1.close().after(seconds(1)).all()).containsExactly("test1-1", "test2-1",
                "test3-1");
        assertThat(channel2.close().after(seconds(1)).all()).containsExactly("test1-2", "test2-2",
                placeholder);
    }

    @Test
    public void testFactory() {

        final InvocationFactory<String, String> factory = Streams.asFactory(
                new Function<StreamChannel<String, String>, StreamChannel<String, String>>() {

                    public StreamChannel<String, String> apply(
                            final StreamChannel<String, String> channel) {

                        return channel.sync().map(new Function<String, String>() {

                            public String apply(final String s) {

                                return s.toUpperCase();
                            }
                        });
                    }
                });
        assertThat(JRoutineCore.on(factory)
                               .async("test1", "test2", "test3")
                               .after(seconds(3))
                               .all()).containsExactly("TEST1", "TEST2", "TEST3");

        try {

            final Channel<String, String> channel = JRoutineCore.on(factory).async();
            channel.abort(new IllegalArgumentException());
            channel.close().after(seconds(3)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        assertThat(Streams.onStream(
                new Function<StreamChannel<String, String>, StreamChannel<String, String>>() {

                    public StreamChannel<String, String> apply(
                            final StreamChannel<String, String> channel) {

                        return channel.sync().map(new Function<String, String>() {

                            public String apply(final String s) {

                                return s.toUpperCase();
                            }
                        });
                    }
                }).async("test1", "test2", "test3").after(seconds(3)).all()).containsExactly(
                "TEST1", "TEST2", "TEST3");

        try {

            final Channel<String, String> channel = Streams.onStream(
                    new Function<StreamChannel<String, String>, StreamChannel<String, String>>() {

                        public StreamChannel<String, String> apply(
                                final StreamChannel<String, String> channel) {

                            return channel.sync().map(new Function<String, String>() {

                                public String apply(final String s) {

                                    return s.toUpperCase();
                                }
                            });
                        }
                    }).async();
            channel.abort(new IllegalArgumentException());
            channel.close().after(seconds(3)).next();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @Test
    public void testFactoryEquals() {

        final Function<StreamChannel<String, String>, StreamChannel<String, String>> function =
                new Function<StreamChannel<String, String>, StreamChannel<String, String>>() {

                    public StreamChannel<String, String> apply(
                            final StreamChannel<String, String> channel) {

                        return channel.sync().map(new Function<String, String>() {

                            public String apply(final String s) {

                                return s.toUpperCase();
                            }
                        });
                    }
                };
        final InvocationFactory<String, String> factory = Streams.asFactory(function);
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(null);
        assertThat(factory).isNotEqualTo("test");
        assertThat(factory).isNotEqualTo(
                Streams.asFactory(Functions.<StreamChannel<Object, Object>>identity()));
        assertThat(factory).isEqualTo(Streams.asFactory(function));
        assertThat(factory.hashCode()).isEqualTo(Streams.asFactory(function).hashCode());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testFactoryError() {

        try {

            Streams.asFactory(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.onStream(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInputSelect() {

        Channel<Selectable<String>, Selectable<String>> channel = JRoutineCore.io().buildChannel();
        Streams.selectInput(channel, 33).buildChannels().pass("test1", "test2", "test3").close();
        assertThat(channel.close().after(seconds(1)).all()).containsExactly(
                new Selectable<String>("test1", 33), new Selectable<String>("test2", 33),
                new Selectable<String>("test3", 33));
        channel = JRoutineCore.io().buildChannel();
        Map<Integer, Channel<String, ?>> channelMap =
                Streams.selectInput(channel, Arrays.asList(1, 2, 3)).buildChannels();
        channelMap.get(1).pass("test1").close();
        channelMap.get(2).pass("test2").close();
        channelMap.get(3).pass("test3").close();
        assertThat(channel.close().after(seconds(1)).all()).containsExactly(
                new Selectable<String>("test1", 1), new Selectable<String>("test2", 2),
                new Selectable<String>("test3", 3));
        channel = JRoutineCore.io().buildChannel();
        channelMap = Streams.selectInput(channel, 1, 2, 3).buildChannels();
        channelMap.get(1).pass("test1").close();
        channelMap.get(2).pass("test2").close();
        channelMap.get(3).pass("test3").close();
        assertThat(channel.close().after(seconds(1)).all()).containsExactly(
                new Selectable<String>("test1", 1), new Selectable<String>("test2", 2),
                new Selectable<String>("test3", 3));
        channel = JRoutineCore.io().buildChannel();
        channelMap = Streams.selectInput(1, 3, channel).buildChannels();
        channelMap.get(1).pass("test1").close();
        channelMap.get(2).pass("test2").close();
        channelMap.get(3).pass("test3").close();
        assertThat(channel.close().after(seconds(1)).all()).containsExactly(
                new Selectable<String>("test1", 1), new Selectable<String>("test2", 2),
                new Selectable<String>("test3", 3));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInputToSelectable() {

        final Channel<String, String> channel = JRoutineCore.io().buildChannel();
        Streams.selectableInput(channel, 33)
               .buildChannels()
               .pass(new Selectable<String>("test1", 33), new Selectable<String>("test2", -33),
                       new Selectable<String>("test3", 33), new Selectable<String>("test4", 333))
               .close();
        assertThat(channel.close().after(seconds(1)).all()).containsExactly("test1", "test3");
    }

    @Test
    public void testJoin() {

        final ChannelBuilder builder = JRoutineCore.io();
        final Routine<List<?>, Character> routine = JRoutineCore.on(new CharAt()).buildRoutine();
        Channel<String, String> channel1;
        Channel<Integer, Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.async(Streams.join(channel1, channel2).buildChannels())
                          .after(seconds(10))
                          .all()).containsExactly('s', '2');
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.async(
                Streams.join(Arrays.<Channel<?, ?>>asList(channel1, channel2)).buildChannels())
                          .after(seconds(10))
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
        assertThat(routine.async(Streams.join(channel1, channel2).buildChannels())
                          .after(seconds(10))
                          .all()).containsExactly('s', '2');
    }

    @Test
    public void testJoinAbort() {

        final ChannelBuilder builder = JRoutineCore.io();
        final Routine<List<?>, Character> routine = JRoutineCore.on(new CharAt()).buildRoutine();
        Channel<String, String> channel1;
        Channel<Integer, Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().abort();

        try {

            routine.async(Streams.join(channel1, channel2).buildChannels()).after(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().abort();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();

        try {

            routine.async(
                    Streams.join(Arrays.<Channel<?, ?>>asList(channel1, channel2)).buildChannels())
                   .after(seconds(1))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @Test
    public void testJoinError() {

        try {

            Streams.join();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.join(Collections.<Channel<?, ?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.join(new Channel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.join(Collections.<Channel<?, ?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testJoinPlaceholder() {

        final ChannelBuilder builder = JRoutineCore.io();
        final Routine<List<?>, Character> routine = JRoutineCore.on(new CharAt()).buildRoutine();
        Channel<String, String> channel1;
        Channel<Integer, Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.async(Streams.join(new Object(), channel1, channel2).buildChannels())
                          .after(seconds(10))
                          .all()).containsExactly('s', '2');
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.async(
                Streams.join(null, Arrays.<Channel<?, ?>>asList(channel1, channel2))
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

            routine.async(Streams.join(new Object(), channel1, channel2).buildChannels())
                   .after(seconds(10))
                   .all();

            fail();

        } catch (final InvocationException ignored) {

        }
    }

    @Test
    public void testJoinPlaceholderAbort() {

        final ChannelBuilder builder = JRoutineCore.io();
        final Routine<List<?>, Character> routine = JRoutineCore.on(new CharAt()).buildRoutine();
        Channel<String, String> channel1;
        Channel<Integer, Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().abort();

        try {

            routine.async(Streams.join((Object) null, channel1, channel2).buildChannels())
                   .after(seconds(1))
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
                    Streams.join(new Object(), Arrays.<Channel<?, ?>>asList(channel1, channel2))
                           .buildChannels()).after(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @Test
    public void testJoinPlaceholderError() {

        try {

            Streams.join(new Object());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.join(null, Collections.<Channel<?, ?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.join(new Object(), new Channel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.join(new Object(), Collections.<Channel<?, ?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testMap() {

        final ChannelBuilder builder =
                JRoutineCore.io().channelConfiguration().withOrder(OrderType.BY_CALL).apply();
        final Channel<String, String> channel1 = builder.buildChannel();
        final Channel<Integer, Integer> channel2 = builder.buildChannel();

        final Channel<?, ? extends Selectable<Object>> channel =
                Streams.merge(Arrays.<Channel<?, ?>>asList(channel1, channel2)).buildChannels();
        final Channel<?, Selectable<Object>> output = JRoutineCore.on(new Sort())
                                                                  .invocationConfiguration()
                                                                  .withInputOrder(OrderType.BY_CALL)
                                                                  .apply()
                                                                  .async(channel);
        final Map<Integer, Channel<?, Object>> channelMap =
                Channels.selectOutput(output, Sort.INTEGER, Sort.STRING).buildChannels();

        for (int i = 0; i < 4; i++) {

            final String input = Integer.toString(i);
            channel1.after(millis(20)).pass(input);
            channel2.after(millis(20)).pass(i);
        }

        channel1.close();
        channel2.close();

        assertThat(Streams.streamOf(channelMap.get(Sort.STRING))
                          .asyncMap(null)
                          .after(seconds(1))
                          .all()).containsExactly("0", "1", "2", "3");
        assertThat(Streams.streamOf(channelMap.get(Sort.INTEGER))
                          .asyncMap(null)
                          .after(seconds(1))
                          .all()).containsExactly(0, 1, 2, 3);
    }

    @Test
    public void testMerge() {

        final ChannelBuilder builder =
                JRoutineCore.io().channelConfiguration().withOrder(OrderType.BY_CALL).apply();
        Channel<String, String> channel1;
        Channel<Integer, Integer> channel2;
        Channel<?, ? extends Selectable<?>> outputChannel;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = Streams.merge(-7, channel1, channel2).buildChannels();
        channel1.pass("test1").close();
        channel2.pass(13).close();
        assertThat(outputChannel.after(seconds(1)).all()).containsOnly(
                new Selectable<String>("test1", -7), new Selectable<Integer>(13, -6));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel =
                Streams.merge(11, Arrays.<Channel<?, ?>>asList(channel1, channel2)).buildChannels();
        channel2.pass(13).close();
        channel1.pass("test1").close();
        assertThat(outputChannel.after(seconds(1)).all()).containsOnly(
                new Selectable<String>("test1", 11), new Selectable<Integer>(13, 12));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = Streams.merge(channel1, channel2).buildChannels();
        channel1.pass("test2").close();
        channel2.pass(-17).close();
        assertThat(outputChannel.after(seconds(1)).all()).containsOnly(
                new Selectable<String>("test2", 0), new Selectable<Integer>(-17, 1));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel =
                Streams.merge(Arrays.<Channel<?, ?>>asList(channel1, channel2)).buildChannels();
        channel1.pass("test2").close();
        channel2.pass(-17).close();
        assertThat(outputChannel.after(seconds(1)).all()).containsOnly(
                new Selectable<String>("test2", 0), new Selectable<Integer>(-17, 1));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        final HashMap<Integer, Channel<?, ?>> channelMap = new HashMap<Integer, Channel<?, ?>>(2);
        channelMap.put(7, channel1);
        channelMap.put(-3, channel2);
        outputChannel = Streams.merge(channelMap).buildChannels();
        channel1.pass("test3").close();
        channel2.pass(111).close();
        assertThat(outputChannel.after(seconds(1)).all()).containsOnly(
                new Selectable<String>("test3", 7), new Selectable<Integer>(111, -3));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMerge4() {

        final ChannelBuilder builder =
                JRoutineCore.io().channelConfiguration().withOrder(OrderType.BY_CALL).apply();
        final Channel<String, String> channel1 = builder.buildChannel();
        final Channel<String, String> channel2 = builder.buildChannel();
        final Channel<String, String> channel3 = builder.buildChannel();
        final Channel<String, String> channel4 = builder.buildChannel();

        final Routine<Selectable<String>, String> routine =
                JRoutineCore.on(factoryOf(new ClassToken<Amb<String>>() {})).buildRoutine();
        final Channel<?, String> outputChannel = routine.async(
                Streams.merge(Arrays.asList(channel1, channel2, channel3, channel4))
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

    @Test
    public void testMergeAbort() {

        final ChannelBuilder builder =
                JRoutineCore.io().channelConfiguration().withOrder(OrderType.BY_CALL).apply();
        Channel<String, String> channel1;
        Channel<Integer, Integer> channel2;
        Channel<?, ? extends Selectable<?>> outputChannel;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = Streams.merge(-7, channel1, channel2).buildChannels();
        channel1.pass("test1").close();
        channel2.abort();

        try {

            outputChannel.after(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel =
                Streams.merge(11, Arrays.<Channel<?, ?>>asList(channel1, channel2)).buildChannels();
        channel2.abort();
        channel1.pass("test1").close();

        try {

            outputChannel.after(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = Streams.merge(channel1, channel2).buildChannels();
        channel1.abort();
        channel2.pass(-17).close();

        try {

            outputChannel.after(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel =
                Streams.merge(Arrays.<Channel<?, ?>>asList(channel1, channel2)).buildChannels();
        channel1.pass("test2").close();
        channel2.abort();

        try {

            outputChannel.after(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        final HashMap<Integer, Channel<?, ?>> channelMap = new HashMap<Integer, Channel<?, ?>>(2);
        channelMap.put(7, channel1);
        channelMap.put(-3, channel2);
        outputChannel = Streams.merge(channelMap).buildChannels();
        channel1.abort();
        channel2.pass(111).close();

        try {

            outputChannel.after(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @Test
    public void testMergeError() {

        try {

            Streams.merge(0, Collections.<Channel<?, Object>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.merge(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.merge(Collections.<Channel<?, Object>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.merge(Collections.<Integer, Channel<?, Object>>emptyMap());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.merge();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.merge(new Channel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.merge(Collections.<Channel<?, ?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.merge(0, new Channel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.merge(0, Collections.<Channel<?, ?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.merge(Collections.<Integer, Channel<?, ?>>singletonMap(1, null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testOutputToSelectable() {

        final Channel<String, String> channel = JRoutineCore.io().buildChannel();
        channel.pass("test1", "test2", "test3").close();
        assertThat(Streams.selectableOutput(channel, 33)
                          .buildChannels()
                          .after(seconds(1))
                          .all()).containsExactly(new Selectable<String>("test1", 33),
                new Selectable<String>("test2", 33), new Selectable<String>("test3", 33));
    }

    @Test
    public void testOutputToSelectableAbort() {

        final Channel<String, String> channel = JRoutineCore.io().buildChannel();
        channel.pass("test1", "test2", "test3").abort();

        try {
            Streams.selectableOutput(channel, 33).buildChannels().after(seconds(1)).all();
            fail();

        } catch (final AbortException ignored) {

        }
    }

    @Test
    public void testRange() {

        assertThat(Streams.streamOf()
                          .async()
                          .thenGetMore(range('a', 'e', new Function<Character, Character>() {

                              public Character apply(final Character character) {

                                  return (char) (character + 1);
                              }
                          }))
                          .after(seconds(3))
                          .all()).containsExactly('a', 'b', 'c', 'd', 'e');
        assertThat(Streams.streamOf()
                          .async()
                          .thenGetMore(range(0, 2, new BigDecimal(0.7)))
                          .after(seconds(3))
                          .all()).isEqualTo(Arrays.asList(new BigDecimal(0), new BigDecimal(0.7),
                new BigDecimal(0.7).add(new BigDecimal(0.7))));
        assertThat(Streams.streamOf()
                          .async()
                          .thenGetMore(range(0, -10, BigInteger.valueOf(-2)))
                          .after(seconds(3))
                          .all()).isEqualTo(
                Arrays.asList(BigInteger.valueOf(0), BigInteger.valueOf(-2), BigInteger.valueOf(-4),
                        BigInteger.valueOf(-6), BigInteger.valueOf(-8), BigInteger.valueOf(-10)));
        assertThat(Streams.streamOf()
                          .async()
                          .thenGetMore(range(0, BigInteger.valueOf(2), 0.7))
                          .after(seconds(3))
                          .all()).isEqualTo(Arrays.asList(new BigDecimal(0), new BigDecimal(0.7),
                new BigDecimal(0.7).add(new BigDecimal(0.7))));
        assertThat(Streams.streamOf()
                          .async()
                          .thenGetMore(range(0, -10, -2))
                          .after(seconds(3))
                          .all()).isEqualTo(Arrays.asList(0, -2, -4, -6, -8, -10));
        assertThat(Streams.streamOf().async().thenGetMore(range(0, 2, 0.7)).after(seconds(3)).all())
                .isEqualTo(Arrays.asList(0d, 0.7d, 1.4d));
        assertThat(Streams.streamOf()
                          .async()
                          .thenGetMore(range(0, 2, 0.7f))
                          .after(seconds(3))
                          .all()).isEqualTo(Arrays.asList(0f, 0.7f, 1.4f));
        assertThat(Streams.streamOf()
                          .async()
                          .thenGetMore(range(0L, -9, -2))
                          .after(seconds(3))
                          .all()).isEqualTo(Arrays.asList(0L, -2L, -4L, -6L, -8L));
        assertThat(Streams.streamOf()
                          .async()
                          .thenGetMore(range(0, (short) 9, 2))
                          .after(seconds(3))
                          .all()).isEqualTo(Arrays.asList(0, 2, 4, 6, 8));
        assertThat(Streams.streamOf()
                          .async()
                          .thenGetMore(range((byte) 0, (short) 9, (byte) 2))
                          .after(seconds(3))
                          .all()).isEqualTo(
                Arrays.asList((short) 0, (short) 2, (short) 4, (short) 6, (short) 8));
        assertThat(Streams.streamOf()
                          .async()
                          .thenGetMore(range((byte) 0, (byte) 10, (byte) 2))
                          .after(seconds(3))
                          .all()).isEqualTo(
                Arrays.asList((byte) 0, (byte) 2, (byte) 4, (byte) 6, (byte) 8, (byte) 10));
        assertThat(Streams.streamOf()
                          .async()
                          .thenGetMore(range(0, new BigDecimal(2)))
                          .after(seconds(3))
                          .all()).isEqualTo(
                Arrays.asList(new BigDecimal(0), new BigDecimal(1), new BigDecimal(2)));
        assertThat(Streams.streamOf()
                          .async()
                          .thenGetMore(range(0, BigInteger.valueOf(-2)))
                          .after(seconds(3))
                          .all()).isEqualTo(
                Arrays.asList(BigInteger.valueOf(0), BigInteger.valueOf(-1),
                        BigInteger.valueOf(-2)));
        assertThat(Streams.streamOf()
                          .async()
                          .thenGetMore(range(0.1, BigInteger.valueOf(2)))
                          .after(seconds(3))
                          .all()).isEqualTo(
                Arrays.asList(new BigDecimal(0.1), new BigDecimal(0.1).add(BigDecimal.ONE)));
        assertThat(Streams.streamOf()
                          .async()
                          .thenGetMore(range(0, -5))
                          .after(seconds(3))
                          .all()).isEqualTo(Arrays.asList(0, -1, -2, -3, -4, -5));
        assertThat(Streams.streamOf()
                          .async()
                          .thenGetMore(range(0, 2.1))
                          .after(seconds(3))
                          .all()).isEqualTo(Arrays.asList(0d, 1d, 2d));
        assertThat(Streams.streamOf()
                          .async()
                          .thenGetMore(range(0, 1.9f))
                          .after(seconds(3))
                          .all()).isEqualTo(Arrays.asList(0f, 1f));
        assertThat(Streams.streamOf()
                          .async()
                          .thenGetMore(range(0L, -4))
                          .after(seconds(3))
                          .all()).isEqualTo(Arrays.asList(0L, -1L, -2L, -3L, -4L));
        assertThat(
                Streams.streamOf().async().thenGetMore(range(0, (short) 4)).after(seconds(3)).all())
                .isEqualTo(Arrays.asList(0, 1, 2, 3, 4));
        assertThat(Streams.streamOf()
                          .async()
                          .thenGetMore(range((byte) 0, (short) 4))
                          .after(seconds(3))
                          .all()).isEqualTo(
                Arrays.asList((short) 0, (short) 1, (short) 2, (short) 3, (short) 4));
        assertThat(Streams.streamOf()
                          .async()
                          .thenGetMore(range((byte) 0, (byte) 5))
                          .after(seconds(3))
                          .all()).isEqualTo(
                Arrays.asList((byte) 0, (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5));
        assertThat(Streams.streamOf()
                          .order(OrderType.BY_CALL)
                          .parallel()
                          .thenGetMore(range('a', 'e', new Function<Character, Character>() {

                              public Character apply(final Character character) {

                                  return (char) (character + 1);
                              }
                          }))
                          .after(seconds(3))
                          .all()).containsExactly('a', 'b', 'c', 'd', 'e');
        assertThat(Streams.streamOf()
                          .order(OrderType.BY_CALL)
                          .parallel()
                          .thenGetMore(range(0, -10, -2))
                          .after(seconds(3))
                          .all()).isEqualTo(Arrays.asList(0, -2, -4, -6, -8, -10));
        assertThat(Streams.streamOf()
                          .order(OrderType.BY_CALL)
                          .parallel()
                          .thenGetMore(range(0, 2, 0.7))
                          .after(seconds(3))
                          .all()).isEqualTo(Arrays.asList(0d, 0.7d, 1.4d));
        assertThat(Streams.streamOf()
                          .order(OrderType.BY_CALL)
                          .parallel()
                          .thenGetMore(range(0, 2, 0.7f))
                          .after(seconds(3))
                          .all()).isEqualTo(Arrays.asList(0f, 0.7f, 1.4f));
        assertThat(Streams.streamOf()
                          .order(OrderType.BY_CALL)
                          .parallel()
                          .thenGetMore(range(0L, -9, -2))
                          .after(seconds(3))
                          .all()).isEqualTo(Arrays.asList(0L, -2L, -4L, -6L, -8L));
        assertThat(Streams.streamOf()
                          .order(OrderType.BY_CALL)
                          .parallel()
                          .thenGetMore(range(0, (short) 9, 2))
                          .after(seconds(3))
                          .all()).isEqualTo(Arrays.asList(0, 2, 4, 6, 8));
        assertThat(Streams.streamOf()
                          .order(OrderType.BY_CALL)
                          .parallel()
                          .thenGetMore(range((byte) 0, (short) 9, (byte) 2))
                          .after(seconds(3))
                          .all()).isEqualTo(
                Arrays.asList((short) 0, (short) 2, (short) 4, (short) 6, (short) 8));
        assertThat(Streams.streamOf()
                          .order(OrderType.BY_CALL)
                          .parallel()
                          .thenGetMore(range((byte) 0, (byte) 10, (byte) 2))
                          .after(seconds(3))
                          .all()).isEqualTo(
                Arrays.asList((byte) 0, (byte) 2, (byte) 4, (byte) 6, (byte) 8, (byte) 10));
        assertThat(Streams.streamOf()
                          .order(OrderType.BY_CALL)
                          .parallel()
                          .thenGetMore(range(0, -5))
                          .after(seconds(3))
                          .all()).isEqualTo(Arrays.asList(0, -1, -2, -3, -4, -5));
        assertThat(Streams.streamOf()
                          .order(OrderType.BY_CALL)
                          .parallel()
                          .thenGetMore(range(0, 2.1))
                          .after(seconds(3))
                          .all()).isEqualTo(Arrays.asList(0d, 1d, 2d));
        assertThat(Streams.streamOf()
                          .order(OrderType.BY_CALL)
                          .parallel()
                          .thenGetMore(range(0, 1.9f))
                          .after(seconds(3))
                          .all()).isEqualTo(Arrays.asList(0f, 1f));
        assertThat(Streams.streamOf()
                          .order(OrderType.BY_CALL)
                          .parallel()
                          .thenGetMore(range(0L, -4))
                          .after(seconds(3))
                          .all()).isEqualTo(Arrays.asList(0L, -1L, -2L, -3L, -4L));
        assertThat(Streams.streamOf()
                          .order(OrderType.BY_CALL)
                          .parallel()
                          .thenGetMore(range(0, (short) 4))
                          .after(seconds(3))
                          .all()).isEqualTo(Arrays.asList(0, 1, 2, 3, 4));
        assertThat(Streams.streamOf()
                          .order(OrderType.BY_CALL)
                          .parallel()
                          .thenGetMore(range((byte) 0, (short) 4))
                          .after(seconds(3))
                          .all()).isEqualTo(
                Arrays.asList((short) 0, (short) 1, (short) 2, (short) 3, (short) 4));
        assertThat(Streams.streamOf()
                          .order(OrderType.BY_CALL)
                          .parallel()
                          .thenGetMore(range((byte) 0, (byte) 5))
                          .after(seconds(3))
                          .all()).isEqualTo(
                Arrays.asList((byte) 0, (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5));
        assertThat(Streams.streamOf()
                          .sync()
                          .thenGetMore(range('a', 'e', new Function<Character, Character>() {

                              public Character apply(final Character character) {

                                  return (char) (character + 1);
                              }
                          }))
                          .all()).containsExactly('a', 'b', 'c', 'd', 'e');
        assertThat(Streams.streamOf().sync().thenGetMore(range(0, -10, -2)).all()).isEqualTo(
                Arrays.asList(0, -2, -4, -6, -8, -10));
        assertThat(Streams.streamOf().sync().thenGetMore(range(0, 2, 0.7)).all()).isEqualTo(
                Arrays.asList(0d, 0.7d, 1.4d));
        assertThat(Streams.streamOf().sync().thenGetMore(range(0, 2, 0.7f)).all()).isEqualTo(
                Arrays.asList(0f, 0.7f, 1.4f));
        assertThat(Streams.streamOf().sync().thenGetMore(range(0L, -9, -2)).all()).isEqualTo(
                Arrays.asList(0L, -2L, -4L, -6L, -8L));
        assertThat(Streams.streamOf().sync().thenGetMore(range(0, (short) 9, 2)).all()).isEqualTo(
                Arrays.asList(0, 2, 4, 6, 8));
        assertThat(Streams.streamOf()
                          .sync()
                          .thenGetMore(range((byte) 0, (short) 9, (byte) 2))
                          .all()).isEqualTo(
                Arrays.asList((short) 0, (short) 2, (short) 4, (short) 6, (short) 8));
        assertThat(Streams.streamOf()
                          .sync()
                          .thenGetMore(range((byte) 0, (byte) 10, (byte) 2))
                          .all()).isEqualTo(
                Arrays.asList((byte) 0, (byte) 2, (byte) 4, (byte) 6, (byte) 8, (byte) 10));
        assertThat(Streams.streamOf().sync().thenGetMore(range(0, -5)).all()).isEqualTo(
                Arrays.asList(0, -1, -2, -3, -4, -5));
        assertThat(Streams.streamOf().sync().thenGetMore(range(0, 2.1)).all()).isEqualTo(
                Arrays.asList(0d, 1d, 2d));
        assertThat(Streams.streamOf().sync().thenGetMore(range(0, 1.9f)).all()).isEqualTo(
                Arrays.asList(0f, 1f));
        assertThat(Streams.streamOf().sync().thenGetMore(range(0L, -4)).all()).isEqualTo(
                Arrays.asList(0L, -1L, -2L, -3L, -4L));
        assertThat(Streams.streamOf().sync().thenGetMore(range(0, (short) 4)).all()).isEqualTo(
                Arrays.asList(0, 1, 2, 3, 4));
        assertThat(
                Streams.streamOf().sync().thenGetMore(range((byte) 0, (short) 4)).all()).isEqualTo(
                Arrays.asList((short) 0, (short) 1, (short) 2, (short) 3, (short) 4));
        assertThat(
                Streams.streamOf().sync().thenGetMore(range((byte) 0, (byte) 5)).all()).isEqualTo(
                Arrays.asList((byte) 0, (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5));
    }

    @Test
    public void testRangeEquals() {

        final Consumer<? extends Channel<? extends Number, ?>> range1 =
                Streams.range(BigDecimal.ONE, 10);
        assertThat(range1).isEqualTo(range1);
        assertThat(range1).isNotEqualTo(null);
        assertThat(range1).isNotEqualTo("test");
        assertThat(range1).isNotEqualTo(Streams.range(BigDecimal.ONE, 10, 3));
        assertThat(range1).isEqualTo(Streams.range(BigDecimal.ONE, 10));
        assertThat(range1.hashCode()).isEqualTo(Streams.range(BigDecimal.ONE, 10).hashCode());

        final Consumer<? extends Channel<? extends Number, ?>> range2 =
                Streams.range(BigInteger.ONE, 10);
        assertThat(range2).isEqualTo(range2);
        assertThat(range2).isNotEqualTo(null);
        assertThat(range2).isNotEqualTo("test");
        assertThat(range2).isNotEqualTo(Streams.range(BigInteger.ONE, 10, 3));
        assertThat(range2).isEqualTo(Streams.range(BigInteger.ONE, 10));
        assertThat(range2.hashCode()).isEqualTo(Streams.range(BigInteger.ONE, 10).hashCode());

        final Consumer<? extends Channel<? extends Number, ?>> range3 = Streams.range(1, 10);
        assertThat(range3).isEqualTo(range3);
        assertThat(range3).isNotEqualTo(null);
        assertThat(range3).isNotEqualTo("test");
        assertThat(range3).isNotEqualTo(Streams.range(1, 10, 3));
        assertThat(range3).isEqualTo(Streams.range(1, 10));
        assertThat(range3.hashCode()).isEqualTo(Streams.range(1, 10).hashCode());

        final Consumer<? extends Channel<? extends Number, ?>> range4 = Streams.range(1, 10, -2);
        assertThat(range4).isEqualTo(range4);
        assertThat(range4).isNotEqualTo(null);
        assertThat(range4).isNotEqualTo("test");
        assertThat(range4).isNotEqualTo(Streams.range(1, 10, 1));
        assertThat(range4).isEqualTo(Streams.range(1, 10, -2));
        assertThat(range4.hashCode()).isEqualTo(Streams.range(1, 10, -2).hashCode());

        final Function<Character, Character> function = new Function<Character, Character>() {

            public Character apply(final Character character) {

                return (char) (character + 1);
            }
        };
        final Consumer<? extends Channel<? extends Character, ?>> range5 =
                Streams.range('a', 'f', function);
        assertThat(range5).isEqualTo(range5);
        assertThat(range5).isNotEqualTo(null);
        assertThat(range5).isNotEqualTo("test");
        assertThat(range5).isNotEqualTo(Streams.range('b', 'f', function));
        assertThat(range5).isEqualTo(Streams.range('a', 'f', function));
        assertThat(range5.hashCode()).isEqualTo(Streams.range('a', 'f', function).hashCode());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testRangeError() {

        try {

            Streams.range(null, 'f', new Function<Character, Character>() {

                public Character apply(final Character character) {

                    return (char) (character + 1);
                }
            });

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.range('a', null, new Function<Character, Character>() {

                public Character apply(final Character character) {

                    return (char) (character + 1);
                }
            });

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.range('a', 'f', null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.range(null, 1, 1);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.range(1, null, 1);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.range(1, 1, (Number) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        final Number number = new Number() {

            @Override
            public int intValue() {

                return 0;
            }

            @Override
            public long longValue() {

                return 0;
            }

            @Override
            public float floatValue() {

                return 0;
            }

            @Override
            public double doubleValue() {

                return 0;
            }
        };

        try {

            Streams.range(number, number, number);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.range(number, number);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testReplay() {

        final Channel<Object, Object> ioChannel = JRoutineCore.io().buildChannel();
        final Channel<?, Object> channel = Streams.replay(ioChannel).buildChannels();
        ioChannel.pass("test1", "test2");
        final Channel<Object, Object> output1 = JRoutineCore.io().buildChannel();
        channel.bind(output1).close();
        assertThat(output1.next()).isEqualTo("test1");
        final Channel<Object, Object> output2 = JRoutineCore.io().buildChannel();
        channel.bind(output2).close();
        ioChannel.pass("test3").close();
        assertThat(output2.all()).containsExactly("test1", "test2", "test3");
        assertThat(output1.all()).containsExactly("test2", "test3");
    }

    @Test
    public void testReplayAbort() {

        final Channel<Object, Object> ioChannel = JRoutineCore.io().buildChannel();
        final Channel<?, Object> channel = Streams.replay(ioChannel).buildChannels();
        ioChannel.pass("test1", "test2");
        final Channel<Object, Object> output1 = JRoutineCore.io().buildChannel();
        channel.bind(output1).close();
        assertThat(output1.next()).isEqualTo("test1");
        final Channel<Object, Object> output2 = JRoutineCore.io().buildChannel();
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

    @Test
    @SuppressWarnings("unchecked")
    public void testSelectMap() {

        final Routine<Selectable<Object>, Selectable<Object>> routine =
                JRoutineCore.on(new Sort()).buildRoutine();
        final Channel<Selectable<Object>, Selectable<Object>> inputChannel =
                JRoutineCore.io().buildChannel();
        final Channel<?, Selectable<Object>> outputChannel = routine.async(inputChannel);
        final StreamChannel<Object, Object> intChannel =
                Streams.selectOutput(outputChannel, Sort.INTEGER, Sort.STRING)
                       .channelConfiguration()
                       .withLogLevel(Level.WARNING)
                       .apply()
                       .buildChannels()
                       .get(Sort.INTEGER);
        final StreamChannel<Object, Object> strChannel =
                Streams.selectOutput(outputChannel, Arrays.asList(Sort.STRING, Sort.INTEGER))
                       .channelConfiguration()
                       .withLogLevel(Level.WARNING)
                       .apply()
                       .buildChannels()
                       .get(Sort.STRING);
        inputChannel.pass(new Selectable<Object>("test21", Sort.STRING),
                new Selectable<Object>(-11, Sort.INTEGER));
        assertThat(intChannel.after(seconds(10)).next()).isEqualTo(-11);
        assertThat(strChannel.after(seconds(10)).next()).isEqualTo("test21");
        inputChannel.pass(new Selectable<Object>(-11, Sort.INTEGER),
                new Selectable<Object>("test21", Sort.STRING));
        assertThat(intChannel.after(seconds(10)).next()).isEqualTo(-11);
        assertThat(strChannel.after(seconds(10)).next()).isEqualTo("test21");
        inputChannel.pass(new Selectable<Object>("test21", Sort.STRING),
                new Selectable<Object>(-11, Sort.INTEGER));
        assertThat(intChannel.after(seconds(10)).next()).isEqualTo(-11);
        assertThat(strChannel.after(seconds(10)).next()).isEqualTo("test21");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSelectMapAbort() {

        final Routine<Selectable<Object>, Selectable<Object>> routine =
                JRoutineCore.on(new Sort()).buildRoutine();
        Channel<Selectable<Object>, Selectable<Object>> inputChannel =
                JRoutineCore.io().buildChannel();
        Channel<?, Selectable<Object>> outputChannel = routine.async(inputChannel);
        Streams.selectOutput(Sort.STRING, 2, outputChannel).buildChannels();
        inputChannel.after(millis(100))
                    .pass(new Selectable<Object>("test21", Sort.STRING),
                            new Selectable<Object>(-11, Sort.INTEGER))
                    .abort();

        try {

            Streams.selectOutput(outputChannel, Sort.STRING, Sort.INTEGER)
                   .buildChannels()
                   .get(Sort.STRING)
                   .after(seconds(1))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            Streams.selectOutput(outputChannel, Sort.INTEGER, Sort.STRING)
                   .buildChannels()
                   .get(Sort.INTEGER)
                   .after(seconds(1))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }

        inputChannel = JRoutineCore.io().buildChannel();
        outputChannel = routine.async(inputChannel);
        Streams.selectOutput(outputChannel, Sort.INTEGER, Sort.STRING).buildChannels();
        inputChannel.after(millis(100))
                    .pass(new Selectable<Object>(-11, Sort.INTEGER),
                            new Selectable<Object>("test21", Sort.STRING))
                    .abort();

        try {

            Streams.selectOutput(outputChannel, Sort.STRING, Sort.INTEGER)
                   .buildChannels()
                   .get(Sort.STRING)
                   .after(seconds(1))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            Streams.selectOutput(outputChannel, Sort.STRING, Sort.INTEGER)
                   .buildChannels()
                   .get(Sort.INTEGER)
                   .after(seconds(1))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }

        inputChannel = JRoutineCore.io().buildChannel();
        outputChannel = routine.async(inputChannel);
        Streams.selectOutput(outputChannel, Arrays.asList(Sort.STRING, Sort.INTEGER))
               .buildChannels();
        inputChannel.after(millis(100))
                    .pass(new Selectable<Object>("test21", Sort.STRING),
                            new Selectable<Object>(-11, Sort.INTEGER))
                    .abort();

        try {

            Streams.selectOutput(outputChannel, Sort.INTEGER, Sort.STRING)
                   .buildChannels()
                   .get(Sort.STRING)
                   .after(seconds(1))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            Streams.selectOutput(outputChannel, Sort.INTEGER, Sort.STRING)
                   .buildChannels()
                   .get(Sort.INTEGER)
                   .after(seconds(1))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @Test
    public void testSequence() {

        assertThat(Streams.streamOf()
                          .async()
                          .thenGetMore(
                                  sequence('a', 5, new BiFunction<Character, Long, Character>() {

                                      public Character apply(final Character character,
                                              final Long n) {

                                          return (char) (character + 1);
                                      }
                                  }))
                          .after(seconds(3))
                          .all()).containsExactly('a', 'b', 'c', 'd', 'e');
        assertThat(Streams.streamOf()
                          .order(OrderType.BY_CALL)
                          .parallel()
                          .thenGetMore(
                                  sequence('a', 5, new BiFunction<Character, Long, Character>() {

                                      public Character apply(final Character character,
                                              final Long n) {

                                          return (char) (character + 1);
                                      }
                                  }))
                          .after(seconds(3))
                          .all()).containsExactly('a', 'b', 'c', 'd', 'e');
        assertThat(Streams.streamOf()
                          .sync()
                          .thenGetMore(
                                  sequence('a', 5, new BiFunction<Character, Long, Character>() {

                                      public Character apply(final Character character,
                                              final Long n) {

                                          return (char) (character + 1);
                                      }
                                  }))
                          .all()).containsExactly('a', 'b', 'c', 'd', 'e');
    }

    @Test
    public void testSequenceEquals() {

        final Consumer<Channel<Integer, ?>> series1 =
                Streams.sequence(1, 10, Functions.<Integer, Long>first());
        assertThat(series1).isEqualTo(series1);
        assertThat(series1).isNotEqualTo(null);
        assertThat(series1).isNotEqualTo("test");
        assertThat(series1).isNotEqualTo(Streams.sequence(1, 9, Functions.<Integer, Long>first()));
        assertThat(series1).isEqualTo(Streams.sequence(1, 10, Functions.<Integer, Long>first()));
        assertThat(series1.hashCode()).isEqualTo(
                Streams.sequence(1, 10, Functions.<Integer, Long>first()).hashCode());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testSequenceError() {

        try {
            Streams.sequence(null, 2, new BiFunction<Character, Long, Character>() {

                public Character apply(final Character character, final Long n) {

                    return (char) (character + 1);
                }
            });
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            Streams.sequence('a', 2, null);
            fail();

        } catch (final NullPointerException ignored) {

        }

        try {
            Streams.sequence(1, -1, Functions.<Integer, Long>first());
            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    private static class Amb<DATA> extends TemplateInvocation<Selectable<DATA>, DATA> {

        private static final int NO_INDEX = Integer.MIN_VALUE;

        private int mFirstIndex;

        @Override
        public void onInput(final Selectable<DATA> input, @NotNull final Channel<DATA, ?> result) {

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

    private static class Sort extends MappingInvocation<Selectable<Object>, Selectable<Object>> {

        private static final int INTEGER = 1;

        private static final int STRING = 0;

        /**
         * Constructor.
         */
        protected Sort() {

            super(null);
        }

        public void onInput(final Selectable<Object> selectable,
                @NotNull final Channel<Selectable<Object>, ?> result) {

            switch (selectable.index) {

                case INTEGER:
                    Channels.<Object, Integer>selectInput(result, INTEGER).buildChannels()
                                                                          .pass(selectable
                                                                                  .<Integer>data())
                                                                          .close();
                    break;

                case STRING:
                    Channels.<Object, String>selectInput(result, STRING).buildChannels()
                                                                        .pass(selectable
                                                                                .<String>data())
                                                                        .close();
                    break;
            }
        }
    }
}
