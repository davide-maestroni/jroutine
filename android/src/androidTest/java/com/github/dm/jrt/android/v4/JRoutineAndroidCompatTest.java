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
import android.os.Build.VERSION_CODES;
import android.support.v4.app.FragmentActivity;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.WrapperRoutineBuilder.ProxyStrategyType;
import com.github.dm.jrt.android.channel.io.ParcelableByteChannel.ParcelableByteChunk;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.CacheStrategyType;
import com.github.dm.jrt.android.core.invocation.CallContextInvocation;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.core.invocation.InvocationFactoryReference;
import com.github.dm.jrt.android.core.invocation.TemplateContextInvocation;
import com.github.dm.jrt.android.core.log.AndroidLog;
import com.github.dm.jrt.android.core.log.AndroidLogs;
import com.github.dm.jrt.android.core.routine.LoaderRoutine;
import com.github.dm.jrt.android.proxy.annotation.LoaderProxyCompat;
import com.github.dm.jrt.android.proxy.annotation.ServiceProxy;
import com.github.dm.jrt.android.test.R;
import com.github.dm.jrt.channel.io.ByteChannel.ByteChunkInputStream;
import com.github.dm.jrt.channel.io.ByteChannel.ByteChunkOutputStream;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.executor.ScheduledExecutors;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.function.util.BiConsumer;
import com.github.dm.jrt.function.util.BiFunction;
import com.github.dm.jrt.function.util.Function;
import com.github.dm.jrt.function.util.Supplier;
import com.github.dm.jrt.operator.JRoutineOperators;
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

