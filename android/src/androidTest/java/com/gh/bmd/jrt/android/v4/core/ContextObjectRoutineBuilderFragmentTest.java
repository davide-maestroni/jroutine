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
package com.gh.bmd.jrt.android.v4.core;

import android.annotation.TargetApi;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;

import com.gh.bmd.jrt.android.R;
import com.gh.bmd.jrt.android.builder.LoaderConfiguration;
import com.gh.bmd.jrt.annotation.Alias;
import com.gh.bmd.jrt.annotation.Param;
import com.gh.bmd.jrt.annotation.Param.PassMode;
import com.gh.bmd.jrt.annotation.ShareGroup;
import com.gh.bmd.jrt.annotation.Timeout;
import com.gh.bmd.jrt.annotation.TimeoutAction;
import com.gh.bmd.jrt.builder.ObjectRoutineBuilder;
import com.gh.bmd.jrt.builder.ProxyConfiguration;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.builder.RoutineConfiguration.OrderType;
import com.gh.bmd.jrt.builder.RoutineConfiguration.TimeoutActionType;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.TransportChannel;
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

import static com.gh.bmd.jrt.builder.RoutineConfiguration.builder;
import static com.gh.bmd.jrt.time.TimeDuration.INFINITY;
import static com.gh.bmd.jrt.time.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Object context routine activity unit tests.
 * <p/>
 * Created by davide-maestroni on 4/7/15.
 */
