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

package com.github.dm.jrt.object.core;

import com.github.dm.jrt.builder.InvocationConfiguration;
import com.github.dm.jrt.builder.InvocationConfiguration.AgingPriority;
import com.github.dm.jrt.builder.InvocationConfiguration.OrderType;
import com.github.dm.jrt.builder.InvocationConfiguration.TimeoutActionType;
import com.github.dm.jrt.channel.AbortException;
import com.github.dm.jrt.channel.Channel.InputChannel;
import com.github.dm.jrt.channel.Channel.OutputChannel;
import com.github.dm.jrt.channel.IOChannel;
import com.github.dm.jrt.channel.InvocationChannel;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.invocation.InvocationException;
import com.github.dm.jrt.log.Log;
import com.github.dm.jrt.log.Log.Level;
import com.github.dm.jrt.log.NullLog;
import com.github.dm.jrt.object.annotation.Alias;
import com.github.dm.jrt.object.annotation.AsyncIn;
import com.github.dm.jrt.object.annotation.AsyncIn.InputMode;
import com.github.dm.jrt.object.annotation.AsyncMethod;
import com.github.dm.jrt.object.annotation.AsyncOut;
import com.github.dm.jrt.object.annotation.AsyncOut.OutputMode;
import com.github.dm.jrt.object.annotation.CoreInstances;
import com.github.dm.jrt.object.annotation.InputLimit;
import com.github.dm.jrt.object.annotation.InputMaxDelay;
import com.github.dm.jrt.object.annotation.InputMaxSize;
import com.github.dm.jrt.object.annotation.InputOrder;
import com.github.dm.jrt.object.annotation.Invoke;
import com.github.dm.jrt.object.annotation.Invoke.InvocationMode;
import com.github.dm.jrt.object.annotation.LogLevel;
import com.github.dm.jrt.object.annotation.MaxInstances;
import com.github.dm.jrt.object.annotation.OutputLimit;
import com.github.dm.jrt.object.annotation.OutputMaxDelay;
import com.github.dm.jrt.object.annotation.OutputMaxSize;
import com.github.dm.jrt.object.annotation.OutputOrder;
import com.github.dm.jrt.object.annotation.Priority;
import com.github.dm.jrt.object.annotation.ReadTimeout;
import com.github.dm.jrt.object.annotation.ReadTimeoutAction;
import com.github.dm.jrt.object.annotation.SharedFields;
import com.github.dm.jrt.object.builder.ObjectRoutineBuilder;
import com.github.dm.jrt.object.builder.ProxyConfiguration;
import com.github.dm.jrt.routine.Routine;
import com.github.dm.jrt.runner.Execution;
import com.github.dm.jrt.runner.Runner;
import com.github.dm.jrt.runner.Runners;
import com.github.dm.jrt.util.ClassToken;
import com.github.dm.jrt.util.TimeDuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.builder.InvocationConfiguration.builder;
import static com.github.dm.jrt.object.core.Builders.configurationWithAnnotations;
import static com.github.dm.jrt.object.core.InvocationTarget.classOfType;
import static com.github.dm.jrt.object.core.InvocationTarget.instance;
import static com.github.dm.jrt.util.TimeDuration.INFINITY;
import static com.github.dm.jrt.util.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Object routine unit tests.
 * <p/>
 * Created by davide-maestroni on 03/27/2015.
 */
public class ObjectRoutineTest {

    @Test
    public void testAgingPriority() {

        final Pass pass = new Pass();
        final TestRunner runner = new TestRunner();
        final PriorityPass priorityPass = JRoutineObject.on(instance(pass))
                                                        .withInvocations()
                                                        .withRunner(runner)
                                                        .getConfigured()
                                                        .buildProxy(PriorityPass.class);
        final OutputChannel<String> output1 = priorityPass.passNormal("test1").eventuallyExit();

        for (int i = 0; i < AgingPriority.HIGH_PRIORITY - 1; i++) {

            priorityPass.passHigh("test2");
            runner.run(1);
            assertThat(output1.all()).isEmpty();
        }

        final OutputChannel<String> output2 = priorityPass.passHigh("test2");
        runner.run(1);
        assertThat(output1.all()).containsExactly("test1");
        runner.run(Integer.MAX_VALUE);
        assertThat(output2.all()).containsExactly("test2");
    }

    @Test
    public void testAliasMethod() throws NoSuchMethodException {

        final TimeDuration timeout = seconds(1);
        final TestClass test = new TestClass();
        final Routine<Object, Object> routine = JRoutineObject.on(instance(test))
                                                              .withInvocations()
                                                              .withRunner(Runners.poolRunner())
                                                              .withMaxInstances(1)
                                                              .withCoreInstances(1)
                                                              .withReadTimeoutAction(
                                                                      TimeoutActionType.EXIT)
                                                              .withLogLevel(Level.DEBUG)
                                                              .withLog(new NullLog())
                                                              .getConfigured()
                                                              .aliasMethod(TestClass.GET);

        assertThat(routine.syncCall().afterMax(timeout).all()).containsExactly(-77L);
    }

    @Test
    public void testAliasStaticMethod() throws NoSuchMethodException {

        final TimeDuration timeout = seconds(1);
        final Routine<Object, Object> routine = JRoutineObject.on(classOfType(TestStatic.class))
                                                              .withInvocations()
                                                              .withRunner(Runners.poolRunner())
                                                              .withLogLevel(Level.DEBUG)
                                                              .withLog(new NullLog())
                                                              .getConfigured()
                                                              .aliasMethod(TestStatic.GET);

        assertThat(routine.syncCall().afterMax(timeout).all()).containsExactly(-77L);
    }

