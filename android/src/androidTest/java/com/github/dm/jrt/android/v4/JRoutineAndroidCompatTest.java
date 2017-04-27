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

package com.github.dm.jrt.android.v4;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION_CODES;
import android.support.v4.app.FragmentActivity;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.WrapperRoutineBuilder.ProxyStrategyType;
import com.github.dm.jrt.android.channel.io.ParcelableByteChannel.ParcelableByteChunk;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.CacheStrategyType;
import com.github.dm.jrt.android.core.invocation.CallContextInvocation;
import com.github.dm.jrt.android.core.invocation.TargetInvocationFactory;
import com.github.dm.jrt.android.core.invocation.TemplateContextInvocation;
import com.github.dm.jrt.android.core.log.AndroidLog;
import com.github.dm.jrt.android.core.log.AndroidLogs;
import com.github.dm.jrt.android.core.service.InvocationService;
import com.github.dm.jrt.android.proxy.annotation.LoaderProxyCompat;
import com.github.dm.jrt.android.proxy.annotation.ServiceProxy;
import com.github.dm.jrt.android.reflect.ContextInvocationTarget;
import com.github.dm.jrt.android.test.R;
import com.github.dm.jrt.android.v4.stream.transform.LoaderTransformationsCompat;
import com.github.dm.jrt.channel.io.ByteChannel.ChunkInputStream;
import com.github.dm.jrt.channel.io.ByteChannel.ChunkOutputStream;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.TemplateChannelConsumer;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.function.util.BiConsumer;
import com.github.dm.jrt.function.util.Consumer;
import com.github.dm.jrt.function.util.Function;
import com.github.dm.jrt.function.util.Predicate;
import com.github.dm.jrt.function.util.Supplier;
import com.github.dm.jrt.operator.Operators;
import com.github.dm.jrt.reflect.annotation.Alias;
import com.github.dm.jrt.reflect.annotation.AsyncOutput;
import com.github.dm.jrt.reflect.annotation.OutputTimeout;

import org.assertj.core.data.Offset;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.android.core.ServiceContext.serviceFrom;
import static com.github.dm.jrt.android.core.invocation.ContextInvocationFactory.factoryOf;
import static com.github.dm.jrt.android.reflect.ContextInvocationTarget.classOfType;
import static com.github.dm.jrt.android.reflect.ContextInvocationTarget.instanceOf;
import static com.github.dm.jrt.android.v4.core.LoaderContextCompat.loaderFrom;
import static com.github.dm.jrt.core.util.ClassToken.tokenOf;
import static com.github.dm.jrt.core.util.DurationMeasure.seconds;
import static com.github.dm.jrt.function.util.SupplierDecorator.constant;
import static com.github.dm.jrt.operator.Operators.appendAccept;
import static com.github.dm.jrt.operator.sequence.Sequences.range;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Android facade unit tests.
 * <p>
 * Created by davide-maestroni on 02/29/2016.
 */
@TargetApi(VERSION_CODES.FROYO)
public class JRoutineAndroidCompatTest extends ActivityInstrumentationTestCase2<TestActivity> {

  public JRoutineAndroidCompatTest() {
    super(TestActivity.class);
  }

  private static void testCallFunction(@NotNull final FragmentActivity activity) {
    final Routine<String, String> routine =
        JRoutineAndroidCompat.on(activity).withCall(new Function<List<String>, String>() {

          public String apply(final List<String> strings) {

            final StringBuilder builder = new StringBuilder();
            for (final String string : strings) {
              builder.append(string);
            }

            return builder.toString();
          }
        }).buildRoutine();
    assertThat(routine.invoke().pass("test", "1").close().in(seconds(10)).all()).containsOnly(
        "test1");
  }

  private static void testConsumerCommand(@NotNull final FragmentActivity activity) {
    final Routine<Void, String> routine = //
        JRoutineAndroidCompat.on(activity).withCommandConsumer(new Consumer<Channel<String, ?>>() {

          public void accept(final Channel<String, ?> result) {

            result.pass("test", "1");
          }
        }).buildRoutine();
    assertThat(routine.invoke().close().in(seconds(10)).all()).containsOnly("test", "1");
  }

  private static void testConsumerFunction(@NotNull final FragmentActivity activity) {
    final Routine<String, String> routine = //
        JRoutineAndroidCompat.on(activity)
                             .withCallConsumer(new BiConsumer<List<String>, Channel<String, ?>>() {

                               public void accept(final List<String> strings,
                                   final Channel<String, ?> result) {

                                 final StringBuilder builder = new StringBuilder();
                                 for (final String string : strings) {
                                   builder.append(string);
                                 }

                                 result.pass(builder.toString());
                               }
                             })
                             .buildRoutine();
    assertThat(routine.invoke().pass("test", "1").close().in(seconds(10)).all()).containsOnly(
        "test1");
  }

