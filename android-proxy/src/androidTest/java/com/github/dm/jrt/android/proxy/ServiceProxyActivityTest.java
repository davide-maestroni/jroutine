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

package com.github.dm.jrt.android.proxy;

import android.annotation.TargetApi;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.core.ServiceContext;
import com.github.dm.jrt.android.proxy.annotation.ServiceProxy;
import com.github.dm.jrt.android.proxy.builder.ServiceProxyObjectBuilder;
import com.github.dm.jrt.android.proxy.builder.ServiceProxyRoutineBuilder;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration.TimeoutActionType;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.executor.ScheduledExecutorDecorator;
import com.github.dm.jrt.core.executor.ScheduledExecutors;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.log.Log;
import com.github.dm.jrt.core.log.Log.Level;
import com.github.dm.jrt.core.log.NullLog;
import com.github.dm.jrt.core.routine.Routine;
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.github.dm.jrt.android.core.ServiceContext.serviceFrom;
import static com.github.dm.jrt.android.reflect.ContextInvocationTarget.classOfType;
import static com.github.dm.jrt.android.reflect.ContextInvocationTarget.instanceOf;
import static com.github.dm.jrt.core.config.InvocationConfiguration.builder;
import static com.github.dm.jrt.core.util.DurationMeasure.indefiniteTime;
import static com.github.dm.jrt.core.util.DurationMeasure.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proxy builder Activity unit tests.
 * <p>
 * Created by davide-maestroni on 05/07/2015.
 */
@TargetApi(VERSION_CODES.FROYO)
public class ServiceProxyActivityTest extends ActivityInstrumentationTestCase2<TestActivity> {

  public ServiceProxyActivityTest() {

    super(TestActivity.class);
  }

  public void testClassStaticMethod() {

    final TestStatic testStatic =
        JRoutineServiceProxy.on(serviceFrom(getActivity(), TestService.class))
                            .with(classOfType(TestClass.class))
                            .withInvocation()
                            .withExecutor(ScheduledExecutors.poolExecutor())
                            .withLogLevel(Level.DEBUG)
                            .withLog(new NullLog())
                            .configured()
                            .buildProxy(TestStatic.class);

    try {

      assertThat(testStatic.getOne().all()).containsExactly(1);

      fail();

    } catch (final InvocationException ignored) {

    }

    assertThat(testStatic.getTwo().all()).containsExactly(2);
  }

  public void testConstructor() {

    boolean failed = false;
    try {
      new JRoutineServiceProxy();
      failed = true;

    } catch (final Throwable ignored) {

    }

    assertThat(failed).isFalse();
  }

  public void testGenericProxyCache() {

    final ServiceProxyRoutineBuilder builder =
        JRoutineServiceProxy.on(serviceFrom(getActivity(), TestService.class))
                            .with(instanceOf(TestList.class))
                            .withInvocation()
                            .withOutputTimeout(seconds(10))
                            .configuration();

    final TestListItf<String> testListItf1 =
        builder.buildProxy(new ClassToken<TestListItf<String>>() {});
    testListItf1.add("test");

    assertThat(testListItf1.get(0)).isEqualTo("test");
    assertThat(builder.buildProxy(new ClassToken<TestListItf<Integer>>() {})).isSameAs(
        testListItf1);

    final TestListItf<Integer> testListItf2 =
        builder.buildProxy(new ClassToken<TestListItf<Integer>>() {});
    assertThat(testListItf2).isSameAs(testListItf1);
    assertThat(builder.buildProxy(new ClassToken<TestListItf<Integer>>() {})).isSameAs(
        testListItf2);

    testListItf2.add(3);
    assertThat(testListItf2.get(1)).isEqualTo(3);
    assertThat(testListItf2.getAsync(1).next()).isEqualTo(3);
  }

  public void testInterface() {

    final ClassToken<TestInterfaceProxy> token = ClassToken.tokenOf(TestInterfaceProxy.class);
    final TestInterfaceProxy testProxy =
        JRoutineServiceProxy.on(serviceFrom(getActivity(), TestService.class))
                            .with(instanceOf(TestClass.class))
                            .buildProxy(token);

    assertThat(testProxy.getOne().next()).isEqualTo(1);
  }