    @Test
    public void testAliasStaticMethodError() {

        try {

            JRoutineObject.on(classOfType(TestStatic.class)).aliasMethod("test");

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testAnnotationGenerics() {

        final Size size = new Size();
        final SizeItf proxy = JRoutineObject.on(instance(size)).buildProxy(SizeItf.class);
        assertThat(
                proxy.getSize(Arrays.asList("test1", "test2", "test3")).afterMax(seconds(3)).next())
                .isEqualTo(3);
        assertThat(proxy.getSize()
                        .pass(Arrays.asList("test1", "test2", "test3"))
                        .result()
                        .afterMax(seconds(3))
                        .next()).isEqualTo(3);
    }

    @Test
    public void testAsyncInputProxyRoutine() {

        final TimeDuration timeout = seconds(1);
        final Sum sum = new Sum();
        final SumItf sumAsync = JRoutineObject.on(instance(sum))
                                              .withInvocations()
                                              .withReadTimeout(timeout)
                                              .getConfigured()
                                              .buildProxy(SumItf.class);
        final IOChannel<Integer> channel3 = JRoutineCore.io().buildChannel();
        channel3.pass(7).close();
        assertThat(sumAsync.compute(3, channel3)).isEqualTo(10);

        final IOChannel<Integer> channel4 = JRoutineCore.io().buildChannel();
        channel4.pass(1, 2, 3, 4).close();
        assertThat(sumAsync.compute(channel4)).isEqualTo(10);

        final IOChannel<int[]> channel5 = JRoutineCore.io().buildChannel();
        channel5.pass(new int[]{1, 2, 3, 4}).close();
        assertThat(sumAsync.compute1(channel5)).isEqualTo(10);
        assertThat(sumAsync.compute2().pass(new int[]{1, 2, 3, 4}).result().next()).isEqualTo(10);
        assertThat(sumAsync.compute3().pass(17).result().next()).isEqualTo(17);

        final IOChannel<Integer> channel6 = JRoutineCore.io().buildChannel();
        channel6.pass(1, 2, 3, 4).close();
        assertThat(sumAsync.computeList(channel6)).isEqualTo(10);

        final IOChannel<Integer> channel7 = JRoutineCore.io().buildChannel();
        channel7.pass(1, 2, 3, 4).close();
        assertThat(sumAsync.computeList1(channel7)).isEqualTo(10);
    }

    @Test
    public void testAsyncOutputProxyRoutine() {

        final TimeDuration timeout = seconds(1);
        final Count count = new Count();
        final CountItf countAsync = JRoutineObject.on(instance(count))
                                                  .withInvocations()
                                                  .withReadTimeout(timeout)
                                                  .getConfigured()
                                                  .buildProxy(CountItf.class);
        assertThat(countAsync.count(3).all()).containsExactly(0, 1, 2);
        assertThat(countAsync.count1(3).all()).containsExactly(new int[]{0, 1, 2});
        assertThat(countAsync.count2(2).all()).containsExactly(0, 1);
        assertThat(countAsync.countList(3).all()).containsExactly(0, 1, 2);
        assertThat(countAsync.countList1(3).all()).containsExactly(0, 1, 2);
    }

    @Test
    public void testBuilderConfigurationThroughAnnotations() throws NoSuchMethodException {

        assertThat(configurationWithAnnotations(InvocationConfiguration.DEFAULT_CONFIGURATION,
                                                AnnotationItf.class.getMethod(
                                                        "toString"))).isEqualTo(
                builder().withCoreInstances(3)
                         .withInputOrder(OrderType.BY_DELAY)
                         .withInputLimit(71)
                         .withInputMaxDelay(7777, TimeUnit.MICROSECONDS)
                         .withInputMaxSize(33)
                         .withLogLevel(Level.WARNING)
                         .withMaxInstances(17)
                         .withOutputOrder(OrderType.BY_CALL)
                         .withOutputLimit(31)
                         .withOutputMaxDelay(3333, TimeUnit.NANOSECONDS)
                         .withOutputMaxSize(77)
                         .withPriority(41)
                         .withReadTimeout(1111, TimeUnit.MICROSECONDS)
                         .withReadTimeoutAction(TimeoutActionType.ABORT)
                         .getConfigured());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testConfigurationErrors() {

        try {

            new DefaultObjectRoutineBuilder(classOfType(TestStatic.class)).setConfiguration(
                    (InvocationConfiguration) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultObjectRoutineBuilder(classOfType(TestStatic.class)).setConfiguration(
                    (ProxyConfiguration) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testDuplicateAnnotationError() {

        try {

            JRoutineObject.on(instance(new DuplicateAnnotation()))
                          .aliasMethod(DuplicateAnnotation.GET);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutineObject.on(classOfType(DuplicateAnnotationStatic.class))
                          .aliasMethod(DuplicateAnnotationStatic.GET);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testException() throws NoSuchMethodException {

        final TimeDuration timeout = seconds(1);
        final TestClass test = new TestClass();
        final Routine<Object, Object> routine3 =
                JRoutineObject.on(instance(test)).aliasMethod(TestClass.THROW);

        try {

            routine3.syncCall(new IllegalArgumentException("test")).afterMax(timeout).all();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
            assertThat(e.getCause().getMessage()).isEqualTo("test");
        }
    }

    @Test
    public void testException2() throws NoSuchMethodException {

        final TimeDuration timeout = seconds(1);

        final Routine<Object, Object> routine3 =
                JRoutineObject.on(classOfType(TestStatic.class)).aliasMethod(TestStatic.THROW);

        try {

            routine3.syncCall(new IllegalArgumentException("test")).afterMax(timeout).all();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
            assertThat(e.getCause().getMessage()).isEqualTo("test");
        }
    }

    @Test
    public void testInterfaceError() {

        try {

            JRoutineObject.on(classOfType(TestItf.class));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testInvalidInputsAnnotationError() {

        final Sum sum = new Sum();

        try {

            JRoutineObject.on(instance(sum)).buildProxy(SumError2.class).compute1();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutineObject.on(instance(sum)).buildProxy(SumError2.class).compute2();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutineObject.on(instance(sum)).buildProxy(SumError2.class).compute3();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutineObject.on(instance(sum)).buildProxy(SumError2.class).compute4();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutineObject.on(instance(sum)).buildProxy(SumError2.class).compute5(7);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testInvalidProxyError() {

        final TestClass test = new TestClass();

        try {

            JRoutineObject.on(instance(test)).buildProxy(TestClass.class);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutineObject.on(instance(test)).buildProxy(ClassToken.tokenOf(TestClass.class));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testInvalidProxyInputAnnotationError() {

        final Sum sum = new Sum();

        try {

            JRoutineObject.on(instance(sum)).buildProxy(SumError.class).compute(1, new int[0]);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutineObject.on(instance(sum)).buildProxy(SumError.class).compute(new String[0]);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutineObject.on(instance(sum)).buildProxy(SumError.class).compute(new int[0]);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutineObject.on(instance(sum))
                          .buildProxy(SumError.class)
                          .compute(Collections.<Integer>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        final IOChannel<Integer> channel = JRoutineCore.io().buildChannel();

        try {

            JRoutineObject.on(instance(sum)).buildProxy(SumError.class).compute(channel);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutineObject.on(instance(sum)).buildProxy(SumError.class).compute(1, channel);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutineObject.on(instance(sum)).buildProxy(SumError.class).compute("test", channel);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testInvalidProxyMethodError() {

        final TestClass test = new TestClass();

        try {

            JRoutineObject.on(instance(test))
                          .withInvocations()
                          .withReadTimeout(INFINITY)
                          .getConfigured()
                          .buildProxy(TestItf.class)
                          .throwException(null);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutineObject.on(instance(test))
                          .withInvocations()
                          .withReadTimeout(INFINITY)
                          .getConfigured()
                          .buildProxy(TestItf.class)
                          .throwException1(null);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutineObject.on(instance(test)).buildProxy(TestItf.class).throwException2(null);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testInvalidProxyOutputAnnotationError() {

        final Count count = new Count();

        try {

            JRoutineObject.on(instance(count)).buildProxy(CountError.class).count(3);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutineObject.on(instance(count)).buildProxy(CountError.class).count1(3);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutineObject.on(instance(count)).buildProxy(CountError.class).count2();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutineObject.on(instance(count)).buildProxy(CountError.class).countList(3);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutineObject.on(instance(count)).buildProxy(CountError.class).countList1(3);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testMethod() throws NoSuchMethodException {

        final TimeDuration timeout = seconds(1);
        final TestClass test = new TestClass();
        final Routine<Object, Object> routine2 = JRoutineObject.on(instance(test))
                                                               .withInvocations()
                                                               .withRunner(Runners.poolRunner())
                                                               .withMaxInstances(1)
                                                               .getConfigured()
                                                               .withProxies()
                                                               .withSharedFields("test")
                                                               .getConfigured()
                                                               .method(TestClass.class.getMethod(
                                                                       "getLong"));

        assertThat(routine2.syncCall().afterMax(timeout).all()).containsExactly(-77L);
    }

    @Test
    public void testMethodBySignature() throws NoSuchMethodException {

        final TimeDuration timeout = seconds(1);
        final TestClass test = new TestClass();
        final Routine<Object, Object> routine1 = JRoutineObject.on(instance(test))
                                                               .withInvocations()
                                                               .withRunner(Runners.poolRunner())
                                                               .getConfigured()
                                                               .method("getLong");

        assertThat(routine1.syncCall().afterMax(timeout).all()).containsExactly(-77L);
    }

    @Test
    public void testMethodInterface() throws NoSuchMethodException {

        final TimeDuration timeout = seconds(1);
        final TestClassImpl test = new TestClassImpl();
        assertThat(JRoutineObject.on(instance(test))
                                 .method(TestClassImpl.class.getMethod("getOne"))
                                 .syncCall()
                                 .afterMax(timeout)
                                 .all()).containsExactly(1);
        assertThat(JRoutineObject.on(instance(test))
                                 .method("getOne")
                                 .syncCall()
                                 .afterMax(timeout)
                                 .all()).containsExactly(1);
        assertThat(JRoutineObject.on(instance(test))
                                 .aliasMethod(TestClassImpl.GET)
                                 .syncCall()
                                 .afterMax(timeout)
                                 .all()).containsExactly(1);
        assertThat(JRoutineObject.on(classOfType(TestClassImpl.class))
                                 .aliasMethod(TestClassImpl.STATIC_GET)
                                 .syncCall(3)
                                 .afterMax(timeout)
                                 .all()).containsExactly(3);
        assertThat(JRoutineObject.on(classOfType(TestClassImpl.class))
                                 .aliasMethod("sget")
                                 .asyncCall(-3)
                                 .afterMax(timeout)
                                 .all()).containsExactly(-3);
        assertThat(JRoutineObject.on(classOfType(TestClassImpl.class))
                                 .method("get", int.class)
                                 .parallelCall(17)
                                 .afterMax(timeout)
                                 .all()).containsExactly(17);

        assertThat(JRoutineObject.on(instance(test))
                                 .buildProxy(TestInterface.class)
                                 .getInt(2)).isEqualTo(2);

        try {

            JRoutineObject.on(classOfType(TestClassImpl.class))
                          .aliasMethod("sget")
                          .asyncCall()
                          .afterMax(timeout)
                          .all();

            fail();

        } catch (final InvocationException ignored) {

        }

        try {

            JRoutineObject.on(classOfType(TestClassImpl.class)).aliasMethod("take");

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        assertThat(JRoutineObject.on(instance(test))
                                 .withInvocations()
                                 .withReadTimeout(timeout)
                                 .getConfigured()
                                 .buildProxy(TestInterfaceAsync.class)
                                 .take(77)).isEqualTo(77);
        assertThat(JRoutineObject.on(instance(test))
                                 .buildProxy(TestInterfaceAsync.class)
                                 .getOne()
                                 .afterMax(timeout)
                                 .next()).isEqualTo(1);

        final TestInterfaceAsync testInterfaceAsync = JRoutineObject.on(instance(test))
                                                                    .withInvocations()
                                                                    .withReadTimeout(1,
                                                                                     TimeUnit.SECONDS)
                                                                    .getConfigured()
                                                                    .buildProxy(
                                                                            TestInterfaceAsync
                                                                                    .class);
        assertThat(testInterfaceAsync.getInt(testInterfaceAsync.getOne())).isEqualTo(1);
    }

    @Test
    public void testMissingAliasMethodError() {

        final TestClass test = new TestClass();

        try {

            JRoutineObject.on(instance(test)).aliasMethod("test");

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testMissingMethodError() {

        final TestClass test = new TestClass();

        try {

            JRoutineObject.on(instance(test)).method("test");

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testNullPointerError() {

        try {

            JRoutineObject.on(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutineObject.on(instance(null));

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutineObject.on(classOfType(null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testNullProxyError() {

        final TestClass test = new TestClass();

        try {

            JRoutineObject.on(instance(test)).buildProxy((Class<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutineObject.on(instance(test)).buildProxy((ClassToken<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testObjectStaticMethod() {

        final TimeDuration timeout = seconds(1);
        final TestStatic test = new TestStatic();
        final Routine<Object, Object> routine = JRoutineObject.on(instance(test))
                                                              .withInvocations()
                                                              .withRunner(Runners.poolRunner())
                                                              .withLogLevel(Level.DEBUG)
                                                              .withLog(new NullLog())
                                                              .getConfigured()
                                                              .aliasMethod(TestStatic.GET);

        assertThat(routine.syncCall().afterMax(timeout).all()).containsExactly(-77L);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testProxyAnnotations() {

        final Impl impl = new Impl();
        final Itf itf = JRoutineObject.on(instance(impl))
                                      .withInvocations()
                                      .withReadTimeout(seconds(10))
                                      .getConfigured()
                                      .buildProxy(Itf.class);

        assertThat(itf.add0('c')).isEqualTo((int) 'c');
        final IOChannel<Character> channel1 = JRoutineCore.io().buildChannel();
        channel1.pass('a').close();
        assertThat(itf.add1(channel1)).isEqualTo((int) 'a');
        final IOChannel<Character> channel2 = JRoutineCore.io().buildChannel();
        channel2.pass('d', 'e', 'f').close();
        assertThat(itf.add2(channel2)).isIn((int) 'd', (int) 'e', (int) 'f');
        assertThat(itf.add3('c').all()).containsExactly((int) 'c');
        final IOChannel<Character> channel3 = JRoutineCore.io().buildChannel();
        channel3.pass('a').close();
        assertThat(itf.add4(channel3).all()).containsExactly((int) 'a');
        final IOChannel<Character> channel4 = JRoutineCore.io().buildChannel();
        channel4.pass('d', 'e', 'f').close();
        assertThat(itf.add5(channel4).all()).containsOnly((int) 'd', (int) 'e', (int) 'f');
        assertThat(itf.add6().pass('d').result().all()).containsOnly((int) 'd');
        assertThat(itf.add7().pass('d', 'e', 'f').result().all()).containsOnly((int) 'd', (int) 'e',
                                                                               (int) 'f');
        assertThat(itf.add10().asyncCall('d').all()).containsOnly((int) 'd');
        assertThat(itf.add11().parallelCall('d', 'e', 'f').all()).containsOnly((int) 'd', (int) 'e',
                                                                               (int) 'f');
        assertThat(itf.addA00(new char[]{'c', 'z'})).isEqualTo(new int[]{'c', 'z'});
        final IOChannel<char[]> channel5 = JRoutineCore.io().buildChannel();
        channel5.pass(new char[]{'a', 'z'}).close();
        assertThat(itf.addA01(channel5)).isEqualTo(new int[]{'a', 'z'});
        final IOChannel<Character> channel6 = JRoutineCore.io().buildChannel();
        channel6.pass('d', 'e', 'f').close();
        assertThat(itf.addA02(channel6)).isEqualTo(new int[]{'d', 'e', 'f'});
        final IOChannel<char[]> channel7 = JRoutineCore.io().buildChannel();
        channel7.pass(new char[]{'d', 'z'}, new char[]{'e', 'z'}, new char[]{'f', 'z'}).close();
        assertThat(itf.addA03(channel7)).isIn(new int[]{'d', 'z'}, new int[]{'e', 'z'},
                                              new int[]{'f', 'z'});
        assertThat(itf.addA04(new char[]{'c', 'z'}).all()).containsExactly(new int[]{'c', 'z'});
        final IOChannel<char[]> channel8 = JRoutineCore.io().buildChannel();
        channel8.pass(new char[]{'a', 'z'}).close();
        assertThat(itf.addA05(channel8).all()).containsExactly(new int[]{'a', 'z'});
        final IOChannel<Character> channel9 = JRoutineCore.io().buildChannel();
        channel9.pass('d', 'e', 'f').close();
        assertThat(itf.addA06(channel9).all()).containsExactly(new int[]{'d', 'e', 'f'});
        final IOChannel<char[]> channel10 = JRoutineCore.io().buildChannel();
        channel10.pass(new char[]{'d', 'z'}, new char[]{'e', 'z'}, new char[]{'f', 'z'}).close();
        assertThat(itf.addA07(channel10).all()).containsOnly(new int[]{'d', 'z'},
                                                             new int[]{'e', 'z'},
                                                             new int[]{'f', 'z'});
        assertThat(itf.addA08(new char[]{'c', 'z'}).all()).containsExactly((int) 'c', (int) 'z');
        final IOChannel<char[]> channel11 = JRoutineCore.io().buildChannel();
        channel11.pass(new char[]{'a', 'z'}).close();
        assertThat(itf.addA09(channel11).all()).containsExactly((int) 'a', (int) 'z');
        final IOChannel<Character> channel12 = JRoutineCore.io().buildChannel();
        channel12.pass('d', 'e', 'f').close();
        assertThat(itf.addA10(channel12).all()).containsExactly((int) 'd', (int) 'e', (int) 'f');
        final IOChannel<char[]> channel13 = JRoutineCore.io().buildChannel();
        channel13.pass(new char[]{'d', 'z'}, new char[]{'e', 'z'}, new char[]{'f', 'z'}).close();
        assertThat(itf.addA11(channel13).all()).containsOnly((int) 'd', (int) 'e', (int) 'f',
                                                             (int) 'z');
        assertThat(itf.addA12().pass(new char[]{'c', 'z'}).result().all()).containsOnly(
                new int[]{'c', 'z'});
        assertThat(itf.addA13()
                      .pass(new char[]{'d', 'z'}, new char[]{'e', 'z'}, new char[]{'f', 'z'})
                      .result()
                      .all()).containsOnly(new int[]{'d', 'z'}, new int[]{'e', 'z'},
                                           new int[]{'f', 'z'});
        assertThat(itf.addA14().asyncCall(new char[]{'c', 'z'}).all()).containsOnly(
                new int[]{'c', 'z'});
        assertThat(itf.addA15()
                      .parallelCall(new char[]{'d', 'z'}, new char[]{'e', 'z'},
                                    new char[]{'f', 'z'})
                      .all()).containsOnly(new int[]{'d', 'z'}, new int[]{'e', 'z'},
                                           new int[]{'f', 'z'});
        assertThat(itf.addA16().pass(new char[]{'c', 'z'}).result().all()).containsExactly(
                (int) 'c', (int) 'z');
        assertThat(itf.addA17()
                      .pass(new char[]{'d', 'z'}, new char[]{'e', 'z'}, new char[]{'f', 'z'})
                      .result()
                      .all()).containsOnly((int) 'd', (int) 'z', (int) 'e', (int) 'z', (int) 'f',
                                           (int) 'z');
        assertThat(itf.addA18().asyncCall(new char[]{'c', 'z'}).all()).containsExactly((int) 'c',
                                                                                       (int) 'z');
        assertThat(itf.addA19()
                      .parallelCall(new char[]{'d', 'z'}, new char[]{'e', 'z'},
                                    new char[]{'f', 'z'})
                      .all()).containsOnly((int) 'd', (int) 'z', (int) 'e', (int) 'z', (int) 'f',
                                           (int) 'z');
        assertThat(itf.addL00(Arrays.asList('c', 'z'))).isEqualTo(
                Arrays.asList((int) 'c', (int) 'z'));
        final IOChannel<List<Character>> channel20 = JRoutineCore.io().buildChannel();
        channel20.pass(Arrays.asList('a', 'z')).close();
        assertThat(itf.addL01(channel20)).isEqualTo(Arrays.asList((int) 'a', (int) 'z'));
        final IOChannel<Character> channel21 = JRoutineCore.io().buildChannel();
        channel21.pass('d', 'e', 'f').close();
        assertThat(itf.addL02(channel21)).isEqualTo(Arrays.asList((int) 'd', (int) 'e', (int) 'f'));
        final IOChannel<List<Character>> channel22 = JRoutineCore.io().buildChannel();
        channel22.pass(Arrays.asList('d', 'z'), Arrays.asList('e', 'z'), Arrays.asList('f', 'z'))
                 .close();
        assertThat(itf.addL03(channel22)).isIn(Arrays.asList((int) 'd', (int) 'z'),
                                               Arrays.asList((int) 'e', (int) 'z'),
                                               Arrays.asList((int) 'f', (int) 'z'));
        assertThat(itf.addL04(Arrays.asList('c', 'z')).all()).containsExactly(
                Arrays.asList((int) 'c', (int) 'z'));
        final IOChannel<List<Character>> channel23 = JRoutineCore.io().buildChannel();
        channel23.pass(Arrays.asList('a', 'z')).close();
        assertThat(itf.addL05(channel23).all()).containsExactly(
                Arrays.asList((int) 'a', (int) 'z'));
        final IOChannel<Character> channel24 = JRoutineCore.io().buildChannel();
        channel24.pass('d', 'e', 'f').close();
        assertThat(itf.addL06(channel24).all()).containsExactly(
                Arrays.asList((int) 'd', (int) 'e', (int) 'f'));
        final IOChannel<List<Character>> channel25 = JRoutineCore.io().buildChannel();
        channel25.pass(Arrays.asList('d', 'z'), Arrays.asList('e', 'z'), Arrays.asList('f', 'z'))
                 .close();
        assertThat(itf.addL07(channel25).all()).containsOnly(Arrays.asList((int) 'd', (int) 'z'),
                                                             Arrays.asList((int) 'e', (int) 'z'),
                                                             Arrays.asList((int) 'f', (int) 'z'));
        assertThat(itf.addL08(Arrays.asList('c', 'z')).all()).containsExactly((int) 'c', (int) 'z');
        final IOChannel<List<Character>> channel26 = JRoutineCore.io().buildChannel();
        channel26.pass(Arrays.asList('a', 'z')).close();
        assertThat(itf.addL09(channel26).all()).containsExactly((int) 'a', (int) 'z');
        final IOChannel<Character> channel27 = JRoutineCore.io().buildChannel();
        channel27.pass('d', 'e', 'f').close();
        assertThat(itf.addL10(channel27).all()).containsExactly((int) 'd', (int) 'e', (int) 'f');
        final IOChannel<List<Character>> channel28 = JRoutineCore.io().buildChannel();
        channel28.pass(Arrays.asList('d', 'z'), Arrays.asList('e', 'z'), Arrays.asList('f', 'z'))
                 .close();
        assertThat(itf.addL11(channel28).all()).containsOnly((int) 'd', (int) 'e', (int) 'f',
                                                             (int) 'z');
        assertThat(itf.addL12().pass(Arrays.asList('c', 'z')).result().all()).containsOnly(
                Arrays.asList((int) 'c', (int) 'z'));
        assertThat(itf.addL13()
                      .pass(Arrays.asList('d', 'z'), Arrays.asList('e', 'z'),
                            Arrays.asList('f', 'z'))
                      .result()
                      .all()).containsOnly(Arrays.asList((int) 'd', (int) 'z'),
                                           Arrays.asList((int) 'e', (int) 'z'),
                                           Arrays.asList((int) 'f', (int) 'z'));
        assertThat(itf.addL14().asyncCall(Arrays.asList('c', 'z')).all()).containsOnly(
                Arrays.asList((int) 'c', (int) 'z'));
        assertThat(itf.addL15()
                      .parallelCall(Arrays.asList('d', 'z'), Arrays.asList('e', 'z'),
                                    Arrays.asList('f', 'z'))
                      .all()).containsOnly(Arrays.asList((int) 'd', (int) 'z'),
                                           Arrays.asList((int) 'e', (int) 'z'),
                                           Arrays.asList((int) 'f', (int) 'z'));
        assertThat(itf.addL16().pass(Arrays.asList('c', 'z')).result().all()).containsExactly(
                (int) 'c', (int) 'z');
        assertThat(itf.addL17()
                      .pass(Arrays.asList('d', 'z'), Arrays.asList('e', 'z'),
                            Arrays.asList('f', 'z'))
                      .result()
                      .all()).containsOnly((int) 'd', (int) 'z', (int) 'e', (int) 'z', (int) 'f',
                                           (int) 'z');
        assertThat(itf.addL18().asyncCall(Arrays.asList('c', 'z')).all()).containsExactly((int) 'c',
                                                                                          (int) 'z');
        assertThat(itf.addL19()
                      .parallelCall(Arrays.asList('d', 'z'), Arrays.asList('e', 'z'),
                                    Arrays.asList('f', 'z'))
                      .all()).containsOnly((int) 'd', (int) 'z', (int) 'e', (int) 'z', (int) 'f',
                                           (int) 'z');
        assertThat(itf.get0()).isEqualTo(31);
        assertThat(itf.get1().all()).containsExactly(31);
        assertThat(itf.get2().result().all()).containsExactly(31);
        assertThat(itf.get4().asyncCall().all()).containsExactly(31);
        assertThat(itf.getA0()).isEqualTo(new int[]{1, 2, 3});
        assertThat(itf.getA1().all()).containsExactly(1, 2, 3);
        assertThat(itf.getA2().result().all()).containsExactly(new int[]{1, 2, 3});
        assertThat(itf.getA3().asyncCall().all()).containsExactly(new int[]{1, 2, 3});
        assertThat(itf.getA4().result().all()).containsExactly(1, 2, 3);
        assertThat(itf.getA5().asyncCall().all()).containsExactly(1, 2, 3);
        assertThat(itf.getL0()).isEqualTo(Arrays.asList(1, 2, 3));
        assertThat(itf.getL1().all()).containsExactly(1, 2, 3);
        assertThat(itf.getL2().result().all()).containsExactly(Arrays.asList(1, 2, 3));
        assertThat(itf.getL3().asyncCall().all()).containsExactly(Arrays.asList(1, 2, 3));
        assertThat(itf.getL4().result().all()).containsExactly(1, 2, 3);
        assertThat(itf.getL5().asyncCall().all()).containsExactly(1, 2, 3);
        itf.set0(-17);
        final IOChannel<Integer> channel35 = JRoutineCore.io().buildChannel();
        channel35.pass(-17).close();
        itf.set1(channel35);
        final IOChannel<Integer> channel36 = JRoutineCore.io().buildChannel();
        channel36.pass(-17).close();
        itf.set2(channel36);
        itf.set3().pass(-17).result().hasCompleted();
        itf.set5().asyncCall(-17).hasCompleted();
        itf.setA0(new int[]{1, 2, 3});
        final IOChannel<int[]> channel37 = JRoutineCore.io().buildChannel();
        channel37.pass(new int[]{1, 2, 3}).close();
        itf.setA1(channel37);
        final IOChannel<Integer> channel38 = JRoutineCore.io().buildChannel();
        channel38.pass(1, 2, 3).close();
        itf.setA2(channel38);
        final IOChannel<int[]> channel39 = JRoutineCore.io().buildChannel();
        channel39.pass(new int[]{1, 2, 3}).close();
        itf.setA3(channel39);
        itf.setA4().pass(new int[]{1, 2, 3}).result().hasCompleted();
        itf.setA6().asyncCall(new int[]{1, 2, 3}).hasCompleted();
        itf.setL0(Arrays.asList(1, 2, 3));
        final IOChannel<List<Integer>> channel40 = JRoutineCore.io().buildChannel();
        channel40.pass(Arrays.asList(1, 2, 3)).close();
        itf.setL1(channel40);
        final IOChannel<Integer> channel41 = JRoutineCore.io().buildChannel();
        channel41.pass(1, 2, 3).close();
        itf.setL2(channel41);
        final IOChannel<List<Integer>> channel42 = JRoutineCore.io().buildChannel();
        channel42.pass(Arrays.asList(1, 2, 3)).close();
        itf.setL3(channel42);
        itf.setL4().pass(Arrays.asList(1, 2, 3)).result().hasCompleted();
        itf.setL6().asyncCall(Arrays.asList(1, 2, 3)).hasCompleted();
    }

    @Test
    @SuppressWarnings("NullArgumentToVariableArgMethod")
    public void testProxyRoutine() {

        final TimeDuration timeout = seconds(1);
        final Square square = new Square();
        final SquareItf squareAsync =
                JRoutineObject.on(instance(square)).buildProxy(SquareItf.class);

        assertThat(squareAsync.compute(3)).isEqualTo(9);

        final IOChannel<Integer> channel1 = JRoutineCore.io().buildChannel();
        channel1.pass(4).close();
        assertThat(squareAsync.computeAsync(channel1)).isEqualTo(16);

        final IOChannel<Integer> channel2 = JRoutineCore.io().buildChannel();
        channel2.pass(1, 2, 3).close();
        assertThat(squareAsync.computeParallel(channel2).afterMax(timeout).all()).containsOnly(1, 4,
                                                                                               9);
    }

    @Test
    public void testRoutineCache() {

        final TestClass test = new TestClass();
        final NullLog nullLog = new NullLog();
        final Routine<Object, Object> routine1 = JRoutineObject.on(instance(test))
                                                               .withInvocations()
                                                               .withRunner(Runners.sharedRunner())
                                                               .withCoreInstances(16)
                                                               .withLogLevel(Level.DEBUG)
                                                               .withLog(nullLog)
                                                               .getConfigured()
                                                               .aliasMethod(TestClass.GET);

        assertThat(routine1.syncCall().all()).containsExactly(-77L);

        final Routine<Object, Object> routine2 = JRoutineObject.on(instance(test))
                                                               .withInvocations()
                                                               .withRunner(Runners.sharedRunner())
                                                               .withCoreInstances(16)
                                                               .withLogLevel(Level.DEBUG)
                                                               .withLog(nullLog)
                                                               .getConfigured()
                                                               .aliasMethod(TestClass.GET);

        assertThat(routine2.syncCall().all()).containsExactly(-77L);
        assertThat(routine1).isEqualTo(routine2);

        final Routine<Object, Object> routine3 = JRoutineObject.on(instance(test))
                                                               .withInvocations()
                                                               .withRunner(Runners.sharedRunner())
                                                               .withCoreInstances(32)
                                                               .withLogLevel(Level.DEBUG)
                                                               .withLog(nullLog)
                                                               .getConfigured()
                                                               .aliasMethod(TestClass.GET);

        assertThat(routine3.syncCall().all()).containsExactly(-77L);
        assertThat(routine1).isNotEqualTo(routine3);
        assertThat(routine2).isNotEqualTo(routine3);

        final Routine<Object, Object> routine4 = JRoutineObject.on(instance(test))
                                                               .withInvocations()
                                                               .withRunner(Runners.sharedRunner())
                                                               .withCoreInstances(32)
                                                               .withLogLevel(Level.WARNING)
                                                               .withLog(nullLog)
                                                               .getConfigured()
                                                               .aliasMethod(TestClass.GET);

        assertThat(routine4.syncCall().all()).containsExactly(-77L);
        assertThat(routine3).isNotEqualTo(routine4);

        final Routine<Object, Object> routine5 = JRoutineObject.on(instance(test))
                                                               .withInvocations()
                                                               .withRunner(Runners.sharedRunner())
                                                               .withCoreInstances(32)
                                                               .withLogLevel(Level.WARNING)
                                                               .withLog(new NullLog())
                                                               .getConfigured()
                                                               .aliasMethod(TestClass.GET);

        assertThat(routine5.syncCall().all()).containsExactly(-77L);
        assertThat(routine4).isNotEqualTo(routine5);
    }

    @Test
    public void testRoutineCache2() {

        final NullLog nullLog = new NullLog();
        final Routine<Object, Object> routine1 = JRoutineObject.on(classOfType(TestStatic.class))
                                                               .withInvocations()
                                                               .withRunner(Runners.sharedRunner())
                                                               .withCoreInstances(16)
                                                               .withLogLevel(Level.DEBUG)
                                                               .withLog(nullLog)
                                                               .getConfigured()
                                                               .aliasMethod(TestStatic.GET);

        assertThat(routine1.syncCall().all()).containsExactly(-77L);

        final Routine<Object, Object> routine2 = JRoutineObject.on(classOfType(TestStatic.class))
                                                               .withInvocations()
                                                               .withRunner(Runners.sharedRunner())
                                                               .withCoreInstances(16)
                                                               .withLogLevel(Level.DEBUG)
                                                               .withLog(nullLog)
                                                               .getConfigured()
                                                               .aliasMethod(TestStatic.GET);

        assertThat(routine2.syncCall().all()).containsExactly(-77L);
        assertThat(routine1).isEqualTo(routine2);

        final Routine<Object, Object> routine3 = JRoutineObject.on(classOfType(TestStatic.class))
                                                               .withInvocations()
                                                               .withRunner(Runners.sharedRunner())
                                                               .withCoreInstances(32)
                                                               .withLogLevel(Level.DEBUG)
                                                               .withLog(nullLog)
                                                               .getConfigured()
                                                               .aliasMethod(TestStatic.GET);

        assertThat(routine3.syncCall().all()).containsExactly(-77L);
        assertThat(routine1).isNotEqualTo(routine3);
        assertThat(routine2).isNotEqualTo(routine3);

        final Routine<Object, Object> routine4 = JRoutineObject.on(classOfType(TestStatic.class))
                                                               .withInvocations()
                                                               .withRunner(Runners.sharedRunner())
                                                               .withCoreInstances(32)
                                                               .withLogLevel(Level.WARNING)
                                                               .withLog(nullLog)
                                                               .getConfigured()
                                                               .aliasMethod(TestStatic.GET);

        assertThat(routine4.syncCall().all()).containsExactly(-77L);
        assertThat(routine3).isNotEqualTo(routine4);

        final Routine<Object, Object> routine5 = JRoutineObject.on(classOfType(TestStatic.class))
                                                               .withInvocations()
                                                               .withRunner(Runners.sharedRunner())
                                                               .withCoreInstances(32)
                                                               .withLogLevel(Level.WARNING)
                                                               .withLog(new NullLog())
                                                               .getConfigured()
                                                               .aliasMethod(TestStatic.GET);

        assertThat(routine5.syncCall().all()).containsExactly(-77L);
        assertThat(routine4).isNotEqualTo(routine5);
    }

    @Test
    public void testSharedFields() throws NoSuchMethodException {

        final TestClass2 test2 = new TestClass2();
        final ObjectRoutineBuilder builder = JRoutineObject.on(instance(test2))
                                                           .withInvocations()
                                                           .withReadTimeout(seconds(2))
                                                           .getConfigured();
        long startTime = System.currentTimeMillis();

        OutputChannel<Object> getOne = builder.withProxies()
                                              .withSharedFields()
                                              .getConfigured()
                                              .method("getOne")
                                              .asyncCall();
        OutputChannel<Object> getTwo = builder.withProxies()
                                              .withSharedFields()
                                              .getConfigured()
                                              .method("getTwo")
                                              .asyncCall();

        assertThat(getOne.hasCompleted()).isTrue();
        assertThat(getTwo.hasCompleted()).isTrue();
        assertThat(System.currentTimeMillis() - startTime).isLessThan(1000);

        startTime = System.currentTimeMillis();

        getOne = builder.withProxies()
                        .withSharedFields("1")
                        .getConfigured()
                        .method("getOne")
                        .asyncCall();
        getTwo = builder.withProxies()
                        .withSharedFields("2")
                        .getConfigured()
                        .method("getTwo")
                        .asyncCall();

        assertThat(getOne.hasCompleted()).isTrue();
        assertThat(getTwo.hasCompleted()).isTrue();
        assertThat(System.currentTimeMillis() - startTime).isLessThan(1000);

        startTime = System.currentTimeMillis();

        getOne = builder.method("getOne").asyncCall();
        getTwo = builder.method("getTwo").asyncCall();

        assertThat(getOne.hasCompleted()).isTrue();
        assertThat(getTwo.hasCompleted()).isTrue();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
    }

    @Test
    public void testSharedFields2() throws NoSuchMethodException {

        final TestClass2 test2 = new TestClass2();
        final ObjectRoutineBuilder builder = JRoutineObject.on(instance(test2))
                                                           .withInvocations()
                                                           .withReadTimeout(seconds(2))
                                                           .getConfigured();
        long startTime = System.currentTimeMillis();

        OutputChannel<Object> getOne = builder.method("getOne").asyncCall();
        OutputChannel<Object> getTwo = builder.withProxies()
                                              .withSharedFields()
                                              .getConfigured()
                                              .method("getTwo")
                                              .asyncCall();

        assertThat(getOne.hasCompleted()).isTrue();
        assertThat(getTwo.hasCompleted()).isTrue();
        assertThat(System.currentTimeMillis() - startTime).isLessThan(1000);

        startTime = System.currentTimeMillis();

        getOne = builder.withProxies()
                        .withSharedFields("1", "2")
                        .getConfigured()
                        .method("getOne")
                        .asyncCall();
        getTwo = builder.withProxies()
                        .withSharedFields()
                        .getConfigured()
                        .method("getTwo")
                        .asyncCall();

        assertThat(getOne.hasCompleted()).isTrue();
        assertThat(getTwo.hasCompleted()).isTrue();
        assertThat(System.currentTimeMillis() - startTime).isLessThan(1000);

        startTime = System.currentTimeMillis();

        getOne = builder.withProxies()
                        .withSharedFields("1", "2")
                        .getConfigured()
                        .method("getOne")
                        .asyncCall();
        getTwo = builder.method("getTwo").asyncCall();

        assertThat(getOne.hasCompleted()).isTrue();
        assertThat(getTwo.hasCompleted()).isTrue();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);

        startTime = System.currentTimeMillis();

        getOne = builder.withProxies()
                        .withSharedFields("1", "2")
                        .getConfigured()
                        .method("getOne")
                        .asyncCall();
        getTwo = builder.withProxies()
                        .withSharedFields("2")
                        .getConfigured()
                        .method("getTwo")
                        .asyncCall();

        assertThat(getOne.hasCompleted()).isTrue();
        assertThat(getTwo.hasCompleted()).isTrue();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
    }

    @Test
    public void testSharedFieldsStatic() throws NoSuchMethodException {

        final ObjectRoutineBuilder builder = JRoutineObject.on(classOfType(TestStatic2.class))
                                                           .withInvocations()
                                                           .withReadTimeout(seconds(2))
                                                           .getConfigured();
        long startTime = System.currentTimeMillis();

        OutputChannel<Object> getOne = builder.withProxies()
                                              .withSharedFields()
                                              .getConfigured()
                                              .method("getOne")
                                              .asyncCall();
        OutputChannel<Object> getTwo = builder.withProxies()
                                              .withSharedFields()
                                              .getConfigured()
                                              .method("getTwo")
                                              .asyncCall();

        assertThat(getOne.hasCompleted()).isTrue();
        assertThat(getTwo.hasCompleted()).isTrue();
        assertThat(System.currentTimeMillis() - startTime).isLessThan(1000);

        startTime = System.currentTimeMillis();

        getOne = builder.withProxies()
                        .withSharedFields("1")
                        .getConfigured()
                        .method("getOne")
                        .asyncCall();
        getTwo = builder.withProxies()
                        .withSharedFields("2")
                        .getConfigured()
                        .method("getTwo")
                        .asyncCall();

        assertThat(getOne.hasCompleted()).isTrue();
        assertThat(getTwo.hasCompleted()).isTrue();
        assertThat(System.currentTimeMillis() - startTime).isLessThan(1000);

        startTime = System.currentTimeMillis();

        getOne = builder.method("getOne").asyncCall();
        getTwo = builder.method("getTwo").asyncCall();

        assertThat(getOne.hasCompleted()).isTrue();
        assertThat(getTwo.hasCompleted()).isTrue();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
    }

    @Test
    public void testSharedFieldsStatic2() throws NoSuchMethodException {

        final ObjectRoutineBuilder builder = JRoutineObject.on(classOfType(TestStatic2.class))
                                                           .withInvocations()
                                                           .withReadTimeout(seconds(2))
                                                           .getConfigured();
        long startTime = System.currentTimeMillis();

        OutputChannel<Object> getOne = builder.method("getOne").asyncCall();
        OutputChannel<Object> getTwo = builder.withProxies()
                                              .withSharedFields()
                                              .getConfigured()
                                              .method("getTwo")
                                              .asyncCall();

        assertThat(getOne.hasCompleted()).isTrue();
        assertThat(getTwo.hasCompleted()).isTrue();
        assertThat(System.currentTimeMillis() - startTime).isLessThan(1000);

        startTime = System.currentTimeMillis();

        getOne = builder.withProxies()
                        .withSharedFields("1", "2")
                        .getConfigured()
                        .method("getOne")
                        .asyncCall();
        getTwo = builder.withProxies()
                        .withSharedFields()
                        .getConfigured()
                        .method("getTwo")
                        .asyncCall();

        assertThat(getOne.hasCompleted()).isTrue();
        assertThat(getTwo.hasCompleted()).isTrue();
        assertThat(System.currentTimeMillis() - startTime).isLessThan(1000);

        startTime = System.currentTimeMillis();

        getOne = builder.withProxies()
                        .withSharedFields("1", "2")
                        .getConfigured()
                        .method("getOne")
                        .asyncCall();
        getTwo = builder.method("getTwo").asyncCall();

        assertThat(getOne.hasCompleted()).isTrue();
        assertThat(getTwo.hasCompleted()).isTrue();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);

        startTime = System.currentTimeMillis();

        getOne = builder.withProxies()
                        .withSharedFields("1", "2")
                        .getConfigured()
                        .method("getOne")
                        .asyncCall();
        getTwo = builder.withProxies()
                        .withSharedFields("2")
                        .getConfigured()
                        .method("getTwo")
                        .asyncCall();

        assertThat(getOne.hasCompleted()).isTrue();
        assertThat(getTwo.hasCompleted()).isTrue();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
    }

    @Test
    public void testStaticMethod() throws NoSuchMethodException {

        final TimeDuration timeout = seconds(1);
        final Routine<Object, Object> routine2 = JRoutineObject.on(classOfType(TestStatic.class))
                                                               .withInvocations()
                                                               .withRunner(Runners.poolRunner())
                                                               .withMaxInstances(1)
                                                               .withCoreInstances(0)
                                                               .getConfigured()
                                                               .withProxies()
                                                               .withSharedFields("test")
                                                               .getConfigured()
                                                               .method(TestStatic.class.getMethod(
                                                                       "getLong"));

        assertThat(routine2.syncCall().afterMax(timeout).all()).containsExactly(-77L);
    }

    @Test
    public void testStaticMethodBySignature() throws NoSuchMethodException {

        final TimeDuration timeout = seconds(1);
        final Routine<Object, Object> routine1 = JRoutineObject.on(classOfType(TestStatic.class))
                                                               .withInvocations()
                                                               .withRunner(Runners.poolRunner())
                                                               .withMaxInstances(1)
                                                               .getConfigured()
                                                               .method("getLong");

        assertThat(routine1.syncCall().afterMax(timeout).all()).containsExactly(-77L);

    }

    @Test
    public void testStaticMethodError() {

        try {

            JRoutineObject.on(classOfType(TestStatic.class)).method("test");

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testTimeoutActionAnnotation() throws NoSuchMethodException {

        final TestTimeout testTimeout = new TestTimeout();
        assertThat(JRoutineObject.on(instance(testTimeout))
                                 .withInvocations()
                                 .withReadTimeout(seconds(1))
                                 .getConfigured()
                                 .aliasMethod("test")
                                 .asyncCall()
                                 .next()).isEqualTo(31);

        try {

            JRoutineObject.on(instance(testTimeout))
                          .withInvocations()
                          .withReadTimeoutAction(TimeoutActionType.THROW)
                          .getConfigured()
                          .aliasMethod("test")
                          .asyncCall()
                          .next();

            fail();

        } catch (final NoSuchElementException ignored) {

        }

        assertThat(JRoutineObject.on(instance(testTimeout))
                                 .withInvocations()
                                 .withReadTimeout(seconds(1))
                                 .getConfigured()
                                 .method("getInt")
                                 .asyncCall()
                                 .next()).isEqualTo(31);

        try {

            JRoutineObject.on(instance(testTimeout))
                          .withInvocations()
                          .withReadTimeoutAction(TimeoutActionType.THROW)
                          .getConfigured()
                          .method("getInt")
                          .asyncCall()
                          .next();

            fail();

        } catch (final NoSuchElementException ignored) {

        }

        assertThat(JRoutineObject.on(instance(testTimeout))
                                 .withInvocations()
                                 .withReadTimeout(seconds(1))
                                 .getConfigured()
                                 .method(TestTimeout.class.getMethod("getInt"))
                                 .asyncCall()
                                 .next()).isEqualTo(31);

        try {

            JRoutineObject.on(instance(testTimeout))
                          .withInvocations()
                          .withReadTimeoutAction(TimeoutActionType.THROW)
                          .getConfigured()
                          .method(TestTimeout.class.getMethod("getInt"))
                          .asyncCall()
                          .next();

            fail();

        } catch (final NoSuchElementException ignored) {

        }

        assertThat(JRoutineObject.on(instance(testTimeout))
                                 .withInvocations()
                                 .withReadTimeout(seconds(1))
                                 .getConfigured()
                                 .buildProxy(TestTimeoutItf.class)
                                 .getInt()).isEqualTo(31);

        try {

            JRoutineObject.on(instance(testTimeout))
                          .withInvocations()
                          .withReadTimeoutAction(TimeoutActionType.THROW)
                          .getConfigured()
                          .buildProxy(TestTimeoutItf.class)
                          .getInt();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public interface AnnotationItf {

        @CoreInstances(3)
        @InputLimit(71)
        @InputMaxDelay(value = 7777, unit = TimeUnit.MICROSECONDS)
        @InputMaxSize(33)
        @InputOrder(OrderType.BY_DELAY)
        @LogLevel(Level.WARNING)
        @MaxInstances(17)
        @OutputLimit(31)
        @OutputMaxDelay(value = 3333, unit = TimeUnit.NANOSECONDS)
        @OutputMaxSize(77)
        @OutputOrder(OrderType.BY_CALL)
        @Priority(41)
        @ReadTimeout(value = 1111, unit = TimeUnit.MICROSECONDS)
        @ReadTimeoutAction(TimeoutActionType.ABORT)
        String toString();
    }

    public interface Itf {

        @Alias("a")
        int add0(char c);

        @Alias("a")
        int add1(@AsyncIn(value = char.class, mode = InputMode.VALUE) OutputChannel<Character> c);

        @Alias("a")
        @AsyncMethod(char.class)
        Routine<Character, Integer> add10();

        @Alias("a")
        @Invoke(InvocationMode.PARALLEL)
        @AsyncMethod(char.class)
        Routine<Character, Integer> add11();

        @Alias("a")
        @Invoke(InvocationMode.PARALLEL)
        int add2(@AsyncIn(value = char.class, mode = InputMode.VALUE) OutputChannel<Character> c);

        @Alias("a")
        @AsyncOut(OutputMode.VALUE)
        OutputChannel<Integer> add3(char c);

        @Alias("a")
        @AsyncOut(OutputMode.VALUE)
        OutputChannel<Integer> add4(
                @AsyncIn(value = char.class, mode = InputMode.VALUE) OutputChannel<Character> c);

        @Alias("a")
        @Invoke(InvocationMode.PARALLEL)
        @AsyncOut(OutputMode.VALUE)
        OutputChannel<Integer> add5(
                @AsyncIn(value = char.class, mode = InputMode.VALUE) OutputChannel<Character> c);

        @Alias("a")
        @AsyncMethod(char.class)
        InvocationChannel<Character, Integer> add6();

        @Alias("a")
        @Invoke(InvocationMode.PARALLEL)
        @AsyncMethod(char.class)
        InvocationChannel<Character, Integer> add7();

        @Alias("aa")
        int[] addA00(char[] c);

        @Alias("aa")
        int[] addA01(@AsyncIn(value = char[].class,
                mode = InputMode.VALUE) OutputChannel<char[]> c);

        @Alias("aa")
        int[] addA02(@AsyncIn(value = char[].class,
                mode = InputMode.COLLECTION) OutputChannel<Character> c);

        @Alias("aa")
        @Invoke(InvocationMode.PARALLEL)
        int[] addA03(@AsyncIn(value = char[].class,
                mode = InputMode.VALUE) OutputChannel<char[]> c);

        @Alias("aa")
        @AsyncOut(OutputMode.VALUE)
        OutputChannel<int[]> addA04(char[] c);

        @Alias("aa")
        @AsyncOut(OutputMode.VALUE)
        OutputChannel<int[]> addA05(
                @AsyncIn(value = char[].class, mode = InputMode.VALUE) OutputChannel<char[]> c);

        @Alias("aa")
        @AsyncOut(OutputMode.VALUE)
        OutputChannel<int[]> addA06(@AsyncIn(value = char[].class,
                mode = InputMode.COLLECTION) OutputChannel<Character> c);

        @Alias("aa")
        @Invoke(InvocationMode.PARALLEL)
        @AsyncOut(OutputMode.VALUE)
        OutputChannel<int[]> addA07(@AsyncIn(value = char[].class,
                mode = InputMode.VALUE) OutputChannel<char[]> c);

        @Alias("aa")
        @AsyncOut(OutputMode.ELEMENT)
        OutputChannel<Integer> addA08(char[] c);

        @Alias("aa")
        @AsyncOut(OutputMode.ELEMENT)
        OutputChannel<Integer> addA09(
                @AsyncIn(value = char[].class, mode = InputMode.VALUE) OutputChannel<char[]> c);

        @Alias("aa")
        @AsyncOut(OutputMode.ELEMENT)
        OutputChannel<Integer> addA10(@AsyncIn(value = char[].class,
                mode = InputMode.COLLECTION) OutputChannel<Character> c);

        @Alias("aa")
        @Invoke(InvocationMode.PARALLEL)
        @AsyncOut(OutputMode.ELEMENT)
        OutputChannel<Integer> addA11(@AsyncIn(value = char[].class,
                mode = InputMode.VALUE) OutputChannel<char[]> c);

        @Alias("aa")
        @AsyncMethod(char[].class)
        InvocationChannel<char[], int[]> addA12();

        @Alias("aa")
        @Invoke(InvocationMode.PARALLEL)
        @AsyncMethod(char[].class)
        InvocationChannel<char[], int[]> addA13();

        @Alias("aa")
        @AsyncMethod(char[].class)
        Routine<char[], int[]> addA14();

        @Alias("aa")
        @Invoke(InvocationMode.PARALLEL)
        @AsyncMethod(char[].class)
        Routine<char[], int[]> addA15();

        @Alias("aa")
        @AsyncMethod(value = char[].class, mode = OutputMode.ELEMENT)
        InvocationChannel<char[], Integer> addA16();

        @Alias("aa")
        @Invoke(InvocationMode.PARALLEL)
        @AsyncMethod(value = char[].class, mode = OutputMode.ELEMENT)
        InvocationChannel<char[], Integer> addA17();

        @Alias("aa")
        @AsyncMethod(value = char[].class, mode = OutputMode.ELEMENT)
        Routine<char[], Integer> addA18();

        @Alias("aa")
        @Invoke(InvocationMode.PARALLEL)
        @AsyncMethod(value = char[].class, mode = OutputMode.ELEMENT)
        Routine<char[], Integer> addA19();

        @Alias("al")
        List<Integer> addL00(List<Character> c);

        @Alias("al")
        List<Integer> addL01(@AsyncIn(value = List.class,
                mode = InputMode.VALUE) OutputChannel<List<Character>> c);

        @Alias("al")
        List<Integer> addL02(@AsyncIn(value = List.class,
                mode = InputMode.COLLECTION) OutputChannel<Character> c);

        @Alias("al")
        @Invoke(InvocationMode.PARALLEL)
        List<Integer> addL03(@AsyncIn(value = List.class,
                mode = InputMode.VALUE) OutputChannel<List<Character>> c);

        @Alias("al")
        @AsyncOut(OutputMode.VALUE)
        OutputChannel<List<Integer>> addL04(List<Character> c);

        @Alias("al")
        @AsyncOut(OutputMode.VALUE)
        OutputChannel<List<Integer>> addL05(@AsyncIn(value = List.class,
                mode = InputMode.VALUE) OutputChannel<List<Character>> c);

        @Alias("al")
        @AsyncOut(OutputMode.VALUE)
        OutputChannel<List<Integer>> addL06(@AsyncIn(value = List.class,
                mode = InputMode.COLLECTION) OutputChannel<Character> c);

        @Alias("al")
        @Invoke(InvocationMode.PARALLEL)
        @AsyncOut(OutputMode.VALUE)
        OutputChannel<List<Integer>> addL07(@AsyncIn(value = List.class,
                mode = InputMode.VALUE) OutputChannel<List<Character>> c);

        @Alias("al")
        @AsyncOut(OutputMode.ELEMENT)
        OutputChannel<Integer> addL08(List<Character> c);

        @Alias("al")
        @AsyncOut(OutputMode.ELEMENT)
        OutputChannel<Integer> addL09(@AsyncIn(value = List.class,
                mode = InputMode.VALUE) OutputChannel<List<Character>> c);

        @Alias("al")
        @AsyncOut(OutputMode.ELEMENT)
        OutputChannel<Integer> addL10(@AsyncIn(value = List.class,
                mode = InputMode.COLLECTION) OutputChannel<Character> c);

        @Alias("al")
        @Invoke(InvocationMode.PARALLEL)
        @AsyncOut(OutputMode.ELEMENT)
        OutputChannel<Integer> addL11(@AsyncIn(value = List.class,
                mode = InputMode.VALUE) OutputChannel<List<Character>> c);

        @Alias("al")
        @AsyncMethod(List.class)
        InvocationChannel<List<Character>, List<Integer>> addL12();

        @Alias("al")
        @Invoke(InvocationMode.PARALLEL)
        @AsyncMethod(List.class)
        InvocationChannel<List<Character>, List<Integer>> addL13();

        @Alias("al")
        @AsyncMethod(List.class)
        Routine<List<Character>, List<Integer>> addL14();

        @Alias("al")
        @Invoke(InvocationMode.PARALLEL)
        @AsyncMethod(List.class)
        Routine<List<Character>, List<Integer>> addL15();

        @Alias("al")
        @AsyncMethod(value = List.class, mode = OutputMode.ELEMENT)
        InvocationChannel<List<Character>, Integer> addL16();

        @Alias("al")
        @Invoke(InvocationMode.PARALLEL)
        @AsyncMethod(value = List.class, mode = OutputMode.ELEMENT)
        InvocationChannel<List<Character>, Integer> addL17();

        @Alias("al")
        @AsyncMethod(value = List.class, mode = OutputMode.ELEMENT)
        Routine<List<Character>, Integer> addL18();

        @Alias("al")
        @Invoke(InvocationMode.PARALLEL)
        @AsyncMethod(value = List.class, mode = OutputMode.ELEMENT)
        Routine<List<Character>, Integer> addL19();

        @Alias("g")
        int get0();

        @Alias("s")
        void set0(int i);

        @Alias("g")
        @AsyncOut(OutputMode.VALUE)
        OutputChannel<Integer> get1();

        @Alias("s")
        void set1(@AsyncIn(value = int.class, mode = InputMode.VALUE) OutputChannel<Integer> i);

        @Alias("g")
        @AsyncMethod({})
        InvocationChannel<Void, Integer> get2();

        @Alias("s")
        @Invoke(InvocationMode.PARALLEL)
        void set2(@AsyncIn(value = int.class, mode = InputMode.VALUE) OutputChannel<Integer> i);

        @Alias("g")
        @AsyncMethod({})
        Routine<Void, Integer> get4();

        @Alias("ga")
        int[] getA0();

        @Alias("sa")
        void setA0(int[] i);

        @Alias("ga")
        @AsyncOut(OutputMode.ELEMENT)
        OutputChannel<Integer> getA1();

        @Alias("sa")
        void setA1(@AsyncIn(value = int[].class, mode = InputMode.VALUE) OutputChannel<int[]> i);

        @Alias("ga")
        @AsyncMethod({})
        InvocationChannel<Void, int[]> getA2();

        @Alias("sa")
        void setA2(@AsyncIn(value = int[].class,
                mode = InputMode.COLLECTION) OutputChannel<Integer> i);

        @Alias("ga")
        @AsyncMethod({})
        Routine<Void, int[]> getA3();

        @Alias("sa")
        @Invoke(InvocationMode.PARALLEL)
        void setA3(@AsyncIn(value = int[].class, mode = InputMode.VALUE) OutputChannel<int[]> i);

        @Alias("ga")
        @AsyncMethod(value = {}, mode = OutputMode.ELEMENT)
        InvocationChannel<Void, Integer> getA4();

        @Alias("ga")
        @AsyncMethod(value = {}, mode = OutputMode.ELEMENT)
        Routine<Void, Integer> getA5();

        @Alias("gl")
        List<Integer> getL0();

        @Alias("sl")
        void setL0(List<Integer> i);

        @Alias("gl")
        @AsyncOut(OutputMode.ELEMENT)
        OutputChannel<Integer> getL1();

        @Alias("sl")
        void setL1(@AsyncIn(value = List.class,
                mode = InputMode.VALUE) OutputChannel<List<Integer>> i);

        @Alias("gl")
        @AsyncMethod({})
        InvocationChannel<Void, List<Integer>> getL2();

        @Alias("sl")
        void setL2(
                @AsyncIn(value = List.class, mode = InputMode.COLLECTION) OutputChannel<Integer> i);

        @Alias("gl")
        @AsyncMethod({})
        Routine<Void, List<Integer>> getL3();

        @Alias("sl")
        @Invoke(InvocationMode.PARALLEL)
        void setL3(@AsyncIn(value = List.class,
                mode = InputMode.VALUE) OutputChannel<List<Integer>> i);

        @Alias("gl")
        @AsyncMethod(value = {}, mode = OutputMode.ELEMENT)
        InvocationChannel<Void, Integer> getL4();

        @Alias("gl")
        @AsyncMethod(value = {}, mode = OutputMode.ELEMENT)
        Routine<Void, Integer> getL5();

        @Alias("s")
        @AsyncMethod(int.class)
        InvocationChannel<Integer, Void> set3();

        @Alias("s")
        @AsyncMethod(int.class)
        Routine<Integer, Void> set5();

        @Alias("sa")
        @AsyncMethod(int[].class)
        InvocationChannel<int[], Void> setA4();

        @Alias("sa")
        @AsyncMethod(int[].class)
        Routine<int[], Void> setA6();

        @Alias("sl")
        @AsyncMethod(List.class)
        InvocationChannel<List<Integer>, Void> setL4();

        @Alias("sl")
        @AsyncMethod(List.class)
        Routine<List<Integer>, Void> setL6();
    }

    public interface SumError2 {

        @Alias("compute")
        @AsyncMethod({int.class, int.class})
        int compute1();

        @Alias("compute")
        @AsyncOut
        @AsyncMethod({int.class, int.class})
        InputChannel<Integer> compute2();

        @Alias("compute")
        @Invoke(InvocationMode.PARALLEL)
        @AsyncMethod({int.class, int.class})
        InputChannel<Integer> compute3();

        @Alias("compute")
        @AsyncMethod({int[].class, int.class})
        InputChannel<Integer> compute4();

        @Alias("compute")
        @AsyncMethod({int.class, int.class})
        InputChannel<Integer> compute5(int i);
    }

    private interface CountError {

        @AsyncOut
        String[] count(int length);

        @Alias("count")
        @AsyncOut(OutputMode.ELEMENT)
        String[] count1(int length);

        @Alias("count")
        @AsyncOut(OutputMode.ELEMENT)
        OutputChannel<Integer> count2();

        @AsyncOut(OutputMode.VALUE)
        List<Integer> countList(int length);

        @Alias("countList")
        @AsyncOut(OutputMode.ELEMENT)
        List<Integer> countList1(int length);
    }

    private interface CountItf {

        @AsyncOut(OutputMode.ELEMENT)
        OutputChannel<Integer> count(int length);

        @Alias("count")
        @AsyncOut(OutputMode.VALUE)
        OutputChannel<int[]> count1(int length);

        @Alias("count")
        @AsyncOut(OutputMode.ELEMENT)
        OutputChannel<Integer> count2(int length);

        @AsyncOut(OutputMode.ELEMENT)
        OutputChannel<Integer> countList(int length);

        @Alias("countList")
        @AsyncOut(OutputMode.ELEMENT)
        OutputChannel<Integer> countList1(int length);
    }

    private interface PriorityPass {

        @AsyncOut
        @Alias("pass")
        @Priority(AgingPriority.HIGH_PRIORITY)
        OutputChannel<String> passHigh(String s);

        @AsyncOut
        @Alias("pass")
        @Priority(AgingPriority.NORMAL_PRIORITY)
        OutputChannel<String> passNormal(String s);
    }

    private interface SizeItf {

        @AsyncMethod(List.class)
        InvocationChannel<List<String>, Integer> getSize();

        @AsyncOut
        OutputChannel<Integer> getSize(List<String> l);
    }

    private interface SquareItf {

        @ReadTimeout(value = 1, unit = TimeUnit.SECONDS)
        int compute(int i);

        @Alias("compute")
        @ReadTimeout(1000)
        int computeAsync(@AsyncIn(int.class) OutputChannel<Integer> i);

        @SharedFields({})
        @Alias("compute")
        @Invoke(InvocationMode.PARALLEL)
        @AsyncOut
        OutputChannel<Integer> computeParallel(@AsyncIn(int.class) OutputChannel<Integer> i);
    }

    private interface SumError {

        int compute(int a, @AsyncIn(int.class) int[] b);

        int compute(@AsyncIn(int.class) String[] ints);

        int compute(@AsyncIn(value = int.class, mode = InputMode.VALUE) int[] ints);

        int compute(
                @AsyncIn(value = int.class, mode = InputMode.COLLECTION) Iterable<Integer> ints);

        int compute(@AsyncIn(value = int.class,
                mode = InputMode.COLLECTION) OutputChannel<Integer> ints);

        int compute(int a, @AsyncIn(value = int[].class,
                mode = InputMode.COLLECTION) OutputChannel<Integer> b);

        @Invoke(InvocationMode.PARALLEL)
        int compute(String text,
                @AsyncIn(value = int.class, mode = InputMode.VALUE) OutputChannel<Integer> ints);
    }

    private interface SumItf {

        int compute(int a, @AsyncIn(int.class) OutputChannel<Integer> b);

        int compute(@AsyncIn(value = int[].class,
                mode = InputMode.COLLECTION) OutputChannel<Integer> ints);

        @Alias("compute")
        int compute1(@AsyncIn(int[].class) OutputChannel<int[]> ints);

        @Alias("compute")
        @AsyncMethod(int[].class)
        InvocationChannel<int[], Integer> compute2();

        @Alias("compute")
        @AsyncMethod(int.class)
        InvocationChannel<Integer, Integer> compute3();

        @Alias("compute")
        int computeList(@AsyncIn(value = List.class,
                mode = InputMode.COLLECTION) OutputChannel<Integer> ints);

        @Alias("compute")
        int computeList1(@AsyncIn(value = List.class,
                mode = InputMode.COLLECTION) OutputChannel<Integer> ints);
    }

    private interface TestInterface {

        @ReadTimeout(value = 1, unit = TimeUnit.SECONDS)
        int getInt(int i);
    }

    private interface TestInterfaceAsync {

        int getInt(@AsyncIn(int.class) OutputChannel<Integer> i);

        @AsyncOut
        OutputChannel<Integer> getOne();

        @Alias(value = "getInt")
        int take(int i);
    }

    private interface TestItf {

        void throwException(@AsyncIn(int.class) RuntimeException ex);

        @Alias(TestClass.THROW)
        @AsyncOut
        void throwException1(RuntimeException ex);

        @Alias(TestClass.THROW)
        int throwException2(RuntimeException ex);
    }

    private interface TestTimeoutItf {

        @ReadTimeoutAction(TimeoutActionType.ABORT)
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

    @SuppressWarnings("unused")
    private static class Count {

        public int count() {

            return 0;
        }

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
    private static class DuplicateAnnotationStatic {

        public static final String GET = "get";

        @Alias(GET)
        public static int getOne() {

            return 1;
        }

        @Alias(GET)
        public static int getTwo() {

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
    private static class Pass {

        public String pass(final String s) {

            return s;
        }
    }

    @SuppressWarnings("unused")
    private static class Size {

        public int getSize(final List<String> l) {

            return l.size();
        }

        public int getSize(final String s) {

            return s.length();
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

        public int compute(final int a) {

            return a;
        }

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

            TimeDuration.millis(500).sleepAtLeast();

            return 1;
        }

        public int getTwo() throws InterruptedException {

            TimeDuration.millis(500).sleepAtLeast();

            return 2;
        }
    }

    @SuppressWarnings("unused")
    private static class TestClassImpl implements TestInterface {

        public static final String GET = "get";

        public static final String STATIC_GET = "sget";

        @Alias(STATIC_GET)
        public static int get(final int i) {

            return i;
        }

        public int getInt(final int i) {

            return i;
        }

        @Alias(GET)
        public int getOne() {

            return 1;
        }
    }

    private static class TestRunner implements Runner {

        private final ArrayList<Execution> mExecutions = new ArrayList<Execution>();

        public void cancel(@NotNull final Execution execution) {

        }

        public boolean isExecutionThread() {

            return true;
        }

        public void run(@NotNull final Execution execution, final long delay,
                @NotNull final TimeUnit timeUnit) {

            mExecutions.add(execution);
        }

        private void run(int count) {

            final Iterator<Execution> iterator = mExecutions.iterator();

            while (iterator.hasNext() && (count-- > 0)) {

                final Execution execution = iterator.next();
                iterator.remove();
                execution.run();
            }
        }
    }

    @SuppressWarnings("unused")
    private static class TestStatic {

        public static final String GET = "get";

        public static final String THROW = "throw";

        @Alias(GET)
        public static long getLong() {

            return -77;
        }

        @Alias(THROW)
        public static void throwException(final RuntimeException ex) {

            throw ex;
        }
    }

    @SuppressWarnings("unused")
    private static class TestStatic2 {

        public static int getOne() throws InterruptedException {

            TimeDuration.millis(500).sleepAtLeast();

            return 1;
        }

        public static int getTwo() throws InterruptedException {

            TimeDuration.millis(500).sleepAtLeast();

            return 2;
        }
    }

    private static class TestTimeout {

        @Alias("test")
        @ReadTimeoutAction(TimeoutActionType.EXIT)
        public int getInt() throws InterruptedException {

            Thread.sleep(100);
            return 31;
        }
    }
}
