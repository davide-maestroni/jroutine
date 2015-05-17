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
package com.gh.bmd.jrt.android.proxy.v11.core;

import android.annotation.TargetApi;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;

import com.gh.bmd.jrt.android.proxy.annotation.V11Proxy;
import com.gh.bmd.jrt.android.proxy.builder.LoaderProxyBuilder;
import com.gh.bmd.jrt.android.proxy.builder.LoaderProxyRoutineBuilder;
import com.gh.bmd.jrt.android.v4.core.JRoutine;
import com.gh.bmd.jrt.annotation.Alias;
import com.gh.bmd.jrt.annotation.Param;
import com.gh.bmd.jrt.annotation.Param.PassMode;
import com.gh.bmd.jrt.annotation.Timeout;
import com.gh.bmd.jrt.annotation.TimeoutAction;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.builder.RoutineConfiguration.TimeoutActionType;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.StandaloneChannel;
import com.gh.bmd.jrt.common.AbortException;
import com.gh.bmd.jrt.common.ClassToken;
import com.gh.bmd.jrt.log.Log;
import com.gh.bmd.jrt.log.Log.LogLevel;
import com.gh.bmd.jrt.log.NullLog;
import com.gh.bmd.jrt.runner.Runner;
import com.gh.bmd.jrt.runner.Runners;
import com.gh.bmd.jrt.time.TimeDuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.gh.bmd.jrt.builder.RoutineConfiguration.builder;
import static com.gh.bmd.jrt.time.TimeDuration.INFINITY;
import static com.gh.bmd.jrt.time.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proxy builder activity unit tests.
 * <p/>
 * Created by davide-maestroni on 07/05/15.
 */
@TargetApi(VERSION_CODES.HONEYCOMB)
public class ContextProxyActivityTest extends ActivityInstrumentationTestCase2<TestActivity> {

    public ContextProxyActivityTest() {

        super(TestActivity.class);
    }

