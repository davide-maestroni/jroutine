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

import com.github.dm.jrt.builder.IOChannelBuilder;
import com.github.dm.jrt.builder.InvocationConfiguration.OrderType;
import com.github.dm.jrt.channel.AbortException;
import com.github.dm.jrt.channel.Channel.InputChannel;
import com.github.dm.jrt.channel.Channel.OutputChannel;
import com.github.dm.jrt.channel.IOChannel;
import com.github.dm.jrt.channel.InvocationChannel;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.core.JRoutine;
import com.github.dm.jrt.ext.channel.Channels;
import com.github.dm.jrt.ext.channel.Selectable;
import com.github.dm.jrt.function.BiConsumerWrapper;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Functions;
import com.github.dm.jrt.invocation.FilterInvocation;
import com.github.dm.jrt.invocation.InvocationException;
import com.github.dm.jrt.invocation.InvocationFactory;
import com.github.dm.jrt.invocation.PassingInvocation;
import com.github.dm.jrt.invocation.TemplateInvocation;
import com.github.dm.jrt.log.Log.Level;
import com.github.dm.jrt.routine.Routine;
import com.github.dm.jrt.stream.Streams.RangeConsumer;
import com.github.dm.jrt.util.ClassToken;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.dm.jrt.function.Functions.wrap;
import static com.github.dm.jrt.invocation.Invocations.factoryOf;
import static com.github.dm.jrt.stream.Streams.range;
import static com.github.dm.jrt.util.TimeDuration.millis;
import static com.github.dm.jrt.util.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Streams unit tests.
 * <p/>
 * Created by davide-maestroni on 12/24/2015.
 */
public class StreamsTest {

