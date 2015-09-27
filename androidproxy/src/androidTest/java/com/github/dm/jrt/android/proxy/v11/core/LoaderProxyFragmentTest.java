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
package com.github.dm.jrt.android.proxy.v11.core;

import android.annotation.TargetApi;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.proxy.R;
import com.github.dm.jrt.android.proxy.annotation.V11Proxy;
import com.github.dm.jrt.android.proxy.builder.LoaderProxyObjectBuilder;
import com.github.dm.jrt.android.proxy.builder.LoaderProxyRoutineBuilder;
import com.github.dm.jrt.android.v4.core.JRoutine;
import com.github.dm.jrt.annotation.Alias;
import com.github.dm.jrt.annotation.Input;
import com.github.dm.jrt.annotation.Input.InputMode;
import com.github.dm.jrt.annotation.Inputs;
import com.github.dm.jrt.annotation.Invoke;
import com.github.dm.jrt.annotation.Invoke.InvocationMode;
import com.github.dm.jrt.annotation.Output;
import com.github.dm.jrt.annotation.Output.OutputMode;
import com.github.dm.jrt.annotation.Timeout;
import com.github.dm.jrt.annotation.TimeoutAction;
import com.github.dm.jrt.builder.InvocationConfiguration;
import com.github.dm.jrt.builder.InvocationConfiguration.TimeoutActionType;
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
import com.github.dm.jrt.runner.Runner;
import com.github.dm.jrt.runner.Runners;
import com.github.dm.jrt.util.ClassToken;
import com.github.dm.jrt.util.TimeDuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static com.github.dm.jrt.android.core.ContextInvocationTarget.classOfType;
import static com.github.dm.jrt.android.core.ContextInvocationTarget.instanceOf;
import static com.github.dm.jrt.android.v11.core.LoaderContext.contextFrom;
import static com.github.dm.jrt.builder.InvocationConfiguration.builder;
import static com.github.dm.jrt.util.TimeDuration.INFINITY;
import static com.github.dm.jrt.util.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proxy builder fragment unit tests.
 * <p/>
 * Created by davide-maestroni on 05/11/2015.
 */
@TargetApi(VERSION_CODES.HONEYCOMB)
public class LoaderProxyFragmentTest extends ActivityInstrumentationTestCase2<TestActivity> {

    public LoaderProxyFragmentTest() {

        super(TestActivity.class);
    }

    public void testClassStaticMethod() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final TestStatic testStatic = JRoutineProxy.with(contextFrom(fragment))
                                                   .on(classOfType(TestClass.class))
                                                   .invocations()
                                                   .withRunner(Runners.poolRunner())
                                                   .withLogLevel(LogLevel.DEBUG)
                                                   .withLog(new NullLog())
                                                   .set()
                                                   .buildProxy(TestStatic.class);

        try {

            assertThat(testStatic.getOne().all()).containsExactly(1);

            fail();

        } catch (final InvocationException ignored) {

        }