  private static void testConsumerMapping(@NotNull final FragmentActivity activity) {
    final Routine<Object, String> routine = //
        JRoutineAndroidCompat.on(activity)
                             .withMappingConsumer(new BiConsumer<Object, Channel<String, ?>>() {

                               public void accept(final Object o, final Channel<String, ?> result) {

                                 result.pass(o.toString());
                               }
                             })
                             .buildRoutine();
    assertThat(routine.invoke().pass("test", 1).close().in(seconds(10)).all()).containsOnly("test",
        "1");
  }

  private static void testFunctionMapping(@NotNull final FragmentActivity activity) {
    final Routine<Object, String> routine =
        JRoutineAndroidCompat.on(activity).withMapping(new Function<Object, String>() {

          public String apply(final Object o) {

            return o.toString();
          }
        }).buildRoutine();
    assertThat(routine.invoke().pass("test", 1).close().in(seconds(10)).all()).containsOnly("test",
        "1");
  }

  private static void testPredicateFilter(@NotNull final FragmentActivity activity) {
    final Routine<String, String> routine =
        JRoutineAndroidCompat.on(activity).withFilter(new Predicate<String>() {

          public boolean test(final String s) {

            return s.length() > 1;
          }
        }).buildRoutine();
    assertThat(routine.invoke().pass("test", "1").close().in(seconds(10)).all()).containsOnly(
        "test");
  }

  private static void testStream(@NotNull final FragmentActivity activity) {
    assertThat(JRoutineAndroidCompat.<Integer>withStream().map(appendAccept(range(1, 1000)))
                                                          .map(new Function<Number, Double>() {

                                                            public Double apply(
                                                                final Number number) {
                                                              final double value =
                                                                  number.doubleValue();
                                                              return Math.sqrt(value);
                                                            }
                                                          })
                                                          .sync()
                                                          .map(Operators.average(Double.class))
                                                          .lift(
                                                              LoaderTransformationsCompat
                                                                  .<Integer, Double>runOn(
                                                                  loaderFrom(
                                                                      activity)).buildFunction())
                                                          .invoke()
                                                          .close()
                                                          .in(seconds(10))
                                                          .next()).isCloseTo(21,
        Offset.offset(0.1));
  }

  private static void testSupplierCommand(@NotNull final FragmentActivity activity) {
    final Routine<Void, String> routine =
        JRoutineAndroidCompat.on(activity).withCommand(new Supplier<String>() {

          public String get() {

            return "test";
          }
        }).buildRoutine();
    assertThat(routine.invoke().close().in(seconds(10)).all()).containsOnly("test");
  }

  private static void testSupplierContextFactory(@NotNull final FragmentActivity activity) {
    final Routine<String, String> routine =
        JRoutineAndroidCompat.on(activity).withContextFactory(new Supplier<PassString>() {

          public PassString get() {

            return new PassString();
          }
        }).buildRoutine();
    assertThat(routine.invoke().pass("TEST").close().in(seconds(10)).all()).containsOnly("TEST");
  }

  private static void testSupplierFactory(@NotNull final FragmentActivity activity) {
    final Routine<String, String> routine =
        JRoutineAndroidCompat.on(activity).withFactory(new Supplier<PassString>() {

          public PassString get() {

            return new PassString();
          }
        }).buildRoutine();
    assertThat(routine.invoke().pass("TEST").close().in(seconds(10)).all()).containsOnly("TEST");
  }

  public void testCallFunction() {
    testCallFunction(getActivity());
  }

  public void testConcatReadOutput() throws IOException {
    final Channel<ParcelableByteChunk, ParcelableByteChunk> channel =
        JRoutineAndroidCompat.<ParcelableByteChunk>ofData().buildChannel();
    final ChunkOutputStream stream = JRoutineAndroidCompat.withOutput(channel)
                                                          .chunkStreamConfiguration()
                                                          .withChunkSize(3)
                                                          .apply()
                                                          .buildOutputStream();
    stream.write(new byte[]{31, 17, (byte) 155, 13});
    stream.flush();
    final ChunkInputStream inputStream =
        JRoutineAndroidCompat.getInputStream(channel.next(), channel.next());
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    assertThat(inputStream.read(outputStream)).isEqualTo(3);
    assertThat(outputStream.size()).isEqualTo(3);
    assertThat(outputStream.toByteArray()).containsExactly((byte) 31, (byte) 17, (byte) 155);
    assertThat(inputStream.read(outputStream)).isEqualTo(1);
    assertThat(outputStream.size()).isEqualTo(4);
    assertThat(outputStream.toByteArray()).containsExactly((byte) 31, (byte) 17, (byte) 155,
        (byte) 13);
    assertThat(inputStream.read(outputStream)).isEqualTo(-1);
    assertThat(outputStream.size()).isEqualTo(4);
    assertThat(outputStream.toByteArray()).containsExactly((byte) 31, (byte) 17, (byte) 155,
        (byte) 13);
    assertThat(inputStream.read(outputStream)).isEqualTo(-1);
  }