@TargetApi(VERSION_CODES.FROYO)
public class ContextObjectRoutineBuilderFragmentTest
        extends ActivityInstrumentationTestCase2<TestActivity> {

    public ContextObjectRoutineBuilderFragmentTest() {

        super(TestActivity.class);
    }

    public void testAliasMethod() throws NoSuchMethodException {

        final TimeDuration timeout = seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final TimeUnit timeUnit = TimeUnit.SECONDS;
        final Routine<Object, Object> routine = JRoutine.onFragment(fragment, TestClass.class)
                                                        .withRoutine()
                                                        .withSyncRunner(Runners.sequentialRunner())
                                                        .withAsyncRunner(Runners.poolRunner())
                                                        .withMaxInvocations(1)
                                                        .withCoreInvocations(1)
                                                        .withAvailInvocationTimeout(1, timeUnit)
                                                        .withReadTimeoutAction(
                                                                TimeoutActionType.EXIT)
                                                        .withLogLevel(LogLevel.DEBUG)
                                                        .withLog(new NullLog())
                                                        .set()
                                                        .aliasMethod(TestClass.GET);

        assertThat(routine.callSync().afterMax(timeout).readAll()).containsExactly(-77L);
    }

    public void testArgs() {

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        assertThat(JRoutine.onFragment(fragment, TestArgs.class)
                           .withRoutine()
                           .withFactoryArgs(17)
                           .set()
                           .method("getId")
                           .callAsync()
                           .eventually()
                           .readNext()).isEqualTo(17);
    }

    public void testAsyncInputProxyRoutine() {

        final TimeDuration timeout = seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final SumItf sumAsync = JRoutine.onFragment(fragment, Sum.class)
                                        .withRoutine()
                                        .withReadTimeout(timeout)
                                        .set()
                                        .buildProxy(SumItf.class);
        final TransportChannel<Integer> channel3 = JRoutine.transport().buildChannel();
        channel3.input().pass(7).close();
        assertThat(sumAsync.compute(3, channel3.output())).isEqualTo(10);

        final TransportChannel<Integer> channel4 = JRoutine.transport().buildChannel();
        channel4.input().pass(1, 2, 3, 4).close();
        assertThat(sumAsync.compute(channel4.output())).isEqualTo(10);

        final TransportChannel<int[]> channel5 = JRoutine.transport().buildChannel();
        channel5.input().pass(new int[]{1, 2, 3, 4}).close();
        assertThat(sumAsync.compute1(channel5.output())).isEqualTo(10);

        final TransportChannel<Integer> channel6 = JRoutine.transport().buildChannel();
        channel6.input().pass(1, 2, 3, 4).close();
        assertThat(sumAsync.computeList(channel6.output())).isEqualTo(10);

        final TransportChannel<Integer> channel7 = JRoutine.transport().buildChannel();
        channel7.input().pass(1, 2, 3, 4).close();
        assertThat(sumAsync.computeList1(channel7.output())).isEqualTo(10);
    }

    public void testAsyncOutputProxyRoutine() {

        final TimeDuration timeout = seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final CountItf countAsync = JRoutine.onFragment(fragment, Count.class)
                                            .withRoutine()
                                            .withReadTimeout(timeout)
                                            .set()
                                            .buildProxy(CountItf.class);
        assertThat(countAsync.count(3).readAll()).containsExactly(0, 1, 2);
        assertThat(countAsync.count1(3).readAll()).containsExactly(new int[]{0, 1, 2});
        assertThat(countAsync.count2(2).readAll()).containsExactly(0, 1);
        assertThat(countAsync.countList(3).readAll()).containsExactly(0, 1, 2);
        assertThat(countAsync.countList1(3).readAll()).containsExactly(0, 1, 2);
    }

    @SuppressWarnings("ConstantConditions")
    public void testConfigurationErrors() {

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);

        try {

            new DefaultLoaderObjectRoutineBuilder(fragment, TestClass.class).setConfiguration(
                    (RoutineConfiguration) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultLoaderObjectRoutineBuilder(fragment, TestClass.class).setConfiguration(
                    (ProxyConfiguration) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultLoaderObjectRoutineBuilder(fragment, TestClass.class).setConfiguration(
                    (LoaderConfiguration) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testConfigurationWarnings() {

        final CountLog countLog = new CountLog();
        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final RoutineConfiguration configuration = builder().withFactoryArgs()
                                                            .withInputOrder(OrderType.NONE)
                                                            .withInputMaxSize(3)
                                                            .withInputTimeout(seconds(10))
                                                            .withOutputOrder(OrderType.NONE)
                                                            .withOutputMaxSize(3)
                                                            .withOutputTimeout(seconds(10))
                                                            .withLogLevel(LogLevel.DEBUG)
                                                            .withLog(countLog)
                                                            .set();
        JRoutine.onFragment(fragment, TestClass.class)
                .withRoutine()
                .with(configuration)
                .set()
                .withProxy()
                .withShareGroup("test")
                .set()
                .aliasMethod(TestClass.GET);
        assertThat(countLog.getWrnCount()).isEqualTo(6);

        JRoutine.onFragment(fragment, Square.class)
                .withRoutine()
                .with(configuration)
                .set()
                .withProxy()
                .withShareGroup("test")
                .set()
                .buildProxy(SquareItf.class)
                .compute(3);
        assertThat(countLog.getWrnCount()).isEqualTo(12);
    }

    public void testDuplicateAnnotationError() {

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);

        try {

            JRoutine.onFragment(fragment, DuplicateAnnotation.class);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testException() throws NoSuchMethodException {

        final TimeDuration timeout = seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<Object, Object> routine3 =
                JRoutine.onFragment(fragment, TestClass.class).aliasMethod(TestClass.THROW);

        try {

            routine3.callSync(new IllegalArgumentException("test")).afterMax(timeout).readAll();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
            assertThat(e.getCause().getMessage()).isEqualTo("test");
        }
    }

    public void testInvalidProxyError() {

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);

        try {

            JRoutine.onFragment(fragment, TestClass.class).buildProxy(TestClass.class);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.onFragment(fragment, TestClass.class)
                    .buildProxy(ClassToken.tokenOf(TestClass.class));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testInvalidProxyInputAnnotationError() {

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);

        try {

            JRoutine.onFragment(fragment, Sum.class)
                    .buildProxy(SumError.class)
                    .compute(1, new int[0]);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.onFragment(fragment, Sum.class)
                    .buildProxy(SumError.class)
                    .compute(new String[0]);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.onFragment(fragment, Sum.class).buildProxy(SumError.class).compute(new int[0]);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.onFragment(fragment, Sum.class)
                    .buildProxy(SumError.class)
                    .compute(Collections.<Integer>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        final TransportChannel<Integer> channel = JRoutine.transport().buildChannel();

        try {

            JRoutine.onFragment(fragment, Sum.class)
                    .buildProxy(SumError.class)
                    .compute(channel.output());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.onFragment(fragment, Sum.class)
                    .buildProxy(SumError.class)
                    .compute(1, channel.output());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.onFragment(fragment, Sum.class)
                    .buildProxy(SumError.class)
                    .compute(new Object());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.onFragment(fragment, Sum.class)
                    .buildProxy(SumError.class)
                    .compute(new Object[0]);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.onFragment(fragment, Sum.class)
                    .buildProxy(SumError.class)
                    .compute("test", new int[0]);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testInvalidProxyMethodError() {

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);

        try {

            JRoutine.onFragment(fragment, TestClass.class)
                    .withRoutine()
                    .withReadTimeout(INFINITY)
                    .set()
                    .buildProxy(TestItf.class)
                    .throwException(null);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.onFragment(fragment, TestClass.class)
                    .withRoutine()
                    .withReadTimeout(INFINITY)
                    .set()
                    .buildProxy(TestItf.class)
                    .throwException1(null);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.onFragment(fragment, TestClass.class)
                    .withRoutine()
                    .withReadTimeout(INFINITY)
                    .set()
                    .buildProxy(TestItf.class)
                    .throwException2(null);

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
        }
    }

    public void testInvalidProxyOutputAnnotationError() {

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);

        try {

            JRoutine.onFragment(fragment, Count.class).buildProxy(CountError.class).count(3);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.onFragment(fragment, Count.class).buildProxy(CountError.class).count1(3);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.onFragment(fragment, Count.class).buildProxy(CountError.class).count2(3);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.onFragment(fragment, Count.class).buildProxy(CountError.class).countList(3);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.onFragment(fragment, Count.class).buildProxy(CountError.class).countList1(3);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.onFragment(fragment, Count.class).buildProxy(CountError.class).countList2(3);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testMethod() throws NoSuchMethodException {

        final TimeDuration timeout = seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<Object, Object> routine2 = JRoutine.onFragment(fragment, TestClass.class)
                                                         .withRoutine()
                                                         .withSyncRunner(Runners.queuedRunner())
                                                         .withAsyncRunner(Runners.poolRunner())
                                                         .withMaxInvocations(1)
                                                         .withAvailInvocationTimeout(
                                                                 TimeDuration.ZERO)
                                                         .set()
                                                         .withProxy()
                                                         .withShareGroup("test")
                                                         .set()
                                                         .method(TestClass.class.getMethod(
                                                                 "getLong"));

        assertThat(routine2.callSync().afterMax(timeout).readAll()).containsExactly(-77L);
    }

    public void testMethodBySignature() throws NoSuchMethodException {

        final TimeDuration timeout = seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Routine<Object, Object> routine1 = JRoutine.onFragment(fragment, TestClass.class)
                                                         .withRoutine()
                                                         .withSyncRunner(Runners.queuedRunner())
                                                         .withAsyncRunner(Runners.poolRunner())
                                                         .set()
                                                         .method("getLong");

        assertThat(routine1.callSync().afterMax(timeout).readAll()).containsExactly(-77L);
    }

    public void testMissingAliasMethodError() {

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);

        try {

            JRoutine.onFragment(fragment, TestClass.class).aliasMethod("test");

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testMissingMethodError() {

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);

        try {

            JRoutine.onFragment(fragment, TestClass.class).method("test");

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testNullPointerError() {

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);

        try {

            JRoutine.onFragment(fragment, (Class<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testNullProxyError() {

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);

        try {

            JRoutine.onFragment(fragment, TestClass.class).buildProxy((Class<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.onFragment(fragment, TestClass.class).buildProxy((ClassToken<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @SuppressWarnings("unchecked")
    public void testProxyAnnotations() {

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Itf itf = JRoutine.onFragment(fragment, Impl.class)
                                .withRoutine()
                                .withReadTimeout(INFINITY)
                                .set()
                                .buildProxy(Itf.class);

        assertThat(itf.add0('c')).isEqualTo((int) 'c');
        final TransportChannel<Character> channel1 = JRoutine.transport().buildChannel();
        channel1.input().pass('a').close();
        assertThat(itf.add1(channel1.output())).isEqualTo((int) 'a');
        final TransportChannel<Character> channel2 = JRoutine.transport().buildChannel();
        channel2.input().pass('d', 'e', 'f').close();
        assertThat(itf.add2(channel2.output())).isIn((int) 'd', (int) 'e', (int) 'f');
        assertThat(itf.add3('c').readAll()).containsExactly((int) 'c');
        final TransportChannel<Character> channel3 = JRoutine.transport().buildChannel();
        channel3.input().pass('a').close();
        assertThat(itf.add4(channel3.output()).readAll()).containsExactly((int) 'a');
        final TransportChannel<Character> channel4 = JRoutine.transport().buildChannel();
        channel4.input().pass('d', 'e', 'f').close();
        assertThat(itf.add5(channel4.output()).readAll()).containsOnly((int) 'd', (int) 'e',
                                                                       (int) 'f');
        assertThat(itf.addA00(new char[]{'c', 'z'})).isEqualTo(new int[]{'c', 'z'});
        final TransportChannel<char[]> channel5 = JRoutine.transport().buildChannel();
        channel5.input().pass(new char[]{'a', 'z'}).close();
        assertThat(itf.addA01(channel5.output())).isEqualTo(new int[]{'a', 'z'});
        final TransportChannel<Character> channel6 = JRoutine.transport().buildChannel();
        channel6.input().pass('d', 'e', 'f').close();
        assertThat(itf.addA02(channel6.output())).isEqualTo(new int[]{'d', 'e', 'f'});
        final TransportChannel<char[]> channel7 = JRoutine.transport().buildChannel();
        channel7.input()
                .pass(new char[]{'d', 'z'}, new char[]{'e', 'z'}, new char[]{'f', 'z'})
                .close();
        assertThat(itf.addA03(channel7.output())).isIn(new int[]{'d', 'z'}, new int[]{'e', 'z'},
                                                       new int[]{'f', 'z'});
        assertThat(itf.addA04(new char[]{'c', 'z'}).readAll()).containsExactly(new int[]{'c', 'z'});
        final TransportChannel<char[]> channel8 = JRoutine.transport().buildChannel();
        channel8.input().pass(new char[]{'a', 'z'}).close();
        assertThat(itf.addA05(channel8.output()).readAll()).containsExactly(new int[]{'a', 'z'});
        final TransportChannel<Character> channel9 = JRoutine.transport().buildChannel();
        channel9.input().pass('d', 'e', 'f').close();
        assertThat(itf.addA06(channel9.output()).readAll()).containsExactly(
                new int[]{'d', 'e', 'f'});
        final TransportChannel<char[]> channel10 = JRoutine.transport().buildChannel();
        channel10.input()
                 .pass(new char[]{'d', 'z'}, new char[]{'e', 'z'}, new char[]{'f', 'z'})
                 .close();
        assertThat(itf.addA07(channel10.output()).readAll()).containsOnly(new int[]{'d', 'z'},
                                                                          new int[]{'e', 'z'},
                                                                          new int[]{'f', 'z'});
        assertThat(itf.addA08(new char[]{'c', 'z'}).readAll()).containsExactly((int) 'c',
                                                                               (int) 'z');
        final TransportChannel<char[]> channel11 = JRoutine.transport().buildChannel();
        channel11.input().pass(new char[]{'a', 'z'}).close();
        assertThat(itf.addA09(channel11.output()).readAll()).containsExactly((int) 'a', (int) 'z');
        final TransportChannel<Character> channel12 = JRoutine.transport().buildChannel();
        channel12.input().pass('d', 'e', 'f').close();
        assertThat(itf.addA10(channel12.output()).readAll()).containsExactly((int) 'd', (int) 'e',
                                                                             (int) 'f');
        final TransportChannel<char[]> channel13 = JRoutine.transport().buildChannel();
        channel13.input()
                 .pass(new char[]{'d', 'z'}, new char[]{'e', 'z'}, new char[]{'f', 'z'})
                 .close();
        assertThat(itf.addA11(channel13.output()).readAll()).containsOnly((int) 'd', (int) 'e',
                                                                          (int) 'f', (int) 'z');
        assertThat(itf.addA12(new char[]{'c', 'z'})).containsExactly(new int[]{'c', 'z'});
        final TransportChannel<char[]> channel14 = JRoutine.transport().buildChannel();
        channel14.input().pass(new char[]{'a', 'z'}).close();
        assertThat(itf.addA13(channel14.output())).containsExactly(new int[]{'a', 'z'});
        final TransportChannel<Character> channel15 = JRoutine.transport().buildChannel();
        channel15.input().pass('d', 'e', 'f').close();
        assertThat(itf.addA14(channel15.output())).containsExactly(new int[]{'d', 'e', 'f'});
        final TransportChannel<char[]> channel16 = JRoutine.transport().buildChannel();
        channel16.input()
                 .pass(new char[]{'d', 'z'}, new char[]{'e', 'z'}, new char[]{'f', 'z'})
                 .close();
        assertThat(itf.addA15(channel16.output())).containsOnly(new int[]{'d', 'z'},
                                                                new int[]{'e', 'z'},
                                                                new int[]{'f', 'z'});
        assertThat(itf.addA16(new char[]{'c', 'z'})).containsExactly(new int[]{'c', 'z'});
        final TransportChannel<char[]> channel17 = JRoutine.transport().buildChannel();
        channel17.input().pass(new char[]{'a', 'z'}).close();
        assertThat(itf.addA17(channel17.output())).containsExactly(new int[]{'a', 'z'});
        final TransportChannel<Character> channel18 = JRoutine.transport().buildChannel();
        channel18.input().pass('d', 'e', 'f').close();
        assertThat(itf.addA18(channel18.output())).containsExactly(new int[]{'d', 'e', 'f'});
        final TransportChannel<char[]> channel19 = JRoutine.transport().buildChannel();
        channel19.input()
                 .pass(new char[]{'d', 'z'}, new char[]{'e', 'z'}, new char[]{'f', 'z'})
                 .close();
        assertThat(itf.addA19(channel19.output())).containsOnly(new int[]{'d', 'z'},
                                                                new int[]{'e', 'z'},
                                                                new int[]{'f', 'z'});
        assertThat(itf.addL00(Arrays.asList('c', 'z'))).isEqualTo(
                Arrays.asList((int) 'c', (int) 'z'));
        final TransportChannel<List<Character>> channel20 = JRoutine.transport().buildChannel();
        channel20.input().pass(Arrays.asList('a', 'z')).close();
        assertThat(itf.addL01(channel20.output())).isEqualTo(Arrays.asList((int) 'a', (int) 'z'));
        final TransportChannel<Character> channel21 = JRoutine.transport().buildChannel();
        channel21.input().pass('d', 'e', 'f').close();
        assertThat(itf.addL02(channel21.output())).isEqualTo(
                Arrays.asList((int) 'd', (int) 'e', (int) 'f'));
        final TransportChannel<List<Character>> channel22 = JRoutine.transport().buildChannel();
        channel22.input()
                 .pass(Arrays.asList('d', 'z'), Arrays.asList('e', 'z'), Arrays.asList('f', 'z'))
                 .close();
        assertThat(itf.addL03(channel22.output())).isIn(Arrays.asList((int) 'd', (int) 'z'),
                                                        Arrays.asList((int) 'e', (int) 'z'),
                                                        Arrays.asList((int) 'f', (int) 'z'));
        assertThat(itf.addL04(Arrays.asList('c', 'z')).readAll()).containsExactly(
                Arrays.asList((int) 'c', (int) 'z'));
        final TransportChannel<List<Character>> channel23 = JRoutine.transport().buildChannel();
        channel23.input().pass(Arrays.asList('a', 'z')).close();
        assertThat(itf.addL05(channel23.output()).readAll()).containsExactly(
                Arrays.asList((int) 'a', (int) 'z'));
        final TransportChannel<Character> channel24 = JRoutine.transport().buildChannel();
        channel24.input().pass('d', 'e', 'f').close();
        assertThat(itf.addL06(channel24.output()).readAll()).containsExactly(
                Arrays.asList((int) 'd', (int) 'e', (int) 'f'));
        final TransportChannel<List<Character>> channel25 = JRoutine.transport().buildChannel();
        channel25.input()
                 .pass(Arrays.asList('d', 'z'), Arrays.asList('e', 'z'), Arrays.asList('f', 'z'))
                 .close();
        assertThat(itf.addL07(channel25.output()).readAll()).containsOnly(
                Arrays.asList((int) 'd', (int) 'z'), Arrays.asList((int) 'e', (int) 'z'),
                Arrays.asList((int) 'f', (int) 'z'));
        assertThat(itf.addL08(Arrays.asList('c', 'z')).readAll()).containsExactly((int) 'c',
                                                                                  (int) 'z');
        final TransportChannel<List<Character>> channel26 = JRoutine.transport().buildChannel();
        channel26.input().pass(Arrays.asList('a', 'z')).close();
        assertThat(itf.addL09(channel26.output()).readAll()).containsExactly((int) 'a', (int) 'z');
        final TransportChannel<Character> channel27 = JRoutine.transport().buildChannel();
        channel27.input().pass('d', 'e', 'f').close();
        assertThat(itf.addL10(channel27.output()).readAll()).containsExactly((int) 'd', (int) 'e',
                                                                             (int) 'f');
        final TransportChannel<List<Character>> channel28 = JRoutine.transport().buildChannel();
        channel28.input()
                 .pass(Arrays.asList('d', 'z'), Arrays.asList('e', 'z'), Arrays.asList('f', 'z'))
                 .close();
        assertThat(itf.addL11(channel28.output()).readAll()).containsOnly((int) 'd', (int) 'e',
                                                                          (int) 'f', (int) 'z');
        assertThat(itf.addL12(Arrays.asList('c', 'z'))).containsExactly(
                Arrays.asList((int) 'c', (int) 'z'));
        final TransportChannel<List<Character>> channel29 = JRoutine.transport().buildChannel();
        channel29.input().pass(Arrays.asList('a', 'z')).close();
        assertThat(itf.addL13(channel29.output())).containsExactly(
                Arrays.asList((int) 'a', (int) 'z'));
        final TransportChannel<Character> channel30 = JRoutine.transport().buildChannel();
        channel30.input().pass('d', 'e', 'f').close();
        assertThat(itf.addL14(channel30.output())).containsExactly(
                Arrays.asList((int) 'd', (int) 'e', (int) 'f'));
        final TransportChannel<List<Character>> channel31 = JRoutine.transport().buildChannel();
        channel31.input()
                 .pass(Arrays.asList('d', 'z'), Arrays.asList('e', 'z'), Arrays.asList('f', 'z'))
                 .close();
        assertThat(itf.addL15(channel31.output())).containsOnly(Arrays.asList((int) 'd', (int) 'z'),
                                                                Arrays.asList((int) 'e', (int) 'z'),
                                                                Arrays.asList((int) 'f',
                                                                              (int) 'z'));
        assertThat(itf.addL16(Arrays.asList('c', 'z'))).containsExactly(
                Arrays.asList((int) 'c', (int) 'z'));
        final TransportChannel<List<Character>> channel32 = JRoutine.transport().buildChannel();
        channel32.input().pass(Arrays.asList('a', 'z')).close();
        assertThat(itf.addL17(channel32.output())).containsExactly(
                Arrays.asList((int) 'a', (int) 'z'));
        final TransportChannel<Character> channel33 = JRoutine.transport().buildChannel();
        channel33.input().pass('d', 'e', 'f').close();
        assertThat(itf.addL18(channel33.output())).containsExactly(
                Arrays.asList((int) 'd', (int) 'e', (int) 'f'));
        final TransportChannel<List<Character>> channel34 = JRoutine.transport().buildChannel();
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
        final TransportChannel<Integer> channel35 = JRoutine.transport().buildChannel();
        channel35.input().pass(-17).close();
        itf.set1(channel35.output());
        final TransportChannel<Integer> channel36 = JRoutine.transport().buildChannel();
        channel36.input().pass(-17).close();
        itf.set2(channel36.output());
        itf.setA0(new int[]{1, 2, 3});
        final TransportChannel<int[]> channel37 = JRoutine.transport().buildChannel();
        channel37.input().pass(new int[]{1, 2, 3}).close();
        itf.setA1(channel37.output());
        final TransportChannel<Integer> channel38 = JRoutine.transport().buildChannel();
        channel38.input().pass(1, 2, 3).close();
        itf.setA2(channel38.output());
        final TransportChannel<int[]> channel39 = JRoutine.transport().buildChannel();
        channel39.input().pass(new int[]{1, 2, 3}).close();
        itf.setA3(channel39.output());
        itf.setL0(Arrays.asList(1, 2, 3));
        final TransportChannel<List<Integer>> channel40 = JRoutine.transport().buildChannel();
        channel40.input().pass(Arrays.asList(1, 2, 3)).close();
        itf.setL1(channel40.output());
        final TransportChannel<Integer> channel41 = JRoutine.transport().buildChannel();
        channel41.input().pass(1, 2, 3).close();
        itf.setL2(channel41.output());
        final TransportChannel<List<Integer>> channel42 = JRoutine.transport().buildChannel();
        channel42.input().pass(Arrays.asList(1, 2, 3)).close();
        itf.setL3(channel42.output());
    }

    @SuppressWarnings("NullArgumentToVariableArgMethod")
    public void testProxyRoutine() {

        final TimeDuration timeout = seconds(10);
        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final SquareItf squareAsync =
                JRoutine.onFragment(fragment, Square.class).buildProxy(SquareItf.class);

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

        final TransportChannel<Integer> channel1 = JRoutine.transport().buildChannel();
        channel1.input().pass(4).close();
        assertThat(squareAsync.computeAsync(channel1.output())).isEqualTo(16);

        final TransportChannel<Integer> channel2 = JRoutine.transport().buildChannel();
        channel2.input().pass(1, 2, 3).close();
        assertThat(squareAsync.computeParallel4(channel2.output())
                              .afterMax(timeout)
                              .readAll()).contains(1, 4, 9);

        final IncItf incItf = JRoutine.onFragment(fragment, Inc.class)
                                      .buildProxy(ClassToken.tokenOf(IncItf.class));
        assertThat(incItf.inc(1, 2, 3, 4)).containsOnly(2, 3, 4, 5);
        assertThat(incItf.incIterable(1, 2, 3, 4)).containsOnly(2, 3, 4, 5);
    }

    public void testShareGroup() throws NoSuchMethodException {

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final ObjectRoutineBuilder builder = JRoutine.onFragment(fragment, TestClass2.class)
                                                     .withRoutine()
                                                     .withReadTimeout(seconds(10))
                                                     .set();

        long startTime = System.currentTimeMillis();

        OutputChannel<Object> getOne =
                builder.withProxy().withShareGroup("1").set().method("getOne").callAsync();
        OutputChannel<Object> getTwo =
                builder.withProxy().withShareGroup("2").set().method("getTwo").callAsync();

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

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        assertThat(JRoutine.onFragment(fragment, TestTimeout.class)
                           .withRoutine()
                           .withReadTimeout(seconds(10))
                           .set()
                           .withLoader()
                           .withId(0)
                           .set()
                           .aliasMethod("test")
                           .callAsync()
                           .readNext()).isEqualTo(31);

        try {

            JRoutine.onFragment(fragment, TestTimeout.class)
                    .withRoutine()
                    .withReadTimeoutAction(TimeoutActionType.DEADLOCK)
                    .set()
                    .withLoader()
                    .withId(1)
                    .set()
                    .aliasMethod("test")
                    .callAsync()
                    .readNext();

            fail();

        } catch (final NoSuchElementException ignored) {

        }

        assertThat(JRoutine.onFragment(fragment, TestTimeout.class)
                           .withRoutine()
                           .withReadTimeout(seconds(10))
                           .set()
                           .withLoader()
                           .withId(2)
                           .set()
                           .method("getInt")
                           .callAsync()
                           .readNext()).isEqualTo(31);

        try {

            JRoutine.onFragment(fragment, TestTimeout.class)
                    .withRoutine()
                    .withReadTimeoutAction(TimeoutActionType.DEADLOCK)
                    .set()
                    .withLoader()
                    .withId(3)
                    .set()
                    .method("getInt")
                    .callAsync()
                    .readNext();

            fail();

        } catch (final NoSuchElementException ignored) {

        }

        assertThat(JRoutine.onFragment(fragment, TestTimeout.class)
                           .withRoutine()
                           .withReadTimeout(seconds(10))
                           .set()
                           .withLoader()
                           .withId(4)
                           .set()
                           .method(TestTimeout.class.getMethod("getInt"))
                           .callAsync()
                           .readNext()).isEqualTo(31);

        try {

            JRoutine.onFragment(fragment, TestTimeout.class)
                    .withRoutine()
                    .withReadTimeoutAction(TimeoutActionType.DEADLOCK)
                    .set()
                    .withLoader()
                    .withId(5)
                    .set()
                    .method(TestTimeout.class.getMethod("getInt"))
                    .callAsync()
                    .readNext();

            fail();

        } catch (final NoSuchElementException ignored) {

        }

        assertThat(JRoutine.onFragment(fragment, TestTimeout.class)
                           .withRoutine()
                           .withReadTimeout(seconds(10))
                           .set()
                           .withLoader()
                           .withId(6)
                           .set()
                           .buildProxy(TestTimeoutItf.class)
                           .getInt()).containsExactly(31);

        try {

            JRoutine.onFragment(fragment, TestTimeout.class)
                    .withRoutine()
                    .withReadTimeoutAction(TimeoutActionType.DEADLOCK)
                    .set()
                    .withLoader()
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
        int add1(@Param(value = char.class, mode = PassMode.VALUE) OutputChannel<Character> c);

        @Alias("a")
        int add2(@Param(value = char.class, mode = PassMode.PARALLEL) OutputChannel<Character> c);

        @Alias("a")
        @Param(value = int.class, mode = PassMode.VALUE)
        OutputChannel<Integer> add3(char c);

        @Alias("a")
        @Param(value = int.class, mode = PassMode.VALUE)
        OutputChannel<Integer> add4(
                @Param(value = char.class, mode = PassMode.VALUE) OutputChannel<Character> c);

        @Alias("a")
        @Param(value = int.class, mode = PassMode.VALUE)
        OutputChannel<Integer> add5(
                @Param(value = char.class, mode = PassMode.PARALLEL) OutputChannel<Character> c);

        @Alias("aa")
        int[] addA00(char[] c);

        @Alias("aa")
        int[] addA01(@Param(value = char[].class,
                mode = PassMode.VALUE) OutputChannel<char[]> c);

        @Alias("aa")
        int[] addA02(@Param(value = char[].class,
                mode = PassMode.COLLECTION) OutputChannel<Character> c);

        @Alias("aa")
        int[] addA03(@Param(value = char[].class,
                mode = PassMode.PARALLEL) OutputChannel<char[]> c);

        @Alias("aa")
        @Param(value = int[].class, mode = PassMode.VALUE)
        OutputChannel<int[]> addA04(char[] c);

        @Alias("aa")
        @Param(value = int[].class, mode = PassMode.VALUE)
        OutputChannel<int[]> addA05(
                @Param(value = char[].class, mode = PassMode.VALUE) OutputChannel<char[]> c);

        @Alias("aa")
        @Param(value = int[].class, mode = PassMode.VALUE)
        OutputChannel<int[]> addA06(@Param(value = char[].class,
                mode = PassMode.COLLECTION) OutputChannel<Character> c);

        @Alias("aa")
        @Param(value = int[].class, mode = PassMode.VALUE)
        OutputChannel<int[]> addA07(@Param(value = char[].class,
                mode = PassMode.PARALLEL) OutputChannel<char[]> c);

        @Alias("aa")
        @Param(value = int[].class, mode = PassMode.COLLECTION)
        OutputChannel<Integer> addA08(char[] c);

        @Alias("aa")
        @Param(value = int[].class, mode = PassMode.COLLECTION)
        OutputChannel<Integer> addA09(
                @Param(value = char[].class, mode = PassMode.VALUE) OutputChannel<char[]> c);

        @Alias("aa")
        @Param(value = int[].class, mode = PassMode.COLLECTION)
        OutputChannel<Integer> addA10(@Param(value = char[].class,
                mode = PassMode.COLLECTION) OutputChannel<Character> c);

        @Alias("aa")
        @Param(value = int[].class, mode = PassMode.COLLECTION)
        OutputChannel<Integer> addA11(@Param(value = char[].class,
                mode = PassMode.PARALLEL) OutputChannel<char[]> c);

        @Alias("aa")
        @Param(value = int[].class, mode = PassMode.PARALLEL)
        List<int[]> addA12(char[] c);

        @Alias("aa")
        @Param(value = int[].class, mode = PassMode.PARALLEL)
        List<int[]> addA13(
                @Param(value = char[].class, mode = PassMode.VALUE) OutputChannel<char[]> c);

        @Alias("aa")
        @Param(value = int[].class, mode = PassMode.PARALLEL)
        List<int[]> addA14(@Param(value = char[].class,
                mode = PassMode.COLLECTION) OutputChannel<Character> c);

        @Alias("aa")
        @Param(value = int[].class, mode = PassMode.PARALLEL)
        List<int[]> addA15(@Param(value = char[].class,
                mode = PassMode.PARALLEL) OutputChannel<char[]> c);

        @Alias("aa")
        @Param(value = int[].class, mode = PassMode.PARALLEL)
        int[][] addA16(char[] c);

        @Alias("aa")
        @Param(value = int[].class, mode = PassMode.PARALLEL)
        int[][] addA17(@Param(value = char[].class, mode = PassMode.VALUE) OutputChannel<char[]> c);

        @Alias("aa")
        @Param(value = int[].class, mode = PassMode.PARALLEL)
        int[][] addA18(@Param(value = char[].class,
                mode = PassMode.COLLECTION) OutputChannel<Character> c);

        @Alias("aa")
        @Param(value = int[].class, mode = PassMode.PARALLEL)
        int[][] addA19(@Param(value = char[].class,
                mode = PassMode.PARALLEL) OutputChannel<char[]> c);

        @Alias("al")
        List<Integer> addL00(List<Character> c);

        @Alias("al")
        List<Integer> addL01(@Param(value = List.class,
                mode = PassMode.VALUE) OutputChannel<List<Character>> c);

        @Alias("al")
        List<Integer> addL02(@Param(value = List.class,
                mode = PassMode.COLLECTION) OutputChannel<Character> c);

        @Alias("al")
        List<Integer> addL03(@Param(value = List.class,
                mode = PassMode.PARALLEL) OutputChannel<List<Character>> c);

        @Alias("al")
        @Param(value = List.class, mode = PassMode.VALUE)
        OutputChannel<List<Integer>> addL04(List<Character> c);

        @Alias("al")
        @Param(value = List.class, mode = PassMode.VALUE)
        OutputChannel<List<Integer>> addL05(@Param(value = List.class,
                mode = PassMode.VALUE) OutputChannel<List<Character>> c);

        @Alias("al")
        @Param(value = List.class, mode = PassMode.VALUE)
        OutputChannel<List<Integer>> addL06(@Param(value = List.class,
                mode = PassMode.COLLECTION) OutputChannel<Character> c);

        @Alias("al")
        @Param(value = List.class, mode = PassMode.VALUE)
        OutputChannel<List<Integer>> addL07(@Param(value = List.class,
                mode = PassMode.PARALLEL) OutputChannel<List<Character>> c);

        @Alias("al")
        @Param(value = List.class, mode = PassMode.COLLECTION)
        OutputChannel<Integer> addL08(List<Character> c);

        @Alias("al")
        @Param(value = List.class, mode = PassMode.COLLECTION)
        OutputChannel<Integer> addL09(@Param(value = List.class,
                mode = PassMode.VALUE) OutputChannel<List<Character>> c);

        @Alias("al")
        @Param(value = List.class, mode = PassMode.COLLECTION)
        OutputChannel<Integer> addL10(@Param(value = List.class,
                mode = PassMode.COLLECTION) OutputChannel<Character> c);

        @Alias("al")
        @Param(value = List.class, mode = PassMode.COLLECTION)
        OutputChannel<Integer> addL11(@Param(value = List.class,
                mode = PassMode.PARALLEL) OutputChannel<List<Character>> c);

        @Alias("al")
        @Param(value = List.class, mode = PassMode.PARALLEL)
        List<List<Integer>> addL12(List<Character> c);

        @Alias("al")
        @Param(value = List.class, mode = PassMode.PARALLEL)
        List<List<Integer>> addL13(@Param(value = List.class,
                mode = PassMode.VALUE) OutputChannel<List<Character>> c);

        @Alias("al")
        @Param(value = List.class, mode = PassMode.PARALLEL)
        List<List<Integer>> addL14(@Param(value = List.class,
                mode = PassMode.COLLECTION) OutputChannel<Character> c);

        @Alias("al")
        @Param(value = List.class, mode = PassMode.PARALLEL)
        List<List<Integer>> addL15(@Param(value = List.class,
                mode = PassMode.PARALLEL) OutputChannel<List<Character>> c);

        @Alias("al")
        @Param(value = List.class, mode = PassMode.PARALLEL)
        List[] addL16(List<Character> c);

        @Alias("al")
        @Param(value = List.class, mode = PassMode.PARALLEL)
        List[] addL17(@Param(value = List.class,
                mode = PassMode.VALUE) OutputChannel<List<Character>> c);

        @Alias("al")
        @Param(value = List.class, mode = PassMode.PARALLEL)
        List[] addL18(@Param(value = List.class,
                mode = PassMode.COLLECTION) OutputChannel<Character> c);

        @Alias("al")
        @Param(value = List.class, mode = PassMode.PARALLEL)
        List[] addL19(@Param(value = List.class,
                mode = PassMode.PARALLEL) OutputChannel<List<Character>> c);

        @Alias("g")
        int get0();

        @Alias("s")
        void set0(int i);

        @Alias("g")
        @Param(value = int.class, mode = PassMode.VALUE)
        OutputChannel<Integer> get1();

        @Alias("s")
        void set1(@Param(value = int.class, mode = PassMode.VALUE) OutputChannel<Integer> i);

        @Alias("ga")
        int[] getA0();

        @Alias("sa")
        void setA0(int[] i);

        @Alias("ga")
        @Param(value = int[].class, mode = PassMode.COLLECTION)
        OutputChannel<Integer> getA1();

        @Alias("sa")
        void setA1(@Param(value = int[].class, mode = PassMode.VALUE) OutputChannel<int[]> i);

        @Alias("ga")
        @Param(value = int[].class, mode = PassMode.PARALLEL)
        List<int[]> getA2();

        @Alias("sa")
        void setA2(
                @Param(value = int[].class, mode = PassMode.COLLECTION) OutputChannel<Integer> i);

        @Alias("ga")
        @Param(value = int[].class, mode = PassMode.PARALLEL)
        int[][] getA3();

        @Alias("sa")
        void setA3(@Param(value = int[].class, mode = PassMode.PARALLEL) OutputChannel<int[]> i);

        @Alias("gl")
        List<Integer> getL0();

        @Alias("sl")
        void setL0(List<Integer> i);

        @Alias("gl")
        @Param(value = List.class, mode = PassMode.COLLECTION)
        OutputChannel<Integer> getL1();

        @Alias("sl")
        void setL1(@Param(value = List.class,
                mode = PassMode.VALUE) OutputChannel<List<Integer>> i);

        @Alias("gl")
        @Param(value = List.class, mode = PassMode.PARALLEL)
        List<List<Integer>> getL2();

        @Alias("sl")
        void setL2(@Param(value = List.class, mode = PassMode.COLLECTION) OutputChannel<Integer> i);

        @Alias("gl")
        @Param(value = List.class, mode = PassMode.PARALLEL)
        List[] getL3();

        @Alias("sl")
        void setL3(@Param(value = List.class,
                mode = PassMode.PARALLEL) OutputChannel<List<Integer>> i);

        @Alias("s")
        void set2(@Param(value = int.class, mode = PassMode.PARALLEL) OutputChannel<Integer> i);
    }

    private interface CountError {

        @Param(int.class)
        String[] count(int length);

        @Alias("count")
        @Param(value = int.class, mode = PassMode.COLLECTION)
        OutputChannel<Integer> count1(int length);

        @Alias("count")
        @Param(value = int.class, mode = PassMode.PARALLEL)
        String[] count2(int length);

        @Param(value = List.class, mode = PassMode.VALUE)
        List<Integer> countList(int length);

        @Alias("countList")
        @Param(value = List.class, mode = PassMode.COLLECTION)
        List<Integer> countList1(int length);

        @Alias("countList")
        @Param(value = List.class, mode = PassMode.PARALLEL)
        OutputChannel<Integer> countList2(int length);
    }

    private interface CountItf {

        @Param(int[].class)
        OutputChannel<Integer> count(int length);

        @Alias("count")
        @Param(value = int[].class, mode = PassMode.VALUE)
        OutputChannel<int[]> count1(int length);

        @Alias("count")
        @Param(value = int[].class, mode = PassMode.COLLECTION)
        OutputChannel<Integer> count2(int length);

        @Param(List.class)
        OutputChannel<Integer> countList(int length);

        @Alias("countList")
        @Param(value = List.class, mode = PassMode.COLLECTION)
        OutputChannel<Integer> countList1(int length);
    }

    private interface IncItf {

        @Timeout(10000)
        @Param(int.class)
        int[] inc(@Param(int.class) int... i);

        @Timeout(10000)
        @Alias("inc")
        @Param(int.class)
        Iterable<Integer> incIterable(@Param(int.class) int... i);
    }

    private interface SquareItf {

        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        int compute(int i);

        @Alias("compute")
        @Param(value = int.class, mode = PassMode.PARALLEL)
        @Timeout(10000)
        int[] compute1(int length);

        @Alias("compute")
        @Param(value = int.class, mode = PassMode.PARALLEL)
        @Timeout(10000)
        List<Integer> compute2(int length);

        @Alias("compute")
        @Timeout(10000)
        int computeAsync(@Param(int.class) OutputChannel<Integer> i);

        @ShareGroup(ShareGroup.NONE)
        @Alias("compute")
        @Param(int.class)
        OutputChannel<Integer> computeParallel1(@Param(int.class) int... i);

        @Alias("compute")
        @Param(int.class)
        OutputChannel<Integer> computeParallel2(@Param(int.class) Integer... i);

        @ShareGroup(ShareGroup.NONE)
        @Alias("compute")
        @Param(int.class)
        OutputChannel<Integer> computeParallel3(@Param(int.class) List<Integer> i);

        @ShareGroup(ShareGroup.NONE)
        @Alias("compute")
        @Param(int.class)
        OutputChannel<Integer> computeParallel4(
                @Param(value = int.class, mode = PassMode.PARALLEL) OutputChannel<Integer> i);
    }

    private interface SumError {

        int compute(int a, @Param(int.class) int[] b);

        int compute(@Param(int.class) String[] ints);

        int compute(@Param(value = int.class, mode = PassMode.VALUE) int[] ints);

        int compute(@Param(value = int.class, mode = PassMode.COLLECTION) Iterable<Integer> ints);

        int compute(@Param(value = int.class,
                mode = PassMode.COLLECTION) OutputChannel<Integer> ints);

        int compute(int a,
                @Param(value = int[].class, mode = PassMode.COLLECTION) OutputChannel<Integer> b);

        int compute(@Param(value = int.class, mode = PassMode.PARALLEL) Object ints);

        int compute(@Param(value = int.class, mode = PassMode.PARALLEL) Object[] ints);

        int compute(String text, @Param(value = int.class, mode = PassMode.PARALLEL) int[] ints);
    }

    private interface SumItf {

        int compute(int a, @Param(int.class) OutputChannel<Integer> b);

        int compute(@Param(int[].class) OutputChannel<Integer> ints);

        @Alias("compute")
        int compute1(@Param(value = int[].class, mode = PassMode.VALUE) OutputChannel<int[]> ints);

        @Alias("compute")
        int computeList(@Param(List.class) OutputChannel<Integer> ints);

        @Alias("compute")
        int computeList1(@Param(value = List.class,
                mode = PassMode.COLLECTION) OutputChannel<Integer> ints);
    }

    @SuppressWarnings("unused")
    private interface TestItf {

        void throwException(@Param(int.class) RuntimeException ex);

        @Alias(TestClass.THROW)
        @Param(int.class)
        void throwException1(RuntimeException ex);

        @Alias(TestClass.THROW)
        int throwException2(RuntimeException ex);
    }

    private interface TestTimeoutItf {

        @Param(int.class)
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