        assertThat(testStatic.getTwo().all()).containsExactly(2);
    }

    public void testGenericProxyCache() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final LoaderProxyRoutineBuilder builder = JRoutineProxy.with(contextFrom(fragment))
                                                               .on(instanceOf(TestList.class))
                                                               .invocations()
                                                               .withExecutionTimeout(seconds(10))
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
        assertThat(testListItf2.getAsync(1).next()).isEqualTo(3);
        assertThat(testListItf2.getList(1)).containsExactly(3);
    }

    public void testInterface() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final ClassToken<TestInterfaceProxy> token = ClassToken.tokenOf(TestInterfaceProxy.class);
        final TestInterfaceProxy testProxy = JRoutineProxy.with(contextFrom(fragment))
                                                          .on(instanceOf(TestClass.class))
                                                          .buildProxy(token);

        assertThat(testProxy.getOne().next()).isEqualTo(1);
    }

    @SuppressWarnings("ConstantConditions")
    public void testNullPointerError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);

        try {

            JRoutineProxy.with(contextFrom(fragment))
                         .on(instanceOf(TestClass.class))
                         .buildProxy((Class<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutineProxy.with(contextFrom(fragment))
                         .on(instanceOf(TestClass.class))
                         .buildProxy((ClassToken<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testObjectStaticMethod() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final TestStatic testStatic = JRoutineProxy.with(contextFrom(fragment))
                                                   .on(instanceOf(TestClass.class))
                                                   .invocations()
                                                   .withRunner(Runners.poolRunner())
                                                   .withLogLevel(LogLevel.DEBUG)
                                                   .withLog(new NullLog())
                                                   .set()
                                                   .buildProxy(TestStatic.class);

        assertThat(testStatic.getOne().all()).containsExactly(1);
        assertThat(testStatic.getTwo().all()).containsExactly(2);
    }

    public void testProxy() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final NullLog log = new NullLog();
        final Runner runner = Runners.poolRunner();
        final TestProxy testProxy = JRoutineProxy.with(contextFrom(fragment))
                                                 .on(instanceOf(TestClass.class))
                                                 .invocations()
                                                 .withRunner(runner)
                                                 .withLogLevel(LogLevel.DEBUG)
                                                 .withLog(log)
                                                 .set()
                                                 .buildProxy(ClassToken.tokenOf(TestProxy.class));

        assertThat(testProxy.getOne().next()).isEqualTo(1);
        assertThat(testProxy.getString(1, 2, 3)).isIn("1", "2", "3");
        assertThat(testProxy.getString(new HashSet<Integer>(Arrays.asList(1, 2, 3)))
                            .all()).containsOnly("1", "2", "3");
        assertThat(testProxy.getString(Arrays.asList(1, 2, 3))).containsOnly("1", "2", "3");
        assertThat(testProxy.getString((Iterable<Integer>) Arrays.asList(1, 2, 3))).containsOnly(
                "1", "2", "3");
        assertThat(testProxy.getString((Collection<Integer>) Arrays.asList(1, 2, 3))).containsOnly(
                "1", "2", "3");

        final ArrayList<String> list = new ArrayList<String>();
        assertThat(testProxy.getList(Collections.singletonList(list))).containsExactly(list);

        final IOChannel<Integer, Integer> ioChannel = JRoutine.io().buildChannel();
        ioChannel.pass(3).close();
        assertThat(testProxy.getString(ioChannel)).isEqualTo("3");
    }

    public void testProxyBuilder() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final NullLog log = new NullLog();
        final Runner runner = Runners.poolRunner();
        final InvocationConfiguration configuration =
                builder().withRunner(runner).withLogLevel(LogLevel.DEBUG).withLog(log).set();
        final LoaderProxyObjectBuilder<TestProxy> builder =
                com.github.dm.jrt.android.proxy.V11Proxy_TestFragment.with(contextFrom(fragment))
                                                                     .on(instanceOf(
                                                                             TestClass.class));
        final TestProxy testProxy = builder.invocations().with(configuration).set().buildProxy();

        assertThat(testProxy.getOne().next()).isEqualTo(1);
        assertThat(testProxy.getString(1, 2, 3)).isIn("1", "2", "3");
        assertThat(testProxy.getString(new HashSet<Integer>(Arrays.asList(1, 2, 3)))
                            .all()).containsOnly("1", "2", "3");
        assertThat(testProxy.getString(Arrays.asList(1, 2, 3))).containsOnly("1", "2", "3");
        assertThat(testProxy.getString((Iterable<Integer>) Arrays.asList(1, 2, 3))).containsOnly(
                "1", "2", "3");
        assertThat(testProxy.getString((Collection<Integer>) Arrays.asList(1, 2, 3))).containsOnly(
                "1", "2", "3");

        final ArrayList<String> list = new ArrayList<String>();
        assertThat(testProxy.getList(Collections.singletonList(list))).containsExactly(list);

        final IOChannel<Integer, Integer> ioChannel = JRoutine.io().buildChannel();
        ioChannel.pass(3).close();
        assertThat(testProxy.getString(ioChannel)).isEqualTo("3");

        assertThat(JRoutineProxy.with(contextFrom(fragment))
                                .on(instanceOf(TestClass.class))
                                .invocations()
                                .with(configuration)
                                .set()
                                .buildProxy(ClassToken.tokenOf(TestProxy.class))).isSameAs(
                testProxy);
    }

    public void testProxyCache() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final NullLog log = new NullLog();
        final Runner runner = Runners.poolRunner();
        final InvocationConfiguration configuration =
                builder().withRunner(runner).withLogLevel(LogLevel.DEBUG).withLog(log).set();
        final TestProxy testProxy = JRoutineProxy.with(contextFrom(fragment))
                                                 .on(instanceOf(TestClass.class))
                                                 .invocations()
                                                 .with(configuration)
                                                 .set()
                                                 .buildProxy(ClassToken.tokenOf(TestProxy.class));

        assertThat(JRoutineProxy.with(contextFrom(fragment))
                                .on(instanceOf(TestClass.class))
                                .invocations()
                                .with(configuration)
                                .set()
                                .buildProxy(ClassToken.tokenOf(TestProxy.class))).isSameAs(
                testProxy);
    }

    public void testProxyError() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);

        try {

            JRoutineProxy.with(contextFrom(fragment))
                         .on(instanceOf(TestClass.class))
                         .buildProxy(TestClass.class);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutineProxy.with(contextFrom(fragment))
                         .on(instanceOf(TestClass.class))
                         .buildProxy(ClassToken.tokenOf(TestClass.class));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testShareGroup() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final LoaderProxyRoutineBuilder builder = JRoutineProxy.with(contextFrom(fragment))
                                                               .on(instanceOf(TestClass2.class))
                                                               .invocations()
                                                               .withExecutionTimeout(seconds(10))
                                                               .set();

        long startTime = System.currentTimeMillis();

        OutputChannel<Integer> getOne = builder.proxies()
                                               .withShareGroup("1")
                                               .set()
                                               .buildProxy(TestClassAsync.class)
                                               .getOne();
        OutputChannel<Integer> getTwo = builder.proxies()
                                               .withShareGroup("2")
                                               .set()
                                               .buildProxy(TestClassAsync.class)
                                               .getTwo();

        assertThat(getOne.next()).isEqualTo(1);
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

        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        final Itf itf = JRoutineProxy.with(contextFrom(fragment))
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
        assertThat(itf.addA22().pass('d', 'e', 'f').result().all()).containsOnly(
                new int[]{'d', 'e', 'f'});
        assertThat(itf.addA23().pass(new char[]{'c', 'z'}).close().all()).containsOnly(
                new int[]{'c', 'z'});
        assertThat(itf.addA24()
                      .pass(new char[]{'d', 'z'}, new char[]{'e', 'z'}, new char[]{'f', 'z'})
                      .close()
                      .all()).containsOnly(new int[]{'d', 'z'}, new int[]{'e', 'z'},
                                           new int[]{'f', 'z'});
        assertThat(itf.addA25().pass('d', 'e', 'f').close().all()).containsOnly(
                new int[]{'d', 'e', 'f'});
        assertThat(itf.addA26().asyncCall(new char[]{'c', 'z'}).all()).containsOnly(
                new int[]{'c', 'z'});
        assertThat(itf.addA27()
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
        assertThat(itf.addL22().pass('d', 'e', 'f').result().all()).containsOnly(
                Arrays.asList((int) 'd', (int) 'e', (int) 'f'));
        assertThat(itf.addL23().pass(Arrays.asList('c', 'z')).close().all()).containsOnly(
                Arrays.asList((int) 'c', (int) 'z'));
        assertThat(itf.addL24()
                      .pass(Arrays.asList('d', 'z'), Arrays.asList('e', 'z'),
                            Arrays.asList('f', 'z'))
                      .close()
                      .all()).containsOnly(Arrays.asList((int) 'd', (int) 'z'),
                                           Arrays.asList((int) 'e', (int) 'z'),
                                           Arrays.asList((int) 'f', (int) 'z'));
        assertThat(itf.addL25().pass('d', 'e', 'f').close().all()).containsOnly(
                Arrays.asList((int) 'd', (int) 'e', (int) 'f'));
        assertThat(itf.addL26().asyncCall(Arrays.asList('c', 'z')).all()).containsOnly(
                Arrays.asList((int) 'c', (int) 'z'));
        assertThat(itf.addL27()
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
        itf.setA5().pass(1, 2, 3).result().checkComplete();
        itf.setA6().pass(new int[]{1, 2, 3}).close().checkComplete();
        itf.setA7().pass(1, 2, 3).close().checkComplete();
        itf.setA8().asyncCall(new int[]{1, 2, 3}).checkComplete();
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
        itf.setL5().pass(1, 2, 3).result().checkComplete();
        itf.setL6().pass(Arrays.asList(1, 2, 3)).close().checkComplete();
        itf.setL7().pass(1, 2, 3).close().checkComplete();
        itf.setL8().asyncCall(Arrays.asList(1, 2, 3)).checkComplete();
    }

    public void testTimeoutActionAnnotation() throws NoSuchMethodException {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            return;
        }

        final TestFragment fragment = (TestFragment) getActivity().getFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        assertThat(JRoutineProxy.with(contextFrom(fragment))
                                .on(instanceOf(TestTimeout.class))
                                .invocations()
                                .withExecutionTimeout(seconds(10))
                                .set()
                                .buildProxy(TestTimeoutItf.class)
                                .getInt()).containsExactly(31);

        try {

            JRoutineProxy.with(contextFrom(fragment))
                         .on(instanceOf(TestTimeout.class))
                         .invocations()
                         .withExecutionTimeoutAction(TimeoutActionType.THROW)
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
        int add1(@Input(value = char.class, mode = InputMode.VALUE) OutputChannel<Character> c);

        @Alias("a")
        @Inputs(value = char.class, mode = InputMode.VALUE)
        Routine<Character, Integer> add10();

        @Alias("a")
        @Invoke(InvocationMode.PARALLEL)
        @Inputs(value = char.class, mode = InputMode.ELEMENT)
        Routine<Character, Integer> add11();

        @Alias("a")
        @Invoke(InvocationMode.PARALLEL)
        int add2(@Input(value = char.class, mode = InputMode.ELEMENT) OutputChannel<Character> c);

        @Alias("a")
        @Output(OutputMode.VALUE)
        OutputChannel<Integer> add3(char c);

        @Alias("a")
        @Output(OutputMode.VALUE)
        OutputChannel<Integer> add4(
                @Input(value = char.class, mode = InputMode.VALUE) OutputChannel<Character> c);

        @Alias("a")
        @Invoke(InvocationMode.PARALLEL)
        @Output(OutputMode.VALUE)
        OutputChannel<Integer> add5(
                @Input(value = char.class, mode = InputMode.ELEMENT) OutputChannel<Character> c);

        @Alias("a")
        @Inputs(value = char.class, mode = InputMode.VALUE)
        InvocationChannel<Character, Integer> add6();

        @Alias("a")
        @Invoke(InvocationMode.PARALLEL)
        @Inputs(value = char.class, mode = InputMode.ELEMENT)
        InvocationChannel<Character, Integer> add7();

        @Alias("a")
        @Inputs(value = char.class, mode = InputMode.VALUE)
        StreamingChannel<Character, Integer> add8();

        @Alias("a")
        @Invoke(InvocationMode.PARALLEL)
        @Inputs(value = char.class, mode = InputMode.ELEMENT)
        StreamingChannel<Character, Integer> add9();

        @Alias("aa")
        int[] addA00(char[] c);

        @Alias("aa")
        int[] addA01(@Input(value = char[].class,
                mode = InputMode.VALUE) OutputChannel<char[]> c);

        @Alias("aa")
        int[] addA02(@Input(value = char[].class,
                mode = InputMode.COLLECTION) OutputChannel<Character> c);

        @Alias("aa")
        @Invoke(InvocationMode.PARALLEL)
        int[] addA03(@Input(value = char[].class,
                mode = InputMode.ELEMENT) OutputChannel<char[]> c);

        @Alias("aa")
        @Output(OutputMode.VALUE)
        OutputChannel<int[]> addA04(char[] c);

        @Alias("aa")
        @Output(OutputMode.VALUE)
        OutputChannel<int[]> addA05(
                @Input(value = char[].class, mode = InputMode.VALUE) OutputChannel<char[]> c);

        @Alias("aa")
        @Output(OutputMode.VALUE)
        OutputChannel<int[]> addA06(@Input(value = char[].class,
                mode = InputMode.COLLECTION) OutputChannel<Character> c);

        @Alias("aa")
        @Invoke(InvocationMode.PARALLEL)
        @Output(OutputMode.VALUE)
        OutputChannel<int[]> addA07(@Input(value = char[].class,
                mode = InputMode.ELEMENT) OutputChannel<char[]> c);

        @Alias("aa")
        @Output(OutputMode.ELEMENT)
        OutputChannel<Integer> addA08(char[] c);

        @Alias("aa")
        @Output(OutputMode.ELEMENT)
        OutputChannel<Integer> addA09(
                @Input(value = char[].class, mode = InputMode.VALUE) OutputChannel<char[]> c);

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
                @Input(value = char[].class, mode = InputMode.VALUE) OutputChannel<char[]> c);

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
                @Input(value = char[].class, mode = InputMode.VALUE) OutputChannel<char[]> c);

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
        @Inputs(value = char[].class, mode = InputMode.VALUE)
        InvocationChannel<char[], int[]> addA20();

        @Alias("aa")
        @Invoke(InvocationMode.PARALLEL)
        @Inputs(value = char[].class, mode = InputMode.ELEMENT)
        InvocationChannel<char[], int[]> addA21();

        @Alias("aa")
        @Inputs(value = char[].class, mode = InputMode.COLLECTION)
        InvocationChannel<Character, int[]> addA22();

        @Alias("aa")
        @Inputs(value = char[].class, mode = InputMode.VALUE)
        StreamingChannel<char[], int[]> addA23();

        @Alias("aa")
        @Invoke(InvocationMode.PARALLEL)
        @Inputs(value = char[].class, mode = InputMode.ELEMENT)
        StreamingChannel<char[], int[]> addA24();

        @Alias("aa")
        @Inputs(value = char[].class, mode = InputMode.COLLECTION)
        StreamingChannel<Character, int[]> addA25();

        @Alias("aa")
        @Inputs(value = char[].class, mode = InputMode.VALUE)
        Routine<char[], int[]> addA26();

        @Alias("aa")
        @Invoke(InvocationMode.PARALLEL)
        @Inputs(value = char[].class, mode = InputMode.ELEMENT)
        Routine<char[], int[]> addA27();

        @Alias("al")
        List<Integer> addL00(List<Character> c);

        @Alias("al")
        List<Integer> addL01(@Input(value = List.class,
                mode = InputMode.VALUE) OutputChannel<List<Character>> c);

        @Alias("al")
        List<Integer> addL02(@Input(value = List.class,
                mode = InputMode.COLLECTION) OutputChannel<Character> c);

        @Alias("al")
        @Invoke(InvocationMode.PARALLEL)
        List<Integer> addL03(@Input(value = List.class,
                mode = InputMode.ELEMENT) OutputChannel<List<Character>> c);

        @Alias("al")
        @Output(OutputMode.VALUE)
        OutputChannel<List<Integer>> addL04(List<Character> c);

        @Alias("al")
        @Output(OutputMode.VALUE)
        OutputChannel<List<Integer>> addL05(@Input(value = List.class,
                mode = InputMode.VALUE) OutputChannel<List<Character>> c);

        @Alias("al")
        @Output(OutputMode.VALUE)
        OutputChannel<List<Integer>> addL06(@Input(value = List.class,
                mode = InputMode.COLLECTION) OutputChannel<Character> c);

        @Alias("al")
        @Invoke(InvocationMode.PARALLEL)
        @Output(OutputMode.VALUE)
        OutputChannel<List<Integer>> addL07(@Input(value = List.class,
                mode = InputMode.ELEMENT) OutputChannel<List<Character>> c);

        @Alias("al")
        @Output(OutputMode.ELEMENT)
        OutputChannel<Integer> addL08(List<Character> c);

        @Alias("al")
        @Output(OutputMode.ELEMENT)
        OutputChannel<Integer> addL09(@Input(value = List.class,
                mode = InputMode.VALUE) OutputChannel<List<Character>> c);

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
                mode = InputMode.VALUE) OutputChannel<List<Character>> c);

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
                mode = InputMode.VALUE) OutputChannel<List<Character>> c);

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
        @Inputs(value = List.class, mode = InputMode.VALUE)
        InvocationChannel<List<Character>, List<Integer>> addL20();

        @Alias("al")
        @Invoke(InvocationMode.PARALLEL)
        @Inputs(value = List.class, mode = InputMode.ELEMENT)
        InvocationChannel<List<Character>, List<Integer>> addL21();

        @Alias("al")
        @Inputs(value = List.class, mode = InputMode.COLLECTION)
        InvocationChannel<Character, List<Integer>> addL22();

        @Alias("al")
        @Inputs(value = List.class, mode = InputMode.VALUE)
        StreamingChannel<List<Character>, List<Integer>> addL23();

        @Alias("al")
        @Invoke(InvocationMode.PARALLEL)
        @Inputs(value = List.class, mode = InputMode.ELEMENT)
        StreamingChannel<List<Character>, List<Integer>> addL24();

        @Alias("al")
        @Inputs(value = List.class, mode = InputMode.COLLECTION)
        StreamingChannel<Character, List<Integer>> addL25();

        @Alias("al")
        @Inputs(value = List.class, mode = InputMode.VALUE)
        Routine<List<Character>, List<Integer>> addL26();

        @Alias("al")
        @Invoke(InvocationMode.PARALLEL)
        @Inputs(value = List.class, mode = InputMode.ELEMENT)
        Routine<List<Character>, List<Integer>> addL27();

        @Alias("g")
        int get0();

        @Alias("s")
        void set0(int i);

        @Alias("g")
        @Output(OutputMode.VALUE)
        OutputChannel<Integer> get1();

        @Alias("s")
        void set1(@Input(value = int.class, mode = InputMode.VALUE) OutputChannel<Integer> i);

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
        void setA1(@Input(value = int[].class, mode = InputMode.VALUE) OutputChannel<int[]> i);

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
                mode = InputMode.VALUE) OutputChannel<List<Integer>> i);

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
        @Inputs(value = int.class, mode = InputMode.VALUE)
        InvocationChannel<Integer, Void> set3();

        @Alias("s")
        @Inputs(value = int.class, mode = InputMode.VALUE)
        StreamingChannel<Integer, Void> set4();

        @Alias("s")
        @Inputs(value = int.class, mode = InputMode.VALUE)
        Routine<Integer, Void> set5();

        @Alias("sa")
        @Inputs(value = int[].class, mode = InputMode.VALUE)
        InvocationChannel<int[], Void> setA4();

        @Alias("sa")
        @Inputs(value = int[].class, mode = InputMode.COLLECTION)
        InvocationChannel<Integer, Void> setA5();

        @Alias("sa")
        @Inputs(value = int[].class, mode = InputMode.VALUE)
        StreamingChannel<int[], Void> setA6();

        @Alias("sa")
        @Inputs(value = int[].class, mode = InputMode.COLLECTION)
        StreamingChannel<Integer, Void> setA7();

        @Alias("sa")
        @Inputs(value = int[].class, mode = InputMode.VALUE)
        Routine<int[], Void> setA8();

        @Alias("sl")
        @Inputs(value = List.class, mode = InputMode.VALUE)
        InvocationChannel<List<Integer>, Void> setL4();

        @Alias("sl")
        @Inputs(value = List.class, mode = InputMode.COLLECTION)
        InvocationChannel<Integer, Void> setL5();

        @Alias("sl")
        @Inputs(value = List.class, mode = InputMode.VALUE)
        StreamingChannel<List<Integer>, Void> setL6();

        @Alias("sl")
        @Inputs(value = List.class, mode = InputMode.COLLECTION)
        StreamingChannel<Integer, Void> setL7();

        @Alias("sl")
        @Inputs(value = List.class, mode = InputMode.VALUE)
        Routine<List<Integer>, Void> setL8();
    }

    @V11Proxy(TestClass2.class)
    public interface TestClassAsync {

        @Output
        OutputChannel<Integer> getOne();

        @Output
        OutputChannel<Integer> getTwo();
    }

    @SuppressWarnings("unused")
    public interface TestClassInterface {

        int getOne();
    }

    @V11Proxy(TestClassInterface.class)
    public interface TestInterfaceProxy {

        @Timeout(3000)
        @Output
        OutputChannel<Integer> getOne();
    }

    @V11Proxy(TestList.class)
    public interface TestListItf<TYPE> {

        void add(Object t);

        TYPE get(int i);

        @Alias("get")
        @Output
        OutputChannel<TYPE> getAsync(int i);

        @Alias("get")
        @Output(OutputMode.COLLECTION)
        List<TYPE> getList(int i);
    }

    @V11Proxy(value = TestClass.class, className = "TestFragment",
            classPackage = "com.github.dm.jrt.android.proxy")
    public interface TestProxy {

        @Timeout(3000)
        @Invoke(InvocationMode.PARALLEL)
        @Output
        Iterable<Iterable> getList(@Input(value = List.class,
                mode = InputMode.ELEMENT) List<? extends List<String>> i);

        @Timeout(3000)
        @Output
        OutputChannel<Integer> getOne();

        @Timeout(3000)
        @Invoke(InvocationMode.PARALLEL)
        String getString(@Input(value = int.class, mode = InputMode.ELEMENT) int... i);

        @Timeout(3000)
        @Invoke(InvocationMode.PARALLEL)
        @Output
        OutputChannel<String> getString(
                @Input(value = int.class, mode = InputMode.ELEMENT) HashSet<Integer> i);

        @Timeout(3000)
        @Invoke(InvocationMode.PARALLEL)
        @Output(OutputMode.COLLECTION)
        List<String> getString(
                @Input(value = int.class, mode = InputMode.ELEMENT) List<Integer> i);

        @Timeout(3000)
        @Invoke(InvocationMode.PARALLEL)
        @Output(OutputMode.COLLECTION)
        Iterable<String> getString(
                @Input(value = int.class, mode = InputMode.ELEMENT) Iterable<Integer> i);

        @Timeout(3000)
        @Invoke(InvocationMode.PARALLEL)
        @Output(OutputMode.COLLECTION)
        String[] getString(
                @Input(value = int.class, mode = InputMode.ELEMENT) Collection<Integer> i);

        @Timeout(3000)
        String getString(@Input(int.class) OutputChannel<Integer> i);
    }

    @V11Proxy(TestClass.class)
    public interface TestStatic {

        @Timeout(3000)
        @Output
        OutputChannel<Integer> getOne();

        @Timeout(3000)
        @Output
        OutputChannel<Integer> getTwo();
    }

    @V11Proxy(TestTimeout.class)
    public interface TestTimeoutItf {

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
}
