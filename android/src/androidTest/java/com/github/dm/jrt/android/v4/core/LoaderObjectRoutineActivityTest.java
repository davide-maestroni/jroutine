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
package com.github.dm.jrt.android.v4.core;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.builder.LoaderConfiguration;
import com.github.dm.jrt.android.builder.LoaderObjectRoutineBuilder;
import com.github.dm.jrt.android.core.ContextInvocationTarget;
import com.github.dm.jrt.android.core.FactoryContextWrapper;
import com.github.dm.jrt.annotation.Alias;
import com.github.dm.jrt.annotation.Input;
import com.github.dm.jrt.annotation.Input.InputMode;
import com.github.dm.jrt.annotation.Inputs;
import com.github.dm.jrt.annotation.Invoke;
import com.github.dm.jrt.annotation.Invoke.InvocationMode;
import com.github.dm.jrt.annotation.Output;
import com.github.dm.jrt.annotation.Output.OutputMode;
import com.github.dm.jrt.annotation.ShareGroup;
import com.github.dm.jrt.annotation.Timeout;
import com.github.dm.jrt.annotation.TimeoutAction;
import com.github.dm.jrt.builder.InvocationConfiguration;
import com.github.dm.jrt.builder.InvocationConfiguration.OrderType;
import com.github.dm.jrt.builder.InvocationConfiguration.TimeoutActionType;
import com.github.dm.jrt.builder.ProxyConfiguration;
import com.github.dm.jrt.channel.AbortException;
import com.github.dm.jrt.channel.IOChannel;
import com.github.dm.jrt.channel.InvocationChannel;
import com.github.dm.jrt.channel.OutputChannel;
import com.github.dm.jrt.channel.StreamingChannel;
import com.github.dm.jrt.invocation.InvocationException;
import com.github.dm.jrt.log.Log;
import com.github.dm.jrt.log.Log.LogLevel;
import com.github.dm.jrt.log.NullLog;
import com.github.dm.jrt.routine.Routine;
import com.github.dm.jrt.runner.Runners;
import com.github.dm.jrt.util.ClassToken;
import com.github.dm.jrt.util.TimeDuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.android.core.ContextInvocationTarget.instanceOf;
import static com.github.dm.jrt.android.v4.core.LoaderContext.contextFrom;
import static com.github.dm.jrt.builder.InvocationConfiguration.builder;
import static com.github.dm.jrt.util.TimeDuration.INFINITY;
import static com.github.dm.jrt.util.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loader object routine activity unit tests.
 * <p/>
 * Created by davide-maestroni on 04/07/2015.
 */