  public void testConcatReadOutput2() throws IOException {
    final Channel<ParcelableByteChunk, ParcelableByteChunk> channel =
        JRoutineAndroidCompat.<ParcelableByteChunk>ofData().buildChannel();
    final ChunkOutputStream stream = JRoutineAndroidCompat.withOutput(channel)
                                                          .chunkStreamConfiguration()
                                                          .withChunkSize(3)
                                                          .apply()
                                                          .buildOutputStream();
    stream.write(new byte[]{31, 17, (byte) 155, 13});
    stream.flush();
    final ChunkInputStream inputStream =
        JRoutineAndroidCompat.getInputStream(channel.eventuallyContinue().all());
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    assertThat(inputStream.read(outputStream)).isEqualTo(3);
    assertThat(outputStream.size()).isEqualTo(3);
    assertThat(outputStream.toByteArray()).containsExactly((byte) 31, (byte) 17, (byte) 155);
    assertThat(inputStream.read(outputStream)).isEqualTo(1);
    assertThat(outputStream.size()).isEqualTo(4);
    assertThat(outputStream.toByteArray()).containsExactly((byte) 31, (byte) 17, (byte) 155,
        (byte) 13);
    assertThat(inputStream.read(outputStream)).isEqualTo(-1);
    assertThat(outputStream.size()).isEqualTo(4);
    assertThat(outputStream.toByteArray()).containsExactly((byte) 31, (byte) 17, (byte) 155,
        (byte) 13);
    assertThat(inputStream.read(outputStream)).isEqualTo(-1);
  }

  public void testConstructor() {
    boolean failed = false;
    try {
      new JRoutineAndroidCompat();
      failed = true;

    } catch (final Throwable ignored) {
    }

    assertThat(failed).isFalse();
  }

  public void testConsumerCommand() {
    testConsumerCommand(getActivity());
  }

  public void testConsumerFunction() {
    testConsumerFunction(getActivity());
  }

  public void testConsumerMapping() {
    testConsumerMapping(getActivity());
  }

  public void testFunctionMapping() {
    testFunctionMapping(getActivity());
  }

  public void testIOChannel() {
    assertThat(JRoutineAndroidCompat.of("test").buildChannel().next()).isEqualTo("test");
  }

