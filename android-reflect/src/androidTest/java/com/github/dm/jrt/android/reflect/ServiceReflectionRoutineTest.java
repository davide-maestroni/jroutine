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

package com.github.dm.jrt.android.reflect;

import android.annotation.TargetApi;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.core.config.ServiceConfiguration;
import com.github.dm.jrt.android.reflect.builder.ServiceReflectionRoutineBuilder;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration.TimeoutActionType;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.log.Log;
import com.github.dm.jrt.core.log.Log.Level;
import com.github.dm.jrt.core.log.NullLog;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.runner.RunnerDecorator;
import com.github.dm.jrt.core.runner.Runners;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.core.util.DurationMeasure;
import com.github.dm.jrt.reflect.annotation.Alias;
import com.github.dm.jrt.reflect.annotation.AsyncInput;
import com.github.dm.jrt.reflect.annotation.AsyncInput.InputMode;
import com.github.dm.jrt.reflect.annotation.AsyncMethod;
import com.github.dm.jrt.reflect.annotation.AsyncOutput;
import com.github.dm.jrt.reflect.annotation.AsyncOutput.OutputMode;
import com.github.dm.jrt.reflect.annotation.OutputTimeout;
import com.github.dm.jrt.reflect.annotation.OutputTimeoutAction;
import com.github.dm.jrt.reflect.config.WrapperConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.android.core.ServiceContext.serviceFrom;
import static com.github.dm.jrt.android.reflect.ContextInvocationTarget.instanceOf;
import static com.github.dm.jrt.core.util.DurationMeasure.indefiniteTime;
import static com.github.dm.jrt.core.util.DurationMeasure.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Service reflection routine unit tests.
 * <p>
 * Created by davide-maestroni on 03/30/2015.
 */
@TargetApi(VERSION_CODES.FROYO)
public class ServiceReflectionRoutineTest extends ActivityInstrumentationTestCase2<TestActivity> {

  public ServiceReflectionRoutineTest() {

    super(TestActivity.class);
  }

  public void testAliasMethod() throws NoSuchMethodException {

    final DurationMeasure timeout = seconds(10);
    final Routine<Object, Object> routine = JRoutineServiceReflection.on(serviceFrom(getActivity()))
                                                                     .with(instanceOf(
                                                                         TestClass.class))
                                                                     .invocationConfiguration()
                                                                     .withRunner(
                                                                         Runners.syncRunner())
                                                                     .withMaxInvocations(1)
                                                                     .withCoreInvocations(1)
                                                                     .withOutputTimeoutAction(
                                                                         TimeoutActionType.CONTINUE)
                                                                     .withLogLevel(Level.DEBUG)
                                                                     .withLog(new NullLog())
                                                                     .apply()
                                                                     .method(TestClass.GET);
    assertThat(routine.invoke().close().in(timeout).all()).containsExactly(-77L);
  }

  public void testArgs() {

    assertThat(JRoutineServiceReflection.on(serviceFrom(getActivity()))
                                        .with(instanceOf(TestArgs.class, 17))
                                        .method("getId")
                                        .invoke()
                                        .close()
                                        .in(seconds(10))
                                        .next()).isEqualTo(17);
  }

  public void testAsyncInputProxyRoutine() {

    final DurationMeasure timeout = seconds(10);
    final SumItf sumAsync = JRoutineServiceReflection.on(serviceFrom(getActivity()))
                                                     .with(instanceOf(Sum.class))
                                                     .invocationConfiguration()
                                                     .withOutputTimeout(timeout)
                                                     .apply()
                                                     .buildProxy(ClassToken.tokenOf(SumItf.class));
    final Channel<Integer, Integer> channel3 = JRoutineCore.<Integer>ofData().buildChannel();
    channel3.pass(7).close();
    assertThat(sumAsync.compute(3, channel3)).isEqualTo(10);

    final Channel<Integer, Integer> channel4 = JRoutineCore.<Integer>ofData().buildChannel();
    channel4.pass(1, 2, 3, 4).close();
    assertThat(sumAsync.compute(channel4)).isEqualTo(10);

    final Channel<int[], int[]> channel5 = JRoutineCore.<int[]>ofData().buildChannel();
    channel5.pass(new int[]{1, 2, 3, 4}).close();
    assertThat(sumAsync.compute1(channel5)).isEqualTo(10);

    final Channel<Integer, Integer> channel6 = JRoutineCore.<Integer>ofData().buildChannel();
    channel6.pass(1, 2, 3, 4).close();
    assertThat(sumAsync.computeList(channel6)).isEqualTo(10);

    final Channel<Integer, Integer> channel7 = JRoutineCore.<Integer>ofData().buildChannel();
    channel7.pass(1, 2, 3, 4).close();
    assertThat(sumAsync.computeList1(channel7)).isEqualTo(10);
  }

  public void testAsyncOutputProxyRoutine() {

    final DurationMeasure timeout = seconds(10);
    final CountItf countAsync = JRoutineServiceReflection.on(serviceFrom(getActivity()))
                                                         .with(instanceOf(Count.class))
                                                         .invocationConfiguration()
                                                         .withOutputTimeout(timeout)
                                                         .apply()
                                                         .buildProxy(CountItf.class);
    assertThat(countAsync.count(3).all()).containsExactly(0, 1, 2);
    assertThat(countAsync.count1(3).all()).containsExactly(new int[]{0, 1, 2});
    assertThat(countAsync.count2(2).all()).containsExactly(0, 1);
    assertThat(countAsync.countList(3).all()).containsExactly(0, 1, 2);
    assertThat(countAsync.countList1(3).all()).containsExactly(0, 1, 2);
  }

