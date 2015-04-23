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
package com.gh.bmd.jrt.android.v11.core;

import android.annotation.TargetApi;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;

import com.gh.bmd.jrt.annotation.Bind;
import com.gh.bmd.jrt.annotation.Pass;
import com.gh.bmd.jrt.annotation.Pass.PassMode;
import com.gh.bmd.jrt.annotation.ShareGroup;
import com.gh.bmd.jrt.annotation.Timeout;
import com.gh.bmd.jrt.annotation.TimeoutAction;
import com.gh.bmd.jrt.builder.ObjectRoutineBuilder;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.builder.RoutineConfiguration.OrderType;
import com.gh.bmd.jrt.builder.RoutineConfiguration.TimeoutActionType;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.StandaloneChannel;
import com.gh.bmd.jrt.common.AbortException;
import com.gh.bmd.jrt.common.ClassToken;
import com.gh.bmd.jrt.common.InvocationException;
import com.gh.bmd.jrt.log.Log;
import com.gh.bmd.jrt.log.Log.LogLevel;
import com.gh.bmd.jrt.log.NullLog;
import com.gh.bmd.jrt.routine.Routine;
import com.gh.bmd.jrt.runner.Runners;
import com.gh.bmd.jrt.time.TimeDuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.gh.bmd.jrt.android.builder.ContextInvocationConfiguration.withId;
import static com.gh.bmd.jrt.builder.ProxyConfiguration.withShareGroup;
import static com.gh.bmd.jrt.builder.RoutineConfiguration.builder;
import static com.gh.bmd.jrt.builder.RoutineConfiguration.onReadTimeout;
import static com.gh.bmd.jrt.builder.RoutineConfiguration.withFactoryArgs;
import static com.gh.bmd.jrt.builder.RoutineConfiguration.withReadTimeout;
import static com.gh.bmd.jrt.time.TimeDuration.INFINITY;
import static com.gh.bmd.jrt.time.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Object context routine activity unit tests.
 * <p/>
 * Created by davide on 4/7/15.
 */