    @Test
    public void testBlend() {

        StreamChannel<String> channel1 = Streams.streamOf("test1", "test2", "test3");
        StreamChannel<String> channel2 = Streams.streamOf("test4", "test5", "test6");
        assertThat(
                Streams.blend(channel2, channel1).build().afterMax(seconds(1)).all()).containsOnly(
                "test1", "test2", "test3", "test4", "test5", "test6");
        channel1 = Streams.streamOf("test1", "test2", "test3");
        channel2 = Streams.streamOf("test4", "test5", "test6");
        assertThat(Streams.blend(Arrays.<StreamChannel<?>>asList(channel1, channel2))
                          .build()
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

            routine.asyncCall(Streams.blend(channel1, channel2).build()).afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().abort();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();

        try {

            routine.asyncCall(
                    Streams.blend(Arrays.<OutputChannel<?>>asList(channel1, channel2)).build())
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

            Streams.blend();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.blend((OutputChannel<?>[]) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.blend(new OutputChannel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.blend(Collections.<OutputChannel<?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.blend((List<OutputChannel<?>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.blend(Collections.<OutputChannel<?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testConcat() {

        StreamChannel<String> channel1 = Streams.streamOf("test1", "test2", "test3");
        StreamChannel<String> channel2 = Streams.streamOf("test4", "test5", "test6");
        assertThat(Streams.concat(channel2, channel1)
                          .build()
                          .afterMax(seconds(1))
                          .all()).containsExactly("test4", "test5", "test6", "test1", "test2",
                                                  "test3");
        channel1 = Streams.streamOf("test1", "test2", "test3");
        channel2 = Streams.streamOf("test4", "test5", "test6");
        assertThat(Streams.concat(Arrays.<StreamChannel<?>>asList(channel1, channel2))
                          .build()
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

            routine.asyncCall(Streams.concat(channel1, channel2).build())
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
                    Streams.concat(Arrays.<OutputChannel<?>>asList(channel1, channel2)).build())
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

            Streams.concat();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.concat((OutputChannel<?>[]) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.concat(new OutputChannel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.concat(Collections.<OutputChannel<?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.concat((List<OutputChannel<?>>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.concat(Collections.<OutputChannel<?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testFactory() {

        final InvocationFactory<String, String> factory = Streams.factory(
                new Function<StreamChannel<? extends String>, StreamChannel<String>>() {

                    public StreamChannel<String> apply(
                            final StreamChannel<? extends String> channel) {

                        return channel.sync().map(new Function<String, String>() {

                            public String apply(final String s) {

                                return s.toUpperCase();
                            }
                        });
                    }
                });
        assertThat(JRoutine.on(factory)
                           .asyncCall("test1", "test2", "test3")
                           .afterMax(seconds(3))
                           .all()).containsExactly("TEST1", "TEST2", "TEST3");

        try {

            final InvocationChannel<String, String> channel = JRoutine.on(factory).asyncInvoke();
            channel.abort(new IllegalArgumentException());
            channel.result().afterMax(seconds(3)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        assertThat(Streams.onStream(new Function<StreamChannel<String>, StreamChannel<String>>() {

            public StreamChannel<String> apply(final StreamChannel<String> channel) {

                return channel.sync().map(new Function<String, String>() {

                    public String apply(final String s) {

                        return s.toUpperCase();
                    }
                });
            }
        }).asyncCall("test1", "test2", "test3").afterMax(seconds(3)).all()).containsExactly("TEST1",
                                                                                            "TEST2",
                                                                                            "TEST3");

        try {

            final InvocationChannel<String, String> channel =
                    Streams.onStream(new Function<StreamChannel<String>, StreamChannel<String>>() {

                        public StreamChannel<String> apply(final StreamChannel<String> channel) {

                            return channel.sync().map(new Function<String, String>() {

                                public String apply(final String s) {

                                    return s.toUpperCase();
                                }
                            });
                        }
                    }).asyncInvoke();
            channel.abort(new IllegalArgumentException());
            channel.result().afterMax(seconds(3)).next();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @Test
    public void testFactoryEquals() {

        final Function<StreamChannel<String>, StreamChannel<String>> function =
                new Function<StreamChannel<String>, StreamChannel<String>>() {

                    public StreamChannel<String> apply(final StreamChannel<String> channel) {

                        return channel.sync().map(new Function<String, String>() {

                            public String apply(final String s) {

                                return s.toUpperCase();
                            }
                        });
                    }
                };
        final InvocationFactory<String, String> factory = Streams.factory(function);
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(null);
        assertThat(factory).isNotEqualTo("test");
        assertThat(factory).isNotEqualTo(Streams.factory(Functions.<StreamChannel<?>>identity()));
        assertThat(factory).isEqualTo(Streams.factory(function));
        assertThat(factory.hashCode()).isEqualTo(Streams.factory(function).hashCode());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testFactoryError() {

        try {

            Streams.factory(null);

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
    public void testGroupBy() {

        assertThat(Streams.streamOf()
                          .sync()
                          .then(range(1, 10))
                          .async()
                          .map(Streams.<Number>groupBy(3))
                          .afterMax(seconds(3))
                          .all()).containsExactly(Arrays.<Number>asList(1, 2, 3),
                                                  Arrays.<Number>asList(4, 5, 6),
                                                  Arrays.<Number>asList(7, 8, 9),
                                                  Collections.<Number>singletonList(10));
        assertThat(Streams.streamOf()
                          .sync()
                          .then(range(1, 10))
                          .async()
                          .map(Streams.<Number>groupBy(13))
                          .afterMax(seconds(3))
                          .all()).containsExactly(
                Arrays.<Number>asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
    }

    @Test
    public void testGroupByEquals() {

        final InvocationFactory<Object, List<Object>> factory = Streams.groupBy(2);
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(null);
        assertThat(factory).isNotEqualTo("test");
        assertThat(factory).isNotEqualTo(Streams.groupBy(3));
        assertThat(factory).isEqualTo(Streams.groupBy(2));
        assertThat(factory.hashCode()).isEqualTo(Streams.groupBy(2).hashCode());
    }

    @Test
    public void testGroupByError() {

        try {

            Streams.groupBy(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.groupBy(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGroupByPlaceholder() {

        assertThat(Streams.streamOf()
                          .sync()
                          .then(range(1, 10))
                          .async()
                          .map(Streams.<Number>groupBy(3, 0))
                          .afterMax(seconds(3))
                          .all()).containsExactly(Arrays.<Number>asList(1, 2, 3),
                                                  Arrays.<Number>asList(4, 5, 6),
                                                  Arrays.<Number>asList(7, 8, 9),
                                                  Arrays.<Number>asList(10, 0, 0));
        assertThat(Streams.streamOf()
                          .sync()
                          .then(range(1, 10))
                          .async()
                          .map(Streams.<Number>groupBy(13, -1))
                          .afterMax(seconds(3))
                          .all()).containsExactly(
                Arrays.<Number>asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, -1, -1, -1));
        assertThat(Streams.streamOf()
                          .sync()
                          .then(range(1, 10))
                          .async()
                          .map(Streams.<Number>groupBy(3, -31))
                          .afterMax(seconds(3))
                          .all()).containsExactly(Arrays.<Number>asList(1, 2, 3),
                                                  Arrays.<Number>asList(4, 5, 6),
                                                  Arrays.<Number>asList(7, 8, 9),
                                                  Arrays.<Number>asList(10, -31, -31));
        assertThat(Streams.streamOf()
                          .sync()
                          .then(range(1, 10))
                          .async()
                          .map(Streams.<Number>groupBy(13, 71))
                          .afterMax(seconds(3))
                          .all()).containsExactly(
                Arrays.<Number>asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 71, 71, 71));
    }

    @Test
    public void testGroupByPlaceholderEquals() {

        final Object placeholder = -11;
        final InvocationFactory<Object, List<Object>> factory = Streams.groupBy(2, placeholder);
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(null);
        assertThat(factory).isNotEqualTo("test");
        assertThat(factory).isNotEqualTo(Streams.groupBy(3, -11));
        assertThat(factory).isEqualTo(Streams.groupBy(2, -11));
        assertThat(factory.hashCode()).isEqualTo(Streams.groupBy(2, -11).hashCode());
    }

    @Test
    public void testGroupByPlaceholderError() {

        try {

            Streams.groupBy(-1, 77);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.groupBy(0, null);

            fail();

        } catch (final IllegalArgumentException ignored) {

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
        assertThat(routine.asyncCall(Streams.join(channel1, channel2).build())
                          .afterMax(seconds(10))
                          .all()).containsExactly('s', '2');
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.asyncCall(
                Streams.join(Arrays.<OutputChannel<?>>asList(channel1, channel2)).build())
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
        assertThat(routine.asyncCall(Streams.join(channel1, channel2).build())
                          .afterMax(seconds(10))
                          .all()).containsExactly('s', '2');
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

            routine.asyncCall(Streams.join(channel1, channel2).build()).afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().abort();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();

        try {

            routine.asyncCall(
                    Streams.join(Arrays.<OutputChannel<?>>asList(channel1, channel2)).build())
                   .afterMax(seconds(1))
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

            Streams.join(Collections.<OutputChannel<?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.join(new OutputChannel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.join(Collections.<OutputChannel<?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testJoinPlaceholder() {

        final IOChannelBuilder builder = JRoutine.io();
        final Routine<List<?>, Character> routine = JRoutine.on(new CharAt()).buildRoutine();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.asyncCall(Streams.join(new Object(), channel1, channel2).build())
                          .afterMax(seconds(10))
                          .all()).containsExactly('s', '2');
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.asyncCall(
                Streams.join(null, Arrays.<OutputChannel<?>>asList(channel1, channel2)).build())
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

            routine.asyncCall(Streams.join(new Object(), channel1, channel2).build())
                   .afterMax(seconds(10))
                   .all();

            fail();

        } catch (final InvocationException ignored) {

        }
    }

    @Test
    public void testJoinPlaceholderAbort() {

        final IOChannelBuilder builder = JRoutine.io();
        final Routine<List<?>, Character> routine = JRoutine.on(new CharAt()).buildRoutine();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().abort();

        try {

            routine.asyncCall(Streams.join((Object) null, channel1, channel2).build())
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
                    Streams.join(new Object(), Arrays.<OutputChannel<?>>asList(channel1, channel2))
                           .build()).afterMax(seconds(1)).all();

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

            Streams.join(null, Collections.<OutputChannel<?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.join(new Object(), new OutputChannel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.join(new Object(), Collections.<OutputChannel<?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testLimit() {

        assertThat(Streams.streamOf()
                          .sync()
                          .then(range(1, 10))
                          .async()
                          .map(Streams.limit(5))
                          .afterMax(seconds(3))
                          .all()).containsExactly(1, 2, 3, 4, 5);
        assertThat(Streams.streamOf()
                          .sync()
                          .then(range(1, 10))
                          .async()
                          .map(Streams.limit(0))
                          .afterMax(seconds(3))
                          .all()).isEmpty();
        assertThat(Streams.streamOf()
                          .sync()
                          .then(range(1, 10))
                          .async()
                          .map(Streams.limit(15))
                          .afterMax(seconds(3))
                          .all()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(Streams.streamOf()
                          .sync()
                          .then(range(1, 10))
                          .async()
                          .map(Streams.limit(0))
                          .afterMax(seconds(3))
                          .all()).isEmpty();
    }

    @Test
    public void testLimitEquals() {

        final InvocationFactory<Object, Object> factory = Streams.limit(2);
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(null);
        assertThat(factory).isNotEqualTo("test");
        assertThat(factory).isNotEqualTo(Streams.limit(3));
        assertThat(factory).isEqualTo(Streams.limit(2));
        assertThat(factory.hashCode()).isEqualTo(Streams.limit(2).hashCode());
    }

    @Test
    public void testLimitError() {

        try {

            Streams.limit(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testMap() {

        final IOChannelBuilder builder =
                JRoutine.io().withChannels().withChannelOrder(OrderType.BY_CALL).getConfigured();
        final IOChannel<String> channel1 = builder.buildChannel();
        final IOChannel<Integer> channel2 = builder.buildChannel();

        final OutputChannel<? extends Selectable<Object>> channel =
                Streams.merge(Arrays.<OutputChannel<?>>asList(channel1, channel2)).build();
        final OutputChannel<Selectable<Object>> output = JRoutine.on(new Sort())
                                                                 .withInvocations()
                                                                 .withInputOrder(OrderType.BY_CALL)
                                                                 .getConfigured()
                                                                 .asyncCall(channel);
        final Map<Integer, OutputChannel<Object>> channelMap =
                Channels.select(output, Sort.INTEGER, Sort.STRING).build();

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
                JRoutine.io().withChannels().withChannelOrder(OrderType.BY_CALL).getConfigured();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        OutputChannel<? extends Selectable<?>> outputChannel;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = Streams.merge(-7, channel1, channel2).build();
        channel1.pass("test1").close();
        channel2.pass(13).close();
        assertThat(outputChannel.afterMax(seconds(1)).all()).containsOnly(
                new Selectable<String>("test1", -7), new Selectable<Integer>(13, -6));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel =
                Streams.merge(11, Arrays.<OutputChannel<?>>asList(channel1, channel2)).build();
        channel2.pass(13).close();
        channel1.pass("test1").close();
        assertThat(outputChannel.afterMax(seconds(1)).all()).containsOnly(
                new Selectable<String>("test1", 11), new Selectable<Integer>(13, 12));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = Streams.merge(channel1, channel2).build();
        channel1.pass("test2").close();
        channel2.pass(-17).close();
        assertThat(outputChannel.afterMax(seconds(1)).all()).containsOnly(
                new Selectable<String>("test2", 0), new Selectable<Integer>(-17, 1));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = Streams.merge(Arrays.<OutputChannel<?>>asList(channel1, channel2)).build();
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
        outputChannel = Streams.merge(channelMap).build();
        channel1.pass("test3").close();
        channel2.pass(111).close();
        assertThat(outputChannel.afterMax(seconds(1)).all()).containsOnly(
                new Selectable<String>("test3", 7), new Selectable<Integer>(111, -3));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMerge4() {

        final IOChannelBuilder builder =
                JRoutine.io().withChannels().withChannelOrder(OrderType.BY_CALL).getConfigured();
        final IOChannel<String> channel1 = builder.buildChannel();
        final IOChannel<String> channel2 = builder.buildChannel();
        final IOChannel<String> channel3 = builder.buildChannel();
        final IOChannel<String> channel4 = builder.buildChannel();

        final Routine<Selectable<String>, String> routine =
                JRoutine.on(factoryOf(new ClassToken<Amb<String>>() {})).buildRoutine();
        final OutputChannel<String> outputChannel = routine.asyncCall(
                Streams.merge(Arrays.asList(channel1, channel2, channel3, channel4)).build());

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
                JRoutine.io().withChannels().withChannelOrder(OrderType.BY_CALL).getConfigured();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        OutputChannel<? extends Selectable<?>> outputChannel;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = Streams.merge(-7, channel1, channel2).build();
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
                Streams.merge(11, Arrays.<OutputChannel<?>>asList(channel1, channel2)).build();
        channel2.abort();
        channel1.pass("test1").close();

        try {

            outputChannel.afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = Streams.merge(channel1, channel2).build();
        channel1.abort();
        channel2.pass(-17).close();

        try {

            outputChannel.afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = Streams.merge(Arrays.<OutputChannel<?>>asList(channel1, channel2)).build();
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
        outputChannel = Streams.merge(channelMap).build();
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

            Streams.merge(0, Collections.<OutputChannel<Object>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.merge(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.merge(Collections.<OutputChannel<Object>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.merge(Collections.<Integer, OutputChannel<Object>>emptyMap());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.merge();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.merge(new OutputChannel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.merge(Collections.<OutputChannel<?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.merge(0, new OutputChannel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.merge(0, Collections.<OutputChannel<?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.merge(Collections.<Integer, OutputChannel<?>>singletonMap(1, null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testOutputToSelectable() {

        final IOChannel<String> channel = JRoutine.io().buildChannel();
        channel.pass("test1", "test2", "test3").close();
        assertThat(Streams.toSelectable(channel.asOutput(), 33)
                          .build()
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
            Streams.toSelectable(channel.asOutput(), 33).build().afterMax(seconds(1)).all();
            fail();

        } catch (final AbortException ignored) {

        }
    }

    @Test
    public void testRange() {

        assertThat(Streams.streamOf()
                          .async()
                          .then(range('a', 'e', new Function<Character, Character>() {

                              public Character apply(final Character character) {

                                  return (char) (character + 1);
                              }
                          }))
                          .afterMax(seconds(3))
                          .all()).containsExactly('a', 'b', 'c', 'd', 'e');
        assertThat(Streams.streamOf()
                          .async()
                          .then(range(0, -10, -2))
                          .afterMax(seconds(3))
                          .all()).isEqualTo(Arrays.asList(0, -2, -4, -6, -8, -10));
        assertThat(Streams.streamOf()
                          .async()
                          .then(range(0, 2, 0.7))
                          .afterMax(seconds(3))
                          .all()).isEqualTo(Arrays.asList(0d, 0.7d, 1.4d));
        assertThat(Streams.streamOf()
                          .async()
                          .then(range(0, 2, 0.7f))
                          .afterMax(seconds(3))
                          .all()).isEqualTo(Arrays.asList(0f, 0.7f, 1.4f));
        assertThat(Streams.streamOf()
                          .async()
                          .then(range(0L, -9, -2))
                          .afterMax(seconds(3))
                          .all()).isEqualTo(Arrays.asList(0L, -2L, -4L, -6L, -8L));
        assertThat(Streams.streamOf()
                          .async()
                          .then(range(0, (short) 9, 2))
                          .afterMax(seconds(3))
                          .all()).isEqualTo(Arrays.asList(0, 2, 4, 6, 8));
        assertThat(Streams.streamOf()
                          .async()
                          .then(range((byte) 0, (short) 9, (byte) 2))
                          .afterMax(seconds(3))
                          .all()).isEqualTo(
                Arrays.asList((short) 0, (short) 2, (short) 4, (short) 6, (short) 8));
        assertThat(Streams.streamOf()
                          .async()
                          .then(range((byte) 0, (byte) 10, (byte) 2))
                          .afterMax(seconds(3))
                          .all()).isEqualTo(
                Arrays.asList((byte) 0, (byte) 2, (byte) 4, (byte) 6, (byte) 8, (byte) 10));
        assertThat(
                Streams.streamOf().async().then(range(0, -5)).afterMax(seconds(3)).all()).isEqualTo(
                Arrays.asList(0, -1, -2, -3, -4, -5));
        assertThat(Streams.streamOf()
                          .async()
                          .then(range(0, 2.1))
                          .afterMax(seconds(3))
                          .all()).isEqualTo(Arrays.asList(0d, 1d, 2d));
        assertThat(Streams.streamOf()
                          .async()
                          .then(range(0, 1.9f))
                          .afterMax(seconds(3))
                          .all()).isEqualTo(Arrays.asList(0f, 1f));
        assertThat(Streams.streamOf()
                          .async()
                          .then(range(0L, -4))
                          .afterMax(seconds(3))
                          .all()).isEqualTo(Arrays.asList(0L, -1L, -2L, -3L, -4L));
        assertThat(Streams.streamOf()
                          .async()
                          .then(range(0, (short) 4))
                          .afterMax(seconds(3))
                          .all()).isEqualTo(Arrays.asList(0, 1, 2, 3, 4));
        assertThat(Streams.streamOf()
                          .async()
                          .then(range((byte) 0, (short) 4))
                          .afterMax(seconds(3))
                          .all()).isEqualTo(
                Arrays.asList((short) 0, (short) 1, (short) 2, (short) 3, (short) 4));
        assertThat(Streams.streamOf()
                          .async()
                          .then(range((byte) 0, (byte) 5))
                          .afterMax(seconds(3))
                          .all()).isEqualTo(
                Arrays.asList((byte) 0, (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5));
        assertThat(Streams.streamOf()
                          .ordered(OrderType.BY_CALL)
                          .parallel()
                          .then(range('a', 'e', new Function<Character, Character>() {

                              public Character apply(final Character character) {

                                  return (char) (character + 1);
                              }
                          }))
                          .afterMax(seconds(3))
                          .all()).containsExactly('a', 'b', 'c', 'd', 'e');
        assertThat(Streams.streamOf()
                          .ordered(OrderType.BY_CALL)
                          .parallel()
                          .then(range(0, -10, -2))
                          .afterMax(seconds(3))
                          .all()).isEqualTo(Arrays.asList(0, -2, -4, -6, -8, -10));
        assertThat(Streams.streamOf()
                          .ordered(OrderType.BY_CALL)
                          .parallel()
                          .then(range(0, 2, 0.7))
                          .afterMax(seconds(3))
                          .all()).isEqualTo(Arrays.asList(0d, 0.7d, 1.4d));
        assertThat(Streams.streamOf()
                          .ordered(OrderType.BY_CALL)
                          .parallel()
                          .then(range(0, 2, 0.7f))
                          .afterMax(seconds(3))
                          .all()).isEqualTo(Arrays.asList(0f, 0.7f, 1.4f));
        assertThat(Streams.streamOf()
                          .ordered(OrderType.BY_CALL)
                          .parallel()
                          .then(range(0L, -9, -2))
                          .afterMax(seconds(3))
                          .all()).isEqualTo(Arrays.asList(0L, -2L, -4L, -6L, -8L));
        assertThat(Streams.streamOf()
                          .ordered(OrderType.BY_CALL)
                          .parallel()
                          .then(range(0, (short) 9, 2))
                          .afterMax(seconds(3))
                          .all()).isEqualTo(Arrays.asList(0, 2, 4, 6, 8));
        assertThat(Streams.streamOf()
                          .ordered(OrderType.BY_CALL)
                          .parallel()
                          .then(range((byte) 0, (short) 9, (byte) 2))
                          .afterMax(seconds(3))
                          .all()).isEqualTo(
                Arrays.asList((short) 0, (short) 2, (short) 4, (short) 6, (short) 8));
        assertThat(Streams.streamOf()
                          .ordered(OrderType.BY_CALL)
                          .parallel()
                          .then(range((byte) 0, (byte) 10, (byte) 2))
                          .afterMax(seconds(3))
                          .all()).isEqualTo(
                Arrays.asList((byte) 0, (byte) 2, (byte) 4, (byte) 6, (byte) 8, (byte) 10));
        assertThat(Streams.streamOf()
                          .ordered(OrderType.BY_CALL)
                          .parallel()
                          .then(range(0, -5))
                          .afterMax(seconds(3))
                          .all()).isEqualTo(Arrays.asList(0, -1, -2, -3, -4, -5));
        assertThat(Streams.streamOf()
                          .ordered(OrderType.BY_CALL)
                          .parallel()
                          .then(range(0, 2.1))
                          .afterMax(seconds(3))
                          .all()).isEqualTo(Arrays.asList(0d, 1d, 2d));
        assertThat(Streams.streamOf()
                          .ordered(OrderType.BY_CALL)
                          .parallel()
                          .then(range(0, 1.9f))
                          .afterMax(seconds(3))
                          .all()).isEqualTo(Arrays.asList(0f, 1f));
        assertThat(Streams.streamOf()
                          .ordered(OrderType.BY_CALL)
                          .parallel()
                          .then(range(0L, -4))
                          .afterMax(seconds(3))
                          .all()).isEqualTo(Arrays.asList(0L, -1L, -2L, -3L, -4L));
        assertThat(Streams.streamOf()
                          .ordered(OrderType.BY_CALL)
                          .parallel()
                          .then(range(0, (short) 4))
                          .afterMax(seconds(3))
                          .all()).isEqualTo(Arrays.asList(0, 1, 2, 3, 4));
        assertThat(Streams.streamOf()
                          .ordered(OrderType.BY_CALL)
                          .parallel()
                          .then(range((byte) 0, (short) 4))
                          .afterMax(seconds(3))
                          .all()).isEqualTo(
                Arrays.asList((short) 0, (short) 1, (short) 2, (short) 3, (short) 4));
        assertThat(Streams.streamOf()
                          .ordered(OrderType.BY_CALL)
                          .parallel()
                          .then(range((byte) 0, (byte) 5))
                          .afterMax(seconds(3))
                          .all()).isEqualTo(
                Arrays.asList((byte) 0, (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5));
        assertThat(Streams.streamOf()
                          .sync()
                          .then(range('a', 'e', new Function<Character, Character>() {

                              public Character apply(final Character character) {

                                  return (char) (character + 1);
                              }
                          }))
                          .all()).containsExactly('a', 'b', 'c', 'd', 'e');
        assertThat(Streams.streamOf().sync().then(range(0, -10, -2)).all()).isEqualTo(
                Arrays.asList(0, -2, -4, -6, -8, -10));
        assertThat(Streams.streamOf().sync().then(range(0, 2, 0.7)).all()).isEqualTo(
                Arrays.asList(0d, 0.7d, 1.4d));
        assertThat(Streams.streamOf().sync().then(range(0, 2, 0.7f)).all()).isEqualTo(
                Arrays.asList(0f, 0.7f, 1.4f));
        assertThat(Streams.streamOf().sync().then(range(0L, -9, -2)).all()).isEqualTo(
                Arrays.asList(0L, -2L, -4L, -6L, -8L));
        assertThat(Streams.streamOf().sync().then(range(0, (short) 9, 2)).all()).isEqualTo(
                Arrays.asList(0, 2, 4, 6, 8));
        assertThat(Streams.streamOf()
                          .sync()
                          .then(range((byte) 0, (short) 9, (byte) 2))
                          .all()).isEqualTo(
                Arrays.asList((short) 0, (short) 2, (short) 4, (short) 6, (short) 8));
        assertThat(Streams.streamOf()
                          .sync()
                          .then(range((byte) 0, (byte) 10, (byte) 2))
                          .all()).isEqualTo(
                Arrays.asList((byte) 0, (byte) 2, (byte) 4, (byte) 6, (byte) 8, (byte) 10));
        assertThat(Streams.streamOf().sync().then(range(0, -5)).all()).isEqualTo(
                Arrays.asList(0, -1, -2, -3, -4, -5));
        assertThat(Streams.streamOf().sync().then(range(0, 2.1)).all()).isEqualTo(
                Arrays.asList(0d, 1d, 2d));
        assertThat(Streams.streamOf().sync().then(range(0, 1.9f)).all()).isEqualTo(
                Arrays.asList(0f, 1f));
        assertThat(Streams.streamOf().sync().then(range(0L, -4)).all()).isEqualTo(
                Arrays.asList(0L, -1L, -2L, -3L, -4L));
        assertThat(Streams.streamOf().sync().then(range(0, (short) 4)).all()).isEqualTo(
                Arrays.asList(0, 1, 2, 3, 4));
        assertThat(Streams.streamOf().sync().then(range((byte) 0, (short) 4)).all()).isEqualTo(
                Arrays.asList((short) 0, (short) 1, (short) 2, (short) 3, (short) 4));
        assertThat(Streams.streamOf().sync().then(range((byte) 0, (byte) 5)).all()).isEqualTo(
                Arrays.asList((byte) 0, (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5));
    }

    @Test
    public void testRangeEquals() {

        final RangeConsumer<? extends Number> range1 = Streams.range(1, 10);
        assertThat(range1).isEqualTo(range1);
        assertThat(range1).isNotEqualTo(null);
        assertThat(range1).isNotEqualTo("test");
        assertThat(range1).isNotEqualTo(Streams.range(1, 10, 3));
        assertThat(range1).isEqualTo(Streams.range(1, 10));
        assertThat(range1.hashCode()).isEqualTo(Streams.range(1, 10).hashCode());

        final RangeConsumer<? extends Number> range2 = Streams.range(1, 10, -2);
        assertThat(range2).isEqualTo(range2);
        assertThat(range2).isNotEqualTo(null);
        assertThat(range2).isNotEqualTo("test");
        assertThat(range2).isNotEqualTo(Streams.range(1, 10, 1));
        assertThat(range2).isEqualTo(Streams.range(1, 10, -2));
        assertThat(range2.hashCode()).isEqualTo(Streams.range(1, 10, -2).hashCode());

        final Function<Character, Character> function = new Function<Character, Character>() {

            public Character apply(final Character character) {

                return (char) (character + 1);
            }
        };
        final RangeConsumer<Character> range3 = Streams.range('a', 'f', function);
        assertThat(range3).isEqualTo(range3);
        assertThat(range3).isNotEqualTo(null);
        assertThat(range3).isNotEqualTo("test");
        assertThat(range3).isNotEqualTo(Streams.range('b', 'f', function));
        assertThat(range3).isEqualTo(Streams.range('a', 'f', function));
        assertThat(range3.hashCode()).isEqualTo(Streams.range('a', 'f', function).hashCode());
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
    public void testRepeat() {

        final IOChannel<Object> ioChannel = JRoutine.io().buildChannel();
        final OutputChannel<Object> channel = Streams.repeat(ioChannel).build();
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
        final OutputChannel<Object> channel = Streams.repeat(ioChannel).build();
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
    public void testSelectMap() {

        final Routine<Selectable<Object>, Selectable<Object>> routine =
                JRoutine.on(new Sort()).buildRoutine();
        final IOChannel<Selectable<Object>> inputChannel = JRoutine.io().buildChannel();
        final OutputChannel<Selectable<Object>> outputChannel = routine.asyncCall(inputChannel);
        final StreamChannel<Object> intChannel =
                Streams.select(outputChannel, Sort.INTEGER, Sort.STRING)
                       .withChannels()
                       .withLogLevel(Level.WARNING)
                       .getConfigured()
                       .build()
                       .get(Sort.INTEGER);
        final StreamChannel<Object> strChannel =
                Streams.select(outputChannel, Arrays.asList(Sort.STRING, Sort.INTEGER))
                       .withChannels()
                       .withLogLevel(Level.WARNING)
                       .getConfigured()
                       .build()
                       .get(Sort.STRING);
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
    public void testSelectMapAbort() {

        final Routine<Selectable<Object>, Selectable<Object>> routine =
                JRoutine.on(new Sort()).buildRoutine();
        IOChannel<Selectable<Object>> inputChannel = JRoutine.io().buildChannel();
        OutputChannel<Selectable<Object>> outputChannel = routine.asyncCall(inputChannel);
        Streams.select(Sort.STRING, 2, outputChannel).build();
        inputChannel.after(millis(100))
                    .pass(new Selectable<Object>("test21", Sort.STRING),
                          new Selectable<Object>(-11, Sort.INTEGER))
                    .abort();

        try {

            Streams.select(outputChannel, Sort.STRING, Sort.INTEGER)
                   .build()
                   .get(Sort.STRING)
                   .afterMax(seconds(1))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            Streams.select(outputChannel, Sort.INTEGER, Sort.STRING)
                   .build()
                   .get(Sort.INTEGER)
                   .afterMax(seconds(1))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }

        inputChannel = JRoutine.io().buildChannel();
        outputChannel = routine.asyncCall(inputChannel);
        Streams.select(outputChannel, Sort.INTEGER, Sort.STRING).build();
        inputChannel.after(millis(100))
                    .pass(new Selectable<Object>(-11, Sort.INTEGER),
                          new Selectable<Object>("test21", Sort.STRING))
                    .abort();

        try {

            Streams.select(outputChannel, Sort.STRING, Sort.INTEGER)
                   .build()
                   .get(Sort.STRING)
                   .afterMax(seconds(1))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            Streams.select(outputChannel, Sort.STRING, Sort.INTEGER)
                   .build()
                   .get(Sort.INTEGER)
                   .afterMax(seconds(1))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }

        inputChannel = JRoutine.io().buildChannel();
        outputChannel = routine.asyncCall(inputChannel);
        Streams.select(outputChannel, Arrays.asList(Sort.STRING, Sort.INTEGER)).build();
        inputChannel.after(millis(100))
                    .pass(new Selectable<Object>("test21", Sort.STRING),
                          new Selectable<Object>(-11, Sort.INTEGER))
                    .abort();

        try {

            Streams.select(outputChannel, Sort.INTEGER, Sort.STRING)
                   .build()
                   .get(Sort.STRING)
                   .afterMax(seconds(1))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            Streams.select(outputChannel, Sort.INTEGER, Sort.STRING)
                   .build()
                   .get(Sort.INTEGER)
                   .afterMax(seconds(1))
                   .all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @Test
    public void testSkip() {

        assertThat(Streams.streamOf()
                          .sync()
                          .then(range(1, 10))
                          .async()
                          .map(Streams.skip(5))
                          .afterMax(seconds(3))
                          .all()).containsExactly(6, 7, 8, 9, 10);
        assertThat(Streams.streamOf()
                          .sync()
                          .then(range(1, 10))
                          .async()
                          .map(Streams.skip(15))
                          .afterMax(seconds(3))
                          .all()).isEmpty();
        assertThat(Streams.streamOf()
                          .sync()
                          .then(range(1, 10))
                          .async()
                          .map(Streams.skip(0))
                          .afterMax(seconds(3))
                          .all()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void testSkipEquals() {

        final InvocationFactory<Object, Object> factory = Streams.skip(2);
        assertThat(factory).isEqualTo(factory);
        assertThat(factory).isNotEqualTo(null);
        assertThat(factory).isNotEqualTo("test");
        assertThat(factory).isNotEqualTo(Streams.skip(3));
        assertThat(factory).isEqualTo(Streams.skip(2));
        assertThat(factory.hashCode()).isEqualTo(Streams.skip(2).hashCode());
    }

    @Test
    public void testSkipError() {

        try {

            Streams.skip(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testUnfold() {

        assertThat(Streams.streamOf()
                          .sync()
                          .then(range(1, 10))
                          .async()
                          .map(Streams.<Number>groupBy(3))
                          .parallel()
                          .map(Streams.<Number>unfold())
                          .afterMax(seconds(3))
                          .all()).containsOnly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void testUnfoldEquals() {

        final BiConsumerWrapper<Iterable<Object>, InputChannel<Object>> consumer =
                wrap(Streams.unfold());
        assertThat(consumer).isEqualTo(consumer);
        assertThat(consumer).isNotEqualTo(null);
        assertThat(consumer).isNotEqualTo("test");
        assertThat(consumer).isNotEqualTo(Streams.groupBy(3));
        assertThat(consumer).isEqualTo(wrap(Streams.unfold()));
        assertThat(consumer.hashCode()).isEqualTo(wrap(Streams.unfold()).hashCode());
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
                            .build()
                            .pass(selectable.<Integer>data())
                            .close();
                    break;

                case STRING:
                    Channels.<Object, String>select(result, STRING)
                            .build()
                            .pass(selectable.<String>data())
                            .close();
                    break;
            }
        }
    }
}
