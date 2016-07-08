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

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.RoutineBuilder;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ExecutionDeadlockException;
import com.github.dm.jrt.core.channel.InputDeadlockException;
import com.github.dm.jrt.core.channel.OutputDeadlockException;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.error.TimeoutException;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.MappingInvocation;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.runner.Runners;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Functions;
import com.github.dm.jrt.operator.Operators;
import com.github.dm.jrt.stream.builder.StreamBuilder;
import com.github.dm.jrt.stream.builder.StreamBuilder.StreamConfiguration;
import com.github.dm.jrt.stream.builder.StreamBuildingException;
import com.github.dm.jrt.stream.processor.Processors;

import org.assertj.core.data.Offset;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.core.invocation.InvocationFactory.factoryOf;
import static com.github.dm.jrt.core.util.UnitDuration.minutes;
import static com.github.dm.jrt.core.util.UnitDuration.seconds;
import static com.github.dm.jrt.function.Functions.functionMapping;
import static com.github.dm.jrt.function.Functions.onOutput;
import static com.github.dm.jrt.operator.Operators.append;
import static com.github.dm.jrt.operator.Operators.filter;
import static com.github.dm.jrt.operator.Operators.instead;
import static com.github.dm.jrt.operator.Operators.insteadAccept;
import static com.github.dm.jrt.operator.Operators.reduce;
import static com.github.dm.jrt.operator.producer.Producers.range;
import static com.github.dm.jrt.stream.processor.Processors.tryCatchAccept;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Stream routine builder unit tests.
 * <p>
 * Created by davide-maestroni on 07/01/2016.
 */
public class StreamBuilderTest {

    private static Runner sSingleThreadRunner;

    @NotNull
    private static Runner getSingleThreadRunner() {
        if (sSingleThreadRunner == null) {
            sSingleThreadRunner = Runners.poolRunner(1);
        }

        return sSingleThreadRunner;
    }

    @Test
    @SuppressWarnings({"ConstantConditions", "ThrowableResultOfMethodCallIgnored"})
    public void testAbort() {
        final Channel<Object, Object> channel = JRoutineCore.io().buildChannel();
        final Channel<Object, Object> streamChannel =
                JRoutineStream.withStream().asyncCall(channel);
        channel.abort(new IllegalArgumentException());
        try {
            streamChannel.after(seconds(3)).throwError();

        } catch (final AbortException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
        }

        assertThat(streamChannel.getError().getCause()).isExactlyInstanceOf(
                IllegalArgumentException.class);
    }

