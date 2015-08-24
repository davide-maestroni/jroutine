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
package com.gh.bmd.jrt.android.proxy.core;

import android.annotation.TargetApi;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;

import com.gh.bmd.jrt.android.core.JRoutine;
import com.gh.bmd.jrt.android.proxy.annotation.ServiceProxy;
import com.gh.bmd.jrt.android.proxy.builder.ServiceProxyBuilder;
import com.gh.bmd.jrt.android.proxy.builder.ServiceProxyRoutineBuilder;
import com.gh.bmd.jrt.annotation.Alias;
import com.gh.bmd.jrt.annotation.Input;
import com.gh.bmd.jrt.annotation.Input.InputMode;
import com.gh.bmd.jrt.annotation.Inputs;
import com.gh.bmd.jrt.annotation.Output;
import com.gh.bmd.jrt.annotation.Output.OutputMode;
import com.gh.bmd.jrt.annotation.Timeout;
import com.gh.bmd.jrt.annotation.TimeoutAction;
import com.gh.bmd.jrt.builder.InvocationConfiguration;
import com.gh.bmd.jrt.builder.InvocationConfiguration.TimeoutActionType;
import com.gh.bmd.jrt.channel.AbortException;
import com.gh.bmd.jrt.channel.InvocationChannel;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.TransportChannel;
import com.gh.bmd.jrt.invocation.InvocationException;
import com.gh.bmd.jrt.log.Log;
import com.gh.bmd.jrt.log.Log.LogLevel;
import com.gh.bmd.jrt.log.NullLog;
import com.gh.bmd.jrt.routine.Routine;
import com.gh.bmd.jrt.runner.Runner;
import com.gh.bmd.jrt.runner.RunnerDecorator;
import com.gh.bmd.jrt.runner.Runners;
import com.gh.bmd.jrt.util.ClassToken;
import com.gh.bmd.jrt.util.TimeDuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.gh.bmd.jrt.android.core.ContextInvocationTarget.targetClass;
import static com.gh.bmd.jrt.android.core.ContextInvocationTarget.targetObject;
import static com.gh.bmd.jrt.android.core.ServiceContext.serviceFrom;
import static com.gh.bmd.jrt.builder.InvocationConfiguration.builder;
import static com.gh.bmd.jrt.util.TimeDuration.INFINITY;
import static com.gh.bmd.jrt.util.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proxy builder activity unit tests.
 * <p/>
 * Created by davide-maestroni on 07/05/15.
 */
@TargetApi(VERSION_CODES.FROYO)
public class ServiceProxyActivityTest extends ActivityInstrumentationTestCase2<TestActivity> {

    public ServiceProxyActivityTest() {

        super(TestActivity.class);
    }

    public void testClassStaticMethod() {

        final TestStatic testStatic =
                JRoutineProxy.on(serviceFrom(getActivity(), TestService.class),
                                 targetClass(TestClass.class))
                             .invocations()
                             .withSyncRunner(Runners.sequentialRunner())
                             .withAsyncRunner(Runners.poolRunner())
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

        final ServiceProxyRoutineBuilder builder =
                JRoutineProxy.on(serviceFrom(getActivity(), TestService.class),
                                 targetObject(TestList.class))
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

        final ClassToken<TestInterfaceProxy> token = ClassToken.tokenOf(TestInterfaceProxy.class);
        final TestInterfaceProxy testProxy =
                JRoutineProxy.on(serviceFrom(getActivity(), TestService.class),
                                 targetObject(TestClass.class))
                             .invocations()
                             .withSyncRunner(Runners.sequentialRunner())
                             .set()
                             .buildProxy(token);

        assertThat(testProxy.getOne().next()).isEqualTo(1);
    }