    public void testGenericProxyCache() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final LoaderProxyRoutineBuilder builder =
                JRoutineProxy.onActivity(getActivity(), TestList.class)
                             .withRoutine()
                             .withReadTimeout(seconds(10))
                             .set();

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
        assertThat(testListItf2.getAsync(1).readNext()).isEqualTo(3);
        assertThat(testListItf2.getList(1)).containsExactly(3);
    }

    public void testInterface() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final ClassToken<TestInterfaceProxy> token = ClassToken.tokenOf(TestInterfaceProxy.class);
        final TestInterfaceProxy testProxy =
                JRoutineProxy.onActivity(getActivity(), TestClass.class)
                             .withRoutine()
                             .withSyncRunner(Runners.sequentialRunner())
                             .set()
                             .buildProxy(token);

        assertThat(testProxy.getOne().readNext()).isEqualTo(1);
    }

    @SuppressWarnings("ConstantConditions")
    public void testNullPointerError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        try {

            JRoutineProxy.onActivity(getActivity(), TestClass.class).buildProxy((Class<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutineProxy.onActivity(getActivity(), TestClass.class)
                         .buildProxy((ClassToken<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testProxy() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final NullLog log = new NullLog();
        final Runner runner = Runners.poolRunner();
        final TestProxy testProxy = JRoutineProxy.onActivity(getActivity(), TestClass.class)
                                                 .withRoutine()
                                                 .withSyncRunner(Runners.sequentialRunner())
                                                 .withAsyncRunner(runner)
                                                 .withLogLevel(LogLevel.DEBUG)
                                                 .withLog(log)
                                                 .set()
                                                 .buildProxy(ClassToken.tokenOf(TestProxy.class));

        assertThat(testProxy.getOne().readNext()).isEqualTo(1);
        assertThat(testProxy.getString(1, 2, 3)).isIn("1", "2", "3");
        assertThat(testProxy.getString(new HashSet<Integer>(Arrays.asList(1, 2, 3)))
                            .readAll()).containsOnly("1", "2", "3");
        assertThat(testProxy.getString(Arrays.asList(1, 2, 3))).containsOnly("1", "2", "3");
        assertThat(testProxy.getString((Iterable<Integer>) Arrays.asList(1, 2, 3))).containsOnly(
                "1", "2", "3");
        assertThat(testProxy.getString((Collection<Integer>) Arrays.asList(1, 2, 3))).containsOnly(
                "1", "2", "3");

        final ArrayList<String> list = new ArrayList<String>();
        assertThat(testProxy.getList(Collections.singletonList(list))).containsExactly(list);

        final StandaloneChannel<Integer> standaloneChannel = JRoutine.standalone().buildChannel();
        standaloneChannel.input().pass(3).close();
        assertThat(testProxy.getString(standaloneChannel.output())).isEqualTo("3");
    }

    public void testProxyBuilder() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final NullLog log = new NullLog();
        final Runner runner = Runners.poolRunner();
        final RoutineConfiguration configuration =
                builder().withSyncRunner(Runners.sequentialRunner())
                         .withAsyncRunner(runner)
                         .withLogLevel(LogLevel.DEBUG)
                         .withLog(log)
                         .set();
        final LoaderProxyBuilder<TestProxy> builder =
                com.gh.bmd.jrt.android.proxy.V11Proxy_TestActivity.onActivity(getActivity(),
                                                                              TestClass.class);
        final TestProxy testProxy = builder.withRoutine().with(configuration).set().buildProxy();

        assertThat(testProxy.getOne().readNext()).isEqualTo(1);
        assertThat(testProxy.getString(1, 2, 3)).isIn("1", "2", "3");
        assertThat(testProxy.getString(new HashSet<Integer>(Arrays.asList(1, 2, 3)))
                            .readAll()).containsOnly("1", "2", "3");
        assertThat(testProxy.getString(Arrays.asList(1, 2, 3))).containsOnly("1", "2", "3");
        assertThat(testProxy.getString((Iterable<Integer>) Arrays.asList(1, 2, 3))).containsOnly(
                "1", "2", "3");
        assertThat(testProxy.getString((Collection<Integer>) Arrays.asList(1, 2, 3))).containsOnly(
                "1", "2", "3");

        final ArrayList<String> list = new ArrayList<String>();
        assertThat(testProxy.getList(Collections.singletonList(list))).containsExactly(list);

        final StandaloneChannel<Integer> standaloneChannel = JRoutine.standalone().buildChannel();
        standaloneChannel.input().pass(3).close();
        assertThat(testProxy.getString(standaloneChannel.output())).isEqualTo("3");

        assertThat(JRoutineProxy.onActivity(getActivity(), TestClass.class)
                                .withRoutine()
                                .with(configuration)
                                .set()
                                .buildProxy(ClassToken.tokenOf(TestProxy.class))).isSameAs(
                testProxy);
    }

    public void testProxyCache() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final NullLog log = new NullLog();
        final Runner runner = Runners.poolRunner();
        final RoutineConfiguration configuration =
                builder().withSyncRunner(Runners.sequentialRunner())
                         .withAsyncRunner(runner)
                         .withLogLevel(LogLevel.DEBUG)
                         .withLog(log)
                         .set();
        final TestProxy testProxy = JRoutineProxy.onActivity(getActivity(), TestClass.class)
                                                 .withRoutine()
                                                 .with(configuration)
                                                 .set()
                                                 .buildProxy(ClassToken.tokenOf(TestProxy.class));

        assertThat(JRoutineProxy.onActivity(getActivity(), TestClass.class)
                                .withRoutine()
                                .with(configuration)
                                .set()
                                .buildProxy(ClassToken.tokenOf(TestProxy.class))).isSameAs(
                testProxy);
    }

    public void testProxyError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        try {

            JRoutineProxy.onActivity(getActivity(), TestClass.class).buildProxy(TestClass.class);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutineProxy.onActivity(getActivity(), TestClass.class)
                         .buildProxy(ClassToken.tokenOf(TestClass.class));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testShareGroup() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final LoaderProxyRoutineBuilder builder =
                JRoutineProxy.onActivity(getActivity(), TestClass2.class)
                             .withRoutine()
                             .withReadTimeout(seconds(10))
                             .set();

        long startTime = System.currentTimeMillis();

        OutputChannel<Integer> getOne = builder.withProxy()
                                               .withShareGroup("1")
                                               .set()
                                               .buildProxy(TestClassAsync.class)
                                               .getOne();
        OutputChannel<Integer> getTwo = builder.withProxy()
                                               .withShareGroup("2")
                                               .set()
                                               .buildProxy(TestClassAsync.class)
                                               .getTwo();

        assertThat(getOne.readNext()).isEqualTo(1);
        assertThat(getOne.checkComplete()).isTrue();
        assertThat(getTwo.checkComplete()).isTrue();
        assertThat(System.currentTimeMillis() - startTime).isLessThan(2000);

        startTime = System.currentTimeMillis();

        getOne = builder.buildProxy(TestClassAsync.class).getOne();
        getTwo = builder.buildProxy(TestClassAsync.class).getTwo();

        assertThat(getOne.checkComplete()).isTrue();
        assertThat(getTwo.checkComplete()).isTrue();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(2000);
    }

    @SuppressWarnings("unchecked")
    public void testTemplates() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final Itf itf = JRoutineProxy.onActivity(getActivity(), Impl.class)
                                     .withRoutine()
                                     .withReadTimeout(INFINITY)
                                     .set()
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

    public void testTimeoutActionAnnotation() throws NoSuchMethodException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        assertThat(JRoutineProxy.onActivity(getActivity(), TestTimeout.class)
                                .withRoutine()
                                .withReadTimeout(seconds(10))
                                .set()
                                .buildProxy(TestTimeoutItf.class)
                                .getInt()).containsExactly(31);

        try {

            JRoutineProxy.onActivity(getActivity(), TestTimeout.class)
                         .withRoutine()
                         .withReadTimeoutAction(TimeoutActionType.DEADLOCK)
                         .set()
                         .buildProxy(TestTimeoutItf.class)
                         .getInt();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @V11Proxy(Impl.class)
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

    @V11Proxy(TestClass2.class)
    public interface TestClassAsync {

        @Param(int.class)
        OutputChannel<Integer> getOne();

        @Param(int.class)
        OutputChannel<Integer> getTwo();
    }

    @SuppressWarnings("unused")
    public interface TestClassInterface {

        int getOne();
    }

    @V11Proxy(TestClassInterface.class)
    public interface TestInterfaceProxy {

        @Timeout(3000)
        @Param(int.class)
        OutputChannel<Integer> getOne();
    }

    @V11Proxy(TestList.class)
    public interface TestListItf<TYPE> {

        void add(Object t);

        TYPE get(int i);

        @Alias("get")
        @Param(Object.class)
        OutputChannel<TYPE> getAsync(int i);

        @Alias("get")
        @Param(Object.class)
        List<TYPE> getList(int i);
    }

    @V11Proxy(value = TestClass.class, generatedClassName = "TestActivity",
            generatedClassPackage = "com.gh.bmd.jrt.android.proxy")
    public interface TestProxy {

        @Timeout(3000)
        @Param(List.class)
        Iterable<Iterable> getList(@Param(List.class) List<? extends List<String>> i);

        @Timeout(3000)
        @Param(int.class)
        OutputChannel<Integer> getOne();

        @Timeout(3000)
        String getString(@Param(int.class) int... i);

        @Timeout(3000)
        @Param(String.class)
        OutputChannel<String> getString(@Param(int.class) HashSet<Integer> i);

        @Timeout(3000)
        @Param(String.class)
        List<String> getString(@Param(int.class) List<Integer> i);

        @Timeout(3000)
        @Param(String.class)
        Iterable<String> getString(@Param(int.class) Iterable<Integer> i);

        @Timeout(3000)
        @Param(String.class)
        String[] getString(@Param(int.class) Collection<Integer> i);

        @Timeout(3000)
        String getString(@Param(int.class) OutputChannel<Integer> i);
    }

    @V11Proxy(TestTimeout.class)
    public interface TestTimeoutItf {

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
    public static class TestClass implements TestClassInterface {

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

            TimeDuration.millis(1000).sleepAtLeast();

            return 1;
        }

        public int getTwo() throws InterruptedException {

            TimeDuration.millis(1000).sleepAtLeast();

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
}