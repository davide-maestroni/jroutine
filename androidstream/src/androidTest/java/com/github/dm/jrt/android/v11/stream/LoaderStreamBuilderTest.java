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
import android.app.Activity;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.v11.core.JRoutineLoader;
import com.github.dm.jrt.android.v11.core.LoaderContext;
import com.github.dm.jrt.android.v11.stream.LoaderStreamBuilder.LoaderStreamConfiguration;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.RoutineBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ExecutionDeadlockException;
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.MappingInvocation;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.runner.Runners;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Functions;
import com.github.dm.jrt.operator.Operators;
import com.github.dm.jrt.stream.builder.StreamBuilder;

import org.assertj.core.data.Offset;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.github.dm.jrt.android.v11.core.LoaderContext.loaderFrom;
import static com.github.dm.jrt.core.util.UnitDuration.minutes;
import static com.github.dm.jrt.core.util.UnitDuration.seconds;
import static com.github.dm.jrt.function.Functions.functionMapping;
import static com.github.dm.jrt.operator.Operators.append;
import static com.github.dm.jrt.operator.Operators.filter;
import static com.github.dm.jrt.operator.Operators.replaceWith;
import static com.github.dm.jrt.operator.producer.Producers.range;
import static com.github.dm.jrt.stream.transform.Transformations.tryCatchWith;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Android stream channel unit tests.
 * <p>
 * Created by davide-maestroni on 07/03/2016.
 */
@TargetApi(VERSION_CODES.FROYO)
public class LoaderStreamBuilderTest extends ActivityInstrumentationTestCase2<TestActivity> {

    public LoaderStreamBuilderTest() {
        super(TestActivity.class);
    }

    private static Function<Number, Double> sqrt() {
        return new Function<Number, Double>() {

            public Double apply(final Number number) {
                return Math.sqrt(number.doubleValue());
            }
        };
    }