  @SuppressWarnings("ConstantConditions")
  public void testNullPointerError() {

    try {
      JRoutineServiceProxy.on(null);
      fail();

    } catch (final NullPointerException ignored) {

    }

    try {
      JRoutineServiceProxy.on(serviceFrom(getActivity()))
                          .with(instanceOf(TestClass.class))
                          .buildProxy((Class<?>) null);
      fail();

    } catch (final NullPointerException ignored) {

    }

    try {
      JRoutineServiceProxy.on(serviceFrom(getActivity()))
                          .with(instanceOf(TestClass.class))
                          .buildProxy((ClassToken<?>) null);
      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  public void testObjectStaticMethod() {

    final TestStatic testStatic =
        JRoutineServiceProxy.on(serviceFrom(getActivity(), TestService.class))
                            .with(instanceOf(TestClass.class))
                            .withInvocation()
                            .withExecutor(ScheduledExecutors.poolExecutor())
                            .withLogLevel(Level.DEBUG)
                            .withLog(new NullLog())
                            .configured()
                            .buildProxy(TestStatic.class);

    assertThat(testStatic.getOne().all()).containsExactly(1);
    assertThat(testStatic.getTwo().all()).containsExactly(2);
  }

  public void testProxy() {

    final NullLog log = new NullLog();
    final TestProxy testProxy =
        JRoutineServiceProxy.on(serviceFrom(getActivity(), TestService.class))
                            .with(instanceOf(TestClass.class))
                            .withInvocation()
                            .withExecutor(ScheduledExecutors.poolExecutor())
                            .withLogLevel(Level.DEBUG)
                            .withLog(log)
                            .configured()
                            .buildProxy(ClassToken.tokenOf(TestProxy.class));

    assertThat(testProxy.getOne().next()).isEqualTo(1);

    final ArrayList<String> list = new ArrayList<String>();
    assertThat(testProxy.getList(JRoutineCore.<List<String>>of(list).buildChannel())
                        .iterator()
                        .next()).isEqualTo(list);

    assertThat(testProxy.getString(JRoutineCore.of(3).buildChannel())).isEqualTo("3");
  }

  public void testProxyBuilder() {

    final NullLog log = new NullLog();
    final InvocationConfiguration configuration =
        builder().withLogLevel(Level.DEBUG).withLog(log).configuration();
    final ServiceContext serviceContext = serviceFrom(getActivity(), TestService.class);
    final ServiceProxyObjectBuilder<TestProxy> builder =
        com.github.dm.jrt.android.proxy.ServiceProxy_Test.on(serviceContext)
                                                         .with(instanceOf(TestClass.class));
    final TestProxy testProxy = builder.withInvocation()
                                       .withPatch(configuration)
                                       .configuration()
                                       .serviceConfiguration()
                                       .withExecutorClass(MyExecutor.class)
                                       .apply()
                                       .withWrapper()
                                       .withSharedFields()
                                       .configuration()
                                       .buildProxy();

    assertThat(testProxy.getOne().next()).isEqualTo(1);

    final ArrayList<String> list = new ArrayList<String>();
    assertThat(testProxy.getList(JRoutineCore.<List<String>>of(list).buildChannel())
                        .iterator()
                        .next()).isEqualTo(list);

    assertThat(testProxy.getString(JRoutineCore.of(3).buildChannel())).isEqualTo("3");

    assertThat(JRoutineServiceProxy.on(serviceFrom(getActivity(), TestService.class))
                                   .with(instanceOf(TestClass.class))
                                   .withInvocation()
                                   .withPatch(configuration)
                                   .configuration()
                                   .serviceConfiguration()
                                   .withExecutorClass(MyExecutor.class)
                                   .apply()
                                   .withWrapper()
                                   .withSharedFields()
                                   .configuration()
                                   .buildProxy(ClassToken.tokenOf(TestProxy.class))).isSameAs(
        testProxy);
  }

  public void testProxyCache() {

    final NullLog log = new NullLog();
    final ScheduledExecutor executor = ScheduledExecutors.poolExecutor();
    final InvocationConfiguration configuration =
        builder().withExecutor(executor).withLogLevel(Level.DEBUG).withLog(log).configured();
    final TestProxy testProxy =
        JRoutineServiceProxy.on(serviceFrom(getActivity(), TestService.class))
                            .with(instanceOf(TestClass.class))
                            .withInvocation()
                            .withPatch(configuration)
                            .configuration()
                            .buildProxy(ClassToken.tokenOf(TestProxy.class));

    assertThat(JRoutineServiceProxy.on(serviceFrom(getActivity(), TestService.class))
                                   .with(instanceOf(TestClass.class))
                                   .withInvocation()
                                   .withPatch(configuration)
                                   .configuration()
                                   .buildProxy(ClassToken.tokenOf(TestProxy.class))).isSameAs(
        testProxy);
  }

  public void testProxyError() {

    try {

      JRoutineServiceProxy.on(serviceFrom(getActivity()))
                          .with(instanceOf(TestClass.class))
                          .buildProxy(TestClass.class);

      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {

      JRoutineServiceProxy.on(serviceFrom(getActivity()))
                          .with(instanceOf(TestClass.class))
                          .buildProxy(ClassToken.tokenOf(TestClass.class));

      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  public void testSharedFields() {

    final ServiceProxyRoutineBuilder builder =
        JRoutineServiceProxy.on(serviceFrom(getActivity(), TestService.class))
                            .with(instanceOf(TestClass2.class))
                            .serviceConfiguration()
                            .withExecutorClass(SharedFieldExecutor.class)
                            .apply()
                            .withInvocation()
                            .withOutputTimeout(seconds(10))
                            .configuration();

    long startTime = System.currentTimeMillis();

    Channel<?, Integer> getOne = builder.withWrapper()
                                        .withSharedFields("1")
                                        .configuration()
                                        .buildProxy(TestClassAsync.class)
                                        .getOne();
    Channel<?, Integer> getTwo = builder.withWrapper()
                                        .withSharedFields("2")
                                        .configuration()
                                        .buildProxy(TestClassAsync.class)
                                        .getTwo();

    assertThat(getOne.next()).isEqualTo(1);
    assertThat(getOne.getComplete()).isTrue();
    assertThat(getTwo.getComplete()).isTrue();
    assertThat(System.currentTimeMillis() - startTime).isLessThan(4000);

    startTime = System.currentTimeMillis();

    getOne = builder.buildProxy(TestClassAsync.class).getOne();
    getTwo = builder.buildProxy(TestClassAsync.class).getTwo();

    assertThat(getOne.getComplete()).isTrue();
    assertThat(getTwo.getComplete()).isTrue();
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(4000);
  }

  @SuppressWarnings("unchecked")
  public void testTemplates() {

    final Itf itf = JRoutineServiceProxy.on(serviceFrom(getActivity(), TestService.class))
                                        .with(instanceOf(Impl.class))
                                        .withInvocation()
                                        .withOutputTimeout(indefiniteTime())
                                        .configuration()
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

  public void testTimeoutActionAnnotation() throws NoSuchMethodException {

    assertThat(JRoutineServiceProxy.on(serviceFrom(getActivity(), TestService.class))
                                   .with(instanceOf(TestTimeout.class))
                                   .withInvocation()
                                   .withOutputTimeout(seconds(10))
                                   .configuration()
                                   .buildProxy(TestTimeoutItf.class)
                                   .getInt()).isEqualTo(31);

    try {

      JRoutineServiceProxy.on(serviceFrom(getActivity(), TestService.class))
                          .with(instanceOf(TestTimeout.class))
                          .withInvocation()
                          .withOutputTimeoutAction(TimeoutActionType.FAIL)
                          .configuration()
                          .buildProxy(TestTimeoutItf.class)
                          .getInt();

      fail();

    } catch (final AbortException ignored) {

    }
  }

  @ServiceProxy(Impl.class)
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

  @ServiceProxy(TestClass2.class)
  public interface TestClassAsync {

    @AsyncOutput
    Channel<?, Integer> getOne();

    @AsyncOutput
    Channel<?, Integer> getTwo();
  }

  @SuppressWarnings("unused")
  public interface TestClassInterface {

    int getOne();
  }

  @ServiceProxy(TestClassInterface.class)
  public interface TestInterfaceProxy {

    @OutputTimeout(10000)
    @AsyncOutput
    Channel<?, Integer> getOne();
  }

  @ServiceProxy(TestList.class)
  public interface TestListItf<TYPE> {

    void add(Object t);

    TYPE get(int i);

    @Alias("get")
    @AsyncOutput
    Channel<?, TYPE> getAsync(int i);
  }

  @ServiceProxy(value = TestClass.class, className = "Test",
      classPackage = "com.github.dm.jrt.android.proxy")
  public interface TestProxy {

    @OutputTimeout(10000)
    @AsyncOutput
    Iterable<Iterable> getList(@AsyncInput(List.class) Channel<?, List<String>> i);

    @OutputTimeout(10000)
    @AsyncOutput
    Channel<?, Integer> getOne();

    @OutputTimeout(10000)
    String getString(@AsyncInput(int.class) Channel<?, Integer> i);
  }

  @ServiceProxy(TestClass.class)
  public interface TestStatic {

    @OutputTimeout(10000)
    @AsyncOutput
    Channel<?, Integer> getOne();

    @OutputTimeout(10000)
    @AsyncOutput
    Channel<?, Integer> getTwo();
  }

  @ServiceProxy(TestTimeout.class)
  public interface TestTimeoutItf {

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

  public static class SharedFieldExecutor extends ScheduledExecutorDecorator {

    public SharedFieldExecutor() {

      super(ScheduledExecutors.poolExecutor(2));
    }
  }

  @SuppressWarnings("unused")
  public static class TestClass implements TestClassInterface {

    public static int getTwo() {

      return 2;
    }

    public List<String> getList(final List<String> list) {

      return list;
    }

    public int getOne() {

      return 1;
    }

    public String getString(final int i) {

      return Integer.toString(i);
    }
  }

  @SuppressWarnings("unused")
  public static class TestClass2 {

    public int getOne() throws InterruptedException {

      DurationMeasure.millis(2000).sleepAtLeast();

      return 1;
    }

    public int getTwo() throws InterruptedException {

      DurationMeasure.millis(2000).sleepAtLeast();

      return 2;
    }
  }

  @SuppressWarnings("unused")
  public static class TestList<TYPE> {

    private final ArrayList<TYPE> mList = new ArrayList<TYPE>();

    public void add(TYPE t) {

      mList.add(t);
    }

    public TYPE get(int i) {

      return mList.get(i);
    }
  }

  @SuppressWarnings("unused")
  public static class TestTimeout {

    public int getInt() throws InterruptedException {

      Thread.sleep(100);
      return 31;
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

  private static class MyExecutor extends ScheduledExecutorDecorator {

    public MyExecutor() {

      super(ScheduledExecutors.poolExecutor());
    }
  }
}