@TargetApi(VERSION_CODES.HONEYCOMB)
public class ObjectContextRoutineBuilderActivityTest
        extends ActivityInstrumentationTestCase2<TestActivity> {

    public ObjectContextRoutineBuilderActivityTest() {

        super(TestActivity.class);
    }

    public void testArgs() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        assertThat(JRoutine.onActivity(getActivity(), TestArgs.class)
                           .configure(withFactoryArgs(17))
                           .method("getId")
                           .callAsync()
                           .eventually()
                           .readNext()).isEqualTo(17);
    }

    public void testAsyncInputProxyRoutine() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = seconds(10);
        final SumItf sumAsync = JRoutine.onActivity(getActivity(), Sum.class)
                                        .configure(withReadTimeout(timeout))
                                        .buildProxy(SumItf.class);
        final StandaloneChannel<Integer> channel3 = JRoutine.standalone().buildChannel();
        channel3.input().pass(7).close();
        assertThat(sumAsync.compute(3, channel3.output())).isEqualTo(10);

        final StandaloneChannel<Integer> channel4 = JRoutine.standalone().buildChannel();
        channel4.input().pass(1, 2, 3, 4).close();
        assertThat(sumAsync.compute(channel4.output())).isEqualTo(10);

        final StandaloneChannel<int[]> channel5 = JRoutine.standalone().buildChannel();
        channel5.input().pass(new int[]{1, 2, 3, 4}).close();
        assertThat(sumAsync.compute1(channel5.output())).isEqualTo(10);

        final StandaloneChannel<Integer> channel6 = JRoutine.standalone().buildChannel();
        channel6.input().pass(1, 2, 3, 4).close();
        assertThat(sumAsync.computeList(channel6.output())).isEqualTo(10);

        final StandaloneChannel<Integer> channel7 = JRoutine.standalone().buildChannel();
        channel7.input().pass(1, 2, 3, 4).close();
        assertThat(sumAsync.computeList1(channel7.output())).isEqualTo(10);
    }

    public void testAsyncOutputProxyRoutine() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = seconds(10);
        final CountItf countAsync = JRoutine.onActivity(getActivity(), Count.class)
                                            .configure(withReadTimeout(timeout))
                                            .buildProxy(CountItf.class);
        assertThat(countAsync.count(3).readAll()).containsExactly(0, 1, 2);
        assertThat(countAsync.count1(3).readAll()).containsExactly(new int[]{0, 1, 2});
        assertThat(countAsync.count2(2).readAll()).containsExactly(0, 1);
        assertThat(countAsync.countList(3).readAll()).containsExactly(0, 1, 2);
        assertThat(countAsync.countList1(3).readAll()).containsExactly(0, 1, 2);
    }

    public void testBoundMethod() throws NoSuchMethodException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = seconds(10);
        final RoutineConfiguration configuration =
                builder().withSyncRunner(Runners.sequentialRunner())
                         .withAsyncRunner(Runners.poolRunner())
                         .withMaxInvocations(1)
                         .withCoreInvocations(1)
                         .withAvailableTimeout(1, TimeUnit.SECONDS)
                         .onReadTimeout(TimeoutActionType.EXIT)
                         .withLogLevel(LogLevel.DEBUG)
                         .withLog(new NullLog())
                         .buildConfiguration();
        final Routine<Object, Object> routine = JRoutine.onActivity(getActivity(), TestClass.class)
                                                        .configure(configuration)
                                                        .boundMethod(TestClass.GET);

        assertThat(routine.callSync().afterMax(timeout).readAll()).containsExactly(-77L);
    }

    public void testConfigurationWarnings() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final CountLog countLog = new CountLog();
        final RoutineConfiguration configuration = builder().withFactoryArgs()
                                                            .withInputOrder(OrderType.NONE)
                                                            .withInputSize(3)
                                                            .withInputTimeout(seconds(10))
                                                            .withOutputOrder(OrderType.NONE)
                                                            .withOutputSize(3)
                                                            .withOutputTimeout(seconds(10))
                                                            .withLogLevel(LogLevel.DEBUG)
                                                            .withLog(countLog)
                                                            .buildConfiguration();
        JRoutine.onActivity(getActivity(), TestClass.class)
                .configure(configuration)
                .members(withShareGroup("test"))
                .boundMethod(TestClass.GET);
        assertThat(countLog.getWrnCount()).isEqualTo(7);

        JRoutine.onActivity(getActivity(), Square.class)
                .configure(configuration)
                .members(withShareGroup("test"))
                .buildProxy(SquareItf.class)
                .compute(3);
        assertThat(countLog.getWrnCount()).isEqualTo(14);
    }

    public void testDuplicateAnnotationError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        try {

            JRoutine.onActivity(getActivity(), DuplicateAnnotation.class);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testException() throws NoSuchMethodException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = seconds(10);
        final Routine<Object, Object> routine3 =
                JRoutine.onActivity(getActivity(), TestClass.class).boundMethod(TestClass.THROW);

        try {

            routine3.callSync(new IllegalArgumentException("test")).afterMax(timeout).readAll();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
            assertThat(e.getCause().getMessage()).isEqualTo("test");
        }
    }

    public void testInvalidProxyError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        try {

            JRoutine.onActivity(getActivity(), TestClass.class).buildProxy(TestClass.class);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.onActivity(getActivity(), TestClass.class)
                    .buildProxy(ClassToken.tokenOf(TestClass.class));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testInvalidProxyInputAnnotationError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        try {

            JRoutine.onActivity(getActivity(), Sum.class)
                    .buildProxy(SumError.class)
                    .compute(1, new int[0]);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.onActivity(getActivity(), Sum.class)
                    .buildProxy(SumError.class)
                    .compute(new String[0]);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.onActivity(getActivity(), Sum.class)
                    .buildProxy(SumError.class)
                    .compute(new int[0]);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.onActivity(getActivity(), Sum.class)
                    .buildProxy(SumError.class)
                    .compute(Collections.<Integer>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        final StandaloneChannel<Integer> channel = JRoutine.standalone().buildChannel();

        try {

            JRoutine.onActivity(getActivity(), Sum.class)
                    .buildProxy(SumError.class)
                    .compute(channel.output());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.onActivity(getActivity(), Sum.class)
                    .buildProxy(SumError.class)
                    .compute(1, channel.output());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.onActivity(getActivity(), Sum.class)
                    .buildProxy(SumError.class)
                    .compute(new Object());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.onActivity(getActivity(), Sum.class)
                    .buildProxy(SumError.class)
                    .compute(new Object[0]);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.onActivity(getActivity(), Sum.class)
                    .buildProxy(SumError.class)
                    .compute("test", new int[0]);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testInvalidProxyMethodError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        try {

            JRoutine.onActivity(getActivity(), TestClass.class)
                    .configure(withReadTimeout(INFINITY))
                    .buildProxy(TestItf.class)
                    .throwException(null);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.onActivity(getActivity(), TestClass.class)
                    .configure(withReadTimeout(INFINITY))
                    .buildProxy(TestItf.class)
                    .throwException1(null);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.onActivity(getActivity(), TestClass.class)
                    .configure(withReadTimeout(INFINITY))
                    .buildProxy(TestItf.class)
                    .throwException2(null);

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
        }
    }

    public void testInvalidProxyOutputAnnotationError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        try {

            JRoutine.onActivity(getActivity(), Count.class).buildProxy(CountError.class).count(3);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.onActivity(getActivity(), Count.class).buildProxy(CountError.class).count1(3);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.onActivity(getActivity(), Count.class).buildProxy(CountError.class).count2(3);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.onActivity(getActivity(), Count.class)
                    .buildProxy(CountError.class)
                    .countList(3);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.onActivity(getActivity(), Count.class)
                    .buildProxy(CountError.class)
                    .countList1(3);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.onActivity(getActivity(), Count.class)
                    .buildProxy(CountError.class)
                    .countList2(3);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testMethod() throws NoSuchMethodException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = seconds(10);
        final RoutineConfiguration configuration2 = builder().withSyncRunner(Runners.queuedRunner())
                                                             .withAsyncRunner(Runners.poolRunner())
                                                             .withMaxInvocations(1)
                                                             .withAvailableTimeout(
                                                                     TimeDuration.ZERO)
                                                             .buildConfiguration();
        final Routine<Object, Object> routine2 = JRoutine.onActivity(getActivity(), TestClass.class)
                                                         .configure(configuration2)
                                                         .members(withShareGroup("test"))
                                                         .method(TestClass.class.getMethod(
                                                                 "getLong"));

        assertThat(routine2.callSync().afterMax(timeout).readAll()).containsExactly(-77L);

    }

    public void testMethodBySignature() throws NoSuchMethodException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = seconds(10);
        final RoutineConfiguration configuration1 = builder().withSyncRunner(Runners.queuedRunner())
                                                             .withAsyncRunner(Runners.poolRunner())
                                                             .buildConfiguration();
        final Routine<Object, Object> routine1 = JRoutine.onActivity(getActivity(), TestClass.class)
                                                         .configure(configuration1)
                                                         .method("getLong");

        assertThat(routine1.callSync().afterMax(timeout).readAll()).containsExactly(-77L);
    }

    public void testMissingBoundMethodError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        try {

            JRoutine.onActivity(getActivity(), TestClass.class).boundMethod("test");

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testMissingMethodError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        try {

            JRoutine.onActivity(getActivity(), TestClass.class).method("test");

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testNullPointerError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        try {

            JRoutine.onActivity(getActivity(), (Class<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testNullProxyError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        try {

            JRoutine.onActivity(getActivity(), TestClass.class).buildProxy((Class<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.onActivity(getActivity(), TestClass.class).buildProxy((ClassToken<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @SuppressWarnings("unchecked")
    public void testProxyAnnotations() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final Itf itf = JRoutine.onActivity(getActivity(), Impl.class)
                                .configure(withReadTimeout(INFINITY))
                                .buildProxy(Itf.class);

        assertThat(itf.add0('c')).isEqualTo((int) 'c');
        final StandaloneChannel<Character> channel1 = JRoutine.standalone().buildChannel();
        channel1.input().pass('a').close();
        assertThat(itf.add1(channel1.output())).isEqualTo((int) 'a');
        final StandaloneChannel<Character> channel2 = JRoutine.standalone().buildChannel();
        channel2.input().pass('d', 'e', 'f').close();
        assertThat(itf.add2(channel2.output())).isIn((int) 'd', (int) 'e', (int) 'f');
        assertThat(itf.add3('c').readAll()).containsExactly((int) 'c');
        final StandaloneChannel<Character> channel3 = JRoutine.standalone().buildChannel();
        channel3.input().pass('a').close();
        assertThat(itf.add4(channel3.output()).readAll()).containsExactly((int) 'a');
        final StandaloneChannel<Character> channel4 = JRoutine.standalone().buildChannel();
        channel4.input().pass('d', 'e', 'f').close();
        assertThat(itf.add5(channel4.output()).readAll()).containsOnly((int) 'd', (int) 'e',
                                                                       (int) 'f');
        assertThat(itf.addA00(new char[]{'c', 'z'})).isEqualTo(new int[]{'c', 'z'});
        final StandaloneChannel<char[]> channel5 = JRoutine.standalone().buildChannel();
        channel5.input().pass(new char[]{'a', 'z'}).close();
        assertThat(itf.addA01(channel5.output())).isEqualTo(new int[]{'a', 'z'});
        final StandaloneChannel<Character> channel6 = JRoutine.standalone().buildChannel();
        channel6.input().pass('d', 'e', 'f').close();
        assertThat(itf.addA02(channel6.output())).isEqualTo(new int[]{'d', 'e', 'f'});
        final StandaloneChannel<char[]> channel7 = JRoutine.standalone().buildChannel();
        channel7.input()
                .pass(new char[]{'d', 'z'}, new char[]{'e', 'z'}, new char[]{'f', 'z'})
                .close();
        assertThat(itf.addA03(channel7.output())).isIn(new int[]{'d', 'z'}, new int[]{'e', 'z'},
                                                       new int[]{'f', 'z'});
        assertThat(itf.addA04(new char[]{'c', 'z'}).readAll()).containsExactly(new int[]{'c', 'z'});
        final StandaloneChannel<char[]> channel8 = JRoutine.standalone().buildChannel();
        channel8.input().pass(new char[]{'a', 'z'}).close();
        assertThat(itf.addA05(channel8.output()).readAll()).containsExactly(new int[]{'a', 'z'});
        final StandaloneChannel<Character> channel9 = JRoutine.standalone().buildChannel();
        channel9.input().pass('d', 'e', 'f').close();
        assertThat(itf.addA06(channel9.output()).readAll()).containsExactly(
                new int[]{'d', 'e', 'f'});
        final StandaloneChannel<char[]> channel10 = JRoutine.standalone().buildChannel();
        channel10.input()
                 .pass(new char[]{'d', 'z'}, new char[]{'e', 'z'}, new char[]{'f', 'z'})
                 .close();
        assertThat(itf.addA07(channel10.output()).readAll()).containsOnly(new int[]{'d', 'z'},
                                                                          new int[]{'e', 'z'},
                                                                          new int[]{'f', 'z'});
        assertThat(itf.addA08(new char[]{'c', 'z'}).readAll()).containsExactly((int) 'c',
                                                                               (int) 'z');
        final StandaloneChannel<char[]> channel11 = JRoutine.standalone().buildChannel();
        channel11.input().pass(new char[]{'a', 'z'}).close();
        assertThat(itf.addA09(channel11.output()).readAll()).containsExactly((int) 'a', (int) 'z');
        final StandaloneChannel<Character> channel12 = JRoutine.standalone().buildChannel();
        channel12.input().pass('d', 'e', 'f').close();
        assertThat(itf.addA10(channel12.output()).readAll()).containsExactly((int) 'd', (int) 'e',
                                                                             (int) 'f');
        final StandaloneChannel<char[]> channel13 = JRoutine.standalone().buildChannel();
        channel13.input()
                 .pass(new char[]{'d', 'z'}, new char[]{'e', 'z'}, new char[]{'f', 'z'})
                 .close();
        assertThat(itf.addA11(channel13.output()).readAll()).containsOnly((int) 'd', (int) 'e',
                                                                          (int) 'f', (int) 'z');
        assertThat(itf.addA12(new char[]{'c', 'z'})).containsExactly(new int[]{'c', 'z'});
        final StandaloneChannel<char[]> channel14 = JRoutine.standalone().buildChannel();
        channel14.input().pass(new char[]{'a', 'z'}).close();
        assertThat(itf.addA13(channel14.output())).containsExactly(new int[]{'a', 'z'});
        final StandaloneChannel<Character> channel15 = JRoutine.standalone().buildChannel();
        channel15.input().pass('d', 'e', 'f').close();
        assertThat(itf.addA14(channel15.output())).containsExactly(new int[]{'d', 'e', 'f'});
        final StandaloneChannel<char[]> channel16 = JRoutine.standalone().buildChannel();
        channel16.input()
                 .pass(new char[]{'d', 'z'}, new char[]{'e', 'z'}, new char[]{'f', 'z'})
                 .close();
        assertThat(itf.addA15(channel16.output())).containsOnly(new int[]{'d', 'z'},
                                                                new int[]{'e', 'z'},
                                                                new int[]{'f', 'z'});
        assertThat(itf.addA16(new char[]{'c', 'z'})).containsExactly(new int[]{'c', 'z'});
        final StandaloneChannel<char[]> channel17 = JRoutine.standalone().buildChannel();
        channel17.input().pass(new char[]{'a', 'z'}).close();
        assertThat(itf.addA17(channel17.output())).containsExactly(new int[]{'a', 'z'});
        final StandaloneChannel<Character> channel18 = JRoutine.standalone().buildChannel();
        channel18.input().pass('d', 'e', 'f').close();
        assertThat(itf.addA18(channel18.output())).containsExactly(new int[]{'d', 'e', 'f'});
        final StandaloneChannel<char[]> channel19 = JRoutine.standalone().buildChannel();
        channel19.input()
                 .pass(new char[]{'d', 'z'}, new char[]{'e', 'z'}, new char[]{'f', 'z'})
                 .close();
        assertThat(itf.addA19(channel19.output())).containsOnly(new int[]{'d', 'z'},
                                                                new int[]{'e', 'z'},
                                                                new int[]{'f', 'z'});
        assertThat(itf.addL00(Arrays.asList('c', 'z'))).isEqualTo(
                Arrays.asList((int) 'c', (int) 'z'));
        final StandaloneChannel<List<Character>> channel20 = JRoutine.standalone().buildChannel();
        channel20.input().pass(Arrays.asList('a', 'z')).close();
        assertThat(itf.addL01(channel20.output())).isEqualTo(Arrays.asList((int) 'a', (int) 'z'));
        final StandaloneChannel<Character> channel21 = JRoutine.standalone().buildChannel();
        channel21.input().pass('d', 'e', 'f').close();
        assertThat(itf.addL02(channel21.output())).isEqualTo(
                Arrays.asList((int) 'd', (int) 'e', (int) 'f'));
        final StandaloneChannel<List<Character>> channel22 = JRoutine.standalone().buildChannel();
        channel22.input()
                 .pass(Arrays.asList('d', 'z'), Arrays.asList('e', 'z'), Arrays.asList('f', 'z'))
                 .close();
        assertThat(itf.addL03(channel22.output())).isIn(Arrays.asList((int) 'd', (int) 'z'),
                                                        Arrays.asList((int) 'e', (int) 'z'),
                                                        Arrays.asList((int) 'f', (int) 'z'));
        assertThat(itf.addL04(Arrays.asList('c', 'z')).readAll()).containsExactly(
                Arrays.asList((int) 'c', (int) 'z'));
        final StandaloneChannel<List<Character>> channel23 = JRoutine.standalone().buildChannel();
        channel23.input().pass(Arrays.asList('a', 'z')).close();
        assertThat(itf.addL05(channel23.output()).readAll()).containsExactly(
                Arrays.asList((int) 'a', (int) 'z'));
        final StandaloneChannel<Character> channel24 = JRoutine.standalone().buildChannel();
        channel24.input().pass('d', 'e', 'f').close();
        assertThat(itf.addL06(channel24.output()).readAll()).containsExactly(
                Arrays.asList((int) 'd', (int) 'e', (int) 'f'));
        final StandaloneChannel<List<Character>> channel25 = JRoutine.standalone().buildChannel();
        channel25.input()
                 .pass(Arrays.asList('d', 'z'), Arrays.asList('e', 'z'), Arrays.asList('f', 'z'))
                 .close();
        assertThat(itf.addL07(channel25.output()).readAll()).containsOnly(
                Arrays.asList((int) 'd', (int) 'z'), Arrays.asList((int) 'e', (int) 'z'),
                Arrays.asList((int) 'f', (int) 'z'));
        assertThat(itf.addL08(Arrays.asList('c', 'z')).readAll()).containsExactly((int) 'c',
                                                                                  (int) 'z');
        final StandaloneChannel<List<Character>> channel26 = JRoutine.standalone().buildChannel();
        channel26.input().pass(Arrays.asList('a', 'z')).close();
        assertThat(itf.addL09(channel26.output()).readAll()).containsExactly((int) 'a', (int) 'z');
        final StandaloneChannel<Character> channel27 = JRoutine.standalone().buildChannel();
        channel27.input().pass('d', 'e', 'f').close();
        assertThat(itf.addL10(channel27.output()).readAll()).containsExactly((int) 'd', (int) 'e',
                                                                             (int) 'f');
        final StandaloneChannel<List<Character>> channel28 = JRoutine.standalone().buildChannel();
        channel28.input()
                 .pass(Arrays.asList('d', 'z'), Arrays.asList('e', 'z'), Arrays.asList('f', 'z'))
                 .close();
        assertThat(itf.addL11(channel28.output()).readAll()).containsOnly((int) 'd', (int) 'e',
                                                                          (int) 'f', (int) 'z');
        assertThat(itf.addL12(Arrays.asList('c', 'z'))).containsExactly(
                Arrays.asList((int) 'c', (int) 'z'));
        final StandaloneChannel<List<Character>> channel29 = JRoutine.standalone().buildChannel();
        channel29.input().pass(Arrays.asList('a', 'z')).close();
        assertThat(itf.addL13(channel29.output())).containsExactly(
                Arrays.asList((int) 'a', (int) 'z'));
        final StandaloneChannel<Character> channel30 = JRoutine.standalone().buildChannel();
        channel30.input().pass('d', 'e', 'f').close();
        assertThat(itf.addL14(channel30.output())).containsExactly(
                Arrays.asList((int) 'd', (int) 'e', (int) 'f'));
        final StandaloneChannel<List<Character>> channel31 = JRoutine.standalone().buildChannel();
        channel31.input()
                 .pass(Arrays.asList('d', 'z'), Arrays.asList('e', 'z'), Arrays.asList('f', 'z'))
                 .close();
        assertThat(itf.addL15(channel31.output())).containsOnly(Arrays.asList((int) 'd', (int) 'z'),
                                                                Arrays.asList((int) 'e', (int) 'z'),
                                                                Arrays.asList((int) 'f',
                                                                              (int) 'z'));
        assertThat(itf.addL16(Arrays.asList('c', 'z'))).containsExactly(
                Arrays.asList((int) 'c', (int) 'z'));
        final StandaloneChannel<List<Character>> channel32 = JRoutine.standalone().buildChannel();
        channel32.input().pass(Arrays.asList('a', 'z')).close();
        assertThat(itf.addL17(channel32.output())).containsExactly(
                Arrays.asList((int) 'a', (int) 'z'));
        final StandaloneChannel<Character> channel33 = JRoutine.standalone().buildChannel();
        channel33.input().pass('d', 'e', 'f').close();
        assertThat(itf.addL18(channel33.output())).containsExactly(
                Arrays.asList((int) 'd', (int) 'e', (int) 'f'));
        final StandaloneChannel<List<Character>> channel34 = JRoutine.standalone().buildChannel();
        channel34.input()
                 .pass(Arrays.asList('d', 'z'), Arrays.asList('e', 'z'), Arrays.asList('f', 'z'))
                 .close();
        assertThat(itf.addL19(channel34.output())).containsOnly(Arrays.asList((int) 'd', (int) 'z'),
                                                                Arrays.asList((int) 'e', (int) 'z'),
                                                                Arrays.asList((int) 'f',
                                                                              (int) 'z'));
        assertThat(itf.get0()).isEqualTo(31);
        assertThat(itf.get1().readAll()).containsExactly(31);
        assertThat(itf.getA0()).isEqualTo(new int[]{1, 2, 3});
        assertThat(itf.getA1().readAll()).containsExactly(1, 2, 3);
        assertThat(itf.getA2()).containsExactly(new int[]{1, 2, 3});
        assertThat(itf.getA3()).containsExactly(new int[]{1, 2, 3});
        assertThat(itf.getL0()).isEqualTo(Arrays.asList(1, 2, 3));
        assertThat(itf.getL1().readAll()).containsExactly(1, 2, 3);
        assertThat(itf.getL2()).containsExactly(Arrays.asList(1, 2, 3));
        assertThat(itf.getL3()).containsExactly(Arrays.asList(1, 2, 3));
        itf.set0(-17);
        final StandaloneChannel<Integer> channel35 = JRoutine.standalone().buildChannel();
        channel35.input().pass(-17).close();
        itf.set1(channel35.output());
        final StandaloneChannel<Integer> channel36 = JRoutine.standalone().buildChannel();
        channel36.input().pass(-17).close();
        itf.set2(channel36.output());
        itf.setA0(new int[]{1, 2, 3});
        final StandaloneChannel<int[]> channel37 = JRoutine.standalone().buildChannel();
        channel37.input().pass(new int[]{1, 2, 3}).close();
        itf.setA1(channel37.output());
        final StandaloneChannel<Integer> channel38 = JRoutine.standalone().buildChannel();
        channel38.input().pass(1, 2, 3).close();
        itf.setA2(channel38.output());
        final StandaloneChannel<int[]> channel39 = JRoutine.standalone().buildChannel();
        channel39.input().pass(new int[]{1, 2, 3}).close();
        itf.setA3(channel39.output());
        itf.setL0(Arrays.asList(1, 2, 3));
        final StandaloneChannel<List<Integer>> channel40 = JRoutine.standalone().buildChannel();
        channel40.input().pass(Arrays.asList(1, 2, 3)).close();
        itf.setL1(channel40.output());
        final StandaloneChannel<Integer> channel41 = JRoutine.standalone().buildChannel();
        channel41.input().pass(1, 2, 3).close();
        itf.setL2(channel41.output());
        final StandaloneChannel<List<Integer>> channel42 = JRoutine.standalone().buildChannel();
        channel42.input().pass(Arrays.asList(1, 2, 3)).close();
        itf.setL3(channel42.output());
    }

    @SuppressWarnings("NullArgumentToVariableArgMethod")
    public void testProxyRoutine() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TimeDuration timeout = seconds(10);
        final SquareItf squareAsync =
                JRoutine.onActivity(getActivity(), Square.class).buildProxy(SquareItf.class);

        assertThat(squareAsync.compute(3)).isEqualTo(9);
        assertThat(squareAsync.compute1(3)).containsExactly(9);
        assertThat(squareAsync.compute2(3)).containsExactly(9);
        assertThat(squareAsync.computeParallel1(1, 2, 3).afterMax(timeout).readAll()).contains(1, 4,
                                                                                               9);
        assertThat(squareAsync.computeParallel1().afterMax(timeout).readAll()).isEmpty();
        assertThat(squareAsync.computeParallel1(null).afterMax(timeout).readAll()).isEmpty();
        assertThat(squareAsync.computeParallel2(1, 2, 3).afterMax(timeout).readAll()).contains(1, 4,
                                                                                               9);
        assertThat(squareAsync.computeParallel2().afterMax(timeout).readAll()).isEmpty();
        assertThat(squareAsync.computeParallel2((Integer[]) null)
                              .afterMax(timeout)
                              .readAll()).isEmpty();
        assertThat(squareAsync.computeParallel3(Arrays.asList(1, 2, 3)).afterMax(timeout).readAll())
                .contains(1, 4, 9);
        assertThat(squareAsync.computeParallel3(Collections.<Integer>emptyList())
                              .afterMax(timeout)
                              .readAll()).isEmpty();
        assertThat(squareAsync.computeParallel3(null).afterMax(timeout).readAll()).isEmpty();

        final StandaloneChannel<Integer> channel1 = JRoutine.standalone().buildChannel();
        channel1.input().pass(4).close();
        assertThat(squareAsync.computeAsync(channel1.output())).isEqualTo(16);

        final StandaloneChannel<Integer> channel2 = JRoutine.standalone().buildChannel();
        channel2.input().pass(1, 2, 3).close();
        assertThat(squareAsync.computeParallel4(channel2.output())
                              .afterMax(timeout)
                              .readAll()).contains(1, 4, 9);

        final IncItf incItf = JRoutine.onActivity(getActivity(), Inc.class)
                                      .buildProxy(ClassToken.tokenOf(IncItf.class));
        assertThat(incItf.inc(1, 2, 3, 4)).containsOnly(2, 3, 4, 5);
        assertThat(incItf.incIterable(1, 2, 3, 4)).containsOnly(2, 3, 4, 5);
    }

    public void testShareGroup() throws NoSuchMethodException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final ObjectRoutineBuilder builder = JRoutine.onActivity(getActivity(), TestClass2.class)
                                                     .configure(withReadTimeout(seconds(9)));

        long startTime = System.currentTimeMillis();

        OutputChannel<Object> getOne =
                builder.members(withShareGroup("1")).method("getOne").callAsync();
        OutputChannel<Object> getTwo =
                builder.members(withShareGroup("2")).method("getTwo").callAsync();

        assertThat(getOne.checkComplete()).isTrue();
        assertThat(getTwo.checkComplete()).isTrue();
        assertThat(System.currentTimeMillis() - startTime).isLessThan(2000);

        startTime = System.currentTimeMillis();

        getOne = builder.method("getOne").callAsync();
        getTwo = builder.method("getTwo").callAsync();

        assertThat(getOne.checkComplete()).isTrue();
        assertThat(getTwo.checkComplete()).isTrue();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(2000);
    }

    public void testTimeoutActionAnnotation() throws NoSuchMethodException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        assertThat(JRoutine.onActivity(getActivity(), TestTimeout.class)
                           .configure(withReadTimeout(seconds(10)))
                           .invocations(withId(0))
                           .boundMethod("test")
                           .callAsync()
                           .readNext()).isEqualTo(31);

        try {

            JRoutine.onActivity(getActivity(), TestTimeout.class)
                    .configure(onReadTimeout(TimeoutActionType.DEADLOCK))
                    .invocations(withId(1))
                    .boundMethod("test")
                    .callAsync()
                    .readNext();

            fail();

        } catch (final NoSuchElementException ignored) {

        }

        assertThat(JRoutine.onActivity(getActivity(), TestTimeout.class)
                           .configure(withReadTimeout(seconds(10)))
                           .invocations(withId(2))
                           .method("getInt")
                           .callAsync()
                           .readNext()).isEqualTo(31);

        try {

            JRoutine.onActivity(getActivity(), TestTimeout.class)
                    .configure(onReadTimeout(TimeoutActionType.DEADLOCK))
                    .invocations(withId(3))
                    .method("getInt")
                    .callAsync()
                    .readNext();

            fail();

        } catch (final NoSuchElementException ignored) {

        }

        assertThat(JRoutine.onActivity(getActivity(), TestTimeout.class)
                           .configure(withReadTimeout(seconds(10)))
                           .invocations(withId(4))
                           .method(TestTimeout.class.getMethod("getInt"))
                           .callAsync()
                           .readNext()).isEqualTo(31);

        try {

            JRoutine.onActivity(getActivity(), TestTimeout.class)
                    .configure(onReadTimeout(TimeoutActionType.DEADLOCK))
                    .invocations(withId(5))
                    .method(TestTimeout.class.getMethod("getInt"))
                    .callAsync()
                    .readNext();

            fail();

        } catch (final NoSuchElementException ignored) {

        }

        assertThat(JRoutine.onActivity(getActivity(), TestTimeout.class)
                           .configure(withReadTimeout(seconds(10)))
                           .invocations(withId(6))
                           .buildProxy(TestTimeoutItf.class)
                           .getInt()).containsExactly(31);

        try {

            JRoutine.onActivity(getActivity(), TestTimeout.class)
                    .configure(onReadTimeout(TimeoutActionType.DEADLOCK))
                    .invocations(withId(7))
                    .buildProxy(TestTimeoutItf.class)
                    .getInt();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    public interface Itf {

        @Bind("a")
        int add0(char c);

        @Bind("a")
        int add1(@Pass(value = char.class, mode = PassMode.OBJECT) OutputChannel<Character> c);

        @Bind("a")
        int add2(@Pass(value = char.class, mode = PassMode.PARALLEL) OutputChannel<Character> c);

        @Bind("a")
        @Pass(value = int.class, mode = PassMode.OBJECT)
        OutputChannel<Integer> add3(char c);

        @Bind("a")
        @Pass(value = int.class, mode = PassMode.OBJECT)
        OutputChannel<Integer> add4(
                @Pass(value = char.class, mode = PassMode.OBJECT) OutputChannel<Character> c);

        @Bind("a")
        @Pass(value = int.class, mode = PassMode.OBJECT)
        OutputChannel<Integer> add5(
                @Pass(value = char.class, mode = PassMode.PARALLEL) OutputChannel<Character> c);

        @Bind("aa")
        int[] addA00(char[] c);

        @Bind("aa")
        int[] addA01(@Pass(value = char[].class,
                mode = PassMode.OBJECT) OutputChannel<char[]> c);

        @Bind("aa")
        int[] addA02(@Pass(value = char[].class,
                mode = PassMode.COLLECTION) OutputChannel<Character> c);

        @Bind("aa")
        int[] addA03(@Pass(value = char[].class,
                mode = PassMode.PARALLEL) OutputChannel<char[]> c);

        @Bind("aa")
        @Pass(value = int[].class, mode = PassMode.OBJECT)
        OutputChannel<int[]> addA04(char[] c);

        @Bind("aa")
        @Pass(value = int[].class, mode = PassMode.OBJECT)
        OutputChannel<int[]> addA05(
                @Pass(value = char[].class, mode = PassMode.OBJECT) OutputChannel<char[]> c);

        @Bind("aa")
        @Pass(value = int[].class, mode = PassMode.OBJECT)
        OutputChannel<int[]> addA06(@Pass(value = char[].class,
                mode = PassMode.COLLECTION) OutputChannel<Character> c);

        @Bind("aa")
        @Pass(value = int[].class, mode = PassMode.OBJECT)
        OutputChannel<int[]> addA07(@Pass(value = char[].class,
                mode = PassMode.PARALLEL) OutputChannel<char[]> c);

        @Bind("aa")
        @Pass(value = int[].class, mode = PassMode.COLLECTION)
        OutputChannel<Integer> addA08(char[] c);

        @Bind("aa")
        @Pass(value = int[].class, mode = PassMode.COLLECTION)
        OutputChannel<Integer> addA09(
                @Pass(value = char[].class, mode = PassMode.OBJECT) OutputChannel<char[]> c);

        @Bind("aa")
        @Pass(value = int[].class, mode = PassMode.COLLECTION)
        OutputChannel<Integer> addA10(@Pass(value = char[].class,
                mode = PassMode.COLLECTION) OutputChannel<Character> c);

        @Bind("aa")
        @Pass(value = int[].class, mode = PassMode.COLLECTION)
        OutputChannel<Integer> addA11(@Pass(value = char[].class,
                mode = PassMode.PARALLEL) OutputChannel<char[]> c);

        @Bind("aa")
        @Pass(value = int[].class, mode = PassMode.PARALLEL)
        List<int[]> addA12(char[] c);

        @Bind("aa")
        @Pass(value = int[].class, mode = PassMode.PARALLEL)
        List<int[]> addA13(
                @Pass(value = char[].class, mode = PassMode.OBJECT) OutputChannel<char[]> c);

        @Bind("aa")
        @Pass(value = int[].class, mode = PassMode.PARALLEL)
        List<int[]> addA14(@Pass(value = char[].class,
                mode = PassMode.COLLECTION) OutputChannel<Character> c);

        @Bind("aa")
        @Pass(value = int[].class, mode = PassMode.PARALLEL)
        List<int[]> addA15(@Pass(value = char[].class,
                mode = PassMode.PARALLEL) OutputChannel<char[]> c);

        @Bind("aa")
        @Pass(value = int[].class, mode = PassMode.PARALLEL)
        int[][] addA16(char[] c);

        @Bind("aa")
        @Pass(value = int[].class, mode = PassMode.PARALLEL)
        int[][] addA17(@Pass(value = char[].class, mode = PassMode.OBJECT) OutputChannel<char[]> c);

        @Bind("aa")
        @Pass(value = int[].class, mode = PassMode.PARALLEL)
        int[][] addA18(@Pass(value = char[].class,
                mode = PassMode.COLLECTION) OutputChannel<Character> c);

        @Bind("aa")
        @Pass(value = int[].class, mode = PassMode.PARALLEL)
        int[][] addA19(@Pass(value = char[].class,
                mode = PassMode.PARALLEL) OutputChannel<char[]> c);

        @Bind("al")
        List<Integer> addL00(List<Character> c);

        @Bind("al")
        List<Integer> addL01(@Pass(value = List.class,
                mode = PassMode.OBJECT) OutputChannel<List<Character>> c);

        @Bind("al")
        List<Integer> addL02(@Pass(value = List.class,
                mode = PassMode.COLLECTION) OutputChannel<Character> c);

        @Bind("al")
        List<Integer> addL03(@Pass(value = List.class,
                mode = PassMode.PARALLEL) OutputChannel<List<Character>> c);

        @Bind("al")
        @Pass(value = List.class, mode = PassMode.OBJECT)
        OutputChannel<List<Integer>> addL04(List<Character> c);

        @Bind("al")
        @Pass(value = List.class, mode = PassMode.OBJECT)
        OutputChannel<List<Integer>> addL05(@Pass(value = List.class,
                mode = PassMode.OBJECT) OutputChannel<List<Character>> c);

        @Bind("al")
        @Pass(value = List.class, mode = PassMode.OBJECT)
        OutputChannel<List<Integer>> addL06(@Pass(value = List.class,
                mode = PassMode.COLLECTION) OutputChannel<Character> c);

        @Bind("al")
        @Pass(value = List.class, mode = PassMode.OBJECT)
        OutputChannel<List<Integer>> addL07(@Pass(value = List.class,
                mode = PassMode.PARALLEL) OutputChannel<List<Character>> c);

        @Bind("al")
        @Pass(value = List.class, mode = PassMode.COLLECTION)
        OutputChannel<Integer> addL08(List<Character> c);

        @Bind("al")
        @Pass(value = List.class, mode = PassMode.COLLECTION)
        OutputChannel<Integer> addL09(@Pass(value = List.class,
                mode = PassMode.OBJECT) OutputChannel<List<Character>> c);

        @Bind("al")
        @Pass(value = List.class, mode = PassMode.COLLECTION)
        OutputChannel<Integer> addL10(@Pass(value = List.class,
                mode = PassMode.COLLECTION) OutputChannel<Character> c);

        @Bind("al")
        @Pass(value = List.class, mode = PassMode.COLLECTION)
        OutputChannel<Integer> addL11(@Pass(value = List.class,
                mode = PassMode.PARALLEL) OutputChannel<List<Character>> c);

        @Bind("al")
        @Pass(value = List.class, mode = PassMode.PARALLEL)
        List<List<Integer>> addL12(List<Character> c);

        @Bind("al")
        @Pass(value = List.class, mode = PassMode.PARALLEL)
        List<List<Integer>> addL13(@Pass(value = List.class,
                mode = PassMode.OBJECT) OutputChannel<List<Character>> c);

        @Bind("al")
        @Pass(value = List.class, mode = PassMode.PARALLEL)
        List<List<Integer>> addL14(@Pass(value = List.class,
                mode = PassMode.COLLECTION) OutputChannel<Character> c);

        @Bind("al")
        @Pass(value = List.class, mode = PassMode.PARALLEL)
        List<List<Integer>> addL15(@Pass(value = List.class,
                mode = PassMode.PARALLEL) OutputChannel<List<Character>> c);

        @Bind("al")
        @Pass(value = List.class, mode = PassMode.PARALLEL)
        List[] addL16(List<Character> c);

        @Bind("al")
        @Pass(value = List.class, mode = PassMode.PARALLEL)
        List[] addL17(@Pass(value = List.class,
                mode = PassMode.OBJECT) OutputChannel<List<Character>> c);

        @Bind("al")
        @Pass(value = List.class, mode = PassMode.PARALLEL)
        List[] addL18(@Pass(value = List.class,
                mode = PassMode.COLLECTION) OutputChannel<Character> c);

        @Bind("al")
        @Pass(value = List.class, mode = PassMode.PARALLEL)
        List[] addL19(@Pass(value = List.class,
                mode = PassMode.PARALLEL) OutputChannel<List<Character>> c);

        @Bind("g")
        int get0();

        @Bind("s")
        void set0(int i);

        @Bind("g")
        @Pass(value = int.class, mode = PassMode.OBJECT)
        OutputChannel<Integer> get1();

        @Bind("s")
        void set1(@Pass(value = int.class, mode = PassMode.OBJECT) OutputChannel<Integer> i);

        @Bind("ga")
        int[] getA0();

        @Bind("sa")
        void setA0(int[] i);

        @Bind("ga")
        @Pass(value = int[].class, mode = PassMode.COLLECTION)
        OutputChannel<Integer> getA1();

        @Bind("sa")
        void setA1(@Pass(value = int[].class, mode = PassMode.OBJECT) OutputChannel<int[]> i);

        @Bind("ga")
        @Pass(value = int[].class, mode = PassMode.PARALLEL)
        List<int[]> getA2();

        @Bind("sa")
        void setA2(@Pass(value = int[].class, mode = PassMode.COLLECTION) OutputChannel<Integer> i);

        @Bind("ga")
        @Pass(value = int[].class, mode = PassMode.PARALLEL)
        int[][] getA3();

        @Bind("sa")
        void setA3(@Pass(value = int[].class, mode = PassMode.PARALLEL) OutputChannel<int[]> i);

        @Bind("gl")
        List<Integer> getL0();

        @Bind("sl")
        void setL0(List<Integer> i);

        @Bind("gl")
        @Pass(value = List.class, mode = PassMode.COLLECTION)
        OutputChannel<Integer> getL1();

        @Bind("sl")
        void setL1(@Pass(value = List.class,
                mode = PassMode.OBJECT) OutputChannel<List<Integer>> i);

        @Bind("gl")
        @Pass(value = List.class, mode = PassMode.PARALLEL)
        List<List<Integer>> getL2();

        @Bind("sl")
        void setL2(@Pass(value = List.class, mode = PassMode.COLLECTION) OutputChannel<Integer> i);

        @Bind("gl")
        @Pass(value = List.class, mode = PassMode.PARALLEL)
        List[] getL3();

        @Bind("sl")
        void setL3(@Pass(value = List.class,
                mode = PassMode.PARALLEL) OutputChannel<List<Integer>> i);

        @Bind("s")
        void set2(@Pass(value = int.class, mode = PassMode.PARALLEL) OutputChannel<Integer> i);
    }

    private interface CountError {

        @Pass(int.class)
        String[] count(int length);

        @Bind("count")
        @Pass(value = int.class, mode = PassMode.COLLECTION)
        OutputChannel<Integer> count1(int length);

        @Bind("count")
        @Pass(value = int.class, mode = PassMode.PARALLEL)
        String[] count2(int length);

        @Pass(value = List.class, mode = PassMode.OBJECT)
        List<Integer> countList(int length);

        @Bind("countList")
        @Pass(value = List.class, mode = PassMode.COLLECTION)
        List<Integer> countList1(int length);

        @Bind("countList")
        @Pass(value = List.class, mode = PassMode.PARALLEL)
        OutputChannel<Integer> countList2(int length);
    }

    private interface CountItf {

        @Pass(int[].class)
        OutputChannel<Integer> count(int length);

        @Bind("count")
        @Pass(value = int[].class, mode = PassMode.OBJECT)
        OutputChannel<int[]> count1(int length);

        @Bind("count")
        @Pass(value = int[].class, mode = PassMode.COLLECTION)
        OutputChannel<Integer> count2(int length);

        @Pass(List.class)
        OutputChannel<Integer> countList(int length);

        @Bind("countList")
        @Pass(value = List.class, mode = PassMode.COLLECTION)
        OutputChannel<Integer> countList1(int length);
    }

    private interface IncItf {

        @Timeout(10000)
        @Pass(int.class)
        int[] inc(@Pass(int.class) int... i);

        @Timeout(10000)
        @Bind("inc")
        @Pass(int.class)
        Iterable<Integer> incIterable(@Pass(int.class) int... i);
    }

    private interface SquareItf {

        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        int compute(int i);

        @Bind("compute")
        @Pass(value = int.class, mode = PassMode.PARALLEL)
        @Timeout(10000)
        int[] compute1(int length);

        @Bind("compute")
        @Pass(value = int.class, mode = PassMode.PARALLEL)
        @Timeout(10000)
        List<Integer> compute2(int length);

        @Bind("compute")
        @Timeout(10000)
        int computeAsync(@Pass(int.class) OutputChannel<Integer> i);

        @ShareGroup(ShareGroup.NONE)
        @Bind("compute")
        @Pass(int.class)
        OutputChannel<Integer> computeParallel1(@Pass(int.class) int... i);

        @Bind("compute")
        @Pass(int.class)
        OutputChannel<Integer> computeParallel2(@Pass(int.class) Integer... i);

        @ShareGroup(ShareGroup.NONE)
        @Bind("compute")
        @Pass(int.class)
        OutputChannel<Integer> computeParallel3(@Pass(int.class) List<Integer> i);

        @ShareGroup(ShareGroup.NONE)
        @Bind("compute")
        @Pass(int.class)
        OutputChannel<Integer> computeParallel4(
                @Pass(value = int.class, mode = PassMode.PARALLEL) OutputChannel<Integer> i);
    }

    private interface SumError {

        int compute(int a, @Pass(int.class) int[] b);

        int compute(@Pass(int.class) String[] ints);

        int compute(@Pass(value = int.class, mode = PassMode.OBJECT) int[] ints);

        int compute(@Pass(value = int.class, mode = PassMode.COLLECTION) Iterable<Integer> ints);

        int compute(@Pass(value = int.class,
                mode = PassMode.COLLECTION) OutputChannel<Integer> ints);

        int compute(int a,
                @Pass(value = int[].class, mode = PassMode.COLLECTION) OutputChannel<Integer> b);

        int compute(@Pass(value = int.class, mode = PassMode.PARALLEL) Object ints);

        int compute(@Pass(value = int.class, mode = PassMode.PARALLEL) Object[] ints);

        int compute(String text, @Pass(value = int.class, mode = PassMode.PARALLEL) int[] ints);
    }

    private interface SumItf {

        int compute(int a, @Pass(int.class) OutputChannel<Integer> b);

        int compute(@Pass(int[].class) OutputChannel<Integer> ints);

        @Bind("compute")
        int compute1(@Pass(value = int[].class, mode = PassMode.OBJECT) OutputChannel<int[]> ints);

        @Bind("compute")
        int computeList(@Pass(List.class) OutputChannel<Integer> ints);

        @Bind("compute")
        int computeList1(@Pass(value = List.class,
                mode = PassMode.COLLECTION) OutputChannel<Integer> ints);
    }

    @SuppressWarnings("unused")
    private interface TestItf {

        void throwException(@Pass(int.class) RuntimeException ex);

        @Bind(TestClass.THROW)
        @Pass(int.class)
        void throwException1(RuntimeException ex);

        @Bind(TestClass.THROW)
        int throwException2(RuntimeException ex);
    }

    private interface TestTimeoutItf {

        @Pass(int.class)
        @TimeoutAction(TimeoutActionType.ABORT)
        List<Integer> getInt();
    }

    @SuppressWarnings("unused")
    public static class Impl {

        @Bind("a")
        public int add(char c) {

            return c;
        }

        @Bind("aa")
        public int[] addArray(char[] c) {

            final int[] array = new int[c.length];

            for (int i = 0; i < c.length; i++) {

                array[i] = c[i];
            }

            return array;
        }

        @Bind("al")
        public List<Integer> addList(List<Character> c) {

            final ArrayList<Integer> list = new ArrayList<Integer>(c.size());

            for (final Character character : c) {

                list.add((int) character);
            }

            return list;
        }

        @Bind("g")
        public int get() {

            return 31;
        }

        @Bind("ga")
        public int[] getArray() {

            return new int[]{1, 2, 3};
        }

        @Bind("sa")
        public void setArray(int[] i) {

            assertThat(i).containsExactly(1, 2, 3);
        }

        @Bind("gl")
        public List<Integer> getList() {

            return Arrays.asList(1, 2, 3);
        }

        @Bind("sl")
        public void setList(List<Integer> l) {

            assertThat(l).containsExactly(1, 2, 3);
        }

        @Bind("s")
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

        public void dbg(@Nonnull final List<Object> contexts, @Nullable final String message,
                @Nullable final Throwable throwable) {

            ++mDgbCount;
        }

        public void err(@Nonnull final List<Object> contexts, @Nullable final String message,
                @Nullable final Throwable throwable) {

            ++mErrCount;
        }

        public void wrn(@Nonnull final List<Object> contexts, @Nullable final String message,
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

        @Bind(GET)
        public int getOne() {

            return 1;
        }

        @Bind(GET)
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

        @Bind(GET)
        public long getLong() {

            return -77;

        }

        @Bind(THROW)
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

        @Bind("test")
        @TimeoutAction(TimeoutActionType.EXIT)
        public int getInt() throws InterruptedException {

            Thread.sleep(100);
            return 31;
        }
    }
}