    private static void testFlatMap(@NotNull final Activity activity) {
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(activity))
                                     .sync()
                                     .flatMap(new Function<String, Channel<?, String>>() {

                                         public Channel<?, String> apply(final String s) {
                                             return JRoutineStreamLoader //
                                                     .<String>withStream().on(loaderFrom(activity))
                                                                          .sync()
                                                                          .map(filter(
                                                                                  Functions
                                                                                          .<String>isNotNull()))
                                                                          .syncCall(s);
                                         }
                                     })
                                     .syncCall("test1", null, "test2", null)
                                     .all()).containsExactly("test1", "test2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(activity))
                                     .async()
                                     .flatMap(new Function<String, Channel<?, String>>() {

                                         public Channel<?, String> apply(final String s) {
                                             return JRoutineStreamLoader //
                                                     .<String>withStream().on(loaderFrom(activity))
                                                                          .sync()
                                                                          .map(filter(
                                                                                  Functions
                                                                                          .<String>isNotNull()))
                                                                          .syncCall(s);
                                         }
                                     })
                                     .asyncCall("test1", null, "test2", null)
                                     .after(seconds(10))
                                     .all()).containsExactly("test1", "test2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(activity))
                                     .parallel()
                                     .flatMap(new Function<String, Channel<?, String>>() {

                                         public Channel<?, String> apply(final String s) {
                                             return JRoutineStreamLoader //
                                                     .<String>withStream().on(loaderFrom(activity))
                                                                          .sync()
                                                                          .map(filter(
                                                                                  Functions
                                                                                          .<String>isNotNull()))
                                                                          .syncCall(s);
                                         }
                                     })
                                     .asyncCall("test1", null, "test2", null)
                                     .after(seconds(10))
                                     .all()).containsOnly("test1", "test2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(activity))
                                     .sequential()
                                     .flatMap(new Function<String, Channel<?, String>>() {

                                         public Channel<?, String> apply(final String s) {
                                             return JRoutineStreamLoader //
                                                     .<String>withStream().on(loaderFrom(activity))
                                                                          .sync()
                                                                          .map(filter(
                                                                                  Functions
                                                                                          .<String>isNotNull()))
                                                                          .syncCall(s);
                                         }
                                     })
                                     .syncCall("test1", null, "test2", null)
                                     .after(seconds(10))
                                     .all()).containsOnly("test1", "test2");
    }

    private static void testInvocationDeadlock(@NotNull final Activity activity) {
        try {
            final Runner runner1 = Runners.poolRunner(1);
            final Runner runner2 = Runners.poolRunner(1);
            JRoutineStreamLoader //
                    .<String>withStream().on(loaderFrom(activity))
                                         .invocationConfiguration()
                                         .withRunner(runner1)
                                         .applied()
                                         .map(new Function<String, Object>() {

                                             public Object apply(final String s) {
                                                 return JRoutineStreamLoader.withStream()
                                                                            .on(loaderFrom(
                                                                                    activity))
                                                                            .invocationConfiguration()
                                                                            .withRunner(runner1)
                                                                            .applied()
                                                                            .map(Functions
                                                                                    .identity())
                                                                            .invocationConfiguration()
                                                                            .withRunner(runner2)
                                                                            .applied()
                                                                            .map(Functions
                                                                                    .identity())
                                                                            .asyncCall()
                                                                            .close()
                                                                            .after(minutes(3))
                                                                            .next();
                                             }
                                         })
                                         .asyncCall("tests")
                                         .after(minutes(3))
                                         .next();
            fail();

        } catch (final ExecutionDeadlockException ignored) {
        }
    }

    private static void testMapAllConsumer(@NotNull final Activity activity) {
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(activity))
                                     .async()
                                     .mapAllWith(new BiConsumer<List<?
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
                                     })
                                     .asyncCall("test1", "test2", "test3")
                                     .after(seconds(10))
                                     .all()).containsExactly("test1test2test3");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(activity))
                                     .sync()
                                     .mapAllWith(
                                             new BiConsumer<List<? extends String>,
                                                     Channel<String, ?>>() {

                                                 public void accept(
                                                         final List<? extends String> strings,
                                                         final Channel<String, ?> result) {
                                                     final StringBuilder builder =
                                                             new StringBuilder();
                                                     for (final String string : strings) {
                                                         builder.append(string);
                                                     }

                                                     result.pass(builder.toString());
                                                 }
                                             })
                                     .syncCall("test1", "test2", "test3")
                                     .all()).containsExactly("test1test2test3");
    }

    private static void testMapAllFunction(@NotNull final Activity activity) {

        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(activity))
                                     .async()
                                     .mapAll(new Function<List<? extends String>, String>() {

                                         public String apply(final List<? extends String> strings) {
                                             final StringBuilder builder = new StringBuilder();
                                             for (final String string : strings) {
                                                 builder.append(string);
                                             }

                                             return builder.toString();
                                         }
                                     })
                                     .asyncCall("test1", "test2", "test3")
                                     .after(seconds(10))
                                     .all()).containsExactly("test1test2test3");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(activity))
                                     .sync()
                                     .mapAll(new Function<List<? extends String>, String>() {

                                         public String apply(final List<? extends String> strings) {
                                             final StringBuilder builder = new StringBuilder();
                                             for (final String string : strings) {
                                                 builder.append(string);
                                             }

                                             return builder.toString();
                                         }
                                     })
                                     .syncCall("test1", "test2", "test3")
                                     .all()).containsExactly("test1test2test3");
    }

    private static void testMapConsumer(@NotNull final Activity activity) {
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(activity))
                                     .mapWith(new BiConsumer<String, Channel<String, ?>>() {

                                         public void accept(final String s,
                                                 final Channel<String, ?> result) {
                                             result.pass(s.toUpperCase());
                                         }
                                     })
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(activity))
                                     .sorted()
                                     .parallel()
                                     .mapWith(new BiConsumer<String, Channel<String, ?>>() {

                                         public void accept(final String s,
                                                 final Channel<String, ?> result) {
                                             result.pass(s.toUpperCase());
                                         }
                                     })
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(activity))
                                     .sync()
                                     .mapWith(new BiConsumer<String, Channel<String, ?>>() {

                                         public void accept(final String s,
                                                 final Channel<String, ?> result) {
                                             result.pass(s.toUpperCase());
                                         }
                                     })
                                     .syncCall("test1", "test2")
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(activity))
                                     .streamInvocationConfiguration()
                                     .withOutputOrder(OrderType.BY_CALL)
                                     .applied()
                                     .sequential()
                                     .mapWith(new BiConsumer<String, Channel<String, ?>>() {

                                         public void accept(final String s,
                                                 final Channel<String, ?> result) {
                                             result.pass(s.toUpperCase());
                                         }
                                     })
                                     .syncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
    }

    private static void testMapFunction(@NotNull final Activity activity) {
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(activity))
                                     .async()
                                     .map(new Function<String, String>() {

                                         public String apply(final String s) {
                                             return s.toUpperCase();
                                         }
                                     })
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(activity))
                                     .sorted()
                                     .parallel()
                                     .map(new Function<String, String>() {

                                         public String apply(final String s) {

                                             return s.toUpperCase();
                                         }
                                     })
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(activity))
                                     .sync()
                                     .map(new Function<String, String>() {

                                         public String apply(final String s) {

                                             return s.toUpperCase();
                                         }
                                     })
                                     .syncCall("test1", "test2")
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(activity))
                                     .sorted()
                                     .sequential()
                                     .map(new Function<String, String>() {

                                         public String apply(final String s) {

                                             return s.toUpperCase();
                                         }
                                     })
                                     .syncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
    }

    @NotNull
    private static BiFunction<LoaderStreamConfiguration, Function<Channel<?, String>, Channel<?,
            String>>, Function<Channel<?, String>, Channel<?, String>>> transformBiFunction() {
        return new BiFunction<LoaderStreamConfiguration, Function<Channel<?, String>, Channel<?,
                String>>, Function<Channel<?, String>, Channel<?, String>>>() {

            public Function<Channel<?, String>, Channel<?, String>> apply(
                    final LoaderStreamConfiguration configuration,
                    final Function<Channel<?, String>, Channel<?, String>> function) {
                assertThat(configuration.asLoaderConfiguration()).isEqualTo(
                        LoaderConfiguration.defaultConfiguration());
                assertThat(configuration.getLoaderContext()).isInstanceOf(LoaderContext.class);
                return Functions.decorate(function)
                                .andThen(new Function<Channel<?, String>, Channel<?, String>>() {

                                    public Channel<?, String> apply(
                                            final Channel<?, String> channel) {
                                        return JRoutineCore.with(new UpperCase())
                                                           .asyncCall(channel);
                                    }
                                });
            }
        };
    }

    @NotNull
    private static Function<Function<Channel<?, String>, Channel<?, String>>, Function<Channel<?,
            String>, Channel<?, String>>> transformFunction() {
        return new Function<Function<Channel<?, String>, Channel<?, String>>, Function<Channel<?,
                String>, Channel<?, String>>>() {

            public Function<Channel<?, String>, Channel<?, String>> apply(
                    final Function<Channel<?, String>, Channel<?, String>> function) {
                return Functions.decorate(function)
                                .andThen(new Function<Channel<?, String>, Channel<?, String>>() {

                                    public Channel<?, String> apply(
                                            final Channel<?, String> channel) {
                                        return JRoutineCore.with(new UpperCase())
                                                           .asyncCall(channel);
                                    }
                                });
            }
        };
    }

    public void testAsync() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .mapOn(null)
                                       .asyncCall("test")
                                       .after(seconds(10))
                                       .all()).containsExactly("test");
    }

    public void testConstructor() {
        boolean failed = false;
        try {
            new JRoutineStreamLoader();
            failed = true;

        } catch (final Throwable ignored) {
        }

        assertThat(failed).isFalse();
    }

    public void testFlatMap() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        testFlatMap(getActivity());
    }

    @SuppressWarnings("ConstantConditions")
    public void testFlatMapNullPointerError() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            JRoutineStreamLoader.withStream().on(loaderFrom(getActivity())).sync().flatMap(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream().on(loaderFrom(getActivity())).async().flatMap(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .parallel()
                                .flatMap(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .sequential()
                                .flatMap(null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    public void testFlatMapRetry() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        final Routine<Object, String> routine =
                JRoutineCore.with(functionMapping(new Function<Object, String>() {

                    public String apply(final Object o) {
                        return o.toString();
                    }
                })).buildRoutine();
        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .async()
                                .flatMap(new RetryFunction(getActivity(), routine))
                                .asyncCall((Object) null)
                                .after(seconds(10))
                                .all();
            fail();

        } catch (final RoutineException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(NullPointerException.class);
        }
    }

    public void testFlatTransform() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .let(
                                             new Function<StreamBuilder<String, String>,
                                                     StreamBuilder<String, String>>() {

                                                 public StreamBuilder<String, String> apply(
                                                         final StreamBuilder<String, String>
                                                                 builder) {
                                                     return builder.map(append("test2"));
                                                 }
                                             })
                                     .asyncCall("test1")
                                     .after(seconds(10))
                                     .all()).containsExactly("test1", "test2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .let(
                                             new Function<StreamBuilder<String, String>,
                                                     LoaderStreamBuilder<String, String>>() {

                                                 public LoaderStreamBuilder<String, String> apply(
                                                         final StreamBuilder<String, String>
                                                                 builder) {
                                                     return ((LoaderStreamBuilder<String,
                                                             String>) builder)
                                                             .map(append("test2"));
                                                 }
                                             })
                                     .asyncCall("test1")
                                     .after(seconds(10))
                                     .all()).containsExactly("test1", "test2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .letWithConfig(
                                             new BiFunction<LoaderStreamConfiguration,
                                                     StreamBuilder<String, String>,
                                                     StreamBuilder<String, String>>() {

                                                 public StreamBuilder<String, String> apply(
                                                         final LoaderStreamConfiguration
                                                                 configuration,
                                                         final StreamBuilder<String, String>
                                                                 builder) {
                                                     return builder.map(append("test2"));
                                                 }
                                             })
                                     .asyncCall("test1")
                                     .after(seconds(10))
                                     .all()).containsExactly("test1", "test2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .letWithConfig(
                                             new BiFunction<LoaderStreamConfiguration,
                                                     StreamBuilder<String, String>,
                                                     LoaderStreamBuilder<String, String>>() {

                                                 public LoaderStreamBuilder<String, String> apply(
                                                         final LoaderStreamConfiguration
                                                                 configuration,
                                                         final StreamBuilder<String, String>
                                                                 builder) {
                                                     return ((LoaderStreamBuilder<String,
                                                             String>) builder)
                                                             .map(append("test2"));
                                                 }
                                             })
                                     .asyncCall("test1")
                                     .after(seconds(10))
                                     .all()).containsExactly("test1", "test2");
    }

    public void testInvocationDeadlock() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        testInvocationDeadlock(getActivity());
    }

    public void testInvocationMode() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .invocationMode(InvocationMode.ASYNC)
                                       .asyncCall("test1", "test2", "test3")
                                       .after(seconds(10))
                                       .all()).containsExactly("test1", "test2", "test3");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .invocationMode(InvocationMode.PARALLEL)
                                       .asyncCall("test1", "test2", "test3")
                                       .after(seconds(10))
                                       .all()).containsExactly("test1", "test2", "test3");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .invocationMode(InvocationMode.SYNC)
                                       .asyncCall("test1", "test2", "test3")
                                       .after(seconds(10))
                                       .all()).containsExactly("test1", "test2", "test3");
        assertThat(JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(getActivity()))
                                       .invocationMode(InvocationMode.SEQUENTIAL)
                                       .asyncCall("test1", "test2", "test3")
                                       .after(seconds(10))
                                       .all()).containsExactly("test1", "test2", "test3");
    }

    @SuppressWarnings("ConstantConditions")
    public void testInvocationModeNullPointerError() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            JRoutineStreamLoader.withStream().invocationMode(null);
            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testMapAllConsumer() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        testMapAllConsumer(getActivity());
    }

    @SuppressWarnings("ConstantConditions")
    public void testMapAllConsumerNullPointerError() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .async()
                                .mapAllWith(null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    public void testMapAllFunction() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        testMapAllFunction(getActivity());
    }

    @SuppressWarnings("ConstantConditions")
    public void testMapAllFunctionNullPointerError() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            JRoutineStreamLoader.withStream().on(loaderFrom(getActivity())).async().mapAll(null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    public void testMapConsumer() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        testMapConsumer(getActivity());
    }

    @SuppressWarnings("ConstantConditions")
    public void testMapConsumerNullPointerError() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            JRoutineStreamLoader.withStream().on(loaderFrom(getActivity())).async().mapWith(null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    public void testMapContextFactory() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        final ContextInvocationFactory<String, String> factory =
                ContextInvocationFactory.factoryOf(UpperCase.class);
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .async()
                                     .map(factory)
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .sorted()
                                     .parallel()
                                     .map(factory)
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .sync()
                                     .map(factory)
                                     .syncCall("test1", "test2")
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .sorted()
                                     .sequential()
                                     .map(factory)
                                     .syncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
    }

    public void testMapContextFactoryIllegalState() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        final ContextInvocationFactory<String, String> factory =
                ContextInvocationFactory.factoryOf(UpperCase.class);
        try {
            JRoutineStreamLoader.<String>withStream().async().map(factory);
            fail();

        } catch (final IllegalStateException ignored) {
        }

        try {
            JRoutineStreamLoader.<String>withStream().sync().map(factory);
            fail();

        } catch (final IllegalStateException ignored) {
        }

        try {
            JRoutineStreamLoader.<String>withStream().parallel().map(factory);
            fail();

        } catch (final IllegalStateException ignored) {
        }

        try {
            JRoutineStreamLoader.<String>withStream().sequential().map(factory);
            fail();

        } catch (final IllegalStateException ignored) {
        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testMapContextFactoryNullPointerError() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .async()
                                .map((ContextInvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .parallel()
                                .map((ContextInvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .sync()
                                .map((ContextInvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .sequential()
                                .map((ContextInvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    public void testMapFactory() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        final InvocationFactory<String, String> factory =
                InvocationFactory.factoryOf(UpperCase.class);
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .async()
                                     .map(factory)
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .sorted()
                                     .parallel()
                                     .map(factory)
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .sync()
                                     .map(factory)
                                     .syncCall("test1", "test2")
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .sorted()
                                     .sequential()
                                     .map(factory)
                                     .syncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
    }

    @SuppressWarnings("ConstantConditions")
    public void testMapFactoryNullPointerError() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .async()
                                .map((InvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .parallel()
                                .map((InvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .sync()
                                .map((InvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .sequential()
                                .map((InvocationFactory<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    public void testMapFilter() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .async()
                                     .map(new UpperCase())
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .sorted()
                                     .parallel()
                                     .map(new UpperCase())
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .sync()
                                     .map(new UpperCase())
                                     .syncCall("test1", "test2")
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .sorted()
                                     .sequential()
                                     .map(new UpperCase())
                                     .syncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
    }

    @SuppressWarnings("ConstantConditions")
    public void testMapFilterNullPointerError() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .async()
                                .map((MappingInvocation<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .parallel()
                                .map((MappingInvocation<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .sync()
                                .map((MappingInvocation<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .sequential()
                                .map((MappingInvocation<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    public void testMapFunction() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        testMapFunction(getActivity());
    }

    @SuppressWarnings("ConstantConditions")
    public void testMapFunctionNullPointerError() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .async()
                                .map((Function<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .parallel()
                                .map((Function<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .sync()
                                .map((Function<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .sequential()
                                .map((Function<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    public void testMapRoutine() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        final Routine<String, String> routine = JRoutineCore.with(new UpperCase())
                                                            .invocationConfiguration()
                                                            .withOutputOrder(OrderType.BY_CALL)
                                                            .applied()
                                                            .buildRoutine();
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .async()
                                     .map(routine)
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .parallel()
                                     .map(routine)
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .sync()
                                     .map(routine)
                                     .syncCall("test1", "test2")
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .sequential()
                                     .map(routine)
                                     .syncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
    }

    public void testMapRoutineBuilder() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        final RoutineBuilder<String, String> builder = JRoutineCore.with(new UpperCase());
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .async()
                                     .map(builder)
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .parallel()
                                     .map(builder)
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsOnly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .sync()
                                     .map(builder)
                                     .syncCall("test1", "test2")
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .sequential()
                                     .map(builder)
                                     .syncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
        final RoutineBuilder<String, String> loaderBuilder =
                JRoutineLoader.on(loaderFrom(getActivity()))
                              .with(ContextInvocationFactory.factoryOf(UpperCase.class));
        assertThat(JRoutineStreamLoader //
                .<String>withStream().async()
                                     .map(loaderBuilder)
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().parallel()
                                     .map(loaderBuilder)
                                     .asyncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsOnly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().sync()
                                     .map(loaderBuilder)
                                     .syncCall("test1", "test2")
                                     .all()).containsExactly("TEST1", "TEST2");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().sequential()
                                     .map(loaderBuilder)
                                     .syncCall("test1", "test2")
                                     .after(seconds(10))
                                     .all()).containsExactly("TEST1", "TEST2");
    }

    @SuppressWarnings("ConstantConditions")
    public void testMapRoutineNullPointerError() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .async()
                                .map((Routine<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .parallel()
                                .map((Routine<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .sync()
                                .map((Routine<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            JRoutineStreamLoader.withStream()
                                .on(loaderFrom(getActivity()))
                                .sequential()
                                .map((Routine<Object, Object>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    public void testStraight() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        assertThat(JRoutineStreamLoader.withStream()
                                       .straight()
                                       .map(replaceWith(range(1, 1000)))
                                       .streamInvocationConfiguration()
                                       .withInputMaxSize(1)
                                       .withOutputMaxSize(1)
                                       .applied()
                                       .map(sqrt())
                                       .map(Operators.<Double>averageDouble())
                                       .syncCall()
                                       .close()
                                       .next()).isCloseTo(21, Offset.offset(0.1));
    }

    public void testTransform() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return;
        }

        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .liftWithConfig(transformBiFunction())
                                     .asyncCall("test")
                                     .after(seconds(10))
                                     .next()).isEqualTo("TEST");
        assertThat(JRoutineStreamLoader //
                .<String>withStream().on(loaderFrom(getActivity()))
                                     .lift(transformFunction())
                                     .asyncCall("test")
                                     .after(seconds(10))
                                     .next()).isEqualTo("TEST");
    }

    private static class RetryFunction implements Function<Object, Channel<Object, String>> {

        private final Activity mActivity;

        private final Routine<Object, String> mRoutine;

        private RetryFunction(@NotNull final Activity activity,
                @NotNull final Routine<Object, String> routine) {
            mActivity = activity;
            mRoutine = routine;
        }

        private static Channel<Object, String> apply(final Object o,
                @NotNull final Activity activity, @NotNull final Routine<Object, String> routine,
                @NotNull final int[] count) {
            return JRoutineStreamLoader.withStream()
                                       .on(loaderFrom(activity))
                                       .map(routine)
                                       .let(tryCatchWith(
                                               new BiConsumer<RoutineException, Channel<String,
                                                       ?>>() {

                                                   public void accept(final RoutineException e,
                                                           final Channel<String, ?> channel) {
                                                       if (++count[0] < 3) {
                                                           JRoutineStreamLoader.withStream()
                                                                               .on(loaderFrom(
                                                                                       activity))
                                                                               .map(routine)
                                                                               .let(tryCatchWith(
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

        public Channel<Object, String> apply(final Object o) {
            final int[] count = {0};
            return apply(o, mActivity, mRoutine, count);
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
