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
package com.gh.bmd.jrt.core;

import com.gh.bmd.jrt.annotation.Alias;
import com.gh.bmd.jrt.annotation.Input;
import com.gh.bmd.jrt.annotation.Input.InputMode;
import com.gh.bmd.jrt.annotation.Inputs;
import com.gh.bmd.jrt.annotation.Output;
import com.gh.bmd.jrt.annotation.Output.OutputMode;
import com.gh.bmd.jrt.annotation.Priority;
import com.gh.bmd.jrt.annotation.ShareGroup;
import com.gh.bmd.jrt.annotation.Timeout;
import com.gh.bmd.jrt.annotation.TimeoutAction;
import com.gh.bmd.jrt.builder.InvocationConfiguration;
import com.gh.bmd.jrt.builder.InvocationConfiguration.AgingPriority;
import com.gh.bmd.jrt.builder.InvocationConfiguration.TimeoutActionType;
import com.gh.bmd.jrt.builder.ObjectRoutineBuilder;
import com.gh.bmd.jrt.builder.ProxyConfiguration;
import com.gh.bmd.jrt.channel.AbortException;
import com.gh.bmd.jrt.channel.InputChannel;
import com.gh.bmd.jrt.channel.InvocationChannel;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.TransportChannel;
import com.gh.bmd.jrt.invocation.InvocationException;
import com.gh.bmd.jrt.log.Log;
import com.gh.bmd.jrt.log.Log.LogLevel;
import com.gh.bmd.jrt.log.NullLog;
import com.gh.bmd.jrt.routine.Routine;
import com.gh.bmd.jrt.runner.Execution;
import com.gh.bmd.jrt.runner.Runner;
import com.gh.bmd.jrt.runner.Runners;
import com.gh.bmd.jrt.util.ClassToken;
import com.gh.bmd.jrt.util.TimeDuration;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.gh.bmd.jrt.util.TimeDuration.INFINITY;
import static com.gh.bmd.jrt.util.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Object routine unit tests.
 * <p/>
 * Created by davide-maestroni on 3/27/15.
 */
public class ObjectRoutineTest {

    @Test
    public void testAgingPriority() {

        final Pass pass = new Pass();
        final TestRunner runner = new TestRunner();
        final PriorityPass priorityPass = JRoutine.on(pass)
                                                  .invocations()
                                                  .withAsyncRunner(runner)
                                                  .set()
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
        final TimeUnit timeUnit = TimeUnit.SECONDS;
        final Routine<Object, Object> routine = JRoutine.on(test)
                                                        .invocations()
                                                        .withSyncRunner(Runners.sequentialRunner())
                                                        .withAsyncRunner(Runners.poolRunner())
                                                        .withMaxInstances(1)
                                                        .withCoreInstances(1)
                                                        .withAvailInstanceTimeout(1, timeUnit)
                                                        .withReadTimeoutAction(
                                                                TimeoutActionType.EXIT)
                                                        .withLogLevel(LogLevel.DEBUG)
                                                        .withLog(new NullLog())
                                                        .set()
                                                        .aliasMethod(TestClass.GET);

        assertThat(routine.callSync().afterMax(timeout).all()).containsExactly(-77L);
    }

    @Test
    public void testAsyncInputProxyRoutine() {

        final TimeDuration timeout = seconds(1);
        final Sum sum = new Sum();
        final SumItf sumAsync = JRoutine.on(sum)
                                        .invocations()
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

        assertThat(sumAsync.compute2().pass(1, 2, 3, 4).result().next()).isEqualTo(10);
        assertThat(sumAsync.compute3().pass(17).result().next()).isEqualTo(17);

        final TransportChannel<Integer> channel6 = JRoutine.transport().buildChannel();
        channel6.input().pass(1, 2, 3, 4).close();
        assertThat(sumAsync.computeList(channel6.output())).isEqualTo(10);

        final TransportChannel<Integer> channel7 = JRoutine.transport().buildChannel();
        channel7.input().pass(1, 2, 3, 4).close();
        assertThat(sumAsync.computeList1(channel7.output())).isEqualTo(10);
    }

