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
import com.github.dm.jrt.android.core.config.LoaderConfiguration.CacheStrategyType;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.core.invocation.MissingLoaderException;
import com.github.dm.jrt.android.v11.core.JRoutineLoader;
import com.github.dm.jrt.android.v11.core.LoaderContext;
import com.github.dm.jrt.android.v11.stream.LoaderStreamBuilder.LoaderStreamConfiguration;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.RoutineBuilder;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ExecutionDeadlockException;
import com.github.dm.jrt.core.channel.TemplateChannelConsumer;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.MappingInvocation;
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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.android.v11.core.LoaderContext.loaderFrom;
import static com.github.dm.jrt.core.util.DurationMeasure.minutes;
import static com.github.dm.jrt.core.util.DurationMeasure.seconds;
import static com.github.dm.jrt.function.Functions.functionMapping;
import static com.github.dm.jrt.function.SupplierDecorator.constant;
import static com.github.dm.jrt.operator.Operators.append;
import static com.github.dm.jrt.operator.Operators.appendAccept;
import static com.github.dm.jrt.operator.Operators.filter;
import static com.github.dm.jrt.operator.sequence.Sequences.range;
import static com.github.dm.jrt.stream.transform.Transformations.tryCatchAccept;
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
    assertThat(JRoutineLoaderStream //
        .<String>withStream().on(null).sync().flatMap(new Function<String, Channel<?, String>>() {

          public Channel<?, String> apply(final String s) {
            return JRoutineLoaderStream //
                .<String>withStream().sync().map(filter(Functions.<String>isNotNull())).call(s);
          }
        }).call("test1", null, "test2", null).all()).containsExactly("test1", "test2");
    assertThat(JRoutineLoaderStream //
        .<String>withStream().on(loaderFrom(activity))
                             .async()
                             .flatMap(new Function<String, Channel<?, String>>() {

                               public Channel<?, String> apply(final String s) {
                                 return JRoutineLoaderStream //
                                     .<String>withStream().sync()
                                                          .map(
                                                              filter(Functions.<String>isNotNull()))
                                                          .call(s);
                               }
                             })
                             .call("test1", null, "test2", null)
                             .in(seconds(10))
                             .all()).containsExactly("test1", "test2");
    assertThat(JRoutineLoaderStream //
        .<String>withStream().on(loaderFrom(activity))
                             .asyncParallel()
                             .flatMap(new Function<String, Channel<?, String>>() {

                               public Channel<?, String> apply(final String s) {
                                 return JRoutineLoaderStream //
                                     .<String>withStream().sync()
                                                          .map(
                                                              filter(Functions.<String>isNotNull()))
                                                          .call(s);
                               }
                             })
                             .call("test1", null, "test2", null)
                             .in(seconds(10))
                             .all()).containsOnly("test1", "test2");
    assertThat(JRoutineLoaderStream //
        .<String>withStream().on(loaderFrom(activity))
                             .syncParallel()
                             .flatMap(new Function<String, Channel<?, String>>() {

                               public Channel<?, String> apply(final String s) {
                                 return JRoutineLoaderStream //
                                     .<String>withStream().sync()
                                                          .map(
                                                              filter(Functions.<String>isNotNull()))
                                                          .call(s);
                               }
                             })
                             .call("test1", null, "test2", null)
                             .in(seconds(10))
                             .all()).containsOnly("test1", "test2");
  }

  private static void testFlatTransform(@NotNull final Activity activity) {
    assertThat(JRoutineLoaderStream //
        .<String>withStream().on(loaderFrom(activity))
                             .let(
                                 new Function<StreamBuilder<String, String>,
                                     StreamBuilder<String, String>>() {

                                   public StreamBuilder<String, String> apply(
                                       final StreamBuilder<String, String> builder) {
                                     return builder.map(append("test2"));
                                   }
                                 })
                             .call("test1")
                             .in(seconds(10))
                             .all()).containsExactly("test1", "test2");
    assertThat(JRoutineLoaderStream //
        .<String>withStream().on(loaderFrom(activity))
                             .let(
                                 new Function<StreamBuilder<String, String>,
                                     LoaderStreamBuilder<String, String>>() {

                                   public LoaderStreamBuilder<String, String> apply(
                                       final StreamBuilder<String, String> builder) {
                                     return ((LoaderStreamBuilder<String, String>) builder).map(
                                         append("test2"));
                                   }
                                 })
                             .call("test1")
                             .in(seconds(10))
                             .all()).containsExactly("test1", "test2");
    assertThat(JRoutineLoaderStream //
        .<String>withStream().on(loaderFrom(activity))
                             .letWithConfig(
                                 new BiFunction<LoaderStreamConfiguration, StreamBuilder<String,
                                     String>, StreamBuilder<String, String>>() {

                                   public StreamBuilder<String, String> apply(
                                       final LoaderStreamConfiguration configuration,
                                       final StreamBuilder<String, String> builder) {
                                     return builder.map(append("test2"));
                                   }
                                 })
                             .call("test1")
                             .in(seconds(10))
                             .all()).containsExactly("test1", "test2");
    assertThat(JRoutineLoaderStream //
        .<String>withStream().on(loaderFrom(activity))
                             .letWithConfig(
                                 new BiFunction<LoaderStreamConfiguration, StreamBuilder<String,
                                     String>, LoaderStreamBuilder<String, String>>() {

                                   public LoaderStreamBuilder<String, String> apply(
                                       final LoaderStreamConfiguration configuration,
                                       final StreamBuilder<String, String> builder) {
                                     return ((LoaderStreamBuilder<String, String>) builder).map(
                                         append("test2"));
                                   }
                                 })
                             .call("test1")
                             .in(seconds(10))
                             .all()).containsExactly("test1", "test2");
  }

  private static void testInvocationDeadlock(@NotNull final Activity activity) {
    try {
      final Runner runner1 = Runners.poolRunner(1);
      final Runner runner2 = Runners.poolRunner(1);
      JRoutineLoaderStream //
          .<String>withStream().on(loaderFrom(activity))
                               .applyInvocationConfiguration()
                               .withRunner(runner1)
                               .configured()
                               .map(new Function<String, Object>() {

                                 public Object apply(final String s) {
                                   return JRoutineLoaderStream.withStream()
                                                              .on(loaderFrom(activity))
                                                              .applyInvocationConfiguration()
                                                              .withRunner(runner1)
                                                              .configured()
                                                              .map(Functions.identity())
                                                              .applyInvocationConfiguration()
                                                              .withRunner(runner2)
                                                              .configured()
                                                              .map(Functions.identity())
                                                              .close()
                                                              .in(minutes(3))
                                                              .next();
                                 }
                               })
                               .call("tests")
                               .in(minutes(3))
                               .next();
      fail();

    } catch (final ExecutionDeadlockException ignored) {
    }
  }

  private static void testMapAllConsumer(@NotNull final Activity activity) {
    assertThat(JRoutineLoaderStream //
        .<String>withStream().on(loaderFrom(activity)).async().mapAllAccept(new BiConsumer<List<?
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
        }).call("test1", "test2", "test3").in(seconds(10)).all()).containsExactly(
        "test1test2test3");
    assertThat(JRoutineLoaderStream //
        .<String>withStream().on(null)
                             .sync()
                             .mapAllAccept(
                                 new BiConsumer<List<? extends String>, Channel<String, ?>>() {

                                   public void accept(final List<? extends String> strings,
                                       final Channel<String, ?> result) {
                                     final StringBuilder builder = new StringBuilder();
                                     for (final String string : strings) {
                                       builder.append(string);
                                     }

                                     result.pass(builder.toString());
                                   }
                                 })
                             .call("test1", "test2", "test3")
                             .all()).containsExactly("test1test2test3");
  }

  private static void testMapAllFunction(@NotNull final Activity activity) {

    assertThat(JRoutineLoaderStream //
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
                             .call("test1", "test2", "test3")
                             .in(seconds(10))
                             .all()).containsExactly("test1test2test3");
    assertThat(JRoutineLoaderStream //
        .<String>withStream().on(null)
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
                             .call("test1", "test2", "test3")
                             .all()).containsExactly("test1test2test3");
  }

  private static void testMapConsumer(@NotNull final Activity activity) {
    assertThat(JRoutineLoaderStream //
        .<String>withStream().on(loaderFrom(activity))
                             .mapAccept(new BiConsumer<String, Channel<String, ?>>() {

                               public void accept(final String s, final Channel<String, ?> result) {
                                 result.pass(s.toUpperCase());
                               }
                             })
                             .call("test1", "test2")
                             .in(seconds(10))
                             .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineLoaderStream //
        .<String>withStream().on(loaderFrom(activity))
                             .sorted()
                             .asyncParallel()
                             .mapAccept(new BiConsumer<String, Channel<String, ?>>() {

                               public void accept(final String s, final Channel<String, ?> result) {
                                 result.pass(s.toUpperCase());
                               }
                             })
                             .call("test1", "test2")
                             .in(seconds(10))
                             .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineLoaderStream //
        .<String>withStream().on(null)
                             .sync()
                             .mapAccept(new BiConsumer<String, Channel<String, ?>>() {

                               public void accept(final String s, final Channel<String, ?> result) {
                                 result.pass(s.toUpperCase());
                               }
                             })
                             .call("test1", "test2")
                             .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineLoaderStream //
        .<String>withStream().on(loaderFrom(activity))
                             .applyStreamInvocationConfiguration()
                             .withOutputOrder(OrderType.SORTED)
                             .configured()
                             .syncParallel()
                             .mapAccept(new BiConsumer<String, Channel<String, ?>>() {

                               public void accept(final String s, final Channel<String, ?> result) {
                                 result.pass(s.toUpperCase());
                               }
                             })
                             .call("test1", "test2")
                             .in(seconds(10))
                             .all()).containsExactly("TEST1", "TEST2");
  }

  private static void testMapFunction(@NotNull final Activity activity) {
    assertThat(JRoutineLoaderStream //
        .<String>withStream().on(loaderFrom(activity)).async().map(new Function<String, String>() {

          public String apply(final String s) {
            return s.toUpperCase();
          }
        }).call("test1", "test2").in(seconds(10)).all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineLoaderStream //
        .<String>withStream().on(loaderFrom(activity))
                             .sorted()
                             .asyncParallel()
                             .map(new Function<String, String>() {

                               public String apply(final String s) {

                                 return s.toUpperCase();
                               }
                             })
                             .call("test1", "test2")
                             .in(seconds(10))
                             .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineLoaderStream //
        .<String>withStream().on(null).sync().map(new Function<String, String>() {

          public String apply(final String s) {

            return s.toUpperCase();
          }
        }).call("test1", "test2").all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineLoaderStream //
        .<String>withStream().on(loaderFrom(activity))
                             .sorted()
                             .syncParallel()
                             .map(new Function<String, String>() {

                               public String apply(final String s) {

                                 return s.toUpperCase();
                               }
                             })
                             .call("test1", "test2")
                             .in(seconds(10))
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
        assertThat(configuration.toLoaderConfiguration()).isEqualTo(
            LoaderConfiguration.defaultConfiguration());
        assertThat(configuration.getLoaderContext()).isInstanceOf(LoaderContext.class);
        return Functions.decorate(function)
                        .andThen(new Function<Channel<?, String>, Channel<?, String>>() {

                          public Channel<?, String> apply(final Channel<?, String> channel) {
                            return JRoutineCore.with(new UpperCase()).call(channel);
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

                          public Channel<?, String> apply(final Channel<?, String> channel) {
                            return JRoutineCore.with(new UpperCase()).call(channel);
                          }
                        });
      }
    };
  }

  public void testAsync() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    assertThat(JRoutineLoaderStream.withStream()
                                   .on(loaderFrom(getActivity()))
                                   .mapOn(null)
                                   .call("test")
                                   .in(seconds(10))
                                   .all()).containsExactly("test");
  }

  public void testCache() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    assertThat(JRoutineLoaderStream.withStream()
                                   .on(loaderFrom(getActivity()))
                                   .applyLoaderConfiguration()
                                   .withLoaderId(0)
                                   .withCacheStrategy(CacheStrategyType.CACHE)
                                   .configured()
                                   .call("test")
                                   .in(seconds(10))
                                   .getComplete()).isTrue();
    assertThat(JRoutineLoader.on(loaderFrom(getActivity()))
                             .withId(0)
                             .buildChannel()
                             .in(seconds(10))
                             .next()).isEqualTo("test");
    assertThat(JRoutineLoaderStream.withStream()
                                   .on(loaderFrom(getActivity()))
                                   .applyStreamLoaderConfiguration()
                                   .withLoaderId(0)
                                   .withResultStaleTime(1, TimeUnit.MILLISECONDS)
                                   .configured()
                                   .call("test")
                                   .in(seconds(10))
                                   .getComplete()).isTrue();
    assertThat(JRoutineLoader.on(loaderFrom(getActivity()))
                             .withId(0)
                             .buildChannel()
                             .in(seconds(10))
                             .getError()).isExactlyInstanceOf(MissingLoaderException.class);
  }

  public void testConstructor() {
    boolean failed = false;
    try {
      new JRoutineLoaderStream();
      failed = true;

    } catch (final Throwable ignored) {
    }

    assertThat(failed).isFalse();
  }

  public void testFactory() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final LoaderStreamBuilder<String, String> builder =
        JRoutineLoaderStream.<String>withStream().on(loaderFrom(getActivity()))
                                                 .map(new UpperCase());
    assertThat(
        JRoutineCore.with(builder.buildFactory()).call("test").in(seconds(10)).next()).isEqualTo(
        "TEST");
    assertThat(JRoutineLoader.on(loaderFrom(getActivity()))
                             .with(builder.buildContextFactory())
                             .call("test")
                             .in(seconds(10))
                             .next()).isEqualTo("TEST");
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
      JRoutineLoaderStream.withStream().on(loaderFrom(getActivity())).sync().flatMap(null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      JRoutineLoaderStream.withStream().on(loaderFrom(getActivity())).async().flatMap(null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      JRoutineLoaderStream.withStream().on(loaderFrom(getActivity())).asyncParallel().flatMap(null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      JRoutineLoaderStream.withStream().on(loaderFrom(getActivity())).syncParallel().flatMap(null);
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
      JRoutineLoaderStream.withStream()
                          .on(loaderFrom(getActivity()))
                          .async()
                          .flatMap(new RetryFunction(getActivity(), routine))
                          .call((Object) null)
                          .in(seconds(10))
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

    testFlatTransform(getActivity());
  }

  public void testImmediate() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    assertThat(JRoutineLoaderStream //
        .<Integer>withStream().immediate()
                              .map(appendAccept(range(1, 1000)))
                              .applyStreamInvocationConfiguration()
                              .withInputMaxSize(1)
                              .withOutputMaxSize(1)
                              .configured()
                              .map(sqrt())
                              .map(Operators.average(Double.class))
                              .close()
                              .next()).isCloseTo(21, Offset.offset(0.1));
    assertThat(JRoutineLoaderStream //
        .<Integer>withStream().immediateParallel()
                              .map(appendAccept(range(1, 1000)))
                              .applyStreamInvocationConfiguration()
                              .withInputMaxSize(1)
                              .withOutputMaxSize(1)
                              .configured()
                              .map(sqrt())
                              .immediate()
                              .map(Operators.average(Double.class))
                              .close()
                              .next()).isCloseTo(21, Offset.offset(0.1));
  }

  public void testInvocationDeadlock() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    testInvocationDeadlock(getActivity());
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
      JRoutineLoaderStream.withStream().on(loaderFrom(getActivity())).async().mapAllAccept(null);
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
      JRoutineLoaderStream.withStream().on(loaderFrom(getActivity())).async().mapAll(null);
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
      JRoutineLoaderStream.withStream().on(loaderFrom(getActivity())).async().mapAccept(null);
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
    assertThat(JRoutineLoaderStream //
        .<String>withStream().on(loaderFrom(getActivity()))
                             .async()
                             .map(factory)
                             .call("test1", "test2")
                             .in(seconds(10))
                             .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineLoaderStream //
        .<String>withStream().on(loaderFrom(getActivity()))
                             .sorted()
                             .asyncParallel()
                             .map(factory)
                             .call("test1", "test2")
                             .in(seconds(10))
                             .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineLoaderStream //
        .<String>withStream().on(loaderFrom(getActivity()))
                             .sync()
                             .map(factory)
                             .call("test1", "test2")
                             .in(seconds(10))
                             .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineLoaderStream //
        .<String>withStream().on(loaderFrom(getActivity()))
                             .sorted()
                             .syncParallel()
                             .map(factory)
                             .call("test1", "test2")
                             .in(seconds(10))
                             .all()).containsExactly("TEST1", "TEST2");
  }

  public void testMapContextFactoryIllegalState() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final ContextInvocationFactory<String, String> factory =
        ContextInvocationFactory.factoryOf(UpperCase.class);
    try {
      JRoutineLoaderStream.<String>withStream().async().map(factory);
      fail();

    } catch (final IllegalStateException ignored) {
    }

    try {
      JRoutineLoaderStream.<String>withStream().sync().map(factory);
      fail();

    } catch (final IllegalStateException ignored) {
    }

    try {
      JRoutineLoaderStream.<String>withStream().asyncParallel().map(factory);
      fail();

    } catch (final IllegalStateException ignored) {
    }

    try {
      JRoutineLoaderStream.<String>withStream().syncParallel().map(factory);
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
      JRoutineLoaderStream.withStream()
                          .on(loaderFrom(getActivity()))
                          .async()
                          .map((ContextInvocationFactory<Object, Object>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      JRoutineLoaderStream.withStream()
                          .on(loaderFrom(getActivity()))
                          .asyncParallel()
                          .map((ContextInvocationFactory<Object, Object>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      JRoutineLoaderStream.withStream()
                          .on(loaderFrom(getActivity()))
                          .sync()
                          .map((ContextInvocationFactory<Object, Object>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      JRoutineLoaderStream.withStream()
                          .on(loaderFrom(getActivity()))
                          .syncParallel()
                          .map((ContextInvocationFactory<Object, Object>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  public void testMapFactory() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final InvocationFactory<String, String> factory = InvocationFactory.factoryOf(UpperCase.class);
    assertThat(JRoutineLoaderStream //
        .<String>withStream().on(loaderFrom(getActivity()))
                             .async()
                             .map(factory)
                             .call("test1", "test2")
                             .in(seconds(10))
                             .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineLoaderStream //
        .<String>withStream().on(loaderFrom(getActivity()))
                             .sorted()
                             .asyncParallel()
                             .map(factory)
                             .call("test1", "test2")
                             .in(seconds(10))
                             .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineLoaderStream //
        .<String>withStream().on(loaderFrom(getActivity()))
                             .sync()
                             .map(factory)
                             .call("test1", "test2")
                             .in(seconds(10))
                             .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineLoaderStream //
        .<String>withStream().on(loaderFrom(getActivity()))
                             .sorted()
                             .syncParallel()
                             .map(factory)
                             .call("test1", "test2")
                             .in(seconds(10))
                             .all()).containsExactly("TEST1", "TEST2");
  }

  @SuppressWarnings("ConstantConditions")
  public void testMapFactoryNullPointerError() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    try {
      JRoutineLoaderStream.withStream()
                          .on(loaderFrom(getActivity()))
                          .async()
                          .map((InvocationFactory<Object, Object>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      JRoutineLoaderStream.withStream()
                          .on(loaderFrom(getActivity()))
                          .asyncParallel()
                          .map((InvocationFactory<Object, Object>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      JRoutineLoaderStream.withStream()
                          .on(loaderFrom(getActivity()))
                          .sync()
                          .map((InvocationFactory<Object, Object>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      JRoutineLoaderStream.withStream()
                          .on(loaderFrom(getActivity()))
                          .syncParallel()
                          .map((InvocationFactory<Object, Object>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  public void testMapFilter() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    assertThat(JRoutineLoaderStream //
        .<String>withStream().on(loaderFrom(getActivity()))
                             .async()
                             .map(new UpperCase())
                             .call("test1", "test2")
                             .in(seconds(10))
                             .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineLoaderStream //
        .<String>withStream().on(loaderFrom(getActivity()))
                             .sorted()
                             .asyncParallel()
                             .map(new UpperCase())
                             .call("test1", "test2")
                             .in(seconds(10))
                             .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineLoaderStream //
        .<String>withStream().on(loaderFrom(getActivity()))
                             .sync()
                             .map(new UpperCase())
                             .call("test1", "test2")
                             .in(seconds(10))
                             .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineLoaderStream //
        .<String>withStream().on(loaderFrom(getActivity()))
                             .sorted()
                             .syncParallel()
                             .map(new UpperCase())
                             .call("test1", "test2")
                             .in(seconds(10))
                             .all()).containsExactly("TEST1", "TEST2");
  }

  @SuppressWarnings("ConstantConditions")
  public void testMapFilterNullPointerError() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    try {
      JRoutineLoaderStream.withStream()
                          .on(loaderFrom(getActivity()))
                          .async()
                          .map((MappingInvocation<Object, Object>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      JRoutineLoaderStream.withStream()
                          .on(loaderFrom(getActivity()))
                          .asyncParallel()
                          .map((MappingInvocation<Object, Object>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      JRoutineLoaderStream.withStream()
                          .on(loaderFrom(getActivity()))
                          .sync()
                          .map((MappingInvocation<Object, Object>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      JRoutineLoaderStream.withStream()
                          .on(loaderFrom(getActivity()))
                          .syncParallel()
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
      JRoutineLoaderStream.withStream()
                          .on(loaderFrom(getActivity()))
                          .async()
                          .map((Function<Object, Object>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      JRoutineLoaderStream.withStream()
                          .on(loaderFrom(getActivity()))
                          .asyncParallel()
                          .map((Function<Object, Object>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      JRoutineLoaderStream.withStream()
                          .on(loaderFrom(getActivity()))
                          .sync()
                          .map((Function<Object, Object>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      JRoutineLoaderStream.withStream()
                          .on(loaderFrom(getActivity()))
                          .syncParallel()
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
                                                        .applyInvocationConfiguration()
                                                        .withOutputOrder(OrderType.SORTED)
                                                        .configured()
                                                        .buildRoutine();
    assertThat(JRoutineLoaderStream //
        .<String>withStream().on(loaderFrom(getActivity()))
                             .async()
                             .map(routine)
                             .call("test1", "test2")
                             .in(seconds(10))
                             .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineLoaderStream //
        .<String>withStream().on(loaderFrom(getActivity()))
                             .asyncParallel()
                             .map(routine)
                             .call("test1", "test2")
                             .in(seconds(10))
                             .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineLoaderStream //
        .<String>withStream().on(loaderFrom(getActivity()))
                             .sync()
                             .map(routine)
                             .call("test1", "test2")
                             .in(seconds(10))
                             .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineLoaderStream //
        .<String>withStream().on(loaderFrom(getActivity()))
                             .sorted()
                             .syncParallel()
                             .map(routine)
                             .call("test1", "test2")
                             .in(seconds(10))
                             .all()).containsExactly("TEST1", "TEST2");
  }

  public void testMapRoutineBuilder() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    final RoutineBuilder<String, String> builder = JRoutineCore.with(new UpperCase());
    assertThat(JRoutineLoaderStream //
        .<String>withStream().on(loaderFrom(getActivity()))
                             .async()
                             .map(builder)
                             .call("test1", "test2")
                             .in(seconds(10))
                             .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineLoaderStream //
        .<String>withStream().on(loaderFrom(getActivity()))
                             .asyncParallel()
                             .map(builder)
                             .call("test1", "test2")
                             .in(seconds(10))
                             .all()).containsOnly("TEST1", "TEST2");
    assertThat(JRoutineLoaderStream //
        .<String>withStream().on(loaderFrom(getActivity()))
                             .sync()
                             .map(builder)
                             .call("test1", "test2")
                             .in(seconds(10))
                             .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineLoaderStream //
        .<String>withStream().on(loaderFrom(getActivity()))
                             .sorted()
                             .syncParallel()
                             .map(builder)
                             .call("test1", "test2")
                             .in(seconds(10))
                             .all()).containsExactly("TEST1", "TEST2");
    final RoutineBuilder<String, String> loaderBuilder =
        JRoutineLoader.on(loaderFrom(getActivity()))
                      .with(ContextInvocationFactory.factoryOf(UpperCase.class));
    assertThat(JRoutineLoaderStream //
        .<String>withStream().async()
                             .map(loaderBuilder)
                             .call("test1", "test2")
                             .in(seconds(10))
                             .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineLoaderStream //
        .<String>withStream().asyncParallel()
                             .map(loaderBuilder)
                             .call("test1", "test2")
                             .in(seconds(10))
                             .all()).containsOnly("TEST1", "TEST2");
    assertThat(JRoutineLoaderStream //
        .<String>withStream().sync()
                             .map(loaderBuilder)
                             .call("test1", "test2")
                             .in(seconds(10))
                             .all()).containsExactly("TEST1", "TEST2");
    assertThat(JRoutineLoaderStream //
        .<String>withStream().sorted()
                             .syncParallel()
                             .map(loaderBuilder)
                             .call("test1", "test2")
                             .in(seconds(10))
                             .all()).containsExactly("TEST1", "TEST2");
  }

  @SuppressWarnings("ConstantConditions")
  public void testMapRoutineNullPointerError() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    try {
      JRoutineLoaderStream.withStream()
                          .on(loaderFrom(getActivity()))
                          .async()
                          .map((Routine<Object, Object>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      JRoutineLoaderStream.withStream()
                          .on(loaderFrom(getActivity()))
                          .asyncParallel()
                          .map((Routine<Object, Object>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      JRoutineLoaderStream.withStream()
                          .on(loaderFrom(getActivity()))
                          .sync()
                          .map((Routine<Object, Object>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      JRoutineLoaderStream.withStream()
                          .on(loaderFrom(getActivity()))
                          .syncParallel()
                          .map((Routine<Object, Object>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  public void testStreamAccept() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    assertThat(JRoutineLoaderStream.withStreamAccept(range(0, 3))
                                   .immediate()
                                   .close()
                                   .all()).containsExactly(0, 1, 2, 3);
    assertThat(JRoutineLoaderStream.withStreamAccept(2, range(1, 0))
                                   .immediate()
                                   .close()
                                   .all()).containsExactly(1, 0, 1, 0);
  }

  public void testStreamAcceptAbort() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    Channel<Integer, Integer> channel =
        JRoutineLoaderStream.withStreamAccept(range(0, 3)).immediate().call();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.getError()).isInstanceOf(AbortException.class);
    channel = JRoutineLoaderStream.withStreamAccept(2, range(1, 0)).immediate().call();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.getError()).isInstanceOf(AbortException.class);
  }

  @SuppressWarnings({"ConstantConditions", "ThrowableResultOfMethodCallIgnored"})
  public void testStreamAcceptError() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    assertThat(JRoutineLoaderStream.withStreamAccept(range(0, 3))
                                   .immediate()
                                   .call(31)
                                   .getError()
                                   .getCause()).isInstanceOf(IllegalStateException.class);
    assertThat(JRoutineLoaderStream.withStreamAccept(2, range(1, 0))
                                   .immediate()
                                   .call(-17)
                                   .getError()
                                   .getCause()).isInstanceOf(IllegalStateException.class);
  }

  public void testStreamGet() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    assertThat(JRoutineLoaderStream.withStreamGet(constant("test"))
                                   .immediate()
                                   .close()
                                   .all()).containsExactly("test");
    assertThat(JRoutineLoaderStream.withStreamGet(2, constant("test2"))
                                   .immediate()
                                   .close()
                                   .all()).containsExactly("test2", "test2");
  }

  public void testStreamGetAbort() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    Channel<String, String> channel =
        JRoutineLoaderStream.withStreamGet(constant("test")).immediate().immediate().call();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.getError()).isInstanceOf(AbortException.class);
    channel = JRoutineLoaderStream.withStreamGet(2, constant("test2")).immediate().call();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.getError()).isInstanceOf(AbortException.class);
  }

  @SuppressWarnings({"ConstantConditions", "ThrowableResultOfMethodCallIgnored"})
  public void testStreamGetError() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    assertThat(JRoutineLoaderStream.withStreamGet(constant("test"))
                                   .immediate()
                                   .call("test")
                                   .getError()
                                   .getCause()).isInstanceOf(IllegalStateException.class);
    assertThat(JRoutineLoaderStream.withStreamGet(2, constant("test2"))
                                   .immediate()
                                   .call("test")
                                   .getError()
                                   .getCause()).isInstanceOf(IllegalStateException.class);
  }

  public void testStreamOf() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    assertThat(JRoutineLoaderStream.withStreamOf("test")
                                   .on(loaderFrom(getActivity()))
                                   .close()
                                   .in(seconds(10))
                                   .all()).containsExactly("test");
    assertThat(JRoutineLoaderStream.withStreamOf("test1", "test2", "test3")
                                   .on(loaderFrom(getActivity()))
                                   .close()
                                   .in(seconds(10))
                                   .all()).containsExactly("test1", "test2", "test3");
    assertThat(JRoutineLoaderStream.withStreamOf(Arrays.asList("test1", "test2", "test3"))
                                   .on(loaderFrom(getActivity()))
                                   .close()
                                   .in(seconds(10))
                                   .all()).containsExactly("test1", "test2", "test3");
    assertThat(
        JRoutineLoaderStream.withStreamOf(JRoutineCore.of("test1", "test2", "test3").buildChannel())
                            .on(loaderFrom(getActivity()))
                            .close()
                            .in(seconds(10))
                            .all()).containsExactly("test1", "test2", "test3");
  }

  public void testStreamOfAbort() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    Channel<String, String> channel =
        JRoutineLoaderStream.withStreamOf("test").on(loaderFrom(getActivity())).call();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.in(seconds(10)).getError()).isInstanceOf(AbortException.class);
    channel = JRoutineLoaderStream.withStreamOf("test1", "test2", "test3")
                                  .on(loaderFrom(getActivity()))
                                  .call();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.in(seconds(10)).getError()).isInstanceOf(AbortException.class);
    channel = JRoutineLoaderStream.withStreamOf(Arrays.asList("test1", "test2", "test3"))
                                  .on(loaderFrom(getActivity()))
                                  .call();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.in(seconds(10)).getError()).isInstanceOf(AbortException.class);
    channel =
        JRoutineLoaderStream.withStreamOf(JRoutineCore.of("test1", "test2", "test3").buildChannel())
                            .on(loaderFrom(getActivity()))
                            .call();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.in(seconds(10)).getError()).isInstanceOf(AbortException.class);
  }

  @SuppressWarnings({"ConstantConditions", "ThrowableResultOfMethodCallIgnored"})
  public void testStreamOfError() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    assertThat(JRoutineLoaderStream.withStreamOf("test")
                                   .on(loaderFrom(getActivity()))
                                   .call("test")
                                   .in(seconds(10))
                                   .getError()
                                   .getCause()).isInstanceOf(IllegalStateException.class);
    assertThat(JRoutineLoaderStream.withStreamOf("test1", "test2", "test3")
                                   .on(loaderFrom(getActivity()))
                                   .call("test")
                                   .in(seconds(10))
                                   .getError()
                                   .getCause()).isInstanceOf(IllegalStateException.class);
    assertThat(JRoutineLoaderStream.withStreamOf(Arrays.asList("test1", "test2", "test3"))
                                   .on(loaderFrom(getActivity()))
                                   .call("test")
                                   .in(seconds(10))
                                   .getError()
                                   .getCause()).isInstanceOf(IllegalStateException.class);
    assertThat(
        JRoutineLoaderStream.withStreamOf(JRoutineCore.of("test1", "test2", "test3").buildChannel())
                            .on(loaderFrom(getActivity()))
                            .call("test")
                            .in(seconds(10))
                            .getError()
                            .getCause()).isInstanceOf(IllegalStateException.class);
    assertThat(JRoutineLoaderStream.withStreamOf(
        JRoutineCore.ofInputs().buildChannel().bind(new TemplateChannelConsumer<Object>() {}))
                                   .on(loaderFrom(getActivity()))
                                   .close()
                                   .in(seconds(10))
                                   .getError()
                                   .getCause()).isInstanceOf(IllegalStateException.class);
  }

  public void testTransform() {
    if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
      return;
    }

    assertThat(JRoutineLoaderStream //
        .<String>withStream().on(loaderFrom(getActivity()))
                             .liftWithConfig(transformBiFunction())
                             .call("test")
                             .in(seconds(10))
                             .next()).isEqualTo("TEST");
    assertThat(JRoutineLoaderStream //
        .<String>withStream().on(loaderFrom(getActivity()))
                             .lift(transformFunction())
                             .call("test")
                             .in(seconds(10))
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

    private static Channel<Object, String> apply(final Object o, @NotNull final Activity activity,
        @NotNull final Routine<Object, String> routine, @NotNull final int[] count) {
      return JRoutineLoaderStream.withStream()
                                 .on(loaderFrom(activity))
                                 .map(routine)
                                 .let(tryCatchAccept(
                                     new BiConsumer<RoutineException, Channel<String, ?>>() {

                                       public void accept(final RoutineException e,
                                           final Channel<String, ?> channel) {
                                         if (++count[0] < 3) {
                                           JRoutineLoaderStream.withStream()
                                                               .on(loaderFrom(activity))
                                                               .map(routine)
                                                               .let(tryCatchAccept(this))
                                                               .call(o)
                                                               .bind(channel);

                                         } else {
                                           throw e;
                                         }
                                       }
                                     }))
                                 .call(o);

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