import static com.github.dm.jrt.android.core.ServiceSource.serviceOf;
import static com.github.dm.jrt.android.core.invocation.ContextInvocationFactory.factoryOf;
import static com.github.dm.jrt.android.reflect.ContextInvocationTarget.classOfType;
import static com.github.dm.jrt.android.reflect.ContextInvocationTarget.instanceOf;
import static com.github.dm.jrt.android.v4.core.LoaderSourceCompat.loaderOf;
import static com.github.dm.jrt.core.util.ClassToken.tokenOf;
import static com.github.dm.jrt.core.util.DurationMeasure.seconds;
import static com.github.dm.jrt.operator.sequence.Sequence.range;
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

  private static void testIncrement(final FragmentActivity activity) {
    final LoaderRoutine<Integer, Integer> routine =
        JRoutineAndroidCompat.<Integer, Integer>statelessRoutineOn(loaderOf(activity)).onNext(
            new BiConsumer<Integer, Channel<Integer, ?>>() {

              public void accept(final Integer integer, final Channel<Integer, ?> result) {
                result.pass(integer + 1);
              }
            }).create();
    assertThat(routine.invoke().pass(1, 2, 3, 4).close().in(seconds(10)).all()).containsExactly(2,
        3, 4, 5);
  }

  private static void testStatefulFactory(final FragmentActivity activity) {
    final ContextInvocationFactory<Integer, Integer> factory =
        JRoutineAndroidCompat.<Integer, Integer, Integer>statefulContextFactory().onCreate(
            new Supplier<Integer>() {

              public Integer get() {
                return 0;
              }
            }).onNextState(new BiFunction<Integer, Integer, Integer>() {

          public Integer apply(final Integer integer1, final Integer integer2) {
            return integer1 + integer2;
          }
        }).onCompleteArray(new Function<Integer, Integer[]>() {

          public Integer[] apply(final Integer integer) {
            final Integer[] integers = new Integer[1];
            integers[0] = integer;
            return integers;
          }
        }).create();
    assertThat(JRoutineAndroidCompat.routineOn(loaderOf(activity))
                                    .of(factory)
                                    .invoke()
                                    .pass(1, 2, 3, 4)
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsOnly(10);
  }

  private static void testStatelessFactory(final FragmentActivity activity) {
    final ContextInvocationFactory<Integer, Integer> factory =
        JRoutineAndroidCompat.<Integer, Integer>statelessContextFactory().onNext(
            new BiConsumer<Integer, Channel<Integer, ?>>() {

              public void accept(final Integer integer, final Channel<Integer, ?> result) {
                result.pass(integer + 1);
              }
            }).create();
    assertThat(JRoutineAndroidCompat.routineOn(loaderOf(activity))
                                    .of(factory)
                                    .invoke()
                                    .pass(1, 2, 3, 4)
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly(2, 3, 4, 5);
  }

  private static void testStream(@NotNull final FragmentActivity activity) {
    assertThat(JRoutineAndroidCompat.streamOf(JRoutineOperators.appendAllIn(range(1, 1000)))
                                    .map(JRoutineOperators.unary(new Function<Number, Double>() {

                                      public Double apply(final Number number) {
                                        final double value = number.doubleValue();
                                        return Math.sqrt(value);
                                      }
                                    }))
                                    .map(JRoutineOperators.average(Double.class))
                                    .lift(JRoutineAndroidCompat.streamLifterOn(
                                        loaderOf(activity)).<Integer, Double>runOnLoader())
                                    .invoke()
                                    .close()
                                    .in(seconds(10))
                                    .next()).isCloseTo(21, Offset.offset(0.1));
  }

  private static void testSumArray(final FragmentActivity activity) {
    final LoaderRoutine<Integer, Integer> routine =
        JRoutineAndroidCompat.<Integer, Integer, Integer>statefulRoutineOn(
            loaderOf(activity)).onCreate(new Supplier<Integer>() {

          public Integer get() {
            return 0;
          }
        }).onNextState(new BiFunction<Integer, Integer, Integer>() {

          public Integer apply(final Integer integer1, final Integer integer2) {
            return integer1 + integer2;
          }
        }).onCompleteArray(new Function<Integer, Integer[]>() {

          public Integer[] apply(final Integer integer) {
            final Integer[] integers = new Integer[1];
            integers[0] = integer;
            return integers;
          }
        }).create();
    assertThat(routine.invoke().pass(1, 2, 3, 4).close().in(seconds(10)).all()).containsOnly(10);
  }

  private static void testSupplierFactory(final FragmentActivity activity) {
    final LoaderRoutine<String, String> routine =
        JRoutineAndroidCompat.routineOn(loaderOf(activity))
                             .of(JRoutineAndroidCompat.contextFactoryOf(new Supplier<ToCase>() {

                               public ToCase get() {
                                 return new ToCase();
                               }
                             }));
    assertThat(routine.invoke().pass("TEST").close().in(seconds(10)).all()).containsOnly("test");
  }

  public void testConcat() {
    assertThat(JRoutineAndroidCompat.channelHandler()
                                    .concatOutputOf(JRoutineAndroidCompat.channel().of("test1"),
                                        JRoutineAndroidCompat.channel().of("test2"))
                                    .all()).containsExactly("test1", "test2");
    assertThat(JRoutineAndroidCompat.channelHandlerOn(ScheduledExecutors.syncExecutor())
                                    .concatOutputOf(JRoutineAndroidCompat.channel().of("test1"),
                                        JRoutineAndroidCompat.channel().of("test2"))
                                    .all()).containsExactly("test1", "test2");
  }

  public void testConcatReadOutput() throws IOException {
    final Channel<ParcelableByteChunk, ParcelableByteChunk> channel =
        JRoutineAndroidCompat.channel().ofType();
    final ByteChunkOutputStream stream = JRoutineAndroidCompat.parcelableOutputStream()
                                                              .withStream()
                                                              .withChunkSize(3)
                                                              .configuration()
                                                              .of(channel);
    stream.write(new byte[]{31, 17, (byte) 155, 13});
    stream.flush();
    final ByteChunkInputStream inputStream =
        JRoutineAndroidCompat.parcelableInputStream(channel.next(), channel.next());
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
        JRoutineAndroidCompat.channel().ofType();
    final ByteChunkOutputStream stream = JRoutineAndroidCompat.parcelableOutputStream()
                                                              .withStream()
                                                              .withChunkSize(3)
                                                              .configuration()
                                                              .of(channel);
    stream.write(new byte[]{31, 17, (byte) 155, 13});
    stream.flush();
    final ByteChunkInputStream inputStream =
        JRoutineAndroidCompat.parcelableInputStream(channel.eventuallyContinue().all());
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

  public void testIOChannel() {
    assertThat(JRoutineAndroidCompat.channel().of("test").next()).isEqualTo("test");
  }

  public void testIncrement() {
    testIncrement(getActivity());
  }

  public void testLoader() {
    final ClassToken<Join<String>> token = new ClassToken<Join<String>>() {};
    assertThat(JRoutineAndroidCompat.routineOn(loaderOf(getActivity()))
                                    .of(factoryOf(token))
                                    .invoke()
                                    .pass("test")
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test");
    final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                              .findFragmentById(R.id.test_fragment);
    assertThat(JRoutineAndroidCompat.routineOn(loaderOf(fragment))
                                    .of(factoryOf(token))
                                    .invoke()
                                    .pass("test")
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test");
  }

  public void testLoaderClass() throws NoSuchMethodException {
    new TestClass("test");
    assertThat(JRoutineAndroidCompat.wrapperOn(loaderOf(getActivity()))
                                    .methodOf(classOfType(TestClass.class), "getStringUp")
                                    .invoke()
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("TEST");
  }

  public void testLoaderId() throws NoSuchMethodException {
    new TestClass("test");
    assertThat(JRoutineAndroidCompat.wrapperOn(loaderOf(getActivity()))
                                    .withLoader()
                                    .withLoaderId(33)
                                    .withCacheStrategy(CacheStrategyType.CACHE)
                                    .configuration()
                                    .methodOf(instanceOf(TestClass.class), "getStringLow")
                                    .invoke()
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test");
    assertThat(
        JRoutineAndroidCompat.channelOn(loaderOf(getActivity()), 33).ofType().in(seconds(10)).all())
        .containsExactly("test");
  }

  public void testLoaderInstance() throws NoSuchMethodException {
    assertThat(JRoutineAndroidCompat.wrapperOn(loaderOf(getActivity()))
                                    .methodOf(instanceOf(TestClass.class, "TEST"),
                                        TestClass.class.getMethod("getStringLow"))
                                    .invoke()
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test");
    assertThat(JRoutineAndroidCompat.wrapperOn(loaderOf(getActivity()))
                                    .methodOf(instanceOf(TestClass.class), "getStringLow")
                                    .invoke()
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test");
    assertThat(JRoutineAndroidCompat.wrapperOn(loaderOf(getActivity()))
                                    .methodOf(instanceOf(TestClass.class), "test")
                                    .invoke()
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test");
  }

  public void testLoaderInvocation() {
    final ClassToken<JoinString> token = new ClassToken<JoinString>() {};
    assertThat(JRoutineAndroidCompat.routineOn(loaderOf(getActivity()))
                                    .of(factoryOf(token))
                                    .invoke()
                                    .pass("test1", "test2")
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test1,test2");
    assertThat(JRoutineAndroidCompat.routineOn(loaderOf(getActivity()))
                                    .of(factoryOf(token, ";"))
                                    .invoke()
                                    .pass("test1", "test2")
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test1;test2");
    assertThat(JRoutineAndroidCompat.routineOn(loaderOf(getActivity()))
                                    .of(factoryOf(JoinString.class))
                                    .invoke()
                                    .pass("test1", "test2")
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test1,test2");
    assertThat(JRoutineAndroidCompat.routineOn(loaderOf(getActivity()))
                                    .of(factoryOf(JoinString.class, " "))
                                    .invoke()
                                    .pass("test1", "test2")
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test1 test2");
  }

  public void testLoaderProxy() {
    new TestClass("TEST");
    assertThat(JRoutineAndroidCompat.wrapperOn(loaderOf(getActivity()))
                                    .proxyOf(instanceOf(TestClass.class), TestAnnotatedProxy.class)
                                    .getStringLow()
                                    .all()).containsExactly("test");
    assertThat(JRoutineAndroidCompat.wrapperOn(loaderOf(getActivity()))
                                    .proxyOf(instanceOf(TestClass.class),
                                        tokenOf(TestAnnotatedProxy.class))
                                    .getStringLow()
                                    .all()).containsExactly("test");
    assertThat(JRoutineAndroidCompat.wrapperOn(loaderOf(getActivity()))
                                    .withStrategy(ProxyStrategyType.REFLECTION)
                                    .proxyOf(instanceOf(TestClass.class), TestProxy.class)
                                    .getStringLow()
                                    .all()).containsExactly("test");
    assertThat(JRoutineAndroidCompat.wrapperOn(loaderOf(getActivity()))
                                    .withStrategy(ProxyStrategyType.REFLECTION)
                                    .proxyOf(instanceOf(TestClass.class), tokenOf(TestProxy.class))
                                    .getStringLow()
                                    .all()).containsExactly("test");
    assertThat(JRoutineAndroidCompat.wrapperOn(loaderOf(getActivity()))
                                    .withStrategy(ProxyStrategyType.CODE_GENERATION)
                                    .proxyOf(instanceOf(TestClass.class), TestAnnotatedProxy.class)
                                    .getStringLow()
                                    .all()).containsExactly("test");
    assertThat(JRoutineAndroidCompat.wrapperOn(loaderOf(getActivity()))
                                    .withStrategy(ProxyStrategyType.CODE_GENERATION)
                                    .proxyOf(instanceOf(TestClass.class),
                                        tokenOf(TestAnnotatedProxy.class))
                                    .getStringLow()
                                    .all()).containsExactly("test");
  }

  public void testLoaderProxyConfiguration() {
    new TestClass("TEST");
    assertThat(JRoutineAndroidCompat.wrapperOn(loaderOf(getActivity()))
                                    .withInvocation()
                                    .withLog(AndroidLogs.androidLog())
                                    .configuration()
                                    .withWrapper()
                                    .withSharedFields()
                                    .configuration()
                                    .withLoader()
                                    .withInvocationId(11)
                                    .configuration()
                                    .proxyOf(instanceOf(TestClass.class), TestAnnotatedProxy.class)
                                    .getStringLow()
                                    .all()).containsExactly("test");
  }

  public void testOf() {
    final Channel<?, Integer> channel = JRoutineAndroidCompat.channel().of(2);
    assertThat(channel.isOpen()).isFalse();
    assertThat(channel.in(seconds(10)).all()).containsExactly(2);
    assertThat(JRoutineAndroidCompat.channel().of().in(seconds(10)).all()).isEmpty();
    assertThat(JRoutineAndroidCompat.channel().of(-11, 73).in(seconds(10)).all()).containsExactly(
        -11, 73);
    assertThat(JRoutineAndroidCompat.channel().of(Arrays.asList(3, 12, -7))

                                    .in(seconds(10)).all()).containsExactly(3, 12, -7);
    assertThat(JRoutineAndroidCompat.channel().of((Object[]) null).all()).isEmpty();
    assertThat(JRoutineAndroidCompat.channel().of((List<Object>) null).all()).isEmpty();
  }

  public void testReadAll() throws IOException {
    final Channel<ParcelableByteChunk, ParcelableByteChunk> channel =
        JRoutineAndroidCompat.channel().ofType();
    final ByteChunkOutputStream stream = JRoutineAndroidCompat.parcelableOutputStream().of(channel);
    stream.write(new byte[]{31, 17, (byte) 155, 13});
    stream.flush();
    final ByteChunkInputStream inputStream =
        JRoutineAndroidCompat.parcelableInputStream(channel.next());
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
    assertThat(JRoutineAndroidCompat.routineOn(serviceOf(getActivity()))
                                    .of(InvocationFactoryReference.factoryOf(token))
                                    .invoke()
                                    .pass("test")
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test");
  }

  public void testServiceClass() throws NoSuchMethodException {
    new TestClass("test");
    assertThat(JRoutineAndroidCompat.wrapperOn(serviceOf(getActivity()))
                                    .methodOf(classOfType(TestClass.class), "getStringUp")
                                    .invoke()
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("TEST");
    assertThat(JRoutineAndroidCompat.wrapperOn(serviceOf(getActivity()))
                                    .methodOf(classOfType(TestClass.class),
                                        TestClass.class.getMethod("getStringUp"))
                                    .invoke()
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("TEST");
    assertThat(JRoutineAndroidCompat.wrapperOn(serviceOf(getActivity()))
                                    .methodOf(classOfType(TestClass.class), "TEST")
                                    .invoke()
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("TEST");
  }

  public void testServiceInstance() throws NoSuchMethodException {
    assertThat(JRoutineAndroidCompat.wrapperOn(serviceOf(getActivity()))
                                    .methodOf(instanceOf(TestClass.class, "TEST"),
                                        TestClass.class.getMethod("getStringLow"))
                                    .invoke()
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test");
    assertThat(JRoutineAndroidCompat.wrapperOn(serviceOf(getActivity()))
                                    .methodOf(instanceOf(TestClass.class), "getStringLow")
                                    .invoke()
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test");
    assertThat(JRoutineAndroidCompat.wrapperOn(serviceOf(getActivity()))
                                    .methodOf(instanceOf(TestClass.class), "test")
                                    .invoke()
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test");
  }

  public void testServiceInvocation() {
    final ClassToken<Pass<String>> token = new ClassToken<Pass<String>>() {};
    assertThat(JRoutineAndroidCompat.routineOn(serviceOf(getActivity()))
                                    .of(InvocationFactoryReference.factoryOf(token))
                                    .invoke()
                                    .pass("test")
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test");
    assertThat(JRoutineAndroidCompat.routineOn(serviceOf(getActivity()))
                                    .of(InvocationFactoryReference.factoryOf(token, 2))
                                    .invoke()
                                    .pass("test")
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test", "test");
    assertThat(JRoutineAndroidCompat.routineOn(serviceOf(getActivity()))
                                    .of(InvocationFactoryReference.factoryOf(PassString.class))
                                    .invoke()
                                    .pass("test")
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test");
    assertThat(JRoutineAndroidCompat.routineOn(serviceOf(getActivity()))
                                    .of(InvocationFactoryReference.factoryOf(PassString.class, 3))
                                    .invoke()
                                    .pass("test")
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test", "test", "test");
    assertThat(JRoutineAndroidCompat.routineOn(serviceOf(getActivity()))
                                    .of(InvocationFactoryReference.factoryOf(new Pass<String>()))
                                    .invoke()
                                    .pass("test")
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test");
    assertThat(JRoutineAndroidCompat.routineOn(serviceOf(getActivity()))
                                    .of(InvocationFactoryReference.factoryOf(new Pass<String>(), 2))
                                    .invoke()
                                    .pass("test")
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test", "test");
  }

  public void testServiceProxy() {
    new TestClass("TEST");
    assertThat(JRoutineAndroidCompat.wrapperOn(serviceOf(getActivity()))
                                    .proxyOf(instanceOf(TestClass.class), TestAnnotatedProxy.class)
                                    .getStringLow()
                                    .all()).containsExactly("test");
    assertThat(JRoutineAndroidCompat.wrapperOn(serviceOf(getActivity()))
                                    .proxyOf(instanceOf(TestClass.class),
                                        tokenOf(TestAnnotatedProxy.class))
                                    .getStringLow()
                                    .all()).containsExactly("test");
    assertThat(JRoutineAndroidCompat.wrapperOn(serviceOf(getActivity()))
                                    .withStrategy(ProxyStrategyType.REFLECTION)
                                    .proxyOf(instanceOf(TestClass.class), TestProxy.class)
                                    .getStringLow()
                                    .all()).containsExactly("test");
    assertThat(JRoutineAndroidCompat.wrapperOn(serviceOf(getActivity()))
                                    .withStrategy(ProxyStrategyType.REFLECTION)
                                    .proxyOf(instanceOf(TestClass.class), tokenOf(TestProxy.class))
                                    .getStringLow()
                                    .all()).containsExactly("test");
    assertThat(JRoutineAndroidCompat.wrapperOn(serviceOf(getActivity()))
                                    .withStrategy(ProxyStrategyType.CODE_GENERATION)
                                    .proxyOf(instanceOf(TestClass.class), TestAnnotatedProxy.class)
                                    .getStringLow()
                                    .all()).containsExactly("test");
    assertThat(JRoutineAndroidCompat.wrapperOn(serviceOf(getActivity()))
                                    .withStrategy(ProxyStrategyType.CODE_GENERATION)
                                    .proxyOf(instanceOf(TestClass.class),
                                        tokenOf(TestAnnotatedProxy.class))
                                    .getStringLow()
                                    .all()).containsExactly("test");
  }

  public void testServiceProxyConfiguration() {
    new TestClass("TEST");
    assertThat(JRoutineAndroidCompat.wrapperOn(serviceOf(getActivity()))
                                    .withInvocation()
                                    .withLog(AndroidLogs.androidLog())
                                    .configuration()
                                    .withWrapper()
                                    .withSharedFields()
                                    .configuration()
                                    .withService()
                                    .withLogClass(AndroidLog.class)
                                    .configuration()
                                    .proxyOf(instanceOf(TestClass.class), TestAnnotatedProxy.class)
                                    .getStringLow()
                                    .all()).containsExactly("test");
  }

  public void testStatefulFactory() {
    testStatefulFactory(getActivity());
  }

  public void testStatelessFactory() {
    testStatelessFactory(getActivity());
  }

  public void testStream() {
    testStream(getActivity());
  }

  public void testStreamOf() {
    assertThat(JRoutineAndroidCompat.streamOf(JRoutineOperators.append("test"))
                                    .lift(JRoutineAndroidCompat.streamLifterOn(
                                        loaderOf(getActivity())).<String, String>runOnLoader())
                                    .invoke()
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test");
    assertThat(JRoutineAndroidCompat.streamOf(JRoutineOperators.append("test1", "test2", "test3"))
                                    .lift(JRoutineAndroidCompat.streamLifterOn(
                                        loaderOf(getActivity())).<String, String>runOnLoader())
                                    .invoke()
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test1", "test2", "test3");
    assertThat(JRoutineAndroidCompat.streamOf(
        JRoutineOperators.append(Arrays.asList("test1", "test2", "test3")))
                                    .lift(JRoutineAndroidCompat.streamLifterOn(
                                        loaderOf(getActivity())).<String, String>runOnLoader())
                                    .invoke()
                                    .close()
                                    .in(seconds(10))
                                    .all()).containsExactly("test1", "test2", "test3");
  }

  public void testStreamOfAbort() {
    Channel<String, String> channel =
        JRoutineAndroidCompat.streamOf(JRoutineOperators.append("test"))
                             .lift(JRoutineAndroidCompat.streamLifterOn(
                                 loaderOf(getActivity())).<String, String>runOnLoader())
                             .invoke();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.in(seconds(10)).getError()).isInstanceOf(AbortException.class);
    channel = JRoutineAndroidCompat.streamOf(JRoutineOperators.append("test1", "test2", "test3"))
                                   .lift(JRoutineAndroidCompat.streamLifterOn(
                                       loaderOf(getActivity())).<String, String>runOnLoader())
                                   .invoke();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.in(seconds(10)).getError()).isInstanceOf(AbortException.class);
    channel = JRoutineAndroidCompat.streamOf(
        JRoutineOperators.append(Arrays.asList("test1", "test2", "test3")))
                                   .lift(JRoutineAndroidCompat.streamLifterOn(
                                       loaderOf(getActivity())).<String, String>runOnLoader())
                                   .invoke();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.in(seconds(10)).getError()).isInstanceOf(AbortException.class);
  }

  public void testSumArray() {
    testSumArray(getActivity());
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

  public static class ToCase extends TemplateContextInvocation<String, String> {

    private final boolean mIsUpper;

    public ToCase() {
      this(false);
    }

    public ToCase(final boolean isUpper) {
      mIsUpper = isUpper;
    }

    public void onInput(final String input, @NotNull final Channel<String, ?> result) {
      result.pass(mIsUpper ? input.toUpperCase() : input.toLowerCase());
    }
  }
}