    @Test
    public void testAsyncOutputProxyRoutine() {

        final TimeDuration timeout = seconds(1);
        final Count count = new Count();
        final CountItf countAsync = JRoutine.on(count)
                                            .invocations()
                                            .withReadTimeout(timeout)
                                            .set()
                                            .buildProxy(CountItf.class);
        assertThat(countAsync.count(3).all()).containsExactly(0, 1, 2);
        assertThat(countAsync.count1(3).all()).containsExactly(new int[]{0, 1, 2});
        assertThat(countAsync.count2(2).all()).containsExactly(0, 1);
        assertThat(countAsync.countList(3).all()).containsExactly(0, 1, 2);
        assertThat(countAsync.countList1(3).all()).containsExactly(0, 1, 2);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testConfigurationErrors() {

        try {

            new DefaultObjectRoutineBuilder(TestClass.class).setConfiguration(
                    (InvocationConfiguration) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultObjectRoutineBuilder(TestClass.class).setConfiguration(
                    (ProxyConfiguration) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testDuplicateAnnotationError() {

        try {

            new DefaultObjectRoutineBuilder(new DuplicateAnnotation()).aliasMethod(
                    DuplicateAnnotation.GET);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testException() throws NoSuchMethodException {

        final TimeDuration timeout = seconds(1);
        final TestClass test = new TestClass();
        final Routine<Object, Object> routine3 = JRoutine.on(test).aliasMethod(TestClass.THROW);

        try {

            routine3.callSync(new IllegalArgumentException("test")).afterMax(timeout).all();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
            assertThat(e.getCause().getMessage()).isEqualTo("test");
        }
    }

    @Test
    public void testInvalidInputsAnnotationError() {

        final Sum sum = new Sum();

        try {

            new DefaultObjectRoutineBuilder(sum).buildProxy(SumError2.class).compute1();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new DefaultObjectRoutineBuilder(sum).buildProxy(SumError2.class).compute2();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new DefaultObjectRoutineBuilder(sum).buildProxy(SumError2.class).compute3();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new DefaultObjectRoutineBuilder(sum).buildProxy(SumError2.class).compute4();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new DefaultObjectRoutineBuilder(sum).buildProxy(SumError2.class).compute5();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new DefaultObjectRoutineBuilder(sum).buildProxy(SumError2.class).compute6();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new DefaultObjectRoutineBuilder(sum).buildProxy(SumError2.class).compute7(7);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testInvalidProxyError() {

        final TestClass test = new TestClass();

        try {

            new DefaultObjectRoutineBuilder(test).buildProxy(TestClass.class);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new DefaultObjectRoutineBuilder(test).buildProxy(ClassToken.tokenOf(TestClass.class));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testInvalidProxyInputAnnotationError() {

        final Sum sum = new Sum();

        try {

            JRoutine.on(sum).buildProxy(SumError.class).compute(1, new int[0]);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.on(sum).buildProxy(SumError.class).compute(new String[0]);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.on(sum).buildProxy(SumError.class).compute(new int[0]);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.on(sum).buildProxy(SumError.class).compute(Collections.<Integer>emptyList());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        final TransportChannel<Integer> channel = JRoutine.transport().buildChannel();

        try {

            JRoutine.on(sum).buildProxy(SumError.class).compute(channel.output());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.on(sum).buildProxy(SumError.class).compute(1, channel.output());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.on(sum).buildProxy(SumError.class).compute(new Object());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.on(sum).buildProxy(SumError.class).compute(new Object[0]);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.on(sum).buildProxy(SumError.class).compute("test", new int[0]);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testInvalidProxyMethodError() {

        final TestClass test = new TestClass();

        try {

            JRoutine.on(test)
                    .invocations()
                    .withReadTimeout(INFINITY)
                    .set()
                    .buildProxy(TestItf.class)
                    .throwException(null);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.on(test)
                    .invocations()
                    .withReadTimeout(INFINITY)
                    .set()
                    .buildProxy(TestItf.class)
                    .throwException1(null);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.on(test).buildProxy(TestItf.class).throwException2(null);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testInvalidProxyOutputAnnotationError() {

        final Count count = new Count();

        try {

            JRoutine.on(count).buildProxy(CountError.class).count(3);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.on(count).buildProxy(CountError.class).count1(3);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.on(count).buildProxy(CountError.class).count2(3);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.on(count).buildProxy(CountError.class).count3(3);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.on(count).buildProxy(CountError.class).count4();

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.on(count).buildProxy(CountError.class).countList(3);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.on(count).buildProxy(CountError.class).countList1(3);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.on(count).buildProxy(CountError.class).countList2(3);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testMethod() throws NoSuchMethodException {

        final TimeDuration timeout = seconds(1);
        final TestClass test = new TestClass();
        final Routine<Object, Object> routine2 = JRoutine.on(test)
                                                         .invocations()
                                                         .withSyncRunner(Runners.queuedRunner())
                                                         .withAsyncRunner(Runners.poolRunner())
                                                         .withMaxInstances(1)
                                                         .withAvailInstanceTimeout(
                                                                 TimeDuration.ZERO)
                                                         .set()
                                                         .proxies()
                                                         .withShareGroup("test")
                                                         .set()
                                                         .method(TestClass.class.getMethod(
                                                                 "getLong"));

        assertThat(routine2.callSync().afterMax(timeout).all()).containsExactly(-77L);
    }

    @Test
    public void testMethodBySignature() throws NoSuchMethodException {

        final TimeDuration timeout = seconds(1);
        final TestClass test = new TestClass();
        final Routine<Object, Object> routine1 = JRoutine.on(test)
                                                         .invocations()
                                                         .withSyncRunner(Runners.queuedRunner())
                                                         .withAsyncRunner(Runners.poolRunner())
                                                         .set()
                                                         .method("getLong");

        assertThat(routine1.callSync().afterMax(timeout).all()).containsExactly(-77L);
    }

    @Test
    public void testMissingAliasMethodError() {

        final TestClass test = new TestClass();

        try {

            new DefaultObjectRoutineBuilder(test).aliasMethod("test");

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testMissingMethodError() {

        final TestClass test = new TestClass();

        try {

            new DefaultObjectRoutineBuilder(test).method("test");

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testNullPointerError() {

        try {

            new DefaultObjectRoutineBuilder(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testNullProxyError() {

        final TestClass test = new TestClass();

        try {

            new DefaultObjectRoutineBuilder(test).buildProxy((Class<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultObjectRoutineBuilder(test).buildProxy((ClassToken<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testProxyAnnotations() {

        final Impl impl = new Impl();
        final Itf itf = JRoutine.on(impl)
                                .invocations()
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
        assertThat(itf.add3('c').all()).containsExactly((int) 'c');
        final TransportChannel<Character> channel3 = JRoutine.transport().buildChannel();
        channel3.input().pass('a').close();
        assertThat(itf.add4(channel3.output()).all()).containsExactly((int) 'a');
        final TransportChannel<Character> channel4 = JRoutine.transport().buildChannel();
        channel4.input().pass('d', 'e', 'f').close();
        assertThat(itf.add5(channel4.output()).all()).containsOnly((int) 'd', (int) 'e', (int) 'f');
        assertThat(itf.add6().pass('d').result().all()).containsOnly((int) 'd');
        assertThat(itf.add7().pass('d', 'e', 'f').result().all()).containsOnly((int) 'd', (int) 'e',
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
        assertThat(itf.addA04(new char[]{'c', 'z'}).all()).containsExactly(new int[]{'c', 'z'});
        final TransportChannel<char[]> channel8 = JRoutine.transport().buildChannel();
        channel8.input().pass(new char[]{'a', 'z'}).close();
        assertThat(itf.addA05(channel8.output()).all()).containsExactly(new int[]{'a', 'z'});
        final TransportChannel<Character> channel9 = JRoutine.transport().buildChannel();
        channel9.input().pass('d', 'e', 'f').close();
        assertThat(itf.addA06(channel9.output()).all()).containsExactly(new int[]{'d', 'e', 'f'});
        final TransportChannel<char[]> channel10 = JRoutine.transport().buildChannel();
        channel10.input()
                 .pass(new char[]{'d', 'z'}, new char[]{'e', 'z'}, new char[]{'f', 'z'})
                 .close();
        assertThat(itf.addA07(channel10.output()).all()).containsOnly(new int[]{'d', 'z'},
                                                                      new int[]{'e', 'z'},
                                                                      new int[]{'f', 'z'});
        assertThat(itf.addA08(new char[]{'c', 'z'}).all()).containsExactly((int) 'c', (int) 'z');
        final TransportChannel<char[]> channel11 = JRoutine.transport().buildChannel();
        channel11.input().pass(new char[]{'a', 'z'}).close();
        assertThat(itf.addA09(channel11.output()).all()).containsExactly((int) 'a', (int) 'z');
        final TransportChannel<Character> channel12 = JRoutine.transport().buildChannel();
        channel12.input().pass('d', 'e', 'f').close();
        assertThat(itf.addA10(channel12.output()).all()).containsExactly((int) 'd', (int) 'e',
                                                                         (int) 'f');
        final TransportChannel<char[]> channel13 = JRoutine.transport().buildChannel();
        channel13.input()
                 .pass(new char[]{'d', 'z'}, new char[]{'e', 'z'}, new char[]{'f', 'z'})
                 .close();
        assertThat(itf.addA11(channel13.output()).all()).containsOnly((int) 'd', (int) 'e',
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
        assertThat(itf.addA20().pass(new char[]{'c', 'z'}).result().all()).containsOnly(
                new int[]{'c', 'z'});
        assertThat(itf.addA21()
                      .pass(new char[]{'d', 'z'}, new char[]{'e', 'z'}, new char[]{'f', 'z'})
                      .result()
                      .all()).containsOnly(new int[]{'d', 'z'}, new int[]{'e', 'z'},
                                           new int[]{'f', 'z'});
        assertThat(itf.addA22().pass('d', 'e', 'f').result().all()).containsOnly(
                new int[]{'d', 'e', 'f'});
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
        assertThat(itf.addL04(Arrays.asList('c', 'z')).all()).containsExactly(
                Arrays.asList((int) 'c', (int) 'z'));
        final TransportChannel<List<Character>> channel23 = JRoutine.transport().buildChannel();
        channel23.input().pass(Arrays.asList('a', 'z')).close();
        assertThat(itf.addL05(channel23.output()).all()).containsExactly(
                Arrays.asList((int) 'a', (int) 'z'));
        final TransportChannel<Character> channel24 = JRoutine.transport().buildChannel();
        channel24.input().pass('d', 'e', 'f').close();
        assertThat(itf.addL06(channel24.output()).all()).containsExactly(
                Arrays.asList((int) 'd', (int) 'e', (int) 'f'));
        final TransportChannel<List<Character>> channel25 = JRoutine.transport().buildChannel();
        channel25.input()
                 .pass(Arrays.asList('d', 'z'), Arrays.asList('e', 'z'), Arrays.asList('f', 'z'))
                 .close();
        assertThat(itf.addL07(channel25.output()).all()).containsOnly(
                Arrays.asList((int) 'd', (int) 'z'), Arrays.asList((int) 'e', (int) 'z'),
                Arrays.asList((int) 'f', (int) 'z'));
        assertThat(itf.addL08(Arrays.asList('c', 'z')).all()).containsExactly((int) 'c', (int) 'z');
        final TransportChannel<List<Character>> channel26 = JRoutine.transport().buildChannel();
        channel26.input().pass(Arrays.asList('a', 'z')).close();
        assertThat(itf.addL09(channel26.output()).all()).containsExactly((int) 'a', (int) 'z');
        final TransportChannel<Character> channel27 = JRoutine.transport().buildChannel();
        channel27.input().pass('d', 'e', 'f').close();
        assertThat(itf.addL10(channel27.output()).all()).containsExactly((int) 'd', (int) 'e',
                                                                         (int) 'f');
        final TransportChannel<List<Character>> channel28 = JRoutine.transport().buildChannel();
        channel28.input()
                 .pass(Arrays.asList('d', 'z'), Arrays.asList('e', 'z'), Arrays.asList('f', 'z'))
                 .close();
        assertThat(itf.addL11(channel28.output()).all()).containsOnly((int) 'd', (int) 'e',
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
        assertThat(itf.get0()).isEqualTo(31);
        assertThat(itf.get1().all()).containsExactly(31);
        assertThat(itf.get2().result().all()).containsExactly(31);
        assertThat(itf.getA0()).isEqualTo(new int[]{1, 2, 3});
        assertThat(itf.getA1().all()).containsExactly(1, 2, 3);
        assertThat(itf.getA2()).containsExactly(new int[]{1, 2, 3});
        assertThat(itf.getA3()).containsExactly(new int[]{1, 2, 3});
        assertThat(itf.getA4().result().all()).containsExactly(new int[]{1, 2, 3});
        assertThat(itf.getL0()).isEqualTo(Arrays.asList(1, 2, 3));
        assertThat(itf.getL1().all()).containsExactly(1, 2, 3);
        assertThat(itf.getL2()).containsExactly(Arrays.asList(1, 2, 3));
        assertThat(itf.getL3()).containsExactly(Arrays.asList(1, 2, 3));
        assertThat(itf.getL4().result().all()).containsExactly(Arrays.asList(1, 2, 3));
        itf.set0(-17);
        final TransportChannel<Integer> channel35 = JRoutine.transport().buildChannel();
        channel35.input().pass(-17).close();
        itf.set1(channel35.output());
        final TransportChannel<Integer> channel36 = JRoutine.transport().buildChannel();
        channel36.input().pass(-17).close();
        itf.set2(channel36.output());
        itf.set3().pass(-17).result().checkComplete();
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
        itf.setA4().pass(new int[]{1, 2, 3}).result().checkComplete();
        itf.setA5().pass(1, 2, 3).result().checkComplete();
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
        itf.setL4().pass(Arrays.asList(1, 2, 3)).result().checkComplete();
        itf.setL5().pass(1, 2, 3).result().checkComplete();
    }

    @Test
    @SuppressWarnings("NullArgumentToVariableArgMethod")
    public void testProxyRoutine() {

        final TimeDuration timeout = seconds(1);
        final Square square = new Square();
        final SquareItf squareAsync = JRoutine.on(square).buildProxy(SquareItf.class);

        assertThat(squareAsync.compute(3)).isEqualTo(9);
        assertThat(squareAsync.compute1(3)).containsExactly(9);
        assertThat(squareAsync.compute2(3)).containsExactly(9);
        assertThat(squareAsync.computeParallel1(1, 2, 3).afterMax(timeout).all()).contains(1, 4, 9);
        assertThat(squareAsync.computeParallel1().afterMax(timeout).all()).isEmpty();
        assertThat(squareAsync.computeParallel1(null).afterMax(timeout).all()).isEmpty();
        assertThat(squareAsync.computeParallel2(1, 2, 3).afterMax(timeout).all()).contains(1, 4, 9);
        assertThat(squareAsync.computeParallel2().afterMax(timeout).all()).isEmpty();
        assertThat(
                squareAsync.computeParallel2((Integer[]) null).afterMax(timeout).all()).isEmpty();
        assertThat(squareAsync.computeParallel3(Arrays.asList(1, 2, 3))
                              .afterMax(timeout)
                              .all()).contains(1, 4, 9);
        assertThat(squareAsync.computeParallel3(Collections.<Integer>emptyList())
                              .afterMax(timeout)
                              .all()).isEmpty();
        assertThat(squareAsync.computeParallel3(null).afterMax(timeout).all()).isEmpty();

        final TransportChannel<Integer> channel1 = JRoutine.transport().buildChannel();
        channel1.input().pass(4).close();
        assertThat(squareAsync.computeAsync(channel1.output())).isEqualTo(16);

        final TransportChannel<Integer> channel2 = JRoutine.transport().buildChannel();
        channel2.input().pass(1, 2, 3).close();
        assertThat(
                squareAsync.computeParallel4(channel2.output()).afterMax(timeout).all()).contains(1,
                                                                                                  4,
                                                                                                  9);

        final Inc inc = new Inc();
        final IncItf incItf = JRoutine.on(inc).buildProxy(ClassToken.tokenOf(IncItf.class));
        assertThat(incItf.inc(1, 2, 3, 4)).containsOnly(2, 3, 4, 5);
        assertThat(incItf.incIterable(1, 2, 3, 4)).containsOnly(2, 3, 4, 5);
    }

    @Test
    public void testRoutineCache() {

        final TestClass test = new TestClass();
        final NullLog nullLog = new NullLog();
        final Routine<Object, Object> routine1 = JRoutine.on(test)
                                                         .invocations()
                                                         .withSyncRunner(Runners.sequentialRunner())
                                                         .withAsyncRunner(Runners.sharedRunner())
                                                         .withLogLevel(LogLevel.DEBUG)
                                                         .withLog(nullLog)
                                                         .set()
                                                         .aliasMethod(TestClass.GET);

        assertThat(routine1.callSync().all()).containsExactly(-77L);

        final Routine<Object, Object> routine2 = JRoutine.on(test)
                                                         .invocations()
                                                         .withSyncRunner(Runners.sequentialRunner())
                                                         .withAsyncRunner(Runners.sharedRunner())
                                                         .withLogLevel(LogLevel.DEBUG)
                                                         .withLog(nullLog)
                                                         .set()
                                                         .aliasMethod(TestClass.GET);

        assertThat(routine2.callSync().all()).containsExactly(-77L);
        assertThat(routine1).isEqualTo(routine2);

        final Routine<Object, Object> routine3 = JRoutine.on(test)
                                                         .invocations()
                                                         .withSyncRunner(Runners.queuedRunner())
                                                         .withAsyncRunner(Runners.sharedRunner())
                                                         .withLogLevel(LogLevel.DEBUG)
                                                         .withLog(nullLog)
                                                         .set()
                                                         .aliasMethod(TestClass.GET);

        assertThat(routine3.callSync().all()).containsExactly(-77L);
        assertThat(routine1).isNotEqualTo(routine3);
        assertThat(routine2).isNotEqualTo(routine3);

        final Routine<Object, Object> routine4 = JRoutine.on(test)
                                                         .invocations()
                                                         .withSyncRunner(Runners.queuedRunner())
                                                         .withAsyncRunner(Runners.sharedRunner())
                                                         .withLogLevel(LogLevel.WARNING)
                                                         .withLog(nullLog)
                                                         .set()
                                                         .aliasMethod(TestClass.GET);

        assertThat(routine4.callSync().all()).containsExactly(-77L);
        assertThat(routine3).isNotEqualTo(routine4);

        final Routine<Object, Object> routine5 = JRoutine.on(test)
                                                         .invocations()
                                                         .withSyncRunner(Runners.queuedRunner())
                                                         .withAsyncRunner(Runners.sharedRunner())
                                                         .withLogLevel(LogLevel.WARNING)
                                                         .withLog(new NullLog())
                                                         .set()
                                                         .aliasMethod(TestClass.GET);

        assertThat(routine5.callSync().all()).containsExactly(-77L);
        assertThat(routine4).isNotEqualTo(routine5);
    }

    @Test
    public void testShareGroup() throws NoSuchMethodException {

        final TestClass2 test2 = new TestClass2();
        final ObjectRoutineBuilder builder =
                JRoutine.on(test2).invocations().withReadTimeout(seconds(2)).set();

        long startTime = System.currentTimeMillis();

        OutputChannel<Object> getOne =
                builder.proxies().withShareGroup("1").set().method("getOne").callAsync();
        OutputChannel<Object> getTwo =
                builder.proxies().withShareGroup("2").set().method("getTwo").callAsync();

        assertThat(getOne.checkComplete()).isTrue();
        assertThat(getTwo.checkComplete()).isTrue();
        assertThat(System.currentTimeMillis() - startTime).isLessThan(1000);

        startTime = System.currentTimeMillis();

        getOne = builder.method("getOne").callAsync();
        getTwo = builder.method("getTwo").callAsync();

        assertThat(getOne.checkComplete()).isTrue();
        assertThat(getTwo.checkComplete()).isTrue();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
    }

    @Test
    public void testTimeoutActionAnnotation() throws NoSuchMethodException {

        final TestTimeout testTimeout = new TestTimeout();
        assertThat(JRoutine.on(testTimeout)
                           .invocations()
                           .withReadTimeout(seconds(1))
                           .set()
                           .aliasMethod("test")
                           .callAsync()
                           .next()).isEqualTo(31);

        try {

            JRoutine.on(testTimeout)
                    .invocations()
                    .withReadTimeoutAction(TimeoutActionType.DEADLOCK)
                    .set()
                    .aliasMethod("test")
                    .callAsync()
                    .next();

            fail();

        } catch (final NoSuchElementException ignored) {

        }

        assertThat(JRoutine.on(testTimeout)
                           .invocations()
                           .withReadTimeout(seconds(1))
                           .set()
                           .method("getInt")
                           .callAsync()
                           .next()).isEqualTo(31);

        try {

            JRoutine.on(testTimeout)
                    .invocations()
                    .withReadTimeoutAction(TimeoutActionType.DEADLOCK)
                    .set()
                    .method("getInt")
                    .callAsync()
                    .next();

            fail();

        } catch (final NoSuchElementException ignored) {

        }

        assertThat(JRoutine.on(testTimeout)
                           .invocations()
                           .withReadTimeout(seconds(1))
                           .set()
                           .method(TestTimeout.class.getMethod("getInt"))
                           .callAsync()
                           .next()).isEqualTo(31);

        try {

            JRoutine.on(testTimeout)
                    .invocations()
                    .withReadTimeoutAction(TimeoutActionType.DEADLOCK)
                    .set()
                    .method(TestTimeout.class.getMethod("getInt"))
                    .callAsync()
                    .next();

            fail();

        } catch (final NoSuchElementException ignored) {

        }

        assertThat(JRoutine.on(testTimeout)
                           .invocations()
                           .withReadTimeout(seconds(1))
                           .set()
                           .buildProxy(TestTimeoutItf.class)
                           .getInt()).containsExactly(31);

        try {

            JRoutine.on(testTimeout)
                    .invocations()
                    .withReadTimeoutAction(TimeoutActionType.DEADLOCK)
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
        int add1(@Input(value = char.class, mode = InputMode.VALUE) OutputChannel<Character> c);

        @Alias("a")
        int add2(@Input(value = char.class, mode = InputMode.ELEMENT) OutputChannel<Character> c);

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
                @Input(value = char.class, mode = InputMode.ELEMENT) OutputChannel<Character> c);

        @Alias("a")
        @Inputs(value = char.class, mode = InputMode.VALUE)
        InvocationChannel<Character, Integer> add6();

        @Alias("a")
        @Inputs(value = char.class, mode = InputMode.ELEMENT)
        InvocationChannel<Character, Integer> add7();

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
        @Output(OutputMode.COLLECTION)
        int[][] addA19(@Input(value = char[].class,
                mode = InputMode.ELEMENT) OutputChannel<char[]> c);

        @Alias("aa")
        @Inputs(value = char[].class, mode = InputMode.VALUE)
        InvocationChannel<char[], int[]> addA20();

        @Alias("aa")
        @Inputs(value = char[].class, mode = InputMode.ELEMENT)
        InvocationChannel<char[], int[]> addA21();

        @Alias("aa")
        @Inputs(value = char[].class, mode = InputMode.COLLECTION)
        InvocationChannel<Character, int[]> addA22();

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
        @Output(OutputMode.COLLECTION)
        List[] addL19(@Input(value = List.class,
                mode = InputMode.ELEMENT) OutputChannel<List<Character>> c);

        @Alias("al")
        @Inputs(value = List.class, mode = InputMode.VALUE)
        InvocationChannel<List<Character>, List<Integer>> addL20();

        @Alias("al")
        @Inputs(value = List.class, mode = InputMode.ELEMENT)
        InvocationChannel<List<Character>, List<Integer>> addL21();

        @Alias("al")
        @Inputs(value = List.class, mode = InputMode.COLLECTION)
        InvocationChannel<Character, List<Integer>> addL22();

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
        void set2(@Input(value = int.class, mode = InputMode.ELEMENT) OutputChannel<Integer> i);

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
        void setA3(@Input(value = int[].class, mode = InputMode.ELEMENT) OutputChannel<int[]> i);

        @Alias("ga")
        @Inputs({})
        InvocationChannel<Void, int[]> getA4();

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
                mode = InputMode.ELEMENT) OutputChannel<List<Integer>> i);

        @Alias("gl")
        @Inputs({})
        InvocationChannel<Void, List> getL4();

        @Alias("s")
        @Inputs(value = int.class, mode = InputMode.VALUE)
        InvocationChannel<Integer, Void> set3();

        @Alias("sa")
        @Inputs(value = int[].class, mode = InputMode.VALUE)
        InvocationChannel<int[], Void> setA4();

        @Alias("sa")
        @Inputs(value = int[].class, mode = InputMode.COLLECTION)
        InvocationChannel<Integer, Void> setA5();

        @Alias("sl")
        @Inputs(value = List.class, mode = InputMode.VALUE)
        InvocationChannel<List<Integer>, Void> setL4();

        @Alias("sl")
        @Inputs(value = List.class, mode = InputMode.COLLECTION)
        InvocationChannel<Integer, Void> setL5();
    }

    public interface SumError2 {

        @Alias("compute")
        @Inputs({int.class, int.class})
        int compute1();

        @Alias("compute")
        @Output
        @Inputs({int.class, int.class})
        InputChannel<Integer> compute2();

        @Alias("compute")
        @Inputs(value = {int.class, int.class}, mode = InputMode.ELEMENT)
        InputChannel<Integer> compute3();

        @Alias("compute")
        @Inputs(value = {int.class, int.class}, mode = InputMode.COLLECTION)
        InputChannel<Integer> compute4();

        @Alias("compute")
        @Inputs(value = int.class, mode = InputMode.COLLECTION)
        InputChannel<Integer> compute5();

        @Alias("compute")
        @Inputs(value = {int[].class, int.class}, mode = InputMode.COLLECTION)
        InputChannel<Integer> compute6();

        @Alias("compute")
        @Inputs({int.class, int.class})
        InputChannel<Integer> compute7(int i);
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

        @Alias("count")
        @Output(OutputMode.COLLECTION)
        String[] count3(int length);

        @Alias("count")
        @Output(OutputMode.ELEMENT)
        OutputChannel<Integer> count4();

        @Output(OutputMode.VALUE)
        List<Integer> countList(int length);

        @Alias("countList")
        @Output(OutputMode.ELEMENT)
        List<Integer> countList1(int length);

        @Alias("countList")
        @Output(OutputMode.COLLECTION)
        OutputChannel<Integer> countList2(int length);
    }

    private interface CountItf {

        @Output
        OutputChannel<Integer> count(int length);

        @Alias("count")
        @Output(OutputMode.VALUE)
        OutputChannel<int[]> count1(int length);

        @Alias("count")
        @Output(OutputMode.ELEMENT)
        OutputChannel<Integer> count2(int length);

        @Output
        OutputChannel<Integer> countList(int length);

        @Alias("countList")
        @Output(OutputMode.ELEMENT)
        OutputChannel<Integer> countList1(int length);
    }

    private interface IncItf {

        @Timeout(1000)
        @Output
        int[] inc(@Input(int.class) int... i);

        @Timeout(1000)
        @Alias("inc")
        @Output
        Iterable<Integer> incIterable(@Input(int.class) int... i);
    }

    private interface PriorityPass {

        @Output
        @Alias("pass")
        @Priority(AgingPriority.HIGH_PRIORITY)
        OutputChannel<String> passHigh(String s);

        @Output
        @Alias("pass")
        @Priority(AgingPriority.NORMAL_PRIORITY)
        OutputChannel<String> passNormal(String s);
    }

    private interface SquareItf {

        @Timeout(value = 1, unit = TimeUnit.SECONDS)
        int compute(int i);

        @Alias("compute")
        @Output(OutputMode.COLLECTION)
        @Timeout(1000)
        int[] compute1(int length);

        @Alias("compute")
        @Output(OutputMode.COLLECTION)
        @Timeout(1000)
        List<Integer> compute2(int length);

        @Alias("compute")
        @Timeout(1000)
        int computeAsync(@Input(int.class) OutputChannel<Integer> i);

        @ShareGroup(ShareGroup.NONE)
        @Alias("compute")
        @Output
        OutputChannel<Integer> computeParallel1(@Input(int.class) int... i);

        @Alias("compute")
        @Output
        OutputChannel<Integer> computeParallel2(@Input(int.class) Integer... i);

        @ShareGroup(ShareGroup.NONE)
        @Alias("compute")
        @Output
        OutputChannel<Integer> computeParallel3(@Input(int.class) List<Integer> i);

        @ShareGroup(ShareGroup.NONE)
        @Alias("compute")
        @Output
        OutputChannel<Integer> computeParallel4(
                @Input(value = int.class, mode = InputMode.ELEMENT) OutputChannel<Integer> i);
    }

    private interface SumError {

        int compute(int a, @Input(int.class) int[] b);

        int compute(@Input(int.class) String[] ints);

        int compute(@Input(value = int.class, mode = InputMode.VALUE) int[] ints);

        int compute(@Input(value = int.class, mode = InputMode.COLLECTION) Iterable<Integer> ints);

        int compute(@Input(value = int.class,
                mode = InputMode.COLLECTION) OutputChannel<Integer> ints);

        int compute(int a,
                @Input(value = int[].class, mode = InputMode.COLLECTION) OutputChannel<Integer> b);

        int compute(@Input(value = int.class, mode = InputMode.ELEMENT) Object ints);

        int compute(@Input(value = int.class, mode = InputMode.ELEMENT) Object[] ints);

        int compute(String text, @Input(value = int.class, mode = InputMode.ELEMENT) int[] ints);
    }

    private interface SumItf {

        int compute(int a, @Input(int.class) OutputChannel<Integer> b);

        int compute(@Input(int[].class) OutputChannel<Integer> ints);

        @Alias("compute")
        int compute1(@Input(value = int[].class, mode = InputMode.VALUE) OutputChannel<int[]> ints);

        @Alias("compute")
        @Inputs(int[].class)
        InvocationChannel<Integer, Integer> compute2();

        @Alias("compute")
        @Inputs(int.class)
        InvocationChannel<Integer, Integer> compute3();

        @Alias("compute")
        int computeList(@Input(List.class) OutputChannel<Integer> ints);

        @Alias("compute")
        int computeList1(@Input(value = List.class,
                mode = InputMode.COLLECTION) OutputChannel<Integer> ints);
    }

    private interface TestItf {

        void throwException(@Input(int.class) RuntimeException ex);

        @Alias(TestClass.THROW)
        @Output
        void throwException1(RuntimeException ex);

        @Alias(TestClass.THROW)
        int throwException2(RuntimeException ex);
    }

    private interface TestTimeoutItf {

        @Output
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

    private static class Pass {

        public String pass(final String s) {

            return s;
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

    private static class TestRunner implements Runner {

        private final ArrayList<Execution> mExecutions = new ArrayList<Execution>();

        public boolean isRunnerThread() {

            return false;
        }

        public void run(@Nonnull final Execution execution, final long delay,
                @Nonnull final TimeUnit timeUnit) {

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

    private static class TestTimeout {

        @Alias("test")
        @TimeoutAction(TimeoutActionType.EXIT)
        public int getInt() throws InterruptedException {

            Thread.sleep(100);
            return 31;
        }
    }
}
