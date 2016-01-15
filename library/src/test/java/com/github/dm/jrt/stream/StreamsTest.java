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
package com.github.dm.jrt.stream;

import com.github.dm.jrt.builder.IOChannelBuilder;
import com.github.dm.jrt.builder.InvocationConfiguration.OrderType;
import com.github.dm.jrt.channel.AbortException;
import com.github.dm.jrt.channel.Channel.OutputChannel;
import com.github.dm.jrt.channel.IOChannel;
import com.github.dm.jrt.channel.InvocationChannel;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.core.Channels;
import com.github.dm.jrt.core.Channels.Selectable;
import com.github.dm.jrt.core.JRoutine;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Functions;
import com.github.dm.jrt.invocation.FilterInvocation;
import com.github.dm.jrt.invocation.InvocationException;
import com.github.dm.jrt.invocation.InvocationFactory;
import com.github.dm.jrt.invocation.PassingInvocation;
import com.github.dm.jrt.invocation.TemplateInvocation;
import com.github.dm.jrt.routine.Routine;
import com.github.dm.jrt.util.ClassToken;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

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
 * Streams unit tests.
 * <p/>
 * Created by davide-maestroni on 12/24/2015.
 */
public class StreamsTest {

//    @Test
//    public void muori() {
//
//        final Runner runner1 = Runners.poolRunner(1);
//        final Runner runner2 = Runners.poolRunner(1);
//
//        for (int i = 0; i < 1; i++) {
//
//            final IOChannel<Object> ioChannel = JRoutine.io()
//                                                        .withChannels()
//                                                        .withRunner(Runners.sharedRunner())
//                                                        .set()
//                                                        .buildChannel();
//            final AtomicInteger count = new AtomicInteger();
//            Streams.streamOf(ioChannel)
//                   .withInvocations()
//                   .withRunner(runner1)
//                   .withInputLimit(1)
//                   .withInputMaxDelay(seconds(3))
//                   .set()
//                   .asyncMap(new Function<Object, Object>() {
//
//                       public Object apply(final Object o) {
//
//                           return o;
//                       }
//                   })
//                   .withInvocations()
//                   .withRunner(runner2)
//                   .withInputLimit(1)
//                   .withInputMaxDelay(seconds(3))
//                   .set()
//                   .asyncMap(new BiConsumer<Object, ResultChannel<? extends Object>>() {
//
//                       public void accept(final Object o,
//                               final ResultChannel<? extends Object> result) {
//
//                           if (count.incrementAndGet() > 10) {
//
//                               result.abort();
//
//                           } else {
//
//                               result.pass(o);
//                           }
//                       }
//                   })
//                   .passTo(ioChannel)
//                   .pass("test0", "test1")
//                   .pass("test2", "test3")
//                   .pass("test4", "test5")
//                   .pass("test6", "test7")
//                   .pass("test8", "test9")
//                   .pass("end")
//                   .afterMax(days(3))
//                   .checkComplete();
//        }
//    }
//
//    @Test
//    public void muori2() {
//
//        final Runner runner1 = Runners.poolRunner(1);
//        final Runner runner2 = Runners.poolRunner(1);
//
//        try {
//
//            Streams.streamOf("test")
//                   .withInvocations()
//                   .withRunner(runner1)
//                   .set()
//                   .asyncMap(new Function<String, Object>() {
//
//                       public Object apply(final String s) {
//
//                           return Streams.streamOf(s)
//                                         .runOn(runner1)
//                                         .runOn(runner2)
//                                         .afterMax(days(3))
//                                         .next();
//                       }
//                   })
//                   .afterMax(days(3))
//                   .next();
//
//            fail();
//
//        } catch (final ExecutionDeadlockException ignored) {
//
//        }
//    }
//
//    @Ignore
//    @Test
//    public void muori3() throws InterruptedException {
//
//        final ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
//        final Runner runner1 = Runners.scheduledRunner(service);
//        final Runner runner2 = Runners.poolRunner(1);
//        final Semaphore semaphore = new Semaphore(0);
//
//        service.execute(new Runnable() {
//
//            public void run() {
//
//                try {
//
//                    Streams.streamOf("test").runOn(runner1).runOn(runner2).afterMax(days(3)).next();
//
//                } catch (final Throwable ignored) {
//
//                }
//
//                semaphore.release();
//            }
//        });
//
//        semaphore.acquire();
//    }
//
//    @Test
//    public void muori4() throws InterruptedException {
//
//        final Runner runner1 = Runners.poolRunner(1);
//        final Runner runner2 = Runners.poolRunner(1);
//
//        for (int i = 0; i < 1; i++) {
//
//            final IOChannel<Object> ioChannel = JRoutine.io().buildChannel();
//            final IOChannel<Object> ioChannel1 = JRoutine.io()
//                                                         .withChannels()
//                                                         .withRunner(Runners.sharedRunner())
//                                                         .set()
//                                                         .buildChannel();
//            Streams.streamOf(ioChannel)
//                   .withInvocations()
//                   .withRunner(runner1)
//                   .withInputLimit(1)
//                   .withInputMaxDelay(seconds(3))
//                   .set()
//                   .asyncMap(new Function<Object, Object>() {
//
//                       public Object apply(final Object o) {
//
//                           return o;
//                       }
//                   })
//                   .withInvocations()
//                   .withRunner(runner2)
//                   .withInputLimit(1)
//                   .withInputMaxDelay(seconds(3))
//                   .set()
//                   .asyncMap(new Function<Object, Object>() {
//
//                       public Object apply(final Object o) {
//
//                           return o;
//                       }
//                   })
//                   .withInvocations()
//                   .withRunner(runner1)
//                   .withInputLimit(1)
//                   .withInputMaxDelay(seconds(3))
//                   .set()
//                   .asyncMap(new Function<Object, Object>() {
//
//                       public Object apply(final Object o) {
//
//                           return o;
//                       }
//                   })
//                   .passTo(ioChannel1);
//            ioChannel.pass("test", "test")
//                     .pass("test", "test")
//                     .pass("test", "test")
//                     .pass("test", "test")
//                     .pass("test", "test")
//                     .pass("end")
//                     .close();
//            ioChannel1.close().afterMax(days(3)).all();
//        }
//    }
//
//    @Test
//    public void muori5() throws InterruptedException {
//
//        final Runner runner1 = Runners.poolRunner(1);
//        final Runner runner2 = Runners.poolRunner(1);
//
//        final IOChannel<Object> channel1 = JRoutine.io()
//                                                   .withChannels()
//                                                   .withRunner(runner1)
//                                                   .withChannelLimit(1)
//                                                   .withChannelMaxDelay(seconds(3))
//                                                   .set()
//                                                   .buildChannel();
//        final IOChannel<Object> channel2 = JRoutine.io()
//                                                   .withChannels()
//                                                   .withRunner(runner2)
//                                                   .withChannelLimit(1)
//                                                   .withChannelMaxDelay(seconds(3))
//                                                   .set()
//                                                   .buildChannel();
//        final IOChannel<Object> channel3 = JRoutine.io()
//                                                   .withChannels()
//                                                   .withRunner(runner1)
//                                                   .withChannelLimit(1)
//                                                   .withChannelMaxDelay(seconds(3))
//                                                   .set()
//                                                   .buildChannel();
//
//        channel1.passTo(channel2).passTo(channel3);
//        channel1.pass("test", "test")
//                .pass("test", "test")
//                .pass("test", "test")
//                .pass("test", "test")
//                .pass("test", "test")
//                .pass("end")
//                .close();
//        channel2.close();
//        channel3.close().afterMax(days(3)).all();
//    }