    @Test
    public void testChannel() {
        try {
            JRoutineStream.withStream().sync().all();
            fail();

        } catch (final TimeoutException ignored) {
        }

        try {
            JRoutineStream.withStream().sync().next();
            fail();

        } catch (final TimeoutException ignored) {
        }

        try {
            JRoutineStream.withStream().sync().next(2);
            fail();

        } catch (final TimeoutException ignored) {
        }

        try {
            JRoutineStream.withStream().sync().nextOrElse(2);
            fail();

        } catch (final TimeoutException ignored) {
        }

        try {
            JRoutineStream.withStream().sync().skipNext(2);
            fail();

        } catch (final TimeoutException ignored) {
        }

        assertThat(JRoutineStream.withStream().sync().abort()).isTrue();
        assertThat(JRoutineStream.withStream().sync().abort(new IllegalStateException())).isTrue();
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .map(instead("test"))
                                 .after(seconds(3))
                                 .immediately()
                                 .close()
                                 .next()).isEqualTo("test");
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .map(instead("test"))
                                 .after(3, TimeUnit.SECONDS)
                                 .immediately()
                                 .close()
                                 .next()).isEqualTo("test");
        try {
            final ArrayList<String> results = new ArrayList<String>();
            JRoutineStream.withStream().sync().map(instead("test")).allInto(results).hasCompleted();
            fail();

        } catch (final TimeoutException ignored) {
        }

        try {
            JRoutineStream.withStream()
                          .sync()
                          .map(instead("test"))
                          .bind(JRoutineCore.io().buildChannel())
                          .next();
            fail();

        } catch (final TimeoutException ignored) {
        }

        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .map(instead("test"))
                                 .bind(onOutput(new Consumer<String>() {

                                     public void accept(final String s) {
                                         assertThat(s).isEqualTo("test");
                                     }
                                 }))
                                 .close()
                                 .getError()).isNull();
        assertThat(
                JRoutineStream.withStream().sync().map(instead("test")).close().next()).isEqualTo(
                "test");
        try {
            JRoutineStream.withStream().sync().map(instead("test")).eventualIterator().next();
            fail();

        } catch (final TimeoutException ignored) {
        }

        try {
            JRoutineStream.withStream().sync().map(instead("test")).iterator().next();
            fail();

        } catch (final TimeoutException ignored) {
        }

        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .map(instead("test"))
                                 .eventuallyAbort()
                                 .close()
                                 .next()).isEqualTo("test");
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .map(instead("test"))
                                 .eventuallyAbort(new IllegalStateException())
                                 .close()
                                 .next()).isEqualTo("test");
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .map(instead("test"))
                                 .eventuallyBreak()
                                 .close()
                                 .next()).isEqualTo("test");
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .map(instead("test"))
                                 .eventuallyFail()
                                 .close()
                                 .next()).isEqualTo("test");
        assertThat(JRoutineStream.withStream().sync().getError()).isNull();
        assertThat(JRoutineStream.withStream().sync().hasCompleted()).isFalse();
        try {
            JRoutineStream.withStream().sync().hasNext();
            fail();

        } catch (final TimeoutException ignored) {
        }

        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .map(instead("test"))
                                 .immediately()
                                 .close()
                                 .next()).isEqualTo("test");
        assertThat(JRoutineStream.withStream().sync().inputCount()).isZero();
        assertThat(JRoutineStream.withStream().sync().outputCount()).isZero();
        assertThat(JRoutineStream.withStream().sync().size()).isZero();
        assertThat(JRoutineStream.withStream().sync().isBound()).isFalse();
        assertThat(JRoutineStream.withStream().sync().isEmpty()).isTrue();
        assertThat(JRoutineStream.withStream().sync().isOpen()).isTrue();
        assertThat(JRoutineStream.withStream().sync().pass("test").next()).isEqualTo("test");
        assertThat(JRoutineStream.withStream().sync().pass("test", "test").next()).isEqualTo(
                "test");
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .pass(Arrays.asList("test", "test"))
                                 .next()).isEqualTo("test");
        assertThat(JRoutineStream.withStream()
                                 .sync()
                                 .pass(JRoutineCore.io().of("test"))
                                 .next()).isEqualTo("test");
        assertThat(JRoutineStream.withStream().sync().sortedByCall().pass("test").next()).isEqualTo(
                "test");
        assertThat(
                JRoutineStream.withStream().sync().sortedByDelay().pass("test").next()).isEqualTo(
                "test");
        JRoutineStream.withStream().sync().throwError();
        try {
            JRoutineStream.withStream().sync().remove();
            fail();

        } catch (final UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void testConstructor() {
        boolean failed = false;
        try {
            new JRoutineStream();
            failed = true;

        } catch (final Throwable ignored) {
        }

        assertThat(failed).isFalse();
    }

    @Test
    public void testFlatMap() {
        assertThat(JRoutineStream //
                .<String>withStream().sync().flatMap(new Function<String, Channel<?, String>>() {

                    public Channel<?, String> apply(final String s) {
                        return JRoutineStream.<String>withStream().sync()
                                                                  .map(filter(
                                                                          Functions
                                                                                  .<String>isNotNull()))
                                                                  .syncCall(s);
                    }
                }).syncCall("test1", null, "test2", null).all()).containsExactly("test1", "test2");
        assertThat(JRoutineStream //
                .<String>withStream().async().flatMap(new Function<String, Channel<?, String>>() {

                    public Channel<?, String> apply(final String s) {
                        return JRoutineStream.<String>withStream().sync()
                                                                  .map(filter(
                                                                          Functions
                                                                                  .<String>isNotNull()))
                                                                  .syncCall(s);
                    }
                }).syncCall("test1", null, "test2", null).after(seconds(3)).all()).containsExactly(
                "test1", "test2");
        assertThat(JRoutineStream //
                .<String>withStream().parallel()
                                     .flatMap(new Function<String, Channel<?, String>>() {

                                         public Channel<?, String> apply(final String s) {
                                             return JRoutineStream.<String>withStream().sync()
                                                                                       .map(filter(
                                                                                               Functions.<String>isNotNull()))
                                                                                       .syncCall(s);
                                         }
                                     })
                                     .syncCall("test1", null, "test2", null)
                                     .after(seconds(3))
                                     .all()).containsOnly("test1", "test2");
        assertThat(JRoutineStream //
                .<String>withStream().sequential()
                                     .flatMap(new Function<String, Channel<?, String>>() {

                                         public Channel<?, String> apply(final String s) {
                                             return JRoutineStream.<String>withStream().sync()
                                                                                       .map(filter(
                                                                                               Functions.<String>isNotNull()))
                                                                                       .syncCall(s);
                                         }
                                     })
                                     .syncCall("test1", null, "test2", null)
                                     .after(seconds(3))
                                     .all()).containsOnly("test1", "test2");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testFlatMapNullPointerError() {
        try {
            JRoutineStream.withStream().sync().flatMap(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().async().flatMap(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().parallel().flatMap(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().sequential().flatMap(null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @Test
    public void testFlatMapRetry() {
        final Routine<Object, String> routine =
                JRoutineCore.with(functionMapping(new Function<Object, String>() {

                    public String apply(final Object o) {
                        return o.toString();
                    }
                })).buildRoutine();
        final Function<Object, Channel<Object, String>> retryFunction =
                new Function<Object, Channel<Object, String>>() {

                    public Channel<Object, String> apply(final Object o) {
                        final int[] count = {0};
                        return JRoutineStream.withStream()
                                             .map(routine)
                                             .let(tryCatchAccept(
                                                     new BiConsumer<RoutineException,
                                                             Channel<String, ?>>() {

                                                         public void accept(
                                                                 final RoutineException e,
                                                                 final Channel<String, ?> channel) {
                                                             if (++count[0] < 3) {
                                                                 JRoutineStream.withStream()
                                                                               .map(routine)
                                                                               .let(tryCatchAccept(
                                                                                       this))
                                                                               .asyncCall(o)
                                                                               .bind(channel);

                                                             } else {
                                                                 throw e;
                                                             }
                                                         }
                                                     }))
                                             .asyncCall(o);

                    }
                };

        try {
            JRoutineStream.withStream()
                          .async()
                          .flatMap(retryFunction)
                          .asyncCall((Object) null)
                          .after(seconds(3))
                          .all();
            fail();

        } catch (final RoutineException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(NullPointerException.class);
        }
    }

    @Test
    public void testFlatTransform() {
        assertThat(JRoutineStream.<String>withStream().let(
                new Function<StreamBuilder<String, String>, StreamBuilder<String, String>>() {

                    public StreamBuilder<String, String> apply(
                            final StreamBuilder<String, String> builder) {
                        return builder.map(append("test2"));
                    }
                }).asyncCall("test1").after(seconds(3)).all()).containsExactly("test1", "test2");

        try {
            JRoutineStream.withStream()
                          .let(new Function<StreamBuilder<Object, Object>, StreamBuilder<Object,
                                  Object>>() {

                              public StreamBuilder<Object, Object> apply(
                                      final StreamBuilder<Object, Object> builder) {
                                  throw new NullPointerException();
                              }
                          });
            fail();

        } catch (final StreamBuildingException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(NullPointerException.class);
        }

        assertThat(JRoutineStream.<String>withStream().letWithConfig(
                new BiFunction<StreamConfiguration, StreamBuilder<String, String>,
                        StreamBuilder<String, String>>() {

                    public StreamBuilder<String, String> apply(
                            final StreamConfiguration streamConfiguration,
                            final StreamBuilder<String, String> builder) {
                        return builder.map(append("test2"));
                    }
                }).asyncCall("test1").after(seconds(3)).all()).containsExactly("test1", "test2");

        try {
            JRoutineStream.withStream()
                          .letWithConfig(
                                  new BiFunction<StreamConfiguration, StreamBuilder<Object,
                                          Object>, StreamBuilder<Object, Object>>() {

                                      public StreamBuilder<Object, Object> apply(
                                              final StreamConfiguration streamConfiguration,
                                              final StreamBuilder<Object, Object> builder) {
                                          throw new NullPointerException();
                                      }
                                  });
            fail();

        } catch (final StreamBuildingException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(NullPointerException.class);
        }
    }

    @Test
    public void testInvocationDeadlock() {
        try {
            final Runner runner1 = Runners.poolRunner(1);
            final Runner runner2 = Runners.poolRunner(1);
            final Function<String, Object> function = new Function<String, Object>() {

                public Object apply(final String s) {
                    return JRoutineStream.<String>withStream().invocationConfiguration()
                                                              .withRunner(runner1)
                                                              .applied()
                                                              .map(Functions.identity())
                                                              .invocationConfiguration()
                                                              .withRunner(runner2)
                                                              .applied()
                                                              .map(Functions.identity())
                                                              .asyncCall(s)
                                                              .after(minutes(3))
                                                              .next();
                }
            };
            JRoutineStream.<String>withStream().invocationConfiguration()
                                               .withRunner(runner1)
                                               .applied()
                                               .map(function)
                                               .asyncCall("test")
                                               .after(minutes(3))
                                               .next();
            fail();

        } catch (final ExecutionDeadlockException ignored) {
        }
    }

    @Test
    public void testInvocationMode() {
        assertThat(JRoutineStream.<String>withStream().invocationMode(InvocationMode.ASYNC)
                                                      .asyncCall("test1", "test2", "test3")
                                                      .after(seconds(1))
                                                      .all()).containsExactly("test1", "test2",
                "test3");
        assertThat(JRoutineStream.<String>withStream().invocationMode(InvocationMode.PARALLEL)
                                                      .asyncCall("test1", "test2", "test3")
                                                      .after(seconds(1))
                                                      .all()).containsExactly("test1", "test2",
                "test3");
        assertThat(JRoutineStream.<String>withStream().invocationMode(InvocationMode.SYNC)
                                                      .syncCall("test1", "test2", "test3")
                                                      .all()).containsExactly("test1", "test2",
                "test3");
        assertThat(JRoutineStream.<String>withStream().invocationMode(InvocationMode.SEQUENTIAL)
                                                      .syncCall("test1", "test2", "test3")
                                                      .all()).containsExactly("test1", "test2",
                "test3");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testInvocationModeNullPointerError() {
        try {
            JRoutineStream.withStream().invocationMode(null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @Test
    public void testLag() {
        long startTime = System.currentTimeMillis();
        assertThat(JRoutineStream.<String>withStream().let(
                Processors.<String, String>lag(1, TimeUnit.SECONDS))
                                                      .asyncCall("test")
                                                      .after(seconds(3))
                                                      .next()).isEqualTo("test");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
        startTime = System.currentTimeMillis();
        assertThat(
                JRoutineStream.<String>withStream().let(Processors.<String, String>lag(seconds(1)))
                                                   .asyncCall("test")
                                                   .after(seconds(3))
                                                   .next()).isEqualTo("test");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
        startTime = System.currentTimeMillis();
        assertThat(JRoutineStream.<String>withStream().let(
                Processors.<String, String>lag(1, TimeUnit.SECONDS))
                                                      .asyncCall()
                                                      .close()
                                                      .after(seconds(3))
                                                      .all()).isEmpty();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
        startTime = System.currentTimeMillis();
        assertThat(
                JRoutineStream.<String>withStream().let(Processors.<String, String>lag(seconds(1)))
                                                   .asyncCall()
                                                   .close()
                                                   .after(seconds(3))
                                                   .all()).isEmpty();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
    }

    @Test
    public void testMapAllConsumer() {
        assertThat(JRoutineStream.<String>withStream().async().mapAllWith(new BiConsumer<List<?
                extends String>, Channel<String, ?>>() {

            public void accept(final List<?
                    extends
                    String> strings, final Channel<String, ?> result) {
                final StringBuilder builder = new StringBuilder();
                for (final String string : strings) {
                    builder.append(string);
                }

                result.pass(builder.toString());
            }
        }).asyncCall("test1", "test2", "test3").after(seconds(3)).all()).containsExactly(
                "test1test2test3");
        assertThat(
                JRoutineStream.<String>withStream().sync().mapAllWith(new BiConsumer<List<? extends
                        String>, Channel<String, ?>>() {

                    public void accept(final List<? extends
                            String> strings, final Channel<String, ?> result) {
                        final StringBuilder builder = new StringBuilder();
                        for (final String string : strings) {
                            builder.append(string);
                        }

                        result.pass(builder.toString());
                    }
                }).syncCall("test1", "test2", "test3").all()).containsExactly("test1test2test3");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMapAllConsumerNullPointerError() {
        try {
            JRoutineStream.withStream().async().mapAllWith(null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @Test
    public void testMapAllFunction() {
        assertThat(JRoutineStream.<String>withStream().async().mapAll(new Function<List<? extends
                String>, String>() {

            public String apply(final List<? extends String> strings) {
                final StringBuilder builder = new StringBuilder();
                for (final String string : strings) {
                    builder.append(string);
                }

                return builder.toString();
            }
        }).asyncCall("test1", "test2", "test3").after(seconds(3)).all()).containsExactly(
                "test1test2test3");
        assertThat(JRoutineStream.<String>withStream().sync().mapAll(new Function<List<? extends
                String>, String>() {

            public String apply(final List<? extends String> strings) {
                final StringBuilder builder = new StringBuilder();
                for (final String string : strings) {
                    builder.append(string);
                }

                return builder.toString();
            }
        }).syncCall("test1", "test2", "test3").all()).containsExactly("test1test2test3");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMapAllFunctionNullPointerError() {
        try {
            JRoutineStream.withStream().async().mapAll(null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @Test
    public void testMapConsumer() {
        assertThat(JRoutineStream.<String>withStream().mapWith(
                new BiConsumer<String, Channel<String, ?>>() {

                    public void accept(final String s, final Channel<String, ?> result) {
                        result.pass(s.toUpperCase());
                    }
                }).asyncCall("test1", "test2").after(seconds(3)).all()).containsExactly("TEST1",
                "TEST2");
        assertThat(JRoutineStream //
                .<String>withStream().sorted()
                                     .parallel()
                                     .mapWith(new BiConsumer<String, Channel<String, ?>>() {

                                         public void accept(final String s,
                                                 final Channel<String, ?> result) {
                                             result.pass(s.toUpperCase());
                                         }
                                     })
                                     .asyncCall("test1", "test2")
                                     .after(seconds(3))
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStream//
                .<String>withStream().sync().mapWith(new BiConsumer<String, Channel<String, ?>>() {

                    public void accept(final String s, final Channel<String, ?> result) {
                        result.pass(s.toUpperCase());
                    }
                }).syncCall("test1", "test2").all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStream//
                .<String>withStream().sequential()
                                     .mapWith(new BiConsumer<String, Channel<String, ?>>() {

                                         public void accept(final String s,
                                                 final Channel<String, ?> result) {
                                             result.pass(s.toUpperCase());
                                         }
                                     })
                                     .syncCall("test1", "test2")
                                     .all()).containsExactly("TEST1", "TEST2");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMapConsumerNullPointerError() {
        try {
            JRoutineStream.withStream().async().mapWith(null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @Test
    public void testMapFactory() {
        final InvocationFactory<String, String> factory = factoryOf(UpperCase.class);
        assertThat(JRoutineStream.<String>withStream().async()
                                                      .map(factory)
                                                      .asyncCall("test1", "test2")
                                                      .after(seconds(3))
                                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().sorted()
                                                      .parallel()
                                                      .map(factory)
                                                      .asyncCall("test1", "test2")
                                                      .after(seconds(3))
                                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().sync()
                                                      .map(factory)
                                                      .syncCall("test1", "test2")
                                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().sequential()
                                                      .map(factory)
                                                      .syncCall("test1", "test2")
                                                      .all()).containsExactly("TEST1", "TEST2");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMapFactoryNullPointerError() {
        try {
            JRoutineStream.withStream().async().map((InvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().parallel().map((InvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().sync().map((InvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().sequential().map((InvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @Test
    public void testMapFilter() {
        assertThat(JRoutineStream.<String>withStream().async()
                                                      .map(new UpperCase())
                                                      .asyncCall("test1", "test2")
                                                      .after(seconds(3))
                                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().sorted()
                                                      .parallel()
                                                      .map(new UpperCase())
                                                      .asyncCall("test1", "test2")
                                                      .after(seconds(3))
                                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().sync()
                                                      .map(new UpperCase())
                                                      .syncCall("test1", "test2")
                                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().sequential()
                                                      .map(new UpperCase())
                                                      .syncCall("test1", "test2")
                                                      .all()).containsExactly("TEST1", "TEST2");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMapFilterNullPointerError() {
        try {
            JRoutineStream.withStream().async().map((MappingInvocation<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().parallel().map((MappingInvocation<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().sync().map((MappingInvocation<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().sequential().map((MappingInvocation<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @Test
    public void testMapFunction() {
        assertThat(JRoutineStream.<String>withStream().async().map(new Function<String, String>() {

            public String apply(final String s) {
                return s.toUpperCase();
            }
        }).asyncCall("test1", "test2").after(seconds(3)).all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().sorted()
                                                      .parallel()
                                                      .map(new Function<String, String>() {

                                                          public String apply(final String s) {
                                                              return s.toUpperCase();
                                                          }
                                                      })
                                                      .asyncCall("test1", "test2")
                                                      .after(seconds(3))
                                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().sync().map(new Function<String, String>() {

            public String apply(final String s) {
                return s.toUpperCase();
            }
        }).syncCall("test1", "test2").all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().sequential()
                                                      .map(new Function<String, String>() {

                                                          public String apply(final String s) {
                                                              return s.toUpperCase();
                                                          }
                                                      })
                                                      .syncCall("test1", "test2")
                                                      .all()).containsExactly("TEST1", "TEST2");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMapFunctionNullPointerError() {
        try {
            JRoutineStream.withStream().async().map((Function<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().parallel().map((Function<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().sync().map((Function<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().sequential().map((Function<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @Test
    public void testMapRoutine() {
        final Routine<String, String> routine = JRoutineCore.with(new UpperCase())
                                                            .invocationConfiguration()
                                                            .withOutputOrder(OrderType.BY_CALL)
                                                            .applied()
                                                            .buildRoutine();
        assertThat(JRoutineStream.<String>withStream().async()
                                                      .map(routine)
                                                      .asyncCall("test1", "test2")
                                                      .after(seconds(3))
                                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().parallel()
                                                      .map(routine)
                                                      .asyncCall("test1", "test2")
                                                      .after(seconds(3))
                                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().sync()
                                                      .map(routine)
                                                      .syncCall("test1", "test2")
                                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().sequential()
                                                      .map(routine)
                                                      .syncCall("test1", "test2")
                                                      .all()).containsExactly("TEST1", "TEST2");
    }

    @Test
    public void testMapRoutineBuilder() {
        final RoutineBuilder<String, String> builder = JRoutineCore.with(new UpperCase());
        assertThat(JRoutineStream.<String>withStream().async()
                                                      .map(builder)
                                                      .asyncCall("test1", "test2")
                                                      .after(seconds(3))
                                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().parallel()
                                                      .map(builder)
                                                      .asyncCall("test1", "test2")
                                                      .after(seconds(3))
                                                      .all()).containsOnly("TEST1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().sync()
                                                      .map(builder)
                                                      .syncCall("test1", "test2")
                                                      .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStream.<String>withStream().sequential()
                                                      .map(builder)
                                                      .syncCall("test1", "test2")
                                                      .all()).containsExactly("TEST1", "TEST2");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMapRoutineBuilderNullPointerError() {
        try {
            JRoutineStream.withStream().async().map((RoutineBuilder<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().parallel().map((RoutineBuilder<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().sync().map((RoutineBuilder<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().sequential().map((RoutineBuilder<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMapRoutineNullPointerError() {
        try {
            JRoutineStream.withStream().async().map((Routine<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().parallel().map((Routine<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().sync().map((Routine<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStream.withStream().sequential().map((Routine<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @Test
    public void testMaxSizeDeadlock() {
        try {
            assertThat(JRoutineStream.withStream()
                                     .map(insteadAccept(range(1, 1000)))
                                     .streamInvocationConfiguration()
                                     .withRunner(getSingleThreadRunner())
                                     .withInputLimit(2)
                                     .withInputBackoff(seconds(3))
                                     .withOutputLimit(2)
                                     .withOutputBackoff(seconds(3))
                                     .applied()
                                     .map(Functions.<Number>identity())
                                     .map(new Function<Number, Double>() {

                                         public Double apply(final Number number) {
                                             final double value = number.doubleValue();
                                             return Math.sqrt(value);
                                         }
                                     })
                                     .sync()
                                     .map(new Function<Double, SumData>() {

                                         public SumData apply(final Double aDouble) {
                                             return new SumData(aDouble, 1);
                                         }
                                     })
                                     .map(reduce(new BiFunction<SumData, SumData, SumData>() {

                                         public SumData apply(final SumData data1,
                                                 final SumData data2) {
                                             return new SumData(data1.sum + data2.sum,
                                                     data1.count + data2.count);
                                         }
                                     }))
                                     .map(new Function<SumData, Double>() {

                                         public Double apply(final SumData data) {
                                             return data.sum / data.count;
                                         }
                                     })
                                     .asyncCall()
                                     .close()
                                     .after(minutes(3))
                                     .next()).isCloseTo(21, Offset.offset(0.1));
            fail();

        } catch (final InputDeadlockException ignored) {
        }

        try {
            assertThat(JRoutineStream.withStream()
                                     .map(insteadAccept(range(1, 1000)))
                                     .streamInvocationConfiguration()
                                     .withRunner(getSingleThreadRunner())
                                     .withOutputLimit(2)
                                     .withOutputBackoff(seconds(3))
                                     .applied()
                                     .map(Functions.<Number>identity())
                                     .map(new Function<Number, Double>() {

                                         public Double apply(final Number number) {
                                             final double value = number.doubleValue();
                                             return Math.sqrt(value);
                                         }
                                     })
                                     .sync()
                                     .map(new Function<Double, SumData>() {

                                         public SumData apply(final Double aDouble) {
                                             return new SumData(aDouble, 1);
                                         }
                                     })
                                     .map(reduce(new BiFunction<SumData, SumData, SumData>() {

                                         public SumData apply(final SumData data1,
                                                 final SumData data2) {
                                             return new SumData(data1.sum + data2.sum,
                                                     data1.count + data2.count);
                                         }
                                     }))
                                     .map(new Function<SumData, Double>() {

                                         public Double apply(final SumData data) {
                                             return data.sum / data.count;
                                         }
                                     })
                                     .asyncCall()
                                     .close()
                                     .after(minutes(3))
                                     .next()).isCloseTo(21, Offset.offset(0.1));

        } catch (final OutputDeadlockException ignored) {
        }

        try {
            assertThat(JRoutineStream.withStream()
                                     .map(insteadAccept(range(1, 1000)))
                                     .streamInvocationConfiguration()
                                     .withRunner(getSingleThreadRunner())
                                     .withInputLimit(2)
                                     .withInputBackoff(seconds(3))
                                     .applied()
                                     .map(Functions.<Number>identity())
                                     .map(new Function<Number, Double>() {

                                         public Double apply(final Number number) {
                                             final double value = number.doubleValue();
                                             return Math.sqrt(value);
                                         }
                                     })
                                     .sync()
                                     .map(new Function<Double, SumData>() {

                                         public SumData apply(final Double aDouble) {
                                             return new SumData(aDouble, 1);
                                         }
                                     })
                                     .map(reduce(new BiFunction<SumData, SumData, SumData>() {

                                         public SumData apply(final SumData data1,
                                                 final SumData data2) {
                                             return new SumData(data1.sum + data2.sum,
                                                     data1.count + data2.count);
                                         }
                                     }))
                                     .map(new Function<SumData, Double>() {

                                         public Double apply(final SumData data) {
                                             return data.sum / data.count;
                                         }
                                     })
                                     .asyncCall()
                                     .close()
                                     .after(minutes(3))
                                     .next()).isCloseTo(21, Offset.offset(0.1));
            fail();

        } catch (final InputDeadlockException ignored) {
        }
    }

    @Test
    public void testStraight() {
        assertThat(JRoutineStream.withStream()
                                 .straight()
                                 .map(insteadAccept(range(1, 1000)))
                                 .streamInvocationConfiguration()
                                 .withInputMaxSize(1)
                                 .withOutputMaxSize(1)
                                 .applied()
                                 .map(new Function<Number, Double>() {

                                     public Double apply(final Number number) {
                                         return Math.sqrt(number.doubleValue());
                                     }
                                 })
                                 .map(Operators.averageDouble())
                                 .syncCall()
                                 .close()
                                 .next()).isCloseTo(21, Offset.offset(0.1));
    }

    @Test
    public void testTransform() {
        assertThat(JRoutineStream.<String>withStream().liftWithConfig(
                new BiFunction<StreamConfiguration, Function<Channel<?, String>, Channel<?,
                        String>>, Function<Channel<?, String>, Channel<?, String>>>() {

                    public Function<Channel<?, String>, Channel<?, String>> apply(
                            final StreamConfiguration configuration,
                            final Function<Channel<?, String>, Channel<?, String>> function) {
                        assertThat(configuration.asChannelConfiguration()).isEqualTo(
                                ChannelConfiguration.defaultConfiguration());
                        assertThat(configuration.asInvocationConfiguration()).isEqualTo(
                                InvocationConfiguration.defaultConfiguration());
                        assertThat(configuration.getInvocationMode()).isEqualTo(
                                InvocationMode.ASYNC);
                        return Functions.decorate(function)
                                        .andThen(
                                                new Function<Channel<?, String>, Channel<?,
                                                        String>>() {

                                                    public Channel<?, String> apply(
                                                            final Channel<?, String> channel) {
                                                        return JRoutineCore.with(new UpperCase())
                                                                           .asyncCall(channel);
                                                    }
                                                });
                    }
                }).asyncCall("test").after(seconds(3)).next()).isEqualTo("TEST");
        assertThat(JRoutineStream.<String>withStream().lift(
                new Function<Function<Channel<?, String>, Channel<?, String>>,
                        Function<Channel<?, String>, Channel<?, String>>>() {

                    public Function<Channel<?, String>, Channel<?, String>> apply(
                            final Function<Channel<?, String>, Channel<?, String>> function) {
                        return Functions.decorate(function)
                                        .andThen(
                                                new Function<Channel<?, String>, Channel<?,
                                                        String>>() {

                                                    public Channel<?, String> apply(
                                                            final Channel<?, String> channel) {
                                                        return JRoutineCore.with(new UpperCase())
                                                                           .asyncCall(channel);
                                                    }
                                                });
                    }
                }).asyncCall("test").after(seconds(3)).next()).isEqualTo("TEST");
        try {
            JRoutineStream.withStream()
                          .liftWithConfig(
                                  new BiFunction<StreamConfiguration, Function<Channel<?,
                                          Object>, Channel<?, Object>>, Function<Channel<?,
                                          Object>, Channel<?, Object>>>() {

                                      public Function<Channel<?, Object>, Channel<?, Object>> apply(
                                              final StreamConfiguration configuration,
                                              final Function<Channel<?, Object>, Channel<?,
                                                      Object>> function) {
                                          throw new NullPointerException();
                                      }
                                  });
            fail();

        } catch (final StreamBuildingException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(NullPointerException.class);
        }

        try {
            JRoutineStream.withStream()
                          .lift(new Function<Function<Channel<?, Object>, Channel<?, Object>>,
                                  Function<Channel<?, Object>, Channel<?, Object>>>() {

                              public Function<Channel<?, Object>, Channel<?, Object>> apply(
                                      final Function<Channel<?, Object>, Channel<?, Object>>
                                              function) {
                                  throw new NullPointerException();
                              }
                          });
            fail();

        } catch (final StreamBuildingException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(NullPointerException.class);
        }

        final StreamBuilder<Object, Object> builder = //
                JRoutineStream.withStream()
                              .lift(new Function<Function<Channel<?, Object>, Channel<?,
                                      Object>>, Function<Channel<?, Object>, Channel<?, Object>>>
                                      () {

                                  public Function<Channel<?, Object>, Channel<?, Object>> apply(
                                          final Function<Channel<?, Object>, Channel<?, Object>>
                                                  function) {
                                      return new Function<Channel<?, Object>, Channel<?, Object>>
                                              () {

                                          public Channel<?, Object> apply(
                                                  final Channel<?, Object> objects) {
                                              throw new NullPointerException();
                                          }
                                      };
                                  }
                              });
        try {
            builder.syncCall().close().throwError();
            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(NullPointerException.class);
        }
    }

    private static class SumData {

        private final int count;

        private final double sum;

        private SumData(final double sum, final int count) {

            this.sum = sum;
            this.count = count;
        }
    }

    private static class UpperCase extends MappingInvocation<String, String> {

        /**
         * Constructor.
         */
        protected UpperCase() {
            super(null);
        }

        public void onInput(final String input, @NotNull final Channel<String, ?> result) {
            result.pass(input.toUpperCase());
        }
    }
}