  @SuppressWarnings("ConstantConditions")
  public void testConfigurationErrors() {

    try {

      new DefaultServiceReflectionRoutineBuilder(serviceFrom(getActivity()),
          instanceOf(TestClass.class)).apply((InvocationConfiguration) null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      new DefaultServiceReflectionRoutineBuilder(serviceFrom(getActivity()),
          instanceOf(TestClass.class)).apply((WrapperConfiguration) null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      new DefaultServiceReflectionRoutineBuilder(serviceFrom(getActivity()),
          instanceOf(TestClass.class)).apply((ServiceConfiguration) null);

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  public void testConstructor() {

    boolean failed = false;
    try {
      new JRoutineServiceReflection();
      failed = true;

    } catch (final Throwable ignored) {

    }

    assertThat(failed).isFalse();
  }

  public void testDuplicateAnnotationError() {

    try {

      JRoutineServiceReflection.on(serviceFrom(getActivity()))
                               .with(instanceOf(DuplicateAnnotation.class))
                               .method(DuplicateAnnotation.GET);

      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  public void testException() throws NoSuchMethodException {

    final DurationMeasure timeout = seconds(10);
    final Routine<Object, Object> routine3 =
        JRoutineServiceReflection.on(serviceFrom(getActivity()))
                                 .with(instanceOf(TestClass.class))
                                 .method(TestClass.THROW);

    try {

      routine3.invoke().pass(new IllegalArgumentException("test")).close().in(timeout).all();

      fail();

    } catch (final InvocationException e) {

      assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
      assertThat(e.getCause().getMessage()).isEqualTo("test");
    }
  }

  public void testInvalidProxyError() {

    try {

      JRoutineServiceReflection.on(serviceFrom(getActivity()))
                               .with(instanceOf(TestClass.class))
                               .buildProxy(TestClass.class);

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineServiceReflection.on(serviceFrom(getActivity()))
                               .with(instanceOf(TestClass.class))
                               .buildProxy(ClassToken.tokenOf(TestClass.class));

      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  public void testInvalidProxyInputAnnotationError() {

    try {

      JRoutineServiceReflection.on(serviceFrom(getActivity()))
                               .with(instanceOf(Sum.class))
                               .buildProxy(SumError.class)
                               .compute(1, new int[0]);

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineServiceReflection.on(serviceFrom(getActivity()))
                               .with(instanceOf(Sum.class))
                               .buildProxy(SumError.class)
                               .compute(new String[0]);

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineServiceReflection.on(serviceFrom(getActivity()))
                               .with(instanceOf(Sum.class))
                               .buildProxy(SumError.class)
                               .compute(new int[0]);

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineServiceReflection.on(serviceFrom(getActivity()))
                               .with(instanceOf(Sum.class))
                               .buildProxy(SumError.class)
                               .compute(Collections.<Integer>emptyList());

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    final Channel<Integer, Integer> channel = JRoutineCore.<Integer>ofData().buildChannel();

    try {

      JRoutineServiceReflection.on(serviceFrom(getActivity()))
                               .with(instanceOf(Sum.class))
                               .buildProxy(SumError.class)
                               .compute(channel);

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineServiceReflection.on(serviceFrom(getActivity()))
                               .with(instanceOf(Sum.class))
                               .buildProxy(SumError.class)
                               .compute(1, channel);

      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  public void testInvalidProxyMethodError() {

    try {

      JRoutineServiceReflection.on(serviceFrom(getActivity()))
                               .with(instanceOf(TestClass.class))
                               .invocationConfiguration()
                               .withOutputTimeout(indefiniteTime())
                               .apply()
                               .buildProxy(TestItf.class)
                               .throwException(null);

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineServiceReflection.on(serviceFrom(getActivity()))
                               .with(instanceOf(TestClass.class))
                               .invocationConfiguration()
                               .withOutputTimeout(indefiniteTime())
                               .apply()
                               .buildProxy(TestItf.class)
                               .throwException1(null);

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineServiceReflection.on(serviceFrom(getActivity()))
                               .with(instanceOf(TestClass.class))
                               .invocationConfiguration()
                               .withOutputTimeout(indefiniteTime())
                               .apply()
                               .buildProxy(TestItf.class)
                               .throwException2(null);

      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  public void testInvalidProxyOutputAnnotationError() {

    try {

      JRoutineServiceReflection.on(serviceFrom(getActivity()))
                               .with(instanceOf(Count.class))
                               .buildProxy(CountError.class)
                               .count(3);

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineServiceReflection.on(serviceFrom(getActivity()))
                               .with(instanceOf(Count.class))
                               .buildProxy(CountError.class)
                               .count1(3);

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineServiceReflection.on(serviceFrom(getActivity()))
                               .with(instanceOf(Count.class))
                               .buildProxy(CountError.class)
                               .countList(3);

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineServiceReflection.on(serviceFrom(getActivity()))
                               .with(instanceOf(Count.class))
                               .buildProxy(CountError.class)
                               .countList1(3);

      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  public void testMethod() throws NoSuchMethodException {

    final DurationMeasure timeout = seconds(10);
    final Routine<Object, Object> routine2 =
        JRoutineServiceReflection.on(serviceFrom(getActivity()))
                                 .with(instanceOf(TestClass.class))
                                 .invocationConfiguration()
                                 .withRunner(Runners.poolRunner())
                                 .withMaxInvocations(1)
                                 .apply()
                                 .wrapperConfiguration()
                                 .withSharedFields("test")
                                 .apply()
                                 .method(TestClass.class.getMethod("getLong"));

    assertThat(routine2.invoke().close().in(timeout).all()).containsExactly(-77L);
  }

  public void testMethodBySignature() throws NoSuchMethodException {

    final DurationMeasure timeout = seconds(10);
    final Routine<Object, Object> routine1 =
        JRoutineServiceReflection.on(serviceFrom(getActivity()))
                                 .with(instanceOf(TestClass.class))
                                 .invocationConfiguration()
                                 .withRunner(Runners.poolRunner())
                                 .apply()
                                 .method("getLong");

    assertThat(routine1.invoke().close().in(timeout).all()).containsExactly(-77L);
  }

  public void testMissingAliasMethodError() {

    try {

      JRoutineServiceReflection.on(serviceFrom(getActivity()))
                               .with(instanceOf(TestClass.class))
                               .method("test");

      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  public void testMissingMethodError() {

    try {

      JRoutineServiceReflection.on(serviceFrom(getActivity()))
                               .with(instanceOf(TestClass.class))
                               .method("test");

      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  @SuppressWarnings("ConstantConditions")
  public void testNullPointerError() {

    try {

      JRoutineServiceReflection.on(serviceFrom(getActivity())).with(null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineServiceReflection.on(serviceFrom(getActivity())).with(instanceOf(null));

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @SuppressWarnings("ConstantConditions")
  public void testNullProxyError() {

    try {

      JRoutineServiceReflection.on(serviceFrom(getActivity()))
                               .with(instanceOf(TestClass.class))
                               .buildProxy((Class<?>) null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      JRoutineServiceReflection.on(serviceFrom(getActivity()))
                               .with(instanceOf(TestClass.class))
                               .buildProxy((ClassToken<?>) null);

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @SuppressWarnings("unchecked")
  public void testProxyAnnotations() {

    final Itf itf = JRoutineServiceReflection.on(serviceFrom(getActivity()))
                                             .with(instanceOf(Impl.class))
                                             .invocationConfiguration()
                                             .withOutputTimeout(indefiniteTime())
                                             .apply()
                                             .buildProxy(Itf.class);

    assertThat(itf.add0('c')).isEqualTo((int) 'c');
    final Channel<Character, Character> channel1 = JRoutineCore.<Character>ofData().buildChannel();
    channel1.pass('a').close();
    assertThat(itf.add1(channel1)).isEqualTo((int) 'a');
    assertThat(itf.add3('c').all()).containsExactly((int) 'c');
    final Channel<Character, Character> channel3 = JRoutineCore.<Character>ofData().buildChannel();
    channel3.pass('a').close();
    assertThat(itf.add4(channel3).all()).containsExactly((int) 'a');
    assertThat(itf.add6().pass('d').close().all()).containsOnly((int) 'd');
    assertThat(itf.add10().invoke().pass('d').close().all()).containsOnly((int) 'd');
    assertThat(itf.addA00(new char[]{'c', 'z'})).isEqualTo(new int[]{'c', 'z'});
    final Channel<char[], char[]> channel5 = JRoutineCore.<char[]>ofData().buildChannel();
    channel5.pass(new char[]{'a', 'z'}).close();
    assertThat(itf.addA01(channel5)).isEqualTo(new int[]{'a', 'z'});
    final Channel<Character, Character> channel6 = JRoutineCore.<Character>ofData().buildChannel();
    channel6.pass('d', 'e', 'f').close();
    assertThat(itf.addA02(channel6)).isEqualTo(new int[]{'d', 'e', 'f'});
    assertThat(itf.addA04(new char[]{'c', 'z'}).all()).containsExactly(new int[]{'c', 'z'});
    final Channel<char[], char[]> channel8 = JRoutineCore.<char[]>ofData().buildChannel();
    channel8.pass(new char[]{'a', 'z'}).close();
    assertThat(itf.addA05(channel8).all()).containsExactly(new int[]{'a', 'z'});
    final Channel<Character, Character> channel9 = JRoutineCore.<Character>ofData().buildChannel();
    channel9.pass('d', 'e', 'f').close();
    assertThat(itf.addA06(channel9).all()).containsExactly(new int[]{'d', 'e', 'f'});
    final Channel<char[], char[]> channel10 = JRoutineCore.<char[]>ofData().buildChannel();
    channel10.pass(new char[]{'d', 'z'}, new char[]{'e', 'z'}, new char[]{'f', 'z'}).close();
    assertThat(itf.addA08(new char[]{'c', 'z'}).all()).containsExactly((int) 'c', (int) 'z');
    final Channel<char[], char[]> channel11 = JRoutineCore.<char[]>ofData().buildChannel();
    channel11.pass(new char[]{'a', 'z'}).close();
    assertThat(itf.addA09(channel11).all()).containsExactly((int) 'a', (int) 'z');
    final Channel<Character, Character> channel12 = JRoutineCore.<Character>ofData().buildChannel();
    channel12.pass('d', 'e', 'f').close();
    assertThat(itf.addA10(channel12).all()).containsExactly((int) 'd', (int) 'e', (int) 'f');
    assertThat(itf.addA12().pass(new char[]{'c', 'z'}).close().all()).containsOnly(
        new int[]{'c', 'z'});
    assertThat(itf.addA14().invoke().pass(new char[]{'c', 'z'}).close().all()).containsOnly(
        new int[]{'c', 'z'});
    assertThat(itf.addA16().pass(new char[]{'c', 'z'}).close().all()).containsExactly((int) 'c',
        (int) 'z');
    assertThat(itf.addA18().invoke().pass(new char[]{'c', 'z'}).close().all()).containsExactly(
        (int) 'c', (int) 'z');
    assertThat(itf.addL00(Arrays.asList('c', 'z'))).isEqualTo(Arrays.asList((int) 'c', (int) 'z'));
    final Channel<List<Character>, List<Character>> channel20 =
        JRoutineCore.<List<Character>>ofData().buildChannel();
    channel20.pass(Arrays.asList('a', 'z')).close();
    assertThat(itf.addL01(channel20)).isEqualTo(Arrays.asList((int) 'a', (int) 'z'));
    final Channel<Character, Character> channel21 = JRoutineCore.<Character>ofData().buildChannel();
    channel21.pass('d', 'e', 'f').close();
    assertThat(itf.addL02(channel21)).isEqualTo(Arrays.asList((int) 'd', (int) 'e', (int) 'f'));
    assertThat(itf.addL04(Arrays.asList('c', 'z')).all()).containsExactly(
        Arrays.asList((int) 'c', (int) 'z'));
    final Channel<List<Character>, List<Character>> channel23 =
        JRoutineCore.<List<Character>>ofData().buildChannel();
    channel23.pass(Arrays.asList('a', 'z')).close();
    assertThat(itf.addL05(channel23).all()).containsExactly(Arrays.asList((int) 'a', (int) 'z'));
    final Channel<Character, Character> channel24 = JRoutineCore.<Character>ofData().buildChannel();
    channel24.pass('d', 'e', 'f').close();
    assertThat(itf.addL06(channel24).all()).containsExactly(
        Arrays.asList((int) 'd', (int) 'e', (int) 'f'));
    assertThat(itf.addL08(Arrays.asList('c', 'z')).all()).containsExactly((int) 'c', (int) 'z');
    final Channel<List<Character>, List<Character>> channel26 =
        JRoutineCore.<List<Character>>ofData().buildChannel();
    channel26.pass(Arrays.asList('a', 'z')).close();
    assertThat(itf.addL09(channel26).all()).containsExactly((int) 'a', (int) 'z');
    final Channel<Character, Character> channel27 = JRoutineCore.<Character>ofData().buildChannel();
    channel27.pass('d', 'e', 'f').close();
    assertThat(itf.addL10(channel27).all()).containsExactly((int) 'd', (int) 'e', (int) 'f');
    assertThat(itf.addL12().pass(Arrays.asList('c', 'z')).close().all()).containsOnly(
        Arrays.asList((int) 'c', (int) 'z'));
    assertThat(itf.addL14().invoke().pass(Arrays.asList('c', 'z')).close().all()).containsOnly(
        Arrays.asList((int) 'c', (int) 'z'));
    assertThat(itf.addL16().pass(Arrays.asList('c', 'z')).close().all()).containsExactly((int) 'c',
        (int) 'z');
    assertThat(itf.addL18().invoke().pass(Arrays.asList('c', 'z')).close().all()).containsExactly(
        (int) 'c', (int) 'z');
    assertThat(itf.get0()).isEqualTo(31);
    assertThat(itf.get1().all()).containsExactly(31);
    assertThat(itf.get2().close().all()).containsExactly(31);
    assertThat(itf.get4().invoke().close().all()).containsExactly(31);
    assertThat(itf.getA0()).isEqualTo(new int[]{1, 2, 3});
    assertThat(itf.getA1().all()).containsExactly(1, 2, 3);
    assertThat(itf.getA2().close().all()).containsExactly(new int[]{1, 2, 3});
    assertThat(itf.getA3().invoke().close().all()).containsExactly(new int[]{1, 2, 3});
    assertThat(itf.getA4().close().all()).containsExactly(1, 2, 3);
    assertThat(itf.getA5().invoke().close().all()).containsExactly(1, 2, 3);
    assertThat(itf.getL0()).isEqualTo(Arrays.asList(1, 2, 3));
    assertThat(itf.getL1().all()).containsExactly(1, 2, 3);
    assertThat(itf.getL2().close().all()).containsExactly(Arrays.asList(1, 2, 3));
    assertThat(itf.getL3().invoke().close().all()).containsExactly(Arrays.asList(1, 2, 3));
    assertThat(itf.getL4().close().all()).containsExactly(1, 2, 3);
    assertThat(itf.getL5().invoke().close().all()).containsExactly(1, 2, 3);
    itf.set0(-17);
    final Channel<Integer, Integer> channel35 = JRoutineCore.<Integer>ofData().buildChannel();
    channel35.pass(-17).close();
    itf.set1(channel35);
    itf.set3().pass(-17).close().getComplete();
    itf.set5().invoke().pass(-17).close().getComplete();
    itf.setA0(new int[]{1, 2, 3});
    final Channel<int[], int[]> channel37 = JRoutineCore.<int[]>ofData().buildChannel();
    channel37.pass(new int[]{1, 2, 3}).close();
    itf.setA1(channel37);
    final Channel<Integer, Integer> channel38 = JRoutineCore.<Integer>ofData().buildChannel();
    channel38.pass(1, 2, 3).close();
    itf.setA2(channel38);
    itf.setA4().pass(new int[]{1, 2, 3}).close().getComplete();
    itf.setA6().invoke().pass(new int[]{1, 2, 3}).close().getComplete();
    itf.setL0(Arrays.asList(1, 2, 3));
    final Channel<List<Integer>, List<Integer>> channel40 =
        JRoutineCore.<List<Integer>>ofData().buildChannel();
    channel40.pass(Arrays.asList(1, 2, 3)).close();
    itf.setL1(channel40);
    final Channel<Integer, Integer> channel41 = JRoutineCore.<Integer>ofData().buildChannel();
    channel41.pass(1, 2, 3).close();
    itf.setL2(channel41);
    itf.setL4().pass(Arrays.asList(1, 2, 3)).close().getComplete();
    itf.setL6().invoke().pass(Arrays.asList(1, 2, 3)).close().getComplete();
  }

  @SuppressWarnings("NullArgumentToVariableArgMethod")
  public void testProxyRoutine() {

    final DurationMeasure timeout = seconds(10);
    final SquareItf squareAsync = JRoutineServiceReflection.on(serviceFrom(getActivity()))
                                                           .with(instanceOf(Square.class))
                                                           .buildProxy(SquareItf.class);

    assertThat(squareAsync.compute(3)).isEqualTo(9);

    final Channel<Integer, Integer> channel1 = JRoutineCore.<Integer>ofData().buildChannel();
    channel1.pass(4).close();
    assertThat(squareAsync.computeAsync(channel1)).isEqualTo(16);
  }

  public void testSharedFields() throws NoSuchMethodException {

    final ServiceReflectionRoutineBuilder builder =
        JRoutineServiceReflection.on(serviceFrom(getActivity(), TestService.class))
                                 .with(instanceOf(TestClass2.class))
                                 .serviceConfiguration()
                                 .withRunnerClass(SharedFieldRunner.class)
                                 .apply()
                                 .invocationConfiguration()
                                 .withOutputTimeout(seconds(10))
                                 .apply();

    long startTime = System.currentTimeMillis();

    Channel<?, Object> getOne = builder.wrapperConfiguration()
                                       .withSharedFields("1")
                                       .apply()
                                       .method("getOne")
                                       .invoke()
                                       .close();
    Channel<?, Object> getTwo = builder.wrapperConfiguration()
                                       .withSharedFields("2")
                                       .apply()
                                       .method("getTwo")
                                       .invoke()
                                       .close();

    assertThat(getOne.getComplete()).isTrue();
    assertThat(getTwo.getComplete()).isTrue();
    assertThat(getOne.getError()).isNull();
    assertThat(getTwo.getError()).isNull();
    assertThat(System.currentTimeMillis() - startTime).isLessThan(4000);

    startTime = System.currentTimeMillis();

    getOne = builder.method("getOne").invoke().close();
    getTwo = builder.method("getTwo").invoke().close();

    assertThat(getOne.getComplete()).isTrue();
    assertThat(getTwo.getComplete()).isTrue();
    assertThat(getOne.getError()).isNull();
    assertThat(getTwo.getError()).isNull();
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(4000);
  }

  public void testTimeoutActionAnnotation() throws NoSuchMethodException {

    assertThat(JRoutineServiceReflection.on(serviceFrom(getActivity()))
                                        .with(instanceOf(TestTimeout.class))
                                        .invocationConfiguration()
                                        .withOutputTimeout(seconds(10))
                                        .apply()
                                        .method("test")
                                        .invoke()
                                        .close()
                                        .next()).isEqualTo(31);

    try {

      JRoutineServiceReflection.on(serviceFrom(getActivity()))
                               .with(instanceOf(TestTimeout.class))
                               .invocationConfiguration()
                               .withOutputTimeoutAction(TimeoutActionType.FAIL)
                               .apply()
                               .method("test")
                               .invoke()
                               .close()
                               .next();

      fail();

    } catch (final NoSuchElementException ignored) {

    }

    assertThat(JRoutineServiceReflection.on(serviceFrom(getActivity()))
                                        .with(instanceOf(TestTimeout.class))
                                        .invocationConfiguration()
                                        .withOutputTimeout(seconds(10))
                                        .apply()
                                        .method("getInt")
                                        .invoke()
                                        .close()
                                        .next()).isEqualTo(31);

    try {

      JRoutineServiceReflection.on(serviceFrom(getActivity()))
                               .with(instanceOf(TestTimeout.class))
                               .invocationConfiguration()
                               .withOutputTimeoutAction(TimeoutActionType.FAIL)
                               .apply()
                               .method("getInt")
                               .invoke()
                               .close()
                               .next();

      fail();

    } catch (final NoSuchElementException ignored) {

    }

    assertThat(JRoutineServiceReflection.on(serviceFrom(getActivity()))
                                        .with(instanceOf(TestTimeout.class))
                                        .invocationConfiguration()
                                        .withOutputTimeout(seconds(10))
                                        .apply()
                                        .method(TestTimeout.class.getMethod("getInt"))
                                        .invoke()
                                        .close()
                                        .next()).isEqualTo(31);

    try {

      JRoutineServiceReflection.on(serviceFrom(getActivity()))
                               .with(instanceOf(TestTimeout.class))
                               .invocationConfiguration()
                               .withOutputTimeoutAction(TimeoutActionType.FAIL)
                               .apply()
                               .method(TestTimeout.class.getMethod("getInt"))
                               .invoke()
                               .close()
                               .next();

      fail();

    } catch (final NoSuchElementException ignored) {

    }

    assertThat(JRoutineServiceReflection.on(serviceFrom(getActivity()))
                                        .with(instanceOf(TestTimeout.class))
                                        .invocationConfiguration()
                                        .withOutputTimeout(seconds(10))
                                        .apply()
                                        .buildProxy(TestTimeoutItf.class)
                                        .getInt()).isEqualTo(31);

    try {

      JRoutineServiceReflection.on(serviceFrom(getActivity()))
                               .with(instanceOf(TestTimeout.class))
                               .invocationConfiguration()
                               .withOutputTimeoutAction(TimeoutActionType.FAIL)
                               .apply()
                               .buildProxy(TestTimeoutItf.class)
                               .getInt();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  public interface Itf {

    @Alias("a")
    int add0(char c);

    @Alias("a")
    int add1(@AsyncInput(value = char.class, mode = InputMode.DEFAULT) Channel<?, Character> c);

    @Alias("a")
    @AsyncMethod(char.class)
    Routine<Character, Integer> add10();

    @Alias("a")
    @AsyncOutput(OutputMode.DEFAULT)
    Channel<?, Integer> add3(char c);

    @Alias("a")
    @AsyncOutput(OutputMode.DEFAULT)
    Channel<?, Integer> add4(
        @AsyncInput(value = char.class, mode = InputMode.DEFAULT) Channel<?, Character> c);

    @Alias("a")
    @AsyncMethod(char.class)
    Channel<Character, Integer> add6();

    @Alias("aa")
    int[] addA00(char[] c);

    @Alias("aa")
    int[] addA01(@AsyncInput(value = char[].class, mode = InputMode.DEFAULT) Channel<?, char[]> c);

    @Alias("aa")
    int[] addA02(
        @AsyncInput(value = char[].class, mode = InputMode.COLLECTION) Channel<?, Character> c);

    @Alias("aa")
    @AsyncOutput(OutputMode.DEFAULT)
    Channel<?, int[]> addA04(char[] c);

    @Alias("aa")
    @AsyncOutput(OutputMode.DEFAULT)
    Channel<?, int[]> addA05(
        @AsyncInput(value = char[].class, mode = InputMode.DEFAULT) Channel<?, char[]> c);

    @Alias("aa")
    @AsyncOutput(OutputMode.DEFAULT)
    Channel<?, int[]> addA06(
        @AsyncInput(value = char[].class, mode = InputMode.COLLECTION) Channel<?, Character> c);

    @Alias("aa")
    @AsyncOutput(OutputMode.ELEMENT)
    Channel<?, Integer> addA08(char[] c);

    @Alias("aa")
    @AsyncOutput(OutputMode.ELEMENT)
    Channel<?, Integer> addA09(
        @AsyncInput(value = char[].class, mode = InputMode.DEFAULT) Channel<?, char[]> c);

    @Alias("aa")
    @AsyncOutput(OutputMode.ELEMENT)
    Channel<?, Integer> addA10(
        @AsyncInput(value = char[].class, mode = InputMode.COLLECTION) Channel<?, Character> c);

    @Alias("aa")
    @AsyncMethod(char[].class)
    Channel<char[], int[]> addA12();

    @Alias("aa")
    @AsyncMethod(char[].class)
    Routine<char[], int[]> addA14();

    @Alias("aa")
    @AsyncMethod(value = char[].class, mode = OutputMode.ELEMENT)
    Channel<char[], Integer> addA16();

    @Alias("aa")
    @AsyncMethod(value = char[].class, mode = OutputMode.ELEMENT)
    Routine<char[], Integer> addA18();

    @Alias("al")
    List<Integer> addL00(List<Character> c);

    @Alias("al")
    List<Integer> addL01(
        @AsyncInput(value = List.class, mode = InputMode.DEFAULT) Channel<?, List<Character>> c);

    @Alias("al")
    List<Integer> addL02(
        @AsyncInput(value = List.class, mode = InputMode.COLLECTION) Channel<?, Character> c);

    @Alias("al")
    @AsyncOutput(OutputMode.DEFAULT)
    Channel<?, List<Integer>> addL04(List<Character> c);

    @Alias("al")
    @AsyncOutput(OutputMode.DEFAULT)
    Channel<?, List<Integer>> addL05(
        @AsyncInput(value = List.class, mode = InputMode.DEFAULT) Channel<?, List<Character>> c);

    @Alias("al")
    @AsyncOutput(OutputMode.DEFAULT)
    Channel<?, List<Integer>> addL06(
        @AsyncInput(value = List.class, mode = InputMode.COLLECTION) Channel<?, Character> c);

    @Alias("al")
    @AsyncOutput(OutputMode.ELEMENT)
    Channel<?, Integer> addL08(List<Character> c);

    @Alias("al")
    @AsyncOutput(OutputMode.ELEMENT)
    Channel<?, Integer> addL09(
        @AsyncInput(value = List.class, mode = InputMode.DEFAULT) Channel<?, List<Character>> c);

    @Alias("al")
    @AsyncOutput(OutputMode.ELEMENT)
    Channel<?, Integer> addL10(
        @AsyncInput(value = List.class, mode = InputMode.COLLECTION) Channel<?, Character> c);

    @Alias("al")
    @AsyncMethod(List.class)
    Channel<List<Character>, List<Integer>> addL12();

    @Alias("al")
    @AsyncMethod(List.class)
    Routine<List<Character>, List<Integer>> addL14();

    @Alias("al")
    @AsyncMethod(value = List.class, mode = OutputMode.ELEMENT)
    Channel<List<Character>, Integer> addL16();

    @Alias("al")
    @AsyncMethod(value = List.class, mode = OutputMode.ELEMENT)
    Routine<List<Character>, Integer> addL18();

    @Alias("g")
    int get0();

    @Alias("s")
    void set0(int i);

    @Alias("g")
    @AsyncOutput(OutputMode.DEFAULT)
    Channel<?, Integer> get1();

    @Alias("s")
    void set1(@AsyncInput(value = int.class, mode = InputMode.DEFAULT) Channel<?, Integer> i);

    @Alias("g")
    @AsyncMethod({})
    Channel<Void, Integer> get2();

    @Alias("g")
    @AsyncMethod({})
    Routine<Void, Integer> get4();

    @Alias("ga")
    int[] getA0();

    @Alias("sa")
    void setA0(int[] i);

    @Alias("ga")
    @AsyncOutput(OutputMode.ELEMENT)
    Channel<?, Integer> getA1();

    @Alias("sa")
    void setA1(@AsyncInput(value = int[].class, mode = InputMode.DEFAULT) Channel<?, int[]> i);

    @Alias("ga")
    @AsyncMethod({})
    Channel<Void, int[]> getA2();

    @Alias("sa")
    void setA2(@AsyncInput(value = int[].class, mode = InputMode.COLLECTION) Channel<?, Integer> i);

    @Alias("ga")
    @AsyncMethod({})
    Routine<Void, int[]> getA3();

    @Alias("ga")
    @AsyncMethod(value = {}, mode = OutputMode.ELEMENT)
    Channel<Void, Integer> getA4();

    @Alias("ga")
    @AsyncMethod(value = {}, mode = OutputMode.ELEMENT)
    Routine<Void, Integer> getA5();

    @Alias("gl")
    List<Integer> getL0();

    @Alias("sl")
    void setL0(List<Integer> i);

    @Alias("gl")
    @AsyncOutput(OutputMode.ELEMENT)
    Channel<?, Integer> getL1();

    @Alias("sl")
    void setL1(
        @AsyncInput(value = List.class, mode = InputMode.DEFAULT) Channel<?, List<Integer>> i);

    @Alias("gl")
    @AsyncMethod({})
    Channel<Void, List<Integer>> getL2();

    @Alias("sl")
    void setL2(@AsyncInput(value = List.class, mode = InputMode.COLLECTION) Channel<?, Integer> i);

    @Alias("gl")
    @AsyncMethod({})
    Routine<Void, List<Integer>> getL3();

    @Alias("gl")
    @AsyncMethod(value = {}, mode = OutputMode.ELEMENT)
    Channel<Void, Integer> getL4();

    @Alias("gl")
    @AsyncMethod(value = {}, mode = OutputMode.ELEMENT)
    Routine<Void, Integer> getL5();

    @Alias("s")
    @AsyncMethod(int.class)
    Channel<Integer, Void> set3();

    @Alias("s")
    @AsyncMethod(int.class)
    Routine<Integer, Void> set5();

    @Alias("sa")
    @AsyncMethod(int[].class)
    Channel<int[], Void> setA4();

    @Alias("sa")
    @AsyncMethod(int[].class)
    Routine<int[], Void> setA6();

    @Alias("sl")
    @AsyncMethod(List.class)
    Channel<List<Integer>, Void> setL4();

    @Alias("sl")
    @AsyncMethod(List.class)
    Routine<List<Integer>, Void> setL6();
  }

  private interface CountError {

    @AsyncOutput
    String[] count(int length);

    @Alias("count")
    @AsyncOutput(OutputMode.ELEMENT)
    String[] count1(int length);

    @AsyncOutput(OutputMode.DEFAULT)
    List<Integer> countList(int length);

    @Alias("countList")
    @AsyncOutput(OutputMode.ELEMENT)
    List<Integer> countList1(int length);
  }

  private interface CountItf {

    @AsyncOutput(OutputMode.ELEMENT)
    Channel<?, Integer> count(int length);

    @Alias("count")
    @AsyncOutput(OutputMode.DEFAULT)
    Channel<?, int[]> count1(int length);

    @Alias("count")
    @AsyncOutput(OutputMode.ELEMENT)
    Channel<?, Integer> count2(int length);

    @AsyncOutput(OutputMode.ELEMENT)
    Channel<?, Integer> countList(int length);

    @Alias("countList")
    @AsyncOutput(OutputMode.ELEMENT)
    Channel<?, Integer> countList1(int length);
  }

  private interface SquareItf {

    @OutputTimeout(value = 10, unit = TimeUnit.SECONDS)
    int compute(int i);

    @Alias("compute")
    @OutputTimeout(10000)
    int computeAsync(@AsyncInput(int.class) Channel<?, Integer> i);
  }

  private interface SumError {

    int compute(int a, @AsyncInput(int.class) int[] b);

    int compute(@AsyncInput(int.class) String[] ints);

    int compute(@AsyncInput(value = int.class, mode = InputMode.DEFAULT) int[] ints);

    int compute(@AsyncInput(value = int.class, mode = InputMode.COLLECTION) Iterable<Integer> ints);

    int compute(
        @AsyncInput(value = int.class, mode = InputMode.COLLECTION) Channel<?, Integer> ints);

    int compute(int a,
        @AsyncInput(value = int[].class, mode = InputMode.COLLECTION) Channel<?, Integer> b);
  }

  private interface SumItf {

    int compute(int a, @AsyncInput(int.class) Channel<?, Integer> b);

    int compute(
        @AsyncInput(value = int[].class, mode = InputMode.COLLECTION) Channel<?, Integer> ints);

    @Alias("compute")
    int compute1(@AsyncInput(int[].class) Channel<?, int[]> ints);

    @Alias("compute")
    int computeList(
        @AsyncInput(value = List.class, mode = InputMode.COLLECTION) Channel<?, Integer> ints);

    @Alias("compute")
    int computeList1(
        @AsyncInput(value = List.class, mode = InputMode.COLLECTION) Channel<?, Integer> ints);
  }

  @SuppressWarnings("unused")
  private interface TestItf {

    void throwException(@AsyncInput(int.class) RuntimeException ex);

    @Alias(TestClass.THROW)
    @AsyncOutput
    void throwException1(RuntimeException ex);

    @Alias(TestClass.THROW)
    int throwException2(RuntimeException ex);
  }

  private interface TestTimeoutItf {

    @OutputTimeoutAction(TimeoutActionType.ABORT)
    int getInt();
  }

  @SuppressWarnings("unused")
  public static class Impl {

    @Alias("a")
    public int add(char c) {

      return c;
    }

    @Alias("aa")
    public int[] addArray(char[] c) {

      final int[] array = new int[c.length];

      for (int i = 0; i < c.length; i++) {

        array[i] = c[i];
      }

      return array;
    }

    @Alias("al")
    public List<Integer> addList(List<Character> c) {

      final ArrayList<Integer> list = new ArrayList<Integer>(c.size());

      for (final Character character : c) {

        list.add((int) character);
      }

      return list;
    }

    @Alias("g")
    public int get() {

      return 31;
    }

    @Alias("ga")
    public int[] getArray() {

      return new int[]{1, 2, 3};
    }

    @Alias("sa")
    public void setArray(int[] i) {

      assertThat(i).containsExactly(1, 2, 3);
    }

    @Alias("gl")
    public List<Integer> getList() {

      return Arrays.asList(1, 2, 3);
    }

    @Alias("sl")
    public void setList(List<Integer> l) {

      assertThat(l).containsExactly(1, 2, 3);
    }

    @Alias("s")
    public void set(int i) {

      assertThat(i).isEqualTo(-17);
    }
  }

  public static class SharedFieldRunner extends RunnerDecorator {

    private static final Runner sRunner = Runners.poolRunner(2);

    public SharedFieldRunner() {
      super(sRunner);
    }

    @Override
    public void stop() {
    }
  }

  @SuppressWarnings("unused")
  private static class Count {

    public int[] count(final int length) {

      final int[] array = new int[length];

      for (int i = 0; i < length; i++) {

        array[i] = i;
      }

      return array;
    }

    public List<Integer> countList(final int length) {

      final ArrayList<Integer> list = new ArrayList<Integer>(length);

      for (int i = 0; i < length; i++) {

        list.add(i);
      }

      return list;
    }
  }

  @SuppressWarnings("unused")
  private static class CountLog implements Log {

    private int mDgbCount;

    private int mErrCount;

    private int mWrnCount;

    public void dbg(@NotNull final List<Object> contexts, @Nullable final String message,
        @Nullable final Throwable throwable) {

      ++mDgbCount;
    }

    public void err(@NotNull final List<Object> contexts, @Nullable final String message,
        @Nullable final Throwable throwable) {

      ++mErrCount;
    }

    public void wrn(@NotNull final List<Object> contexts, @Nullable final String message,
        @Nullable final Throwable throwable) {

      ++mWrnCount;
    }

    public int getDgbCount() {

      return mDgbCount;
    }

    public int getErrCount() {

      return mErrCount;
    }

    public int getWrnCount() {

      return mWrnCount;
    }
  }

  @SuppressWarnings("unused")
  private static class DuplicateAnnotation {

    public static final String GET = "get";

    @Alias(GET)
    public int getOne() {

      return 1;
    }

    @Alias(GET)
    public int getTwo() {

      return 2;
    }
  }

  @SuppressWarnings("unused")
  private static class Inc {

    public int inc(final int i) {

      return i + 1;
    }
  }

  @SuppressWarnings("unused")
  private static class Square {

    public int compute(final int i) {

      return i * i;
    }
  }

  @SuppressWarnings("unused")
  private static class Sum {

    public int compute(final int a, final int b) {

      return a + b;
    }

    public int compute(final int... ints) {

      int s = 0;

      for (final int i : ints) {

        s += i;
      }

      return s;
    }

    public int compute(final List<Integer> ints) {

      int s = 0;

      for (final int i : ints) {

        s += i;
      }

      return s;
    }
  }

  @SuppressWarnings("unused")
  private static class TestArgs {

    private final int mId;

    public TestArgs(final int id) {

      mId = id;
    }

    public int getId() {

      return mId;
    }
  }

  @SuppressWarnings("unused")
  private static class TestClass {

    public static final String GET = "get";

    public static final String THROW = "throw";

    @Alias(GET)
    public long getLong() {

      return -77;

    }

    @Alias(THROW)
    public void throwException(final RuntimeException ex) {

      throw ex;
    }
  }

  @SuppressWarnings("unused")
  private static class TestClass2 {

    public int getOne() throws InterruptedException {
      DurationMeasure.seconds(2).sleepAtLeast();
      return 1;
    }

    public int getTwo() throws InterruptedException {
      DurationMeasure.seconds(2).sleepAtLeast();
      return 2;
    }
  }

  private static class TestTimeout {

    @Alias("test")
    @OutputTimeoutAction(TimeoutActionType.CONTINUE)
    public int getInt() throws InterruptedException {
      Thread.sleep(100);
      return 31;
    }
  }
}