    @Test
    public void testBlend() {

        StreamChannel<String> channel1 = Streams.streamOf("test1", "test2", "test3");
        StreamChannel<String> channel2 = Streams.streamOf("test4", "test5", "test6");
        assertThat(Streams.blend(channel2, channel1).afterMax(seconds(1)).all()).containsOnly(
                "test1", "test2", "test3", "test4", "test5", "test6");
        channel1 = Streams.streamOf("test1", "test2", "test3");
        channel2 = Streams.streamOf("test4", "test5", "test6");
        assertThat(Streams.blend(Arrays.<StreamChannel<?>>asList(channel1, channel2))
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

            routine.asyncCall(Streams.blend(channel1, channel2)).afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().abort();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();

        try {

            routine.asyncCall(Streams.blend(Arrays.<OutputChannel<?>>asList(channel1, channel2)))
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
        assertThat(Streams.concat(channel2, channel1).afterMax(seconds(1)).all()).containsExactly(
                "test4", "test5", "test6", "test1", "test2", "test3");
        channel1 = Streams.streamOf("test1", "test2", "test3");
        channel2 = Streams.streamOf("test4", "test5", "test6");
        assertThat(Streams.concat(Arrays.<StreamChannel<?>>asList(channel1, channel2))
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

            routine.asyncCall(Streams.concat(channel1, channel2)).afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().abort();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();

        try {

            routine.asyncCall(Streams.concat(Arrays.<OutputChannel<?>>asList(channel1, channel2)))
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

                        return channel.syncMap(new Function<String, String>() {

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
            channel.after(millis(100)).abort(new IllegalArgumentException());
            channel.result().afterMax(seconds(1)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        assertThat(
                Streams.on(new Function<StreamChannel<? extends String>, StreamChannel<String>>() {

                               public StreamChannel<String> apply(
                                       final StreamChannel<? extends String> channel) {

                                   return channel.syncMap(new Function<String, String>() {

                                       public String apply(final String s) {

                                           return s.toUpperCase();
                                       }
                                   });
                               }
                           })
                       .asyncCall("test1", "test2", "test3")
                       .afterMax(seconds(3))
                       .all()).containsExactly("TEST1", "TEST2", "TEST3");

        try {

            final InvocationChannel<String, String> channel = Streams.on(
                    new Function<StreamChannel<? extends String>, StreamChannel<String>>() {

                        public StreamChannel<String> apply(
                                final StreamChannel<? extends String> channel) {

                            return channel.syncMap(new Function<String, String>() {

                                public String apply(final String s) {

                                    return s.toUpperCase();
                                }
                            });
                        }
                    }).asyncInvoke();
            channel.after(millis(100)).abort(new IllegalArgumentException());
            channel.result().afterMax(seconds(1)).next();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @Test
    public void testFactoryEquals() {

        final Function<StreamChannel<? extends String>, StreamChannel<String>>
                function =
                new Function<StreamChannel<? extends String>, StreamChannel<String>>() {

                    public StreamChannel<String> apply(
                            final StreamChannel<? extends String> channel) {

                        return channel.syncMap(new Function<String, String>() {

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
        assertThat(factory).isNotEqualTo(
                Streams.factory(Functions.<StreamChannel<?>>identity()));
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

            Streams.on(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGroupBy() {

        assertThat(Streams.streamOf()
                          .syncRange(1, 10)
                          .asyncMap(Streams.<Number>groupBy(3))
                          .afterMax(seconds(3))
                          .all()).containsExactly(Arrays.<Number>asList(1, 2, 3),
                                                  Arrays.<Number>asList(4, 5, 6),
                                                  Arrays.<Number>asList(7, 8, 9),
                                                  Collections.<Number>singletonList(10));
        assertThat(Streams.streamOf()
                          .syncRange(1, 10)
                          .asyncMap(Streams.<Number>groupBy(13))
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
    public void testJoin() {

        final IOChannelBuilder builder = JRoutine.io();
        final Routine<List<?>, Character> routine = JRoutine.on(new CharAt()).buildRoutine();
        IOChannel<String> channel1;
        IOChannel<Integer> channel2;
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.asyncCall(Streams.join(channel1, channel2))
                          .afterMax(seconds(10))
                          .all()).containsExactly('s', '2');
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(
                routine.asyncCall(Streams.join(Arrays.<OutputChannel<?>>asList(channel1, channel2)))
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
        assertThat(routine.asyncCall(Streams.join(channel1, channel2))
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

            routine.asyncCall(Streams.join(channel1, channel2)).afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().abort();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();

        try {

            routine.asyncCall(Streams.join(Arrays.<OutputChannel<?>>asList(channel1, channel2)))
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
        assertThat(routine.asyncCall(Streams.joinAndFlush(new Object(), channel1, channel2))
                          .afterMax(seconds(10))
                          .all()).containsExactly('s', '2');
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        channel1.orderByCall().after(millis(100)).pass("testtest").pass("test2").close();
        channel2.orderByCall().after(millis(110)).pass(6).pass(4).close();
        assertThat(routine.asyncCall(
                Streams.joinAndFlush(null, Arrays.<OutputChannel<?>>asList(channel1, channel2)))
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

            routine.asyncCall(Streams.joinAndFlush(new Object(), channel1, channel2))
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

            routine.asyncCall(Streams.joinAndFlush(null, channel1, channel2))
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

            routine.asyncCall(Streams.joinAndFlush(new Object(),
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

            Streams.joinAndFlush(new Object());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.joinAndFlush(null, Collections.<OutputChannel<?>>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            Streams.joinAndFlush(new Object(), new OutputChannel[]{null});

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Streams.joinAndFlush(new Object(), Collections.<OutputChannel<?>>singletonList(null));

            fail();

        } catch (final NullPointerException ignored) {

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
    public void testLimit() {

        assertThat(Streams.streamOf()
                          .syncRange(1, 10)
                          .asyncMap(Streams.limit(5))
                          .afterMax(seconds(3))
                          .all()).containsExactly(1, 2, 3, 4, 5);
        assertThat(Streams.streamOf()
                          .syncRange(1, 10)
                          .asyncMap(Streams.limit(0))
                          .afterMax(seconds(3))
                          .all()).isEmpty();
        assertThat(Streams.streamOf()
                          .syncRange(1, 10)
                          .asyncMap(Streams.limit(15))
                          .afterMax(seconds(3))
                          .all()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(Streams.streamOf()
                          .syncRange(1, 10)
                          .asyncMap(Streams.limit(0))
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
                JRoutine.io().withChannels().withChannelOrder(OrderType.BY_CALL).set();
        final IOChannel<String> channel1 = builder.buildChannel();
        final IOChannel<Integer> channel2 = builder.buildChannel();

        final OutputChannel<? extends Selectable<Object>> channel =
                Streams.merge(Arrays.<OutputChannel<?>>asList(channel1, channel2));
        final OutputChannel<Selectable<Object>> output = JRoutine.on(new Sort())
                                                                 .withInvocations()
                                                                 .withInputOrder(OrderType.BY_CALL)
                                                                 .set()
                                                                 .asyncCall(channel);
        final Map<Integer, OutputChannel<Object>> channelMap =
                Streams.select(output, Sort.INTEGER, Sort.STRING);

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
        outputChannel = Streams.<Object>merge(-7, channel1, channel2);
        channel1.pass("test1").close();
        channel2.pass(13).close();
        assertThat(outputChannel.afterMax(seconds(1)).all()).containsOnly(
                new Selectable<String>("test1", -7), new Selectable<Integer>(13, -6));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel =
                Streams.<Object>merge(11, Arrays.<OutputChannel<?>>asList(channel1, channel2));
        channel2.pass(13).close();
        channel1.pass("test1").close();
        assertThat(outputChannel.afterMax(seconds(1)).all()).containsOnly(
                new Selectable<String>("test1", 11), new Selectable<Integer>(13, 12));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = Streams.<Object>merge(channel1, channel2);
        channel1.pass("test2").close();
        channel2.pass(-17).close();
        assertThat(outputChannel.afterMax(seconds(1)).all()).containsOnly(
                new Selectable<String>("test2", 0), new Selectable<Integer>(-17, 1));
        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = Streams.<Object>merge(Arrays.<OutputChannel<?>>asList(channel1, channel2));
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
        outputChannel = Streams.<Object>merge(channelMap);
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
                Streams.merge(Arrays.asList(channel1, channel2, channel3, channel4)));

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
        outputChannel = Streams.<Object>merge(-7, channel1, channel2);
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
                Streams.<Object>merge(11, Arrays.<OutputChannel<?>>asList(channel1, channel2));
        channel2.abort();
        channel1.pass("test1").close();

        try {

            outputChannel.afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = Streams.<Object>merge(channel1, channel2);
        channel1.abort();
        channel2.pass(-17).close();

        try {

            outputChannel.afterMax(seconds(1)).all();

            fail();

        } catch (final AbortException ignored) {

        }

        channel1 = builder.buildChannel();
        channel2 = builder.buildChannel();
        outputChannel = Streams.<Object>merge(Arrays.<OutputChannel<?>>asList(channel1, channel2));
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
        outputChannel = Streams.<Object>merge(channelMap);
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
    public void testSkip() {

        assertThat(Streams.streamOf()
                          .syncRange(1, 10)
                          .asyncMap(Streams.skip(5))
                          .afterMax(seconds(3))
                          .all()).containsExactly(6, 7, 8, 9, 10);
        assertThat(Streams.streamOf()
                          .syncRange(1, 10)
                          .asyncMap(Streams.skip(15))
                          .afterMax(seconds(3))
                          .all()).isEmpty();
        assertThat(Streams.streamOf()
                          .syncRange(1, 10)
                          .asyncMap(Streams.skip(0))
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