@TargetApi(VERSION_CODES.FROYO)
public class LoaderObjectRoutineActivityTest
        extends ActivityInstrumentationTestCase2<TestActivity> {

    public LoaderObjectRoutineActivityTest() {

        super(TestActivity.class);
    }

    public void testAliasMethod() throws NoSuchMethodException {

        final TimeDuration timeout = seconds(10);
        final Routine<Object, Object> routine = JRoutine.with(contextFrom(getActivity()))
                                                        .on(instanceOf(TestClass.class))
                                                        .invocations()
                                                        .withRunner(Runners.poolRunner())
                                                        .withMaxInstances(1)
                                                        .withCoreInstances(1)
                                                        .withExecutionTimeoutAction(
                                                                TimeoutActionType.EXIT)
                                                        .withLogLevel(LogLevel.DEBUG)
                                                        .withLog(new NullLog())
                                                        .set()
                                                        .aliasMethod(TestClass.GET);

        assertThat(routine.syncCall().afterMax(timeout).all()).containsExactly(-77L);
    }

    public void testArgs() {

        assertThat(JRoutine.with(contextFrom(getActivity()))
                           .on(instanceOf(TestArgs.class, 17))
                           .method("getId")
                           .asyncCall()
                           .eventually()
                           .next()).isEqualTo(17);
    }

    public void testAsyncInputProxyRoutine() {

        final TimeDuration timeout = seconds(10);
        final SumItf sumAsync = JRoutine.with(contextFrom(getActivity()))
                                        .on(instanceOf(Sum.class))
                                        .invocations()
                                        .withExecutionTimeout(timeout)
                                        .set()
                                        .buildProxy(SumItf.class);
        final IOChannel<Integer, Integer> channel3 = JRoutine.io().buildChannel();
        channel3.pass(7).close();
        assertThat(sumAsync.compute(3, channel3)).isEqualTo(10);

        final IOChannel<Integer, Integer> channel4 = JRoutine.io().buildChannel();
        channel4.pass(1, 2, 3, 4).close();
        assertThat(sumAsync.compute(channel4)).isEqualTo(10);

        final IOChannel<int[], int[]> channel5 = JRoutine.io().buildChannel();
        channel5.pass(new int[]{1, 2, 3, 4}).close();
        assertThat(sumAsync.compute1(channel5)).isEqualTo(10);

        final IOChannel<Integer, Integer> channel6 = JRoutine.io().buildChannel();
        channel6.pass(1, 2, 3, 4).close();
        assertThat(sumAsync.computeList(channel6)).isEqualTo(10);

        final IOChannel<Integer, Integer> channel7 = JRoutine.io().buildChannel();
        channel7.pass(1, 2, 3, 4).close();
        assertThat(sumAsync.computeList1(channel7)).isEqualTo(10);
    }

    public void testAsyncOutputProxyRoutine() {

        final TimeDuration timeout = seconds(10);
        final CountItf countAsync = JRoutine.with(contextFrom(getActivity()))
                                            .on(instanceOf(Count.class))
                                            .invocations()
                                            .withExecutionTimeout(timeout)
                                            .set()
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

            new DefaultLoaderObjectRoutineBuilder(contextFrom(getActivity()),
                                                  instanceOf(TestClass.class)).setConfiguration(
                    (InvocationConfiguration) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultLoaderObjectRoutineBuilder(contextFrom(getActivity()),
                                                  instanceOf(TestClass.class)).setConfiguration(
                    (ProxyConfiguration) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultLoaderObjectRoutineBuilder(contextFrom(getActivity()),
                                                  instanceOf(TestClass.class)).setConfiguration(
                    (LoaderConfiguration) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testConfigurationWarnings() {

        final CountLog countLog = new CountLog();
        final InvocationConfiguration configuration = builder().withRunner(Runners.poolRunner())
                                                               .withInputOrder(OrderType.BY_CHANCE)
                                                               .withInputMaxSize(3)
                                                               .withInputTimeout(seconds(10))
                                                               .withOutputOrder(OrderType.BY_CHANCE)
                                                               .withOutputMaxSize(3)
                                                               .withOutputTimeout(seconds(10))
                                                               .withLogLevel(LogLevel.DEBUG)
                                                               .withLog(countLog)
                                                               .set();
        JRoutine.with(contextFrom(getActivity()))
                .on(instanceOf(TestClass.class))
                .invocations()
                .with(configuration)
                .set()
                .proxies()
                .withShareGroup("test")
                .set()
                .aliasMethod(TestClass.GET);
        assertThat(countLog.getWrnCount()).isEqualTo(1);

        JRoutine.with(contextFrom(getActivity()))
                .on(instanceOf(Square.class))
                .invocations()
                .with(configuration)
                .set()
                .proxies()
                .withShareGroup("test")
                .set()
                .buildProxy(SquareItf.class)
                .compute(3);
        assertThat(countLog.getWrnCount()).isEqualTo(2);
    }

    public void testContextWrapper() {

        final TestActivity activity = getActivity();
        final StringContext contextWrapper = new StringContext(activity);
        assertThat(JRoutine.with(contextFrom(activity, contextWrapper))
                           .on(instanceOf(String.class))
                           .method("toString")
                           .asyncCall()
                           .eventually()
                           .next()).isEqualTo("test1");
    }

    public void testDuplicateAnnotationError() {

        try {

            JRoutine.with(contextFrom(getActivity()))
                    .on(instanceOf(DuplicateAnnotation.class))
                    .aliasMethod(DuplicateAnnotation.GET);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testException() throws NoSuchMethodException {

        final TimeDuration timeout = seconds(10);
        final Routine<Object, Object> routine3 = JRoutine.with(contextFrom(getActivity()))
                                                         .on(instanceOf(TestClass.class))
                                                         .aliasMethod(TestClass.THROW);

        try {

            routine3.syncCall(new IllegalArgumentException("test")).afterMax(timeout).all();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
            assertThat(e.getCause().getMessage()).isEqualTo("test");
        }
    }

    public void testInvalidProxyError() {

        try {

            JRoutine.with(contextFrom(getActivity()))
                    .on(instanceOf(TestClass.class))
                    .buildProxy(TestClass.class);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.with(contextFrom(getActivity()))
                    .on(instanceOf(TestClass.class))
                    .buildProxy(ClassToken.tokenOf(TestClass.class));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testInvalidProxyInputAnnotationError() {

        try {

            JRoutine.with(contextFrom(getActivity()))
                    .on(instanceOf(Sum.class))
                    .buildProxy(SumError.class)
                    .compute(1, new int[0]);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.with(contextFrom(getActivity()))
                    .on(instanceOf(Sum.class))
                    .buildProxy(SumError.class)
                    .compute(new String[0]);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.with(contextFrom(getActivity()))
                    .on(instanceOf(Sum.class))
                    .buildProxy(SumError.class)
                    .compute(new int[0]);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.with(contextFrom(getActivity()))
                    .on(instanceOf(Sum.class))
                    .buildProxy(SumError.class)
                    .compute(Collections.<Integer>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        final IOChannel<Integer, Integer> channel = JRoutine.io().buildChannel();

        try {

            JRoutine.with(contextFrom(getActivity()))
                    .on(instanceOf(Sum.class))
                    .buildProxy(SumError.class)
                    .compute(channel);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.with(contextFrom(getActivity()))
                    .on(instanceOf(Sum.class))
                    .buildProxy(SumError.class)
                    .compute(1, channel);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.with(contextFrom(getActivity()))
                    .on(instanceOf(Sum.class))
                    .buildProxy(SumError.class)
                    .compute(new Object());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.with(contextFrom(getActivity()))
                    .on(instanceOf(Sum.class))
                    .buildProxy(SumError.class)
                    .compute(new Object[0]);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.with(contextFrom(getActivity()))
                    .on(instanceOf(Sum.class))
                    .buildProxy(SumError.class)
                    .compute("test", new int[0]);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testInvalidProxyMethodError() {

        try {

            JRoutine.with(contextFrom(getActivity()))
                    .on(instanceOf(TestClass.class))
                    .invocations()
                    .withExecutionTimeout(INFINITY)
                    .set()
                    .buildProxy(TestItf.class)
                    .throwException(null);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.with(contextFrom(getActivity()))
                    .on(instanceOf(TestClass.class))
                    .invocations()
                    .withExecutionTimeout(INFINITY)
                    .set()
                    .buildProxy(TestItf.class)
                    .throwException1(null);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.with(contextFrom(getActivity()))
                    .on(instanceOf(TestClass.class))
                    .invocations()
                    .withExecutionTimeout(INFINITY)
                    .set()
                    .buildProxy(TestItf.class)
                    .throwException2(null);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testInvalidProxyOutputAnnotationError() {

        try {

            JRoutine.with(contextFrom(getActivity()))
                    .on(instanceOf(Count.class))
                    .buildProxy(CountError.class)
                    .count(3);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.with(contextFrom(getActivity()))
                    .on(instanceOf(Count.class))
                    .buildProxy(CountError.class)
                    .count1(3);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.with(contextFrom(getActivity()))
                    .on(instanceOf(Count.class))
                    .buildProxy(CountError.class)
                    .count2(3);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.with(contextFrom(getActivity()))
                    .on(instanceOf(Count.class))
                    .buildProxy(CountError.class)
                    .countList(3);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.with(contextFrom(getActivity()))
                    .on(instanceOf(Count.class))
                    .buildProxy(CountError.class)
                    .countList1(3);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.with(contextFrom(getActivity()))
                    .on(instanceOf(Count.class))
                    .buildProxy(CountError.class)
                    .countList2(3);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testMethod() throws NoSuchMethodException {

        final TimeDuration timeout = seconds(10);
        final Routine<Object, Object> routine2 = JRoutine.with(contextFrom(getActivity()))
                                                         .on(instanceOf(TestClass.class))
                                                         .invocations()
                                                         .withRunner(Runners.poolRunner())
                                                         .withMaxInstances(1)
                                                         .set()
                                                         .proxies()
                                                         .withShareGroup("test")
                                                         .set()
                                                         .method(TestClass.class.getMethod(
                                                                 "getLong"));

        assertThat(routine2.syncCall().afterMax(timeout).all()).containsExactly(-77L);

    }

    public void testMethodBySignature() throws NoSuchMethodException {

        final TimeDuration timeout = seconds(10);
        final Routine<Object, Object> routine1 = JRoutine.with(contextFrom(getActivity()))
                                                         .on(instanceOf(TestClass.class))
                                                         .invocations()
                                                         .withRunner(Runners.poolRunner())
                                                         .set()
                                                         .method("getLong");

        assertThat(routine1.syncCall().afterMax(timeout).all()).containsExactly(-77L);
    }

    public void testMissingAliasMethodError() {

        try {

            JRoutine.with(contextFrom(getActivity()))
                    .on(instanceOf(TestClass.class))
                    .aliasMethod("test");

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testMissingMethodError() {

        try {

            JRoutine.with(contextFrom(getActivity()))
                    .on(instanceOf(TestClass.class))
                    .method("test");

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testNullPointerError() {

        try {

            JRoutine.with(contextFrom(getActivity())).on((ContextInvocationTarget) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.with(contextFrom(getActivity())).on(instanceOf(null));

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testNullProxyError() {

        try {

            JRoutine.with(contextFrom(getActivity()))
                    .on(instanceOf(TestClass.class))
                    .buildProxy((Class<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.with(contextFrom(getActivity()))
                    .on(instanceOf(TestClass.class))
                    .buildProxy((ClassToken<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @SuppressWarnings("unchecked")
    public void testProxyAnnotations() {

        final Itf itf = JRoutine.with(contextFrom(getActivity()))
                                .on(instanceOf(Impl.class))
                                .invocations()
                                .withExecutionTimeout(INFINITY)
                                .set()
                                .buildProxy(Itf.class);

        assertThat(itf.add0('c')).isEqualTo((int) 'c');
        final IOChannel<Character, Character> channel1 = JRoutine.io().buildChannel();
        channel1.pass('a').close();
        assertThat(itf.add1(channel1)).isEqualTo((int) 'a');
        final IOChannel<Character, Character> channel2 = JRoutine.io().buildChannel();
        channel2.pass('d', 'e', 'f').close();
        assertThat(itf.add2(channel2)).isIn((int) 'd', (int) 'e', (int) 'f');
        assertThat(itf.add3('c').all()).containsExactly((int) 'c');
        final IOChannel<Character, Character> channel3 = JRoutine.io().buildChannel();
        channel3.pass('a').close();
        assertThat(itf.add4(channel3).all()).containsExactly((int) 'a');
        final IOChannel<Character, Character> channel4 = JRoutine.io().buildChannel();
        channel4.pass('d', 'e', 'f').close();
        assertThat(itf.add5(channel4).all()).containsOnly((int) 'd', (int) 'e', (int) 'f');
        assertThat(itf.add6().pass('d').result().all()).containsOnly((int) 'd');
        assertThat(itf.add7().pass('d', 'e', 'f').result().all()).containsOnly((int) 'd', (int) 'e',
                                                                               (int) 'f');
        assertThat(itf.add8().pass('d').close().all()).containsOnly((int) 'd');
        assertThat(itf.add9().pass('d', 'e', 'f').close().all()).containsOnly((int) 'd', (int) 'e',
                                                                              (int) 'f');
        assertThat(itf.add10().asyncCall('d').all()).containsOnly((int) 'd');
        assertThat(itf.add11().parallelCall('d', 'e', 'f').all()).containsOnly((int) 'd', (int) 'e',
                                                                               (int) 'f');
        assertThat(itf.addA00(new char[]{'c', 'z'})).isEqualTo(new int[]{'c', 'z'});
        final IOChannel<char[], char[]> channel5 = JRoutine.io().buildChannel();
        channel5.pass(new char[]{'a', 'z'}).close();
        assertThat(itf.addA01(channel5)).isEqualTo(new int[]{'a', 'z'});
        final IOChannel<Character, Character> channel6 = JRoutine.io().buildChannel();
        channel6.pass('d', 'e', 'f').close();
        assertThat(itf.addA02(channel6)).isEqualTo(new int[]{'d', 'e', 'f'});
        final IOChannel<char[], char[]> channel7 = JRoutine.io().buildChannel();
        channel7.pass(new char[]{'d', 'z'}, new char[]{'e', 'z'}, new char[]{'f', 'z'}).close();
        assertThat(itf.addA03(channel7)).isIn(new int[]{'d', 'z'}, new int[]{'e', 'z'},
                                              new int[]{'f', 'z'});
        assertThat(itf.addA04(new char[]{'c', 'z'}).all()).containsExactly(new int[]{'c', 'z'});
        final IOChannel<char[], char[]> channel8 = JRoutine.io().buildChannel();
        channel8.pass(new char[]{'a', 'z'}).close();
        assertThat(itf.addA05(channel8).all()).containsExactly(new int[]{'a', 'z'});
        final IOChannel<Character, Character> channel9 = JRoutine.io().buildChannel();
        channel9.pass('d', 'e', 'f').close();
        assertThat(itf.addA06(channel9).all()).containsExactly(new int[]{'d', 'e', 'f'});
        final IOChannel<char[], char[]> channel10 = JRoutine.io().buildChannel();
        channel10.pass(new char[]{'d', 'z'}, new char[]{'e', 'z'}, new char[]{'f', 'z'}).close();
        assertThat(itf.addA07(channel10).all()).containsOnly(new int[]{'d', 'z'},
                                                             new int[]{'e', 'z'},
                                                             new int[]{'f', 'z'});
        assertThat(itf.addA08(new char[]{'c', 'z'}).all()).containsExactly((int) 'c', (int) 'z');
        final IOChannel<char[], char[]> channel11 = JRoutine.io().buildChannel();
        channel11.pass(new char[]{'a', 'z'}).close();
        assertThat(itf.addA09(channel11).all()).containsExactly((int) 'a', (int) 'z');
        final IOChannel<Character, Character> channel12 = JRoutine.io().buildChannel();
        channel12.pass('d', 'e', 'f').close();
        assertThat(itf.addA10(channel12).all()).containsExactly((int) 'd', (int) 'e', (int) 'f');
        final IOChannel<char[], char[]> channel13 = JRoutine.io().buildChannel();
        channel13.pass(new char[]{'d', 'z'}, new char[]{'e', 'z'}, new char[]{'f', 'z'}).close();
        assertThat(itf.addA11(channel13).all()).containsOnly((int) 'd', (int) 'e', (int) 'f',
                                                             (int) 'z');
        assertThat(itf.addA12(new char[]{'c', 'z'})).containsExactly(new int[]{'c', 'z'});
        final IOChannel<char[], char[]> channel14 = JRoutine.io().buildChannel();
        channel14.pass(new char[]{'a', 'z'}).close();
        assertThat(itf.addA13(channel14)).containsExactly(new int[]{'a', 'z'});
        final IOChannel<Character, Character> channel15 = JRoutine.io().buildChannel();
        channel15.pass('d', 'e', 'f').close();
        assertThat(itf.addA14(channel15)).containsExactly(new int[]{'d', 'e', 'f'});
        final IOChannel<char[], char[]> channel16 = JRoutine.io().buildChannel();
        channel16.pass(new char[]{'d', 'z'}, new char[]{'e', 'z'}, new char[]{'f', 'z'}).close();
        assertThat(itf.addA15(channel16)).containsOnly(new int[]{'d', 'z'}, new int[]{'e', 'z'},
                                                       new int[]{'f', 'z'});
        assertThat(itf.addA16(new char[]{'c', 'z'})).containsExactly(new int[]{'c', 'z'});
        final IOChannel<char[], char[]> channel17 = JRoutine.io().buildChannel();
        channel17.pass(new char[]{'a', 'z'}).close();
        assertThat(itf.addA17(channel17)).containsExactly(new int[]{'a', 'z'});
        final IOChannel<Character, Character> channel18 = JRoutine.io().buildChannel();
        channel18.pass('d', 'e', 'f').close();
        assertThat(itf.addA18(channel18)).containsExactly(new int[]{'d', 'e', 'f'});
        final IOChannel<char[], char[]> channel19 = JRoutine.io().buildChannel();
        channel19.pass(new char[]{'d', 'z'}, new char[]{'e', 'z'}, new char[]{'f', 'z'}).close();
        assertThat(itf.addA19(channel19)).containsOnly(new int[]{'d', 'z'}, new int[]{'e', 'z'},
                                                       new int[]{'f', 'z'});
        assertThat(itf.addA20().pass(new char[]{'c', 'z'}).result().all()).containsOnly(
                new int[]{'c', 'z'});
        assertThat(itf.addA21()
                      .pass(new char[]{'d', 'z'}, new char[]{'e', 'z'}, new char[]{'f', 'z'})
                      .result()
                      .all()).containsOnly(new int[]{'d', 'z'}, new int[]{'e', 'z'},
                                           new int[]{'f', 'z'});
        assertThat(itf.addA22().pass(new char[]{'c', 'z'}).close().all()).containsOnly(
                new int[]{'c', 'z'});
        assertThat(itf.addA23()
                      .pass(new char[]{'d', 'z'}, new char[]{'e', 'z'}, new char[]{'f', 'z'})
                      .close()
                      .all()).containsOnly(new int[]{'d', 'z'}, new int[]{'e', 'z'},
                                           new int[]{'f', 'z'});
        assertThat(itf.addA24().asyncCall(new char[]{'c', 'z'}).all()).containsOnly(
                new int[]{'c', 'z'});
        assertThat(itf.addA25()
                      .parallelCall(new char[]{'d', 'z'}, new char[]{'e', 'z'},
                                    new char[]{'f', 'z'})
                      .all()).containsOnly(new int[]{'d', 'z'}, new int[]{'e', 'z'},
                                           new int[]{'f', 'z'});
        assertThat(itf.addL00(Arrays.asList('c', 'z'))).isEqualTo(
                Arrays.asList((int) 'c', (int) 'z'));
        final IOChannel<List<Character>, List<Character>> channel20 = JRoutine.io().buildChannel();
        channel20.pass(Arrays.asList('a', 'z')).close();
        assertThat(itf.addL01(channel20)).isEqualTo(Arrays.asList((int) 'a', (int) 'z'));
        final IOChannel<Character, Character> channel21 = JRoutine.io().buildChannel();
        channel21.pass('d', 'e', 'f').close();
        assertThat(itf.addL02(channel21)).isEqualTo(Arrays.asList((int) 'd', (int) 'e', (int) 'f'));
        final IOChannel<List<Character>, List<Character>> channel22 = JRoutine.io().buildChannel();
        channel22.pass(Arrays.asList('d', 'z'), Arrays.asList('e', 'z'), Arrays.asList('f', 'z'))
                 .close();
        assertThat(itf.addL03(channel22)).isIn(Arrays.asList((int) 'd', (int) 'z'),
                                               Arrays.asList((int) 'e', (int) 'z'),
                                               Arrays.asList((int) 'f', (int) 'z'));
        assertThat(itf.addL04(Arrays.asList('c', 'z')).all()).containsExactly(
                Arrays.asList((int) 'c', (int) 'z'));
        final IOChannel<List<Character>, List<Character>> channel23 = JRoutine.io().buildChannel();
        channel23.pass(Arrays.asList('a', 'z')).close();
        assertThat(itf.addL05(channel23).all()).containsExactly(
                Arrays.asList((int) 'a', (int) 'z'));
        final IOChannel<Character, Character> channel24 = JRoutine.io().buildChannel();
        channel24.pass('d', 'e', 'f').close();
        assertThat(itf.addL06(channel24).all()).containsExactly(
                Arrays.asList((int) 'd', (int) 'e', (int) 'f'));
        final IOChannel<List<Character>, List<Character>> channel25 = JRoutine.io().buildChannel();
        channel25.pass(Arrays.asList('d', 'z'), Arrays.asList('e', 'z'), Arrays.asList('f', 'z'))
                 .close();
        assertThat(itf.addL07(channel25).all()).containsOnly(Arrays.asList((int) 'd', (int) 'z'),
                                                             Arrays.asList((int) 'e', (int) 'z'),
                                                             Arrays.asList((int) 'f', (int) 'z'));
        assertThat(itf.addL08(Arrays.asList('c', 'z')).all()).containsExactly((int) 'c', (int) 'z');
        final IOChannel<List<Character>, List<Character>> channel26 = JRoutine.io().buildChannel();
        channel26.pass(Arrays.asList('a', 'z')).close();
        assertThat(itf.addL09(channel26).all()).containsExactly((int) 'a', (int) 'z');
        final IOChannel<Character, Character> channel27 = JRoutine.io().buildChannel();
        channel27.pass('d', 'e', 'f').close();
        assertThat(itf.addL10(channel27).all()).containsExactly((int) 'd', (int) 'e', (int) 'f');
        final IOChannel<List<Character>, List<Character>> channel28 = JRoutine.io().buildChannel();
        channel28.pass(Arrays.asList('d', 'z'), Arrays.asList('e', 'z'), Arrays.asList('f', 'z'))
                 .close();
        assertThat(itf.addL11(channel28).all()).containsOnly((int) 'd', (int) 'e', (int) 'f',
                                                             (int) 'z');
        assertThat(itf.addL12(Arrays.asList('c', 'z'))).containsExactly(
                Arrays.asList((int) 'c', (int) 'z'));
        final IOChannel<List<Character>, List<Character>> channel29 = JRoutine.io().buildChannel();
        channel29.pass(Arrays.asList('a', 'z')).close();
        assertThat(itf.addL13(channel29)).containsExactly(Arrays.asList((int) 'a', (int) 'z'));
        final IOChannel<Character, Character> channel30 = JRoutine.io().buildChannel();
        channel30.pass('d', 'e', 'f').close();
        assertThat(itf.addL14(channel30)).containsExactly(
                Arrays.asList((int) 'd', (int) 'e', (int) 'f'));
        final IOChannel<List<Character>, List<Character>> channel31 = JRoutine.io().buildChannel();
        channel31.pass(Arrays.asList('d', 'z'), Arrays.asList('e', 'z'), Arrays.asList('f', 'z'))
                 .close();
        assertThat(itf.addL15(channel31)).containsOnly(Arrays.asList((int) 'd', (int) 'z'),
                                                       Arrays.asList((int) 'e', (int) 'z'),
                                                       Arrays.asList((int) 'f', (int) 'z'));
        assertThat(itf.addL16(Arrays.asList('c', 'z'))).containsExactly(
                Arrays.asList((int) 'c', (int) 'z'));
        final IOChannel<List<Character>, List<Character>> channel32 = JRoutine.io().buildChannel();
        channel32.pass(Arrays.asList('a', 'z')).close();
        assertThat(itf.addL17(channel32)).containsExactly(Arrays.asList((int) 'a', (int) 'z'));
        final IOChannel<Character, Character> channel33 = JRoutine.io().buildChannel();
        channel33.pass('d', 'e', 'f').close();
        assertThat(itf.addL18(channel33)).containsExactly(
                Arrays.asList((int) 'd', (int) 'e', (int) 'f'));
        final IOChannel<List<Character>, List<Character>> channel34 = JRoutine.io().buildChannel();
        channel34.pass(Arrays.asList('d', 'z'), Arrays.asList('e', 'z'), Arrays.asList('f', 'z'))
                 .close();
        assertThat(itf.addL19(channel34)).containsOnly(Arrays.asList((int) 'd', (int) 'z'),
                                                       Arrays.asList((int) 'e', (int) 'z'),
                                                       Arrays.asList((int) 'f', (int) 'z'));
        assertThat(itf.addL20().pass(Arrays.asList('c', 'z')).result().all()).containsOnly(
                Arrays.asList((int) 'c', (int) 'z'));
        assertThat(itf.addL21()
                      .pass(Arrays.asList('d', 'z'), Arrays.asList('e', 'z'),
                            Arrays.asList('f', 'z'))
                      .result()
                      .all()).containsOnly(Arrays.asList((int) 'd', (int) 'z'),
                                           Arrays.asList((int) 'e', (int) 'z'),
                                           Arrays.asList((int) 'f', (int) 'z'));
        assertThat(itf.addL22().pass(Arrays.asList('c', 'z')).close().all()).containsOnly(
                Arrays.asList((int) 'c', (int) 'z'));
        assertThat(itf.addL23()
                      .pass(Arrays.asList('d', 'z'), Arrays.asList('e', 'z'),
                            Arrays.asList('f', 'z'))
                      .close()
                      .all()).containsOnly(Arrays.asList((int) 'd', (int) 'z'),
                                           Arrays.asList((int) 'e', (int) 'z'),
                                           Arrays.asList((int) 'f', (int) 'z'));
        assertThat(itf.addL24().asyncCall(Arrays.asList('c', 'z')).all()).containsOnly(
                Arrays.asList((int) 'c', (int) 'z'));
        assertThat(itf.addL25()
                      .parallelCall(Arrays.asList('d', 'z'), Arrays.asList('e', 'z'),
                                    Arrays.asList('f', 'z'))
                      .all()).containsOnly(Arrays.asList((int) 'd', (int) 'z'),
                                           Arrays.asList((int) 'e', (int) 'z'),
                                           Arrays.asList((int) 'f', (int) 'z'));
        assertThat(itf.get0()).isEqualTo(31);
        assertThat(itf.get1().all()).containsExactly(31);
        assertThat(itf.get2().result().all()).containsExactly(31);
        assertThat(itf.get3().close().all()).containsExactly(31);
        assertThat(itf.get4().asyncCall().all()).containsExactly(31);
        assertThat(itf.getA0()).isEqualTo(new int[]{1, 2, 3});
        assertThat(itf.getA1().all()).containsExactly(1, 2, 3);
        assertThat(itf.getA2()).containsExactly(new int[]{1, 2, 3});
        assertThat(itf.getA3()).containsExactly(new int[]{1, 2, 3});
        assertThat(itf.getA4().result().all()).containsExactly(new int[]{1, 2, 3});
        assertThat(itf.getA5().close().all()).containsExactly(new int[]{1, 2, 3});
        assertThat(itf.getA6().asyncCall().all()).containsExactly(new int[]{1, 2, 3});
        assertThat(itf.getL0()).isEqualTo(Arrays.asList(1, 2, 3));
        assertThat(itf.getL1().all()).containsExactly(1, 2, 3);
        assertThat(itf.getL2()).containsExactly(Arrays.asList(1, 2, 3));
        assertThat(itf.getL3()).containsExactly(Arrays.asList(1, 2, 3));
        assertThat(itf.getL4().result().all()).containsExactly(Arrays.asList(1, 2, 3));
        assertThat(itf.getL5().close().all()).containsExactly(Arrays.asList(1, 2, 3));
        assertThat(itf.getL6().asyncCall().all()).containsExactly(Arrays.asList(1, 2, 3));
        itf.set0(-17);
        final IOChannel<Integer, Integer> channel35 = JRoutine.io().buildChannel();
        channel35.pass(-17).close();
        itf.set1(channel35);
        final IOChannel<Integer, Integer> channel36 = JRoutine.io().buildChannel();
        channel36.pass(-17).close();
        itf.set2(channel36);
        itf.set3().pass(-17).result().checkComplete();
        itf.set4().pass(-17).close().checkComplete();
        itf.set5().asyncCall(-17).checkComplete();
        itf.setA0(new int[]{1, 2, 3});
        final IOChannel<int[], int[]> channel37 = JRoutine.io().buildChannel();
        channel37.pass(new int[]{1, 2, 3}).close();
        itf.setA1(channel37);
        final IOChannel<Integer, Integer> channel38 = JRoutine.io().buildChannel();
        channel38.pass(1, 2, 3).close();
        itf.setA2(channel38);
        final IOChannel<int[], int[]> channel39 = JRoutine.io().buildChannel();
        channel39.pass(new int[]{1, 2, 3}).close();
        itf.setA3(channel39);
        itf.setA4().pass(new int[]{1, 2, 3}).result().checkComplete();
        itf.setA5().pass(new int[]{1, 2, 3}).close().checkComplete();
        itf.setA6().asyncCall(new int[]{1, 2, 3}).checkComplete();
        itf.setL0(Arrays.asList(1, 2, 3));
        final IOChannel<List<Integer>, List<Integer>> channel40 = JRoutine.io().buildChannel();
        channel40.pass(Arrays.asList(1, 2, 3)).close();
        itf.setL1(channel40);
        final IOChannel<Integer, Integer> channel41 = JRoutine.io().buildChannel();
        channel41.pass(1, 2, 3).close();
        itf.setL2(channel41);
        final IOChannel<List<Integer>, List<Integer>> channel42 = JRoutine.io().buildChannel();
        channel42.pass(Arrays.asList(1, 2, 3)).close();
        itf.setL3(channel42);
        itf.setL4().pass(Arrays.asList(1, 2, 3)).result().checkComplete();
        itf.setL5().pass(Arrays.asList(1, 2, 3)).close().checkComplete();
        itf.setL6().asyncCall(Arrays.asList(1, 2, 3)).checkComplete();
    }

    @SuppressWarnings("NullArgumentToVariableArgMethod")
    public void testProxyRoutine() {

        final TimeDuration timeout = seconds(10);
        final SquareItf squareAsync = JRoutine.with(contextFrom(getActivity()))
                                              .on(instanceOf(Square.class))
                                              .buildProxy(SquareItf.class);

        assertThat(squareAsync.compute(3)).isEqualTo(9);
        assertThat(squareAsync.compute1(3)).containsExactly(9);
        assertThat(squareAsync.compute2(3)).containsExactly(9);
        assertThat(squareAsync.computeParallel1(1, 2, 3).afterMax(timeout).all()).containsOnly(1, 4,
                                                                                               9);
        assertThat(squareAsync.computeParallel2(1, 2, 3).afterMax(timeout).all()).containsOnly(1, 4,
                                                                                               9);
        assertThat(squareAsync.computeParallel3(Arrays.asList(1, 2, 3))
                              .afterMax(timeout)
                              .all()).containsOnly(1, 4, 9);

        final IOChannel<Integer, Integer> channel1 = JRoutine.io().buildChannel();
        channel1.pass(4).close();
        assertThat(squareAsync.computeAsync(channel1)).isEqualTo(16);

        final IOChannel<Integer, Integer> channel2 = JRoutine.io().buildChannel();
        channel2.pass(1, 2, 3).close();
        assertThat(squareAsync.computeParallel4(channel2).afterMax(timeout).all()).containsOnly(1,
                                                                                                4,
                                                                                                9);

        final IncItf incItf = JRoutine.with(contextFrom(getActivity()))
                                      .on(instanceOf(Inc.class))
                                      .buildProxy(ClassToken.tokenOf(IncItf.class));
        assertThat(incItf.inc(1, 2, 3, 4)).containsOnly(2, 3, 4, 5);
        assertThat(incItf.incIterable(1, 2, 3, 4)).containsOnly(2, 3, 4, 5);
    }

    public void testShareGroup() throws NoSuchMethodException {

        final LoaderObjectRoutineBuilder builder = JRoutine.with(contextFrom(getActivity()))
                                                           .on(instanceOf(TestClass2.class))
                                                           .invocations()
                                                           .withExecutionTimeout(seconds(10))
                                                           .set();

        long startTime = System.currentTimeMillis();

        OutputChannel<Object> getOne =
                builder.proxies().withShareGroup("1").set().method("getOne").asyncCall();
        OutputChannel<Object> getTwo =
                builder.proxies().withShareGroup("2").set().method("getTwo").asyncCall();

        assertThat(getOne.checkComplete()).isTrue();
        assertThat(getTwo.checkComplete()).isTrue();
        assertThat(System.currentTimeMillis() - startTime).isLessThan(2000);

        startTime = System.currentTimeMillis();

        getOne = builder.method("getOne").asyncCall();
        getTwo = builder.method("getTwo").asyncCall();

        assertThat(getOne.checkComplete()).isTrue();
        assertThat(getTwo.checkComplete()).isTrue();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(2000);
    }

    public void testTimeoutActionAnnotation() throws NoSuchMethodException {

        assertThat(JRoutine.with(contextFrom(getActivity()))
                           .on(instanceOf(TestTimeout.class))
                           .invocations()
                           .withExecutionTimeout(seconds(10))
                           .set()
                           .loaders()
                           .withId(0)
                           .set()
                           .aliasMethod("test")
                           .asyncCall()
                           .next()).isEqualTo(31);

        try {

            JRoutine.with(contextFrom(getActivity()))
                    .on(instanceOf(TestTimeout.class))
                    .invocations()
                    .withExecutionTimeoutAction(TimeoutActionType.THROW)
                    .set()
                    .loaders()
                    .withId(1)
                    .set()
                    .aliasMethod("test")
                    .asyncCall()
                    .next();

            fail();

        } catch (final NoSuchElementException ignored) {

        }

        assertThat(JRoutine.with(contextFrom(getActivity()))
                           .on(instanceOf(TestTimeout.class))
                           .invocations()
                           .withExecutionTimeout(seconds(10))
                           .set()
                           .loaders()
                           .withId(2)
                           .set()
                           .method("getInt")
                           .asyncCall()
                           .next()).isEqualTo(31);

        try {

            JRoutine.with(contextFrom(getActivity()))
                    .on(instanceOf(TestTimeout.class))
                    .invocations()
                    .withExecutionTimeoutAction(TimeoutActionType.THROW)
                    .set()
                    .loaders()
                    .withId(3)
                    .set()
                    .method("getInt")
                    .asyncCall()
                    .next();

            fail();

        } catch (final NoSuchElementException ignored) {

        }

        assertThat(JRoutine.with(contextFrom(getActivity()))
                           .on(instanceOf(TestTimeout.class))
                           .invocations()
                           .withExecutionTimeout(seconds(10))
                           .set()
                           .loaders()
                           .withId(4)
                           .set()
                           .method(TestTimeout.class.getMethod("getInt"))
                           .asyncCall()
                           .next()).isEqualTo(31);

        try {

            JRoutine.with(contextFrom(getActivity()))
                    .on(instanceOf(TestTimeout.class))
                    .invocations()
                    .withExecutionTimeoutAction(TimeoutActionType.THROW)
                    .set()
                    .loaders()
                    .withId(5)
                    .set()
                    .method(TestTimeout.class.getMethod("getInt"))
                    .asyncCall()
                    .next();

            fail();

        } catch (final NoSuchElementException ignored) {

        }

        assertThat(JRoutine.with(contextFrom(getActivity()))
                           .on(instanceOf(TestTimeout.class))
                           .invocations()
                           .withExecutionTimeout(seconds(10))
                           .set()
                           .loaders()
                           .withId(6)
                           .set()
                           .buildProxy(TestTimeoutItf.class)
                           .getInt()).containsExactly(31);

        try {

            JRoutine.with(contextFrom(getActivity()))
                    .on(instanceOf(TestTimeout.class))
                    .invocations()
                    .withExecutionTimeoutAction(TimeoutActionType.THROW)
                    .set()
                    .loaders()
                    .withId(7)
                    .set()
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
        int add1(@Input(value = char.class, mode = InputMode.CHANNEL) OutputChannel<Character> c);

        @Alias("a")
        @Inputs(char.class)
        Routine<Character, Integer> add10();

        @Alias("a")
        @Invoke(InvocationMode.PARALLEL)
        @Inputs(char.class)
        Routine<Character, Integer> add11();

        @Alias("a")
        @Invoke(InvocationMode.PARALLEL)
        int add2(@Input(value = char.class, mode = InputMode.ELEMENT) OutputChannel<Character> c);

        @Alias("a")
        @Output(OutputMode.CHANNEL)
        OutputChannel<Integer> add3(char c);

        @Alias("a")
        @Output(OutputMode.CHANNEL)
        OutputChannel<Integer> add4(
                @Input(value = char.class, mode = InputMode.CHANNEL) OutputChannel<Character> c);

        @Alias("a")
        @Invoke(InvocationMode.PARALLEL)
        @Output(OutputMode.CHANNEL)
        OutputChannel<Integer> add5(
                @Input(value = char.class, mode = InputMode.ELEMENT) OutputChannel<Character> c);

        @Alias("a")
        @Inputs(char.class)
        InvocationChannel<Character, Integer> add6();

        @Alias("a")
        @Invoke(InvocationMode.PARALLEL)
        @Inputs(char.class)
        InvocationChannel<Character, Integer> add7();

        @Alias("a")
        @Inputs(char.class)
        StreamingChannel<Character, Integer> add8();

        @Alias("a")
        @Invoke(InvocationMode.PARALLEL)
        @Inputs(char.class)
        StreamingChannel<Character, Integer> add9();

        @Alias("aa")
        int[] addA00(char[] c);

        @Alias("aa")
        int[] addA01(@Input(value = char[].class,
                mode = InputMode.CHANNEL) OutputChannel<char[]> c);

        @Alias("aa")
        int[] addA02(@Input(value = char[].class,
                mode = InputMode.COLLECTION) OutputChannel<Character> c);

        @Alias("aa")
        @Invoke(InvocationMode.PARALLEL)
        int[] addA03(@Input(value = char[].class,
                mode = InputMode.ELEMENT) OutputChannel<char[]> c);

        @Alias("aa")
        @Output(OutputMode.CHANNEL)
        OutputChannel<int[]> addA04(char[] c);

        @Alias("aa")
        @Output(OutputMode.CHANNEL)
        OutputChannel<int[]> addA05(
                @Input(value = char[].class, mode = InputMode.CHANNEL) OutputChannel<char[]> c);

        @Alias("aa")
        @Output(OutputMode.CHANNEL)
        OutputChannel<int[]> addA06(@Input(value = char[].class,
                mode = InputMode.COLLECTION) OutputChannel<Character> c);

        @Alias("aa")
        @Invoke(InvocationMode.PARALLEL)
        @Output(OutputMode.CHANNEL)
        OutputChannel<int[]> addA07(@Input(value = char[].class,
                mode = InputMode.ELEMENT) OutputChannel<char[]> c);

        @Alias("aa")
        @Output(OutputMode.ELEMENT)
        OutputChannel<Integer> addA08(char[] c);

        @Alias("aa")
        @Output(OutputMode.ELEMENT)
        OutputChannel<Integer> addA09(
                @Input(value = char[].class, mode = InputMode.CHANNEL) OutputChannel<char[]> c);

        @Alias("aa")
        @Output(OutputMode.ELEMENT)
        OutputChannel<Integer> addA10(@Input(value = char[].class,
                mode = InputMode.COLLECTION) OutputChannel<Character> c);

        @Alias("aa")
        @Invoke(InvocationMode.PARALLEL)
        @Output(OutputMode.ELEMENT)
        OutputChannel<Integer> addA11(@Input(value = char[].class,
                mode = InputMode.ELEMENT) OutputChannel<char[]> c);

        @Alias("aa")
        @Output(OutputMode.COLLECTION)
        List<int[]> addA12(char[] c);

        @Alias("aa")
        @Output(OutputMode.COLLECTION)
        List<int[]> addA13(
                @Input(value = char[].class, mode = InputMode.CHANNEL) OutputChannel<char[]> c);

        @Alias("aa")
        @Output(OutputMode.COLLECTION)
        List<int[]> addA14(@Input(value = char[].class,
                mode = InputMode.COLLECTION) OutputChannel<Character> c);

        @Alias("aa")
        @Invoke(InvocationMode.PARALLEL)
        @Output(OutputMode.COLLECTION)
        List<int[]> addA15(@Input(value = char[].class,
                mode = InputMode.ELEMENT) OutputChannel<char[]> c);

        @Alias("aa")
        @Output(OutputMode.COLLECTION)
        int[][] addA16(char[] c);

        @Alias("aa")
        @Output(OutputMode.COLLECTION)
        int[][] addA17(
                @Input(value = char[].class, mode = InputMode.CHANNEL) OutputChannel<char[]> c);

        @Alias("aa")
        @Output(OutputMode.COLLECTION)
        int[][] addA18(@Input(value = char[].class,
                mode = InputMode.COLLECTION) OutputChannel<Character> c);

        @Alias("aa")
        @Invoke(InvocationMode.PARALLEL)
        @Output(OutputMode.COLLECTION)
        int[][] addA19(@Input(value = char[].class,
                mode = InputMode.ELEMENT) OutputChannel<char[]> c);

        @Alias("aa")
        @Inputs(char[].class)
        InvocationChannel<char[], int[]> addA20();

        @Alias("aa")
        @Invoke(InvocationMode.PARALLEL)
        @Inputs(char[].class)
        InvocationChannel<char[], int[]> addA21();

        @Alias("aa")
        @Inputs(char[].class)
        StreamingChannel<char[], int[]> addA22();

        @Alias("aa")
        @Invoke(InvocationMode.PARALLEL)
        @Inputs(char[].class)
        StreamingChannel<char[], int[]> addA23();

        @Alias("aa")
        @Inputs(char[].class)
        Routine<char[], int[]> addA24();

        @Alias("aa")
        @Invoke(InvocationMode.PARALLEL)
        @Inputs(char[].class)
        Routine<char[], int[]> addA25();

        @Alias("al")
        List<Integer> addL00(List<Character> c);

        @Alias("al")
        List<Integer> addL01(@Input(value = List.class,
                mode = InputMode.CHANNEL) OutputChannel<List<Character>> c);

        @Alias("al")
        List<Integer> addL02(@Input(value = List.class,
                mode = InputMode.COLLECTION) OutputChannel<Character> c);

        @Alias("al")
        @Invoke(InvocationMode.PARALLEL)
        List<Integer> addL03(@Input(value = List.class,
                mode = InputMode.ELEMENT) OutputChannel<List<Character>> c);

        @Alias("al")
        @Output(OutputMode.CHANNEL)
        OutputChannel<List<Integer>> addL04(List<Character> c);

        @Alias("al")
        @Output(OutputMode.CHANNEL)
        OutputChannel<List<Integer>> addL05(@Input(value = List.class,
                mode = InputMode.CHANNEL) OutputChannel<List<Character>> c);

        @Alias("al")
        @Output(OutputMode.CHANNEL)
        OutputChannel<List<Integer>> addL06(@Input(value = List.class,
                mode = InputMode.COLLECTION) OutputChannel<Character> c);

        @Alias("al")
        @Invoke(InvocationMode.PARALLEL)
        @Output(OutputMode.CHANNEL)
        OutputChannel<List<Integer>> addL07(@Input(value = List.class,
                mode = InputMode.ELEMENT) OutputChannel<List<Character>> c);

        @Alias("al")
        @Output(OutputMode.ELEMENT)
        OutputChannel<Integer> addL08(List<Character> c);

        @Alias("al")
        @Output(OutputMode.ELEMENT)
        OutputChannel<Integer> addL09(@Input(value = List.class,
                mode = InputMode.CHANNEL) OutputChannel<List<Character>> c);

        @Alias("al")
        @Output(OutputMode.ELEMENT)
        OutputChannel<Integer> addL10(@Input(value = List.class,
                mode = InputMode.COLLECTION) OutputChannel<Character> c);

        @Alias("al")
        @Invoke(InvocationMode.PARALLEL)
        @Output(OutputMode.ELEMENT)
        OutputChannel<Integer> addL11(@Input(value = List.class,
                mode = InputMode.ELEMENT) OutputChannel<List<Character>> c);

        @Alias("al")
        @Output(OutputMode.COLLECTION)
        List<List<Integer>> addL12(List<Character> c);

        @Alias("al")
        @Output(OutputMode.COLLECTION)
        List<List<Integer>> addL13(@Input(value = List.class,
                mode = InputMode.CHANNEL) OutputChannel<List<Character>> c);

        @Alias("al")
        @Output(OutputMode.COLLECTION)
        List<List<Integer>> addL14(@Input(value = List.class,
                mode = InputMode.COLLECTION) OutputChannel<Character> c);

        @Alias("al")
        @Invoke(InvocationMode.PARALLEL)
        @Output(OutputMode.COLLECTION)
        List<List<Integer>> addL15(@Input(value = List.class,
                mode = InputMode.ELEMENT) OutputChannel<List<Character>> c);

        @Alias("al")
        @Output(OutputMode.COLLECTION)
        List[] addL16(List<Character> c);

        @Alias("al")
        @Output(OutputMode.COLLECTION)
        List[] addL17(@Input(value = List.class,
                mode = InputMode.CHANNEL) OutputChannel<List<Character>> c);

        @Alias("al")
        @Output(OutputMode.COLLECTION)
        List[] addL18(@Input(value = List.class,
                mode = InputMode.COLLECTION) OutputChannel<Character> c);

        @Alias("al")
        @Invoke(InvocationMode.PARALLEL)
        @Output(OutputMode.COLLECTION)
        List[] addL19(@Input(value = List.class,
                mode = InputMode.ELEMENT) OutputChannel<List<Character>> c);

        @Alias("al")
        @Inputs(List.class)
        InvocationChannel<List<Character>, List<Integer>> addL20();

        @Alias("al")
        @Invoke(InvocationMode.PARALLEL)
        @Inputs(List.class)
        InvocationChannel<List<Character>, List<Integer>> addL21();

        @Alias("al")
        @Inputs(List.class)
        StreamingChannel<List<Character>, List<Integer>> addL22();

        @Alias("al")
        @Invoke(InvocationMode.PARALLEL)
        @Inputs(List.class)
        StreamingChannel<List<Character>, List<Integer>> addL23();

        @Alias("al")
        @Inputs(List.class)
        Routine<List<Character>, List<Integer>> addL24();

        @Alias("al")
        @Invoke(InvocationMode.PARALLEL)
        @Inputs(List.class)
        Routine<List<Character>, List<Integer>> addL25();

        @Alias("g")
        int get0();

        @Alias("s")
        void set0(int i);

        @Alias("g")
        @Output(OutputMode.CHANNEL)
        OutputChannel<Integer> get1();

        @Alias("s")
        void set1(@Input(value = int.class, mode = InputMode.CHANNEL) OutputChannel<Integer> i);

        @Alias("g")
        @Inputs({})
        InvocationChannel<Void, Integer> get2();

        @Alias("s")
        @Invoke(InvocationMode.PARALLEL)
        void set2(@Input(value = int.class, mode = InputMode.ELEMENT) OutputChannel<Integer> i);

        @Alias("g")
        @Inputs({})
        StreamingChannel<Void, Integer> get3();

        @Alias("g")
        @Inputs({})
        Routine<Void, Integer> get4();

        @Alias("ga")
        int[] getA0();

        @Alias("sa")
        void setA0(int[] i);

        @Alias("ga")
        @Output(OutputMode.ELEMENT)
        OutputChannel<Integer> getA1();

        @Alias("sa")
        void setA1(@Input(value = int[].class, mode = InputMode.CHANNEL) OutputChannel<int[]> i);

        @Alias("ga")
        @Output(OutputMode.COLLECTION)
        List<int[]> getA2();

        @Alias("sa")
        void setA2(
                @Input(value = int[].class, mode = InputMode.COLLECTION) OutputChannel<Integer> i);

        @Alias("ga")
        @Output(OutputMode.COLLECTION)
        int[][] getA3();

        @Alias("sa")
        @Invoke(InvocationMode.PARALLEL)
        void setA3(@Input(value = int[].class, mode = InputMode.ELEMENT) OutputChannel<int[]> i);

        @Alias("ga")
        @Inputs({})
        InvocationChannel<Void, int[]> getA4();

        @Alias("ga")
        @Inputs({})
        StreamingChannel<Void, int[]> getA5();

        @Alias("ga")
        @Inputs({})
        Routine<Void, int[]> getA6();

        @Alias("gl")
        List<Integer> getL0();

        @Alias("sl")
        void setL0(List<Integer> i);

        @Alias("gl")
        @Output(OutputMode.ELEMENT)
        OutputChannel<Integer> getL1();

        @Alias("sl")
        void setL1(@Input(value = List.class,
                mode = InputMode.CHANNEL) OutputChannel<List<Integer>> i);

        @Alias("gl")
        @Output(OutputMode.COLLECTION)
        List<List<Integer>> getL2();

        @Alias("sl")
        void setL2(
                @Input(value = List.class, mode = InputMode.COLLECTION) OutputChannel<Integer> i);

        @Alias("gl")
        @Output(OutputMode.COLLECTION)
        List[] getL3();

        @Alias("sl")
        @Invoke(InvocationMode.PARALLEL)
        void setL3(@Input(value = List.class,
                mode = InputMode.ELEMENT) OutputChannel<List<Integer>> i);

        @Alias("gl")
        @Inputs({})
        InvocationChannel<Void, List<Integer>> getL4();

        @Alias("gl")
        @Inputs({})
        StreamingChannel<Void, List<Integer>> getL5();

        @Alias("gl")
        @Inputs({})
        Routine<Void, List<Integer>> getL6();

        @Alias("s")
        @Inputs(int.class)
        InvocationChannel<Integer, Void> set3();

        @Alias("s")
        @Inputs(int.class)
        StreamingChannel<Integer, Void> set4();

        @Alias("s")
        @Inputs(int.class)
        Routine<Integer, Void> set5();

        @Alias("sa")
        @Inputs(int[].class)
        InvocationChannel<int[], Void> setA4();

        @Alias("sa")
        @Inputs(int[].class)
        StreamingChannel<int[], Void> setA5();

        @Alias("sa")
        @Inputs(int[].class)
        Routine<int[], Void> setA6();

        @Alias("sl")
        @Inputs(List.class)
        InvocationChannel<List<Integer>, Void> setL4();

        @Alias("sl")
        @Inputs(List.class)
        StreamingChannel<List<Integer>, Void> setL5();

        @Alias("sl")
        @Inputs(List.class)
        Routine<List<Integer>, Void> setL6();
    }

    private interface CountError {

        @Output
        String[] count(int length);

        @Alias("count")
        @Output(OutputMode.COLLECTION)
        OutputChannel<Integer> count1(int length);

        @Alias("count")
        @Output(OutputMode.ELEMENT)
        String[] count2(int length);

        @Output(OutputMode.CHANNEL)
        List<Integer> countList(int length);

        @Alias("countList")
        @Output(OutputMode.ELEMENT)
        List<Integer> countList1(int length);

        @Alias("countList")
        @Output(OutputMode.COLLECTION)
        OutputChannel<Integer> countList2(int length);
    }

    private interface CountItf {

        @Output(OutputMode.ELEMENT)
        OutputChannel<Integer> count(int length);

        @Alias("count")
        @Output(OutputMode.CHANNEL)
        OutputChannel<int[]> count1(int length);

        @Alias("count")
        @Output(OutputMode.ELEMENT)
        OutputChannel<Integer> count2(int length);

        @Output(OutputMode.ELEMENT)
        OutputChannel<Integer> countList(int length);

        @Alias("countList")
        @Output(OutputMode.ELEMENT)
        OutputChannel<Integer> countList1(int length);
    }

    private interface IncItf {

        @Timeout(10000)
        @Invoke(InvocationMode.PARALLEL)
        @Output(OutputMode.COLLECTION)
        int[] inc(@Input(value = int.class, mode = InputMode.ELEMENT) int... i);

        @Timeout(10000)
        @Alias("inc")
        @Invoke(InvocationMode.PARALLEL)
        @Output
        Iterable<Integer> incIterable(@Input(value = int.class, mode = InputMode.ELEMENT) int... i);
    }

    private interface SquareItf {

        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        int compute(int i);

        @Alias("compute")
        @Output(OutputMode.COLLECTION)
        @Timeout(10000)
        int[] compute1(int length);

        @Alias("compute")
        @Output(OutputMode.COLLECTION)
        @Timeout(10000)
        List<Integer> compute2(int length);

        @Alias("compute")
        @Timeout(10000)
        int computeAsync(@Input(int.class) OutputChannel<Integer> i);

        @ShareGroup(ShareGroup.NONE)
        @Alias("compute")
        @Invoke(InvocationMode.PARALLEL)
        @Output
        OutputChannel<Integer> computeParallel1(
                @Input(value = int.class, mode = InputMode.ELEMENT) int... i);

        @Alias("compute")
        @Invoke(InvocationMode.PARALLEL)
        @Output
        OutputChannel<Integer> computeParallel2(
                @Input(value = int.class, mode = InputMode.ELEMENT) Integer... i);

        @ShareGroup(ShareGroup.NONE)
        @Alias("compute")
        @Invoke(InvocationMode.PARALLEL)
        @Output
        OutputChannel<Integer> computeParallel3(
                @Input(value = int.class, mode = InputMode.ELEMENT) List<Integer> i);

        @ShareGroup(ShareGroup.NONE)
        @Alias("compute")
        @Invoke(InvocationMode.PARALLEL)
        @Output
        OutputChannel<Integer> computeParallel4(
                @Input(value = int.class, mode = InputMode.ELEMENT) OutputChannel<Integer> i);
    }

    private interface SumError {

        int compute(int a, @Input(int.class) int[] b);

        int compute(@Input(int.class) String[] ints);

        int compute(@Input(value = int.class, mode = InputMode.CHANNEL) int[] ints);

        int compute(@Input(value = int.class, mode = InputMode.COLLECTION) Iterable<Integer> ints);

        int compute(@Input(value = int.class,
                mode = InputMode.COLLECTION) OutputChannel<Integer> ints);

        int compute(int a,
                @Input(value = int[].class, mode = InputMode.COLLECTION) OutputChannel<Integer> b);

        @Invoke(InvocationMode.PARALLEL)
        int compute(@Input(value = int.class, mode = InputMode.ELEMENT) Object ints);

        @Invoke(InvocationMode.PARALLEL)
        int compute(@Input(value = int.class, mode = InputMode.ELEMENT) Object[] ints);

        @Invoke(InvocationMode.PARALLEL)
        int compute(String text, @Input(value = int.class, mode = InputMode.ELEMENT) int[] ints);
    }

    private interface SumItf {

        int compute(int a, @Input(int.class) OutputChannel<Integer> b);

        int compute(@Input(value = int[].class,
                mode = InputMode.COLLECTION) OutputChannel<Integer> ints);

        @Alias("compute")
        int compute1(@Input(int[].class) OutputChannel<int[]> ints);

        @Alias("compute")
        int computeList(@Input(value = List.class,
                mode = InputMode.COLLECTION) OutputChannel<Integer> ints);

        @Alias("compute")
        int computeList1(@Input(value = List.class,
                mode = InputMode.COLLECTION) OutputChannel<Integer> ints);
    }

    @SuppressWarnings("unused")
    private interface TestItf {

        void throwException(@Input(int.class) RuntimeException ex);

        @Alias(TestClass.THROW)
        @Output
        void throwException1(RuntimeException ex);

        @Alias(TestClass.THROW)
        int throwException2(RuntimeException ex);
    }

    private interface TestTimeoutItf {

        @Output(OutputMode.COLLECTION)
        @TimeoutAction(TimeoutActionType.ABORT)
        List<Integer> getInt();
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

    private static class StringContext extends FactoryContextWrapper {

        /**
         * Constructor.
         *
         * @param base the base context.
         */
        public StringContext(@NotNull final Context base) {

            super(base);
        }

        @Nullable
        public <TYPE> TYPE geInstance(@NotNull final Class<? extends TYPE> type,
                @NotNull final Object... args) {

            return type.cast("test1");
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

            TimeDuration.millis(1000).sleepAtLeast();

            return 1;
        }

        public int getTwo() throws InterruptedException {

            TimeDuration.millis(1000).sleepAtLeast();

            return 2;
        }
    }

    private static class TestTimeout {

        @Alias("test")
        @TimeoutAction(TimeoutActionType.EXIT)
        public int getInt() throws InterruptedException {

            Thread.sleep(100);
            return 31;
        }
    }
}