  public void testLoader() {
    final ClassToken<Join<String>> token = new ClassToken<Join<String>>() {};
    assertThat(JRoutineAndroidCompat.on(loaderFrom(getActivity()))
                                    .with(factoryOf(token))
                                    .invoke()
                                    .pass("test")
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test");
    assertThat(JRoutineAndroidCompat.on(getActivity())
                                    .with(factoryOf(token))
                                    .invoke()
                                    .pass("test")
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test");
    assertThat(JRoutineAndroidCompat.on(getActivity(), getActivity())
                                    .with(factoryOf(token))
                                    .invoke()
                                    .pass("test")
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test");
    final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                              .findFragmentById(R.id.test_fragment);
    assertThat(JRoutineAndroidCompat.on(fragment)
                                    .with(factoryOf(token))
                                    .invoke()
                                    .pass("test")
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test");
    assertThat(JRoutineAndroidCompat.on(fragment, getActivity())
                                    .with(factoryOf(token))
                                    .invoke()
                                    .pass("test")
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test");
  }

  public void testLoaderClass() throws NoSuchMethodException {
    new TestClass("test");
    assertThat(JRoutineAndroidCompat.on(getActivity())
                                    .withClassOfType(TestClass.class)
                                    .method("getStringUp")
                                    .invoke()
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("TEST");
    assertThat(JRoutineAndroidCompat.on(getActivity())
                                    .withClassOfType(TestClass.class)
                                    .method(TestClass.class.getMethod("getStringUp"))
                                    .invoke()
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("TEST");
    assertThat(JRoutineAndroidCompat.on(getActivity())
                                    .with(classOfType(TestClass.class))
                                    .method("TEST")
                                    .invoke()
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("TEST");
  }

  public void testLoaderId() throws NoSuchMethodException {
    new TestClass("test");
    assertThat(JRoutineAndroidCompat.on(getActivity())
                                    .withInstanceOf(TestClass.class)
                                    .loaderConfiguration()
                                    .withLoaderId(33)
                                    .withCacheStrategy(CacheStrategyType.CACHE)
                                    .apply()
                                    .method("getStringLow")
                                    .invoke()
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test");
    assertThat(JRoutineAndroidCompat.on(getActivity())
                                    .withId(33)
                                    .buildChannel()
                                    .in(seconds(10))
                                    .all()).containsExactly("test");
  }

  public void testLoaderInstance() throws NoSuchMethodException {
    assertThat(JRoutineAndroidCompat.on(getActivity())
                                    .withInstanceOf(TestClass.class, "TEST")
                                    .method(TestClass.class.getMethod("getStringLow"))
                                    .invoke()
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test");
    assertThat(JRoutineAndroidCompat.on(getActivity())
                                    .withInstanceOf(TestClass.class)
                                    .method("getStringLow")
                                    .invoke()
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test");
    assertThat(JRoutineAndroidCompat.on(getActivity())
                                    .with(instanceOf(TestClass.class))
                                    .method("test")
                                    .invoke()
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test");
  }

  public void testLoaderInvocation() {
    final ClassToken<JoinString> token = new ClassToken<JoinString>() {};
    assertThat(JRoutineAndroidCompat.on(loaderFrom(getActivity()))
                                    .with(token)
                                    .invoke()
                                    .pass("test1", "test2")
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test1,test2");
    assertThat(JRoutineAndroidCompat.on(loaderFrom(getActivity()))
                                    .with(token, ";")
                                    .invoke()
                                    .pass("test1", "test2")
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test1;test2");
    assertThat(JRoutineAndroidCompat.on(loaderFrom(getActivity()))
                                    .with(JoinString.class)
                                    .invoke()
                                    .pass("test1", "test2")
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test1,test2");
    assertThat(JRoutineAndroidCompat.on(loaderFrom(getActivity()))
                                    .with(JoinString.class, " ")
                                    .invoke()
                                    .pass("test1", "test2")
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test1 test2");
    assertThat(JRoutineAndroidCompat.on(loaderFrom(getActivity()))
                                    .with(new JoinString())
                                    .invoke()
                                    .pass("test1", "test2")
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test1,test2");
    assertThat(JRoutineAndroidCompat.on(loaderFrom(getActivity()))
                                    .with(new JoinString(), " ")
                                    .invoke()
                                    .pass("test1", "test2")
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test1 test2");
  }

  public void testLoaderProxy() {
    new TestClass("TEST");
    assertThat(JRoutineAndroidCompat.on(getActivity())
                                    .withInstanceOf(TestClass.class)
                                    .buildProxy(TestAnnotatedProxy.class)
                                    .getStringLow()
                                    .all()).containsExactly("test");
    assertThat(JRoutineAndroidCompat.on(getActivity())
                                    .withInstanceOf(TestClass.class)
                                    .buildProxy(tokenOf(TestAnnotatedProxy.class))
                                    .getStringLow()
                                    .all()).containsExactly("test");
    assertThat(JRoutineAndroidCompat.on(getActivity())
                                    .withInstanceOf(TestClass.class)
                                    .withStrategy(ProxyStrategyType.REFLECTION)
                                    .buildProxy(TestProxy.class)
                                    .getStringLow()
                                    .all()).containsExactly("test");
    assertThat(JRoutineAndroidCompat.on(getActivity())
                                    .withInstanceOf(TestClass.class)
                                    .withStrategy(ProxyStrategyType.REFLECTION)
                                    .buildProxy(tokenOf(TestProxy.class))
                                    .getStringLow()
                                    .all()).containsExactly("test");
    assertThat(JRoutineAndroidCompat.on(getActivity())
                                    .withInstanceOf(TestClass.class)
                                    .withStrategy(ProxyStrategyType.CODE_GENERATION)
                                    .buildProxy(TestAnnotatedProxy.class)
                                    .getStringLow()
                                    .all()).containsExactly("test");
    assertThat(JRoutineAndroidCompat.on(getActivity())
                                    .withInstanceOf(TestClass.class)
                                    .withStrategy(ProxyStrategyType.CODE_GENERATION)
                                    .buildProxy(tokenOf(TestAnnotatedProxy.class))
                                    .getStringLow()
                                    .all()).containsExactly("test");
  }

  public void testLoaderProxyConfiguration() {
    new TestClass("TEST");
    assertThat(JRoutineAndroidCompat.on(getActivity())
                                    .withInstanceOf(TestClass.class)
                                    .withInvocation()
                                    .withLog(AndroidLogs.androidLog())
                                    .configured()
                                    .withWrapper()
                                    .withSharedFields()
                                    .configured()
                                    .loaderConfiguration()
                                    .withInvocationId(11)
                                    .apply()
                                    .buildProxy(TestAnnotatedProxy.class)
                                    .getStringLow()
                                    .all()).containsExactly("test");
  }

  @SuppressWarnings("ConstantConditions")
  public void testLoaderProxyError() {
    try {
      JRoutineAndroidCompat.on(getActivity()).with((ContextInvocationTarget<?>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  public void testOf() {
    final Channel<?, Integer> channel = JRoutineAndroidCompat.of(2).buildChannel();
    assertThat(channel.isOpen()).isFalse();
    assertThat(channel.in(seconds(1)).all()).containsExactly(2);
    assertThat(JRoutineAndroidCompat.of().buildChannel().in(seconds(1)).all()).isEmpty();
    assertThat(
        JRoutineAndroidCompat.of(-11, 73).buildChannel().in(seconds(1)).all()).containsExactly(-11,
        73);
    assertThat(JRoutineAndroidCompat.of(Arrays.asList(3, 12, -7))
                                    .buildChannel()
                                    .in(seconds(1))
                                    .all()).containsExactly(3, 12, -7);
    assertThat(JRoutineAndroidCompat.of((Object[]) null).buildChannel().all()).isEmpty();
    assertThat(JRoutineAndroidCompat.of((List<Object>) null).buildChannel().all()).isEmpty();
  }

  public void testPredicateFilter() {
    testPredicateFilter(getActivity());
  }

  public void testReadAll() throws IOException {
    final Channel<ParcelableByteChunk, ParcelableByteChunk> channel =
        JRoutineAndroidCompat.<ParcelableByteChunk>ofData().buildChannel();
    final ChunkOutputStream stream = JRoutineAndroidCompat.withOutput(channel).buildOutputStream();
    stream.write(new byte[]{31, 17, (byte) 155, 13});
    stream.flush();
    final ChunkInputStream inputStream = JRoutineAndroidCompat.getInputStream(channel.next());
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    assertThat(inputStream.readAll(outputStream)).isEqualTo(4);
    assertThat(outputStream.size()).isEqualTo(4);
    assertThat(outputStream.toByteArray()).containsExactly((byte) 31, (byte) 17, (byte) 155,
        (byte) 13);
    assertThat(inputStream.read(outputStream)).isEqualTo(-1);
    assertThat(outputStream.size()).isEqualTo(4);
    assertThat(outputStream.toByteArray()).containsExactly((byte) 31, (byte) 17, (byte) 155,
        (byte) 13);
  }

  public void testService() {
    final ClassToken<Pass<String>> token = new ClassToken<Pass<String>>() {};
    assertThat(JRoutineAndroidCompat.on(serviceFrom(getActivity()))
                                    .with(TargetInvocationFactory.factoryOf(token))
                                    .invoke()
                                    .pass("test")
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test");
    assertThat(JRoutineAndroidCompat.on((Context) getActivity())
                                    .with(TargetInvocationFactory.factoryOf(token))
                                    .invoke()
                                    .pass("test")
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test");
    assertThat(JRoutineAndroidCompat.on(getActivity(), InvocationService.class)
                                    .with(TargetInvocationFactory.factoryOf(token))
                                    .invoke()
                                    .pass("test")
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test");
    assertThat(
        JRoutineAndroidCompat.on(getActivity(), new Intent(getActivity(), InvocationService.class))
                             .with(TargetInvocationFactory.factoryOf(token))
                             .invoke()
                             .pass("test")
                             .close()
                             .in(seconds(10))
                             .all()).containsExactly("test");
  }

  public void testServiceClass() throws NoSuchMethodException {
    new TestClass("test");
    assertThat(JRoutineAndroidCompat.on((Context) getActivity())
                                    .withClassOfType(TestClass.class)
                                    .method("getStringUp")
                                    .invoke()
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("TEST");
    assertThat(JRoutineAndroidCompat.on((Context) getActivity())
                                    .withClassOfType(TestClass.class)
                                    .method(TestClass.class.getMethod("getStringUp"))
                                    .invoke()
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("TEST");
    assertThat(JRoutineAndroidCompat.on((Context) getActivity())
                                    .with(classOfType(TestClass.class))
                                    .method("TEST")
                                    .invoke()
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("TEST");
  }

  public void testServiceInstance() throws NoSuchMethodException {
    assertThat(JRoutineAndroidCompat.on((Context) getActivity())
                                    .withInstanceOf(TestClass.class, "TEST")
                                    .method(TestClass.class.getMethod("getStringLow"))
                                    .invoke()
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test");
    assertThat(JRoutineAndroidCompat.on((Context) getActivity())
                                    .withInstanceOf(TestClass.class)
                                    .method("getStringLow")
                                    .invoke()
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test");
    assertThat(JRoutineAndroidCompat.on((Context) getActivity())
                                    .with(instanceOf(TestClass.class))
                                    .method("test")
                                    .invoke()
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test");
  }

  public void testServiceInvocation() {
    final ClassToken<Pass<String>> token = new ClassToken<Pass<String>>() {};
    assertThat(JRoutineAndroidCompat.on(serviceFrom(getActivity()))
                                    .with(token)
                                    .invoke()
                                    .pass("test")
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test");
    assertThat(JRoutineAndroidCompat.on(serviceFrom(getActivity()))
                                    .with(token, 2)
                                    .invoke()
                                    .pass("test")
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test", "test");
    assertThat(JRoutineAndroidCompat.on(serviceFrom(getActivity()))
                                    .with(PassString.class)
                                    .invoke()
                                    .pass("test")
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test");
    assertThat(JRoutineAndroidCompat.on(serviceFrom(getActivity()))
                                    .with(PassString.class, 3)
                                    .invoke()
                                    .pass("test")
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test", "test", "test");
    assertThat(JRoutineAndroidCompat.on(serviceFrom(getActivity()))
                                    .with(new Pass<String>())
                                    .invoke()
                                    .pass("test")
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test");
    assertThat(JRoutineAndroidCompat.on(serviceFrom(getActivity()))
                                    .with(new Pass<String>(), 2)
                                    .invoke()
                                    .pass("test")
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test", "test");
  }

  public void testServiceProxy() {
    new TestClass("TEST");
    assertThat(JRoutineAndroidCompat.on((Context) getActivity())
                                    .withInstanceOf(TestClass.class)
                                    .buildProxy(TestAnnotatedProxy.class)
                                    .getStringLow()
                                    .all()).containsExactly("test");
    assertThat(JRoutineAndroidCompat.on((Context) getActivity())
                                    .withInstanceOf(TestClass.class)
                                    .buildProxy(tokenOf(TestAnnotatedProxy.class))
                                    .getStringLow()
                                    .all()).containsExactly("test");
    assertThat(JRoutineAndroidCompat.on((Context) getActivity())
                                    .withInstanceOf(TestClass.class)
                                    .withStrategy(ProxyStrategyType.REFLECTION)
                                    .buildProxy(TestProxy.class)
                                    .getStringLow()
                                    .all()).containsExactly("test");
    assertThat(JRoutineAndroidCompat.on((Context) getActivity())
                                    .withInstanceOf(TestClass.class)
                                    .withStrategy(ProxyStrategyType.REFLECTION)
                                    .buildProxy(tokenOf(TestProxy.class))
                                    .getStringLow()
                                    .all()).containsExactly("test");
    assertThat(JRoutineAndroidCompat.on((Context) getActivity())
                                    .withInstanceOf(TestClass.class)
                                    .withStrategy(ProxyStrategyType.CODE_GENERATION)
                                    .buildProxy(TestAnnotatedProxy.class)
                                    .getStringLow()
                                    .all()).containsExactly("test");
    assertThat(JRoutineAndroidCompat.on((Context) getActivity())
                                    .withInstanceOf(TestClass.class)
                                    .withStrategy(ProxyStrategyType.CODE_GENERATION)
                                    .buildProxy(tokenOf(TestAnnotatedProxy.class))
                                    .getStringLow()
                                    .all()).containsExactly("test");
  }

  public void testServiceProxyConfiguration() {
    new TestClass("TEST");
    assertThat(JRoutineAndroidCompat.on((Context) getActivity())
                                    .withInstanceOf(TestClass.class)
                                    .withInvocation()
                                    .withLog(AndroidLogs.androidLog())
                                    .configured()
                                    .withWrapper()
                                    .withSharedFields()
                                    .configured()
                                    .serviceConfiguration()
                                    .withLogClass(AndroidLog.class)
                                    .apply()
                                    .buildProxy(TestAnnotatedProxy.class)
                                    .getStringLow()
                                    .all()).containsExactly("test");
  }

  @SuppressWarnings("ConstantConditions")
  public void testServiceProxyError() {
    try {
      JRoutineAndroidCompat.on(getActivity()).with((ContextInvocationTarget<?>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  public void testStream() {
    testStream(getActivity());
  }

  public void testStreamAccept() {
    assertThat(JRoutineAndroidCompat.withStreamAccept(range(0, 3))
                                    .immediate()
                                    .invoke()
                                    .close()
                                    .all()).containsExactly(0, 1, 2, 3);
    assertThat(JRoutineAndroidCompat.withStreamAccept(2, range(1, 0))
                                    .immediate()
                                    .invoke()
                                    .close()
                                    .all()).containsExactly(1, 0, 1, 0);
  }

  public void testStreamAcceptAbort() {
    Channel<Integer, Integer> channel =
        JRoutineAndroidCompat.withStreamAccept(range(0, 3)).immediate().invoke();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.getError()).isInstanceOf(AbortException.class);
    channel = JRoutineAndroidCompat.withStreamAccept(2, range(1, 0)).immediate().invoke();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.getError()).isInstanceOf(AbortException.class);
  }

  @SuppressWarnings({"ConstantConditions", "ThrowableResultOfMethodCallIgnored"})
  public void testStreamAcceptError() {
    assertThat(JRoutineAndroidCompat.withStreamAccept(range(0, 3))
                                    .immediate()
                                    .invoke()
                                    .pass(31)
                                    .close()
                                    .getError()
                                    .getCause()).isInstanceOf(IllegalStateException.class);
    assertThat(JRoutineAndroidCompat.withStreamAccept(2, range(1, 0))
                                    .immediate()
                                    .invoke()
                                    .pass(-17)
                                    .close()
                                    .getError()
                                    .getCause()).isInstanceOf(IllegalStateException.class);
  }

  public void testStreamGet() {
    assertThat(JRoutineAndroidCompat.withStreamGet(constant("test"))
                                    .immediate()
                                    .invoke()
                                    .close()
                                    .all()).containsExactly("test");
    assertThat(JRoutineAndroidCompat.withStreamGet(2, constant("test2"))
                                    .immediate()
                                    .invoke()
                                    .close()
                                    .all()).containsExactly("test2", "test2");
  }

  public void testStreamGetAbort() {
    Channel<String, String> channel =
        JRoutineAndroidCompat.withStreamGet(constant("test")).immediate().immediate().invoke();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.getError()).isInstanceOf(AbortException.class);
    channel = JRoutineAndroidCompat.withStreamGet(2, constant("test2")).immediate().invoke();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.getError()).isInstanceOf(AbortException.class);
  }

  @SuppressWarnings({"ConstantConditions", "ThrowableResultOfMethodCallIgnored"})
  public void testStreamGetError() {
    assertThat(JRoutineAndroidCompat.withStreamGet(constant("test"))
                                    .immediate()
                                    .invoke()
                                    .pass("test")
                                    .close()
                                    .getError()
                                    .getCause()).isInstanceOf(IllegalStateException.class);
    assertThat(JRoutineAndroidCompat.withStreamGet(2, constant("test2"))
                                    .immediate()
                                    .invoke()
                                    .pass("test")
                                    .close()
                                    .getError()
                                    .getCause()).isInstanceOf(IllegalStateException.class);
  }

  public void testStreamOf() {
    assertThat(JRoutineAndroidCompat.withStreamOf("test")
                                    .lift(LoaderTransformationsCompat.<String, String>runOn(
                                        loaderFrom(getActivity())).buildFunction())
                                    .invoke()
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test");
    assertThat(JRoutineAndroidCompat.withStreamOf("test1", "test2", "test3")
                                    .lift(LoaderTransformationsCompat.<String, String>runOn(
                                        loaderFrom(getActivity())).buildFunction())
                                    .invoke()
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test1", "test2", "test3");
    assertThat(JRoutineAndroidCompat.withStreamOf(Arrays.asList("test1", "test2", "test3"))
                                    .lift(LoaderTransformationsCompat.<String, String>runOn(
                                        loaderFrom(getActivity())).buildFunction())
                                    .invoke()
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test1", "test2", "test3");
    assertThat(JRoutineAndroidCompat.withStreamOf(
        JRoutineAndroidCompat.of("test1", "test2", "test3").buildChannel())
                                    .lift(LoaderTransformationsCompat.<String, String>runOn(
                                        loaderFrom(getActivity())).buildFunction())
                                    .invoke()
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test1", "test2", "test3");
  }

  public void testStreamOfAbort() {
    Channel<String, String> channel = JRoutineAndroidCompat.withStreamOf("test")
                                                           .lift(
                                                               LoaderTransformationsCompat
                                                                   .<String, String>runOn(
                                                                   loaderFrom(
                                                                       getActivity()))
                                                                   .buildFunction())
                                                           .invoke();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.in(seconds(10)).getError()).isInstanceOf(AbortException.class);
    channel = JRoutineAndroidCompat.withStreamOf("test1", "test2", "test3")
                                   .lift(LoaderTransformationsCompat.<String, String>runOn(
                                       loaderFrom(getActivity())).buildFunction())
                                   .invoke();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.in(seconds(10)).getError()).isInstanceOf(AbortException.class);
    channel = JRoutineAndroidCompat.withStreamOf(Arrays.asList("test1", "test2", "test3"))
                                   .lift(LoaderTransformationsCompat.<String, String>runOn(
                                       loaderFrom(getActivity())).buildFunction())
                                   .invoke();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.in(seconds(10)).getError()).isInstanceOf(AbortException.class);
    channel = JRoutineAndroidCompat.withStreamOf(
        JRoutineAndroidCompat.of("test1", "test2", "test3").buildChannel())
                                   .lift(LoaderTransformationsCompat.<String, String>runOn(
                                       loaderFrom(getActivity())).buildFunction())
                                   .invoke();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.in(seconds(10)).getError()).isInstanceOf(AbortException.class);
  }

  @SuppressWarnings({"ConstantConditions", "ThrowableResultOfMethodCallIgnored"})
  public void testStreamOfError() {
    assertThat(JRoutineAndroidCompat.withStreamOf("test")
                                    .lift(LoaderTransformationsCompat.<String, String>runOn(
                                        loaderFrom(getActivity())).buildFunction())
                                    .invoke()
                                    .pass("test")
                                    .close()
                                    .in(seconds(10))
                                    .getError()
                                    .getCause()).isInstanceOf(IllegalStateException.class);
    assertThat(JRoutineAndroidCompat.withStreamOf("test1", "test2", "test3")
                                    .lift(LoaderTransformationsCompat.<String, String>runOn(
                                        loaderFrom(getActivity())).buildFunction())
                                    .invoke()
                                    .pass("test")
                                    .close()
                                    .in(seconds(10))
                                    .getError()
                                    .getCause()).isInstanceOf(IllegalStateException.class);
    assertThat(JRoutineAndroidCompat.withStreamOf(Arrays.asList("test1", "test2", "test3"))
                                    .lift(LoaderTransformationsCompat.<String, String>runOn(
                                        loaderFrom(getActivity())).buildFunction())
                                    .invoke()
                                    .pass("test")
                                    .close()
                                    .in(seconds(10))
                                    .getError()
                                    .getCause()).isInstanceOf(IllegalStateException.class);
    assertThat(JRoutineAndroidCompat.withStreamOf(
        JRoutineAndroidCompat.of("test1", "test2", "test3").buildChannel())
                                    .lift(LoaderTransformationsCompat.<String, String>runOn(
                                        loaderFrom(getActivity())).buildFunction())
                                    .invoke()
                                    .pass("test")
                                    .close()
                                    .in(seconds(10))
                                    .getError()
                                    .getCause()).isInstanceOf(IllegalStateException.class);
    assertThat(JRoutineAndroidCompat.withStreamOf(JRoutineAndroidCompat.ofData()
                                                                       .buildChannel()
                                                                       .consume(
                                                                           new TemplateChannelConsumer<Object>() {}))
                                    .lift(
                                        LoaderTransformationsCompat.runOn(loaderFrom(getActivity()))
                                                                   .buildFunction())
                                    .invoke()
                                    .close()
                                    .in(seconds(10))
                                    .getError()
                                    .getCause()).isInstanceOf(IllegalStateException.class);
  }

  public void testSupplierCommand() {
    testSupplierCommand(getActivity());
  }

  public void testSupplierContextFactory() {
    testSupplierContextFactory(getActivity());
  }

  public void testSupplierFactory() {
    testSupplierFactory(getActivity());
  }

  @ServiceProxy(TestClass.class)
  @LoaderProxyCompat(TestClass.class)
  public interface TestAnnotatedProxy extends TestProxy {}

  public interface TestProxy {

    @AsyncOutput
    @OutputTimeout(value = 10, unit = TimeUnit.SECONDS)
    Channel<?, String> getStringLow();
  }

  public static class Join<DATA> extends CallContextInvocation<DATA, String> {

    private final String mSeparator;

    public Join() {
      this(",");
    }

    public Join(final String separator) {
      mSeparator = separator;
    }

    @Override
    protected void onCall(@NotNull final List<? extends DATA> inputs,
        @NotNull final Channel<String, ?> result) throws Exception {
      final String separator = mSeparator;
      final StringBuilder builder = new StringBuilder();
      for (final DATA input : inputs) {
        if (builder.length() > 0) {
          builder.append(separator);
        }

        builder.append(input.toString());
      }

      result.pass(builder.toString());
    }
  }

  @SuppressWarnings("unused")
  public static class JoinString extends Join<String> {

    public JoinString() {
    }

    public JoinString(final String separator) {
      super(separator);
    }
  }

  public static class Pass<DATA> extends TemplateContextInvocation<DATA, DATA> {

    private final int mCount;

    public Pass() {
      this(1);
    }

    public Pass(final int count) {
      mCount = count;
    }

    @Override
    public void onInput(final DATA input, @NotNull final Channel<DATA, ?> result) {
      final int count = mCount;
      for (int i = 0; i < count; i++) {
        result.pass(input);
      }
    }

    @Override
    public boolean onRecycle() {
      return true;
    }
  }

  @SuppressWarnings("unused")
  public static class PassString extends Pass<String> {

    public PassString() {
    }

    public PassString(final int count) {
      super(count);
    }
  }

  @SuppressWarnings("unused")
  public static class TestClass {

    private static String sText;

    public TestClass() {
      this("test");
    }

    public TestClass(final String text) {
      sText = text;
    }

    @Alias("TEST")
    public static String getStringUp() {
      return sText.toUpperCase();
    }

    @Alias("test")
    public String getStringLow() {
      return sText.toLowerCase();
    }
  }
}