    @SuppressWarnings("ConstantConditions")
    public void testNullPointerError() {

        try {

            JRoutineProxy.on(serviceFrom(getActivity()), targetObject(TestClass.class))
                         .buildProxy((Class<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutineProxy.on(serviceFrom(getActivity()), targetObject(TestClass.class))
                         .buildProxy((ClassToken<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testObjectStaticMethod() {

        final TestStatic testStatic =
                JRoutineProxy.on(serviceFrom(getActivity(), TestService.class),
                                 targetObject(TestClass.class))
                             .invocations()
                             .withSyncRunner(Runners.sequentialRunner())
                             .withAsyncRunner(Runners.poolRunner())
                             .withLogLevel(LogLevel.DEBUG)
                             .withLog(new NullLog())
                             .set()
                             .buildProxy(TestStatic.class);

        assertThat(testStatic.getOne().all()).containsExactly(1);
        assertThat(testStatic.getTwo().all()).containsExactly(2);
    }

    public void testProxy() {

        final NullLog log = new NullLog();
        final TestProxy testProxy = JRoutineProxy.on(serviceFrom(getActivity(), TestService.class),
                                                     targetObject(TestClass.class))
                                                 .invocations()
                                                 .withSyncRunner(Runners.sequentialRunner())
                                                 .withAsyncRunner(Runners.poolRunner())
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

        final TransportChannel<Integer> transportChannel = JRoutine.transport().buildChannel();
        transportChannel.pass(3).close();
        assertThat(testProxy.getString(transportChannel)).isEqualTo("3");
    }

    public void testProxyBuilder() {

        final NullLog log = new NullLog();
        final InvocationConfiguration configuration =
                builder().withSyncRunner(Runners.sequentialRunner())
                         .withLogLevel(LogLevel.DEBUG)
                         .withLog(log)
                         .set();
        final ServiceProxyBuilder<TestProxy> builder =
                com.gh.bmd.jrt.android.proxy.ServiceProxy_Test.on(
                        serviceFrom(getActivity(), TestService.class),
                        targetObject(TestClass.class));
        final TestProxy testProxy = builder.invocations()
                                           .with(configuration)
                                           .set()
                                           .service()
                                           .withRunnerClass(MyRunner.class)
                                           .set()
                                           .buildProxy();

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

        final TransportChannel<Integer> transportChannel = JRoutine.transport().buildChannel();
        transportChannel.pass(3).close();
        assertThat(testProxy.getString(transportChannel)).isEqualTo("3");

        assertThat(JRoutineProxy.on(serviceFrom(getActivity(), TestService.class),
                                    targetObject(TestClass.class))
                                .invocations()
                                .with(configuration)
                                .set()
                                .service()
                                .withRunnerClass(MyRunner.class)
                                .set()
                                .buildProxy(ClassToken.tokenOf(TestProxy.class))).isSameAs(
                testProxy);
    }

    public void testProxyCache() {

        final NullLog log = new NullLog();
        final Runner runner = Runners.poolRunner();
        final InvocationConfiguration configuration =
                builder().withSyncRunner(Runners.sequentialRunner())
                         .withAsyncRunner(runner)
                         .withLogLevel(LogLevel.DEBUG)
                         .withLog(log)
                         .set();
        final TestProxy testProxy = JRoutineProxy.on(serviceFrom(getActivity(), TestService.class),
                                                     targetObject(TestClass.class))
                                                 .invocations()
                                                 .with(configuration)
                                                 .set()
                                                 .buildProxy(ClassToken.tokenOf(TestProxy.class));

        assertThat(JRoutineProxy.on(serviceFrom(getActivity(), TestService.class),
                                    targetObject(TestClass.class))
                                .invocations()
                                .with(configuration)
                                .set()
                                .buildProxy(ClassToken.tokenOf(TestProxy.class))).isSameAs(
                testProxy);
    }

    public void testProxyError() {

        try {

            JRoutineProxy.on(serviceFrom(getActivity()), targetObject(TestClass.class))
                         .buildProxy(TestClass.class);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutineProxy.on(serviceFrom(getActivity()), targetObject(TestClass.class))
                         .buildProxy(ClassToken.tokenOf(TestClass.class));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testShareGroup() {

        final ServiceProxyRoutineBuilder builder =
                JRoutineProxy.on(serviceFrom(getActivity(), TestService.class),
                                 targetObject(TestClass2.class))
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

        final Itf itf = JRoutineProxy.on(serviceFrom(getActivity(), TestService.class),
                                         targetObject(Impl.class))
                                     .invocations()
                                     .withExecutionTimeout(INFINITY)
                                     .set()
                                     .buildProxy(Itf.class);

        assertThat(itf.add0('c')).isEqualTo((int) 'c');
        final TransportChannel<Character> channel1 = JRoutine.transport().buildChannel();
        channel1.pass('a').close();
        assertThat(itf.add1(channel1)).isEqualTo((int) 'a');
        final TransportChannel<Character> channel2 = JRoutine.transport().buildChannel();
        channel2.pass('d', 'e', 'f').close();
        assertThat(itf.add2(channel2)).isIn((int) 'd', (int) 'e', (int) 'f');
        assertThat(itf.add3('c').all()).containsExactly((int) 'c');
        final TransportChannel<Character> channel3 = JRoutine.transport().buildChannel();
        channel3.pass('a').close();
        assertThat(itf.add4(channel3).all()).containsExactly((int) 'a');
        final TransportChannel<Character> channel4 = JRoutine.transport().buildChannel();
        channel4.pass('d', 'e', 'f').close();
        assertThat(itf.add5(channel4).all()).containsOnly((int) 'd', (int) 'e', (int) 'f');
        assertThat(itf.add6().pass('d').result().all()).containsOnly((int) 'd');
        assertThat(itf.add7().pass('d', 'e', 'f').result().all()).containsOnly((int) 'd', (int) 'e',
                                                                               (int) 'f');
        assertThat(itf.add8().asyncCall('d').all()).containsOnly((int) 'd');
        assertThat(itf.add9().parallelCall('d', 'e', 'f').all()).containsOnly((int) 'd', (int) 'e',
                                                                              (int) 'f');
        assertThat(itf.addA00(new char[]{'c', 'z'})).isEqualTo(new int[]{'c', 'z'});
        final TransportChannel<char[]> channel5 = JRoutine.transport().buildChannel();
        channel5.pass(new char[]{'a', 'z'}).close();
        assertThat(itf.addA01(channel5)).isEqualTo(new int[]{'a', 'z'});
        final TransportChannel<Character> channel6 = JRoutine.transport().buildChannel();
        channel6.pass('d', 'e', 'f').close();
        assertThat(itf.addA02(channel6)).isEqualTo(new int[]{'d', 'e', 'f'});
        final TransportChannel<char[]> channel7 = JRoutine.transport().buildChannel();
        channel7.pass(new char[]{'d', 'z'}, new char[]{'e', 'z'}, new char[]{'f', 'z'}).close();
        assertThat(itf.addA03(channel7)).isIn(new int[]{'d', 'z'}, new int[]{'e', 'z'},
                                              new int[]{'f', 'z'});
        assertThat(itf.addA04(new char[]{'c', 'z'}).all()).containsExactly(new int[]{'c', 'z'});
        final TransportChannel<char[]> channel8 = JRoutine.transport().buildChannel();
        channel8.pass(new char[]{'a', 'z'}).close();
        assertThat(itf.addA05(channel8).all()).containsExactly(new int[]{'a', 'z'});
        final TransportChannel<Character> channel9 = JRoutine.transport().buildChannel();
        channel9.pass('d', 'e', 'f').close();
        assertThat(itf.addA06(channel9).all()).containsExactly(new int[]{'d', 'e', 'f'});
        final TransportChannel<char[]> channel10 = JRoutine.transport().buildChannel();
        channel10.pass(new char[]{'d', 'z'}, new char[]{'e', 'z'}, new char[]{'f', 'z'}).close();
        assertThat(itf.addA07(channel10).all()).containsOnly(new int[]{'d', 'z'},
                                                             new int[]{'e', 'z'},
                                                             new int[]{'f', 'z'});
        assertThat(itf.addA08(new char[]{'c', 'z'}).all()).containsExactly((int) 'c', (int) 'z');
        final TransportChannel<char[]> channel11 = JRoutine.transport().buildChannel();
        channel11.pass(new char[]{'a', 'z'}).close();
        assertThat(itf.addA09(channel11).all()).containsExactly((int) 'a', (int) 'z');
        final TransportChannel<Character> channel12 = JRoutine.transport().buildChannel();
        channel12.pass('d', 'e', 'f').close();
        assertThat(itf.addA10(channel12).all()).containsExactly((int) 'd', (int) 'e', (int) 'f');
        final TransportChannel<char[]> channel13 = JRoutine.transport().buildChannel();
        channel13.pass(new char[]{'d', 'z'}, new char[]{'e', 'z'}, new char[]{'f', 'z'}).close();
        assertThat(itf.addA11(channel13).all()).containsOnly((int) 'd', (int) 'e', (int) 'f',
                                                             (int) 'z');
        assertThat(itf.addA12(new char[]{'c', 'z'})).containsExactly(new int[]{'c', 'z'});
        final TransportChannel<char[]> channel14 = JRoutine.transport().buildChannel();
        channel14.pass(new char[]{'a', 'z'}).close();
        assertThat(itf.addA13(channel14)).containsExactly(new int[]{'a', 'z'});
        final TransportChannel<Character> channel15 = JRoutine.transport().buildChannel();
        channel15.pass('d', 'e', 'f').close();
        assertThat(itf.addA14(channel15)).containsExactly(new int[]{'d', 'e', 'f'});
        final TransportChannel<char[]> channel16 = JRoutine.transport().buildChannel();
        channel16.pass(new char[]{'d', 'z'}, new char[]{'e', 'z'}, new char[]{'f', 'z'}).close();
        assertThat(itf.addA15(channel16)).containsOnly(new int[]{'d', 'z'}, new int[]{'e', 'z'},
                                                       new int[]{'f', 'z'});
        assertThat(itf.addA16(new char[]{'c', 'z'})).containsExactly(new int[]{'c', 'z'});
        final TransportChannel<char[]> channel17 = JRoutine.transport().buildChannel();
        channel17.pass(new char[]{'a', 'z'}).close();
        assertThat(itf.addA17(channel17)).containsExactly(new int[]{'a', 'z'});
        final TransportChannel<Character> channel18 = JRoutine.transport().buildChannel();
        channel18.pass('d', 'e', 'f').close();
        assertThat(itf.addA18(channel18)).containsExactly(new int[]{'d', 'e', 'f'});
        final TransportChannel<char[]> channel19 = JRoutine.transport().buildChannel();
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
        assertThat(itf.addA23().asyncCall(new char[]{'c', 'z'}).all()).containsOnly(
                new int[]{'c', 'z'});
        assertThat(itf.addA24()
                      .parallelCall(new char[]{'d', 'z'}, new char[]{'e', 'z'},
                                    new char[]{'f', 'z'})
                      .all()).containsOnly(new int[]{'d', 'z'}, new int[]{'e', 'z'},
                                           new int[]{'f', 'z'});
        assertThat(itf.addL00(Arrays.asList('c', 'z'))).isEqualTo(
                Arrays.asList((int) 'c', (int) 'z'));
        final TransportChannel<List<Character>> channel20 = JRoutine.transport().buildChannel();
        channel20.pass(Arrays.asList('a', 'z')).close();
        assertThat(itf.addL01(channel20)).isEqualTo(Arrays.asList((int) 'a', (int) 'z'));
        final TransportChannel<Character> channel21 = JRoutine.transport().buildChannel();
        channel21.pass('d', 'e', 'f').close();
        assertThat(itf.addL02(channel21)).isEqualTo(Arrays.asList((int) 'd', (int) 'e', (int) 'f'));
        final TransportChannel<List<Character>> channel22 = JRoutine.transport().buildChannel();
        channel22.pass(Arrays.asList('d', 'z'), Arrays.asList('e', 'z'), Arrays.asList('f', 'z'))
                 .close();
        assertThat(itf.addL03(channel22)).isIn(Arrays.asList((int) 'd', (int) 'z'),
                                               Arrays.asList((int) 'e', (int) 'z'),
                                               Arrays.asList((int) 'f', (int) 'z'));
        assertThat(itf.addL04(Arrays.asList('c', 'z')).all()).containsExactly(
                Arrays.asList((int) 'c', (int) 'z'));
        final TransportChannel<List<Character>> channel23 = JRoutine.transport().buildChannel();
        channel23.pass(Arrays.asList('a', 'z')).close();
        assertThat(itf.addL05(channel23).all()).containsExactly(
                Arrays.asList((int) 'a', (int) 'z'));
        final TransportChannel<Character> channel24 = JRoutine.transport().buildChannel();
        channel24.pass('d', 'e', 'f').close();
        assertThat(itf.addL06(channel24).all()).containsExactly(
                Arrays.asList((int) 'd', (int) 'e', (int) 'f'));
        final TransportChannel<List<Character>> channel25 = JRoutine.transport().buildChannel();
        channel25.pass(Arrays.asList('d', 'z'), Arrays.asList('e', 'z'), Arrays.asList('f', 'z'))
                 .close();
        assertThat(itf.addL07(channel25).all()).containsOnly(Arrays.asList((int) 'd', (int) 'z'),
                                                             Arrays.asList((int) 'e', (int) 'z'),
                                                             Arrays.asList((int) 'f', (int) 'z'));
        assertThat(itf.addL08(Arrays.asList('c', 'z')).all()).containsExactly((int) 'c', (int) 'z');
        final TransportChannel<List<Character>> channel26 = JRoutine.transport().buildChannel();
        channel26.pass(Arrays.asList('a', 'z')).close();
        assertThat(itf.addL09(channel26).all()).containsExactly((int) 'a', (int) 'z');
        final TransportChannel<Character> channel27 = JRoutine.transport().buildChannel();
        channel27.pass('d', 'e', 'f').close();
        assertThat(itf.addL10(channel27).all()).containsExactly((int) 'd', (int) 'e', (int) 'f');
        final TransportChannel<List<Character>> channel28 = JRoutine.transport().buildChannel();
        channel28.pass(Arrays.asList('d', 'z'), Arrays.asList('e', 'z'), Arrays.asList('f', 'z'))
                 .close();
        assertThat(itf.addL11(channel28).all()).containsOnly((int) 'd', (int) 'e', (int) 'f',
                                                             (int) 'z');
        assertThat(itf.addL12(Arrays.asList('c', 'z'))).containsExactly(
                Arrays.asList((int) 'c', (int) 'z'));
        final TransportChannel<List<Character>> channel29 = JRoutine.transport().buildChannel();
        channel29.pass(Arrays.asList('a', 'z')).close();
        assertThat(itf.addL13(channel29)).containsExactly(Arrays.asList((int) 'a', (int) 'z'));
        final TransportChannel<Character> channel30 = JRoutine.transport().buildChannel();
        channel30.pass('d', 'e', 'f').close();
        assertThat(itf.addL14(channel30)).containsExactly(
                Arrays.asList((int) 'd', (int) 'e', (int) 'f'));
        final TransportChannel<List<Character>> channel31 = JRoutine.transport().buildChannel();
        channel31.pass(Arrays.asList('d', 'z'), Arrays.asList('e', 'z'), Arrays.asList('f', 'z'))
                 .close();
        assertThat(itf.addL15(channel31)).containsOnly(Arrays.asList((int) 'd', (int) 'z'),
                                                       Arrays.asList((int) 'e', (int) 'z'),
                                                       Arrays.asList((int) 'f', (int) 'z'));
        assertThat(itf.addL16(Arrays.asList('c', 'z'))).containsExactly(
                Arrays.asList((int) 'c', (int) 'z'));
        final TransportChannel<List<Character>> channel32 = JRoutine.transport().buildChannel();
        channel32.pass(Arrays.asList('a', 'z')).close();
        assertThat(itf.addL17(channel32)).containsExactly(Arrays.asList((int) 'a', (int) 'z'));
        final TransportChannel<Character> channel33 = JRoutine.transport().buildChannel();
        channel33.pass('d', 'e', 'f').close();
        assertThat(itf.addL18(channel33)).containsExactly(
                Arrays.asList((int) 'd', (int) 'e', (int) 'f'));
        final TransportChannel<List<Character>> channel34 = JRoutine.transport().buildChannel();
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
        assertThat(itf.addL23().asyncCall(Arrays.asList('c', 'z')).all()).containsOnly(
                Arrays.asList((int) 'c', (int) 'z'));
        assertThat(itf.addL24()
                      .parallelCall(Arrays.asList('d', 'z'), Arrays.asList('e', 'z'),
                                    Arrays.asList('f', 'z'))
                      .all()).containsOnly(Arrays.asList((int) 'd', (int) 'z'),
                                           Arrays.asList((int) 'e', (int) 'z'),
                                           Arrays.asList((int) 'f', (int) 'z'));
        assertThat(itf.get0()).isEqualTo(31);
        assertThat(itf.get1().all()).containsExactly(31);
        assertThat(itf.get2().result().all()).containsExactly(31);
        assertThat(itf.get3().asyncCall().all()).containsExactly(31);
        assertThat(itf.getA0()).isEqualTo(new int[]{1, 2, 3});
        assertThat(itf.getA1().all()).containsExactly(1, 2, 3);
        assertThat(itf.getA2()).containsExactly(new int[]{1, 2, 3});
        assertThat(itf.getA3()).containsExactly(new int[]{1, 2, 3});
        assertThat(itf.getA4().result().all()).containsExactly(new int[]{1, 2, 3});
        assertThat(itf.getA5().asyncCall().all()).containsExactly(new int[]{1, 2, 3});
        assertThat(itf.getL0()).isEqualTo(Arrays.asList(1, 2, 3));
        assertThat(itf.getL1().all()).containsExactly(1, 2, 3);
        assertThat(itf.getL2()).containsExactly(Arrays.asList(1, 2, 3));
        assertThat(itf.getL3()).containsExactly(Arrays.asList(1, 2, 3));
        assertThat(itf.getL4().result().all()).containsExactly(Arrays.asList(1, 2, 3));
        assertThat(itf.getL5().asyncCall().all()).containsExactly(Arrays.asList(1, 2, 3));
        itf.set0(-17);
        final TransportChannel<Integer> channel35 = JRoutine.transport().buildChannel();
        channel35.pass(-17).close();
        itf.set1(channel35);
        final TransportChannel<Integer> channel36 = JRoutine.transport().buildChannel();
        channel36.pass(-17).close();
        itf.set2(channel36);
        itf.set3().pass(-17).result().checkComplete();
        itf.set4().asyncCall(-17).checkComplete();
        itf.setA0(new int[]{1, 2, 3});
        final TransportChannel<int[]> channel37 = JRoutine.transport().buildChannel();
        channel37.pass(new int[]{1, 2, 3}).close();
        itf.setA1(channel37);
        final TransportChannel<Integer> channel38 = JRoutine.transport().buildChannel();
        channel38.pass(1, 2, 3).close();
        itf.setA2(channel38);
        final TransportChannel<int[]> channel39 = JRoutine.transport().buildChannel();
        channel39.pass(new int[]{1, 2, 3}).close();
        itf.setA3(channel39);
        itf.setA4().pass(new int[]{1, 2, 3}).result().checkComplete();
        itf.setA5().pass(1, 2, 3).result().checkComplete();
        itf.setA6().asyncCall(new int[]{1, 2, 3}).checkComplete();
        itf.setL0(Arrays.asList(1, 2, 3));
        final TransportChannel<List<Integer>> channel40 = JRoutine.transport().buildChannel();
        channel40.pass(Arrays.asList(1, 2, 3)).close();
        itf.setL1(channel40);
        final TransportChannel<Integer> channel41 = JRoutine.transport().buildChannel();
        channel41.pass(1, 2, 3).close();
        itf.setL2(channel41);
        final TransportChannel<List<Integer>> channel42 = JRoutine.transport().buildChannel();
        channel42.pass(Arrays.asList(1, 2, 3)).close();
        itf.setL3(channel42);
        itf.setL4().pass(Arrays.asList(1, 2, 3)).result().checkComplete();
        itf.setL5().pass(1, 2, 3).result().checkComplete();
        itf.setL6().asyncCall(Arrays.asList(1, 2, 3)).checkComplete();
    }

    public void testTimeoutActionAnnotation() throws NoSuchMethodException {

        assertThat(JRoutineProxy.on(serviceFrom(getActivity(), TestService.class),
                                    targetObject(TestTimeout.class))
                                .invocations()
                                .withExecutionTimeout(seconds(10))
                                .set()
                                .buildProxy(TestTimeoutItf.class)
                                .getInt()).containsExactly(31);

        try {

            JRoutineProxy.on(serviceFrom(getActivity(), TestService.class),
                             targetObject(TestTimeout.class))
                         .invocations()
                         .withExecutionTimeoutAction(TimeoutActionType.THROW)
                         .set()
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
        int add1(@Input(value = char.class, mode = InputMode.VALUE) OutputChannel<Character> c);

        @Alias("a")
        int add2(@Input(value = char.class, mode = InputMode.PARALLEL) OutputChannel<Character> c);

        @Alias("a")
        @Output(OutputMode.VALUE)
        OutputChannel<Integer> add3(char c);

        @Alias("a")
        @Output(OutputMode.VALUE)
        OutputChannel<Integer> add4(
                @Input(value = char.class, mode = InputMode.VALUE) OutputChannel<Character> c);

        @Alias("a")
        @Output(OutputMode.VALUE)
        OutputChannel<Integer> add5(
                @Input(value = char.class, mode = InputMode.PARALLEL) OutputChannel<Character> c);

        @Alias("a")
        @Inputs(value = char.class, mode = InputMode.VALUE)
        InvocationChannel<Character, Integer> add6();

        @Alias("a")
        @Inputs(value = char.class, mode = InputMode.PARALLEL)
        InvocationChannel<Character, Integer> add7();

        @Alias("a")
        @Inputs(value = char.class, mode = InputMode.VALUE)
        Routine<Character, Integer> add8();

        @Alias("a")
        @Inputs(value = char.class, mode = InputMode.PARALLEL)
        Routine<Character, Integer> add9();

        @Alias("aa")
        int[] addA00(char[] c);

        @Alias("aa")
        int[] addA01(@Input(value = char[].class,
                mode = InputMode.VALUE) OutputChannel<char[]> c);

        @Alias("aa")
        int[] addA02(@Input(value = char[].class,
                mode = InputMode.COLLECTION) OutputChannel<Character> c);

        @Alias("aa")
        int[] addA03(@Input(value = char[].class,
                mode = InputMode.PARALLEL) OutputChannel<char[]> c);

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
        @Output(OutputMode.VALUE)
        OutputChannel<int[]> addA07(@Input(value = char[].class,
                mode = InputMode.PARALLEL) OutputChannel<char[]> c);

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
        @Output(OutputMode.ELEMENT)
        OutputChannel<Integer> addA11(@Input(value = char[].class,
                mode = InputMode.PARALLEL) OutputChannel<char[]> c);

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
        @Output(OutputMode.COLLECTION)
        List<int[]> addA15(@Input(value = char[].class,
                mode = InputMode.PARALLEL) OutputChannel<char[]> c);

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
        @Output(OutputMode.COLLECTION)
        int[][] addA19(@Input(value = char[].class,
                mode = InputMode.PARALLEL) OutputChannel<char[]> c);

        @Alias("aa")
        @Inputs(value = char[].class, mode = InputMode.VALUE)
        InvocationChannel<char[], int[]> addA20();

        @Alias("aa")
        @Inputs(value = char[].class, mode = InputMode.PARALLEL)
        InvocationChannel<char[], int[]> addA21();

        @Alias("aa")
        @Inputs(value = char[].class, mode = InputMode.COLLECTION)
        InvocationChannel<Character, int[]> addA22();

        @Alias("aa")
        @Inputs(value = char[].class, mode = InputMode.VALUE)
        Routine<char[], int[]> addA23();

        @Alias("aa")
        @Inputs(value = char[].class, mode = InputMode.PARALLEL)
        Routine<char[], int[]> addA24();

        @Alias("al")
        List<Integer> addL00(List<Character> c);

        @Alias("al")
        List<Integer> addL01(@Input(value = List.class,
                mode = InputMode.VALUE) OutputChannel<List<Character>> c);

        @Alias("al")
        List<Integer> addL02(@Input(value = List.class,
                mode = InputMode.COLLECTION) OutputChannel<Character> c);

        @Alias("al")
        List<Integer> addL03(@Input(value = List.class,
                mode = InputMode.PARALLEL) OutputChannel<List<Character>> c);

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
        @Output(OutputMode.VALUE)
        OutputChannel<List<Integer>> addL07(@Input(value = List.class,
                mode = InputMode.PARALLEL) OutputChannel<List<Character>> c);

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
        @Output(OutputMode.ELEMENT)
        OutputChannel<Integer> addL11(@Input(value = List.class,
                mode = InputMode.PARALLEL) OutputChannel<List<Character>> c);

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
        @Output(OutputMode.COLLECTION)
        List<List<Integer>> addL15(@Input(value = List.class,
                mode = InputMode.PARALLEL) OutputChannel<List<Character>> c);

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
        @Output(OutputMode.COLLECTION)
        List[] addL19(@Input(value = List.class,
                mode = InputMode.PARALLEL) OutputChannel<List<Character>> c);

        @Alias("al")
        @Inputs(value = List.class, mode = InputMode.VALUE)
        InvocationChannel<List<Character>, List<Integer>> addL20();

        @Alias("al")
        @Inputs(value = List.class, mode = InputMode.PARALLEL)
        InvocationChannel<List<Character>, List<Integer>> addL21();

        @Alias("al")
        @Inputs(value = List.class, mode = InputMode.COLLECTION)
        InvocationChannel<Character, List<Integer>> addL22();

        @Alias("al")
        @Inputs(value = List.class, mode = InputMode.VALUE)
        Routine<List<Character>, List<Integer>> addL23();

        @Alias("al")
        @Inputs(value = List.class, mode = InputMode.PARALLEL)
        Routine<List<Character>, List<Integer>> addL24();

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
        void set2(@Input(value = int.class, mode = InputMode.PARALLEL) OutputChannel<Integer> i);

        @Alias("g")
        @Inputs({})
        Routine<Void, Integer> get3();

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
        void setA3(@Input(value = int[].class, mode = InputMode.PARALLEL) OutputChannel<int[]> i);

        @Alias("ga")
        @Inputs({})
        InvocationChannel<Void, int[]> getA4();

        @Alias("ga")
        @Inputs({})
        Routine<Void, int[]> getA5();

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
        void setL3(@Input(value = List.class,
                mode = InputMode.PARALLEL) OutputChannel<List<Integer>> i);

        @Alias("gl")
        @Inputs({})
        InvocationChannel<Void, List<Integer>> getL4();

        @Alias("gl")
        @Inputs({})
        Routine<Void, List<Integer>> getL5();

        @Alias("s")
        @Inputs(value = int.class, mode = InputMode.VALUE)
        InvocationChannel<Integer, Void> set3();

        @Alias("s")
        @Inputs(value = int.class, mode = InputMode.VALUE)
        Routine<Integer, Void> set4();

        @Alias("sa")
        @Inputs(value = int[].class, mode = InputMode.VALUE)
        InvocationChannel<int[], Void> setA4();

        @Alias("sa")
        @Inputs(value = int[].class, mode = InputMode.COLLECTION)
        InvocationChannel<Integer, Void> setA5();

        @Alias("sa")
        @Inputs(value = int[].class, mode = InputMode.VALUE)
        Routine<int[], Void> setA6();

        @Alias("sl")
        @Inputs(value = List.class, mode = InputMode.VALUE)
        InvocationChannel<List<Integer>, Void> setL4();

        @Alias("sl")
        @Inputs(value = List.class, mode = InputMode.COLLECTION)
        InvocationChannel<Integer, Void> setL5();

        @Alias("sl")
        @Inputs(value = List.class, mode = InputMode.VALUE)
        Routine<List<Integer>, Void> setL6();
    }

    @ServiceProxy(TestClass2.class)
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

    @ServiceProxy(TestClassInterface.class)
    public interface TestInterfaceProxy {

        @Timeout(3000)
        @Output
        OutputChannel<Integer> getOne();
    }

    @ServiceProxy(TestList.class)
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

    @ServiceProxy(value = TestClass.class, className = "Test",
            classPackage = "com.gh.bmd.jrt.android.proxy")
    public interface TestProxy {

        @Timeout(3000)
        @Output
        Iterable<Iterable> getList(@Input(value = List.class,
                mode = InputMode.PARALLEL) List<? extends List<String>> i);

        @Timeout(3000)
        @Output
        OutputChannel<Integer> getOne();

        @Timeout(3000)
        String getString(@Input(value = int.class, mode = InputMode.PARALLEL) int... i);

        @Timeout(3000)
        @Output
        OutputChannel<String> getString(
                @Input(value = int.class, mode = InputMode.PARALLEL) HashSet<Integer> i);

        @Timeout(3000)
        @Output(OutputMode.COLLECTION)
        List<String> getString(
                @Input(value = int.class, mode = InputMode.PARALLEL) List<Integer> i);

        @Timeout(3000)
        @Output(OutputMode.COLLECTION)
        Iterable<String> getString(
                @Input(value = int.class, mode = InputMode.PARALLEL) Iterable<Integer> i);

        @Timeout(3000)
        @Output(OutputMode.COLLECTION)
        String[] getString(
                @Input(value = int.class, mode = InputMode.PARALLEL) Collection<Integer> i);

        @Timeout(3000)
        String getString(@Input(int.class) OutputChannel<Integer> i);
    }

    @ServiceProxy(TestClass.class)
    public interface TestStatic {

        @Timeout(300)
        @Output
        OutputChannel<Integer> getOne();

        @Timeout(300)
        @Output
        OutputChannel<Integer> getTwo();
    }

    @ServiceProxy(TestTimeout.class)
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

    private static class MyRunner extends RunnerDecorator {

        public MyRunner() {

            super(Runners.poolRunner());
        }
    }
}
