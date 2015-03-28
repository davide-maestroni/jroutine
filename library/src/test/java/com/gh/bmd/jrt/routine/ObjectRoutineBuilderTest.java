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
package com.gh.bmd.jrt.routine;

import com.gh.bmd.jrt.annotation.Bind;
import com.gh.bmd.jrt.annotation.Pass;
import com.gh.bmd.jrt.annotation.Pass.ParamMode;
import com.gh.bmd.jrt.annotation.Share;
import com.gh.bmd.jrt.annotation.Timeout;
import com.gh.bmd.jrt.builder.ObjectRoutineBuilder;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.builder.RoutineConfiguration.OrderType;
import com.gh.bmd.jrt.builder.RoutineConfiguration.TimeoutAction;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.StandaloneChannel;
import com.gh.bmd.jrt.common.ClassToken;
import com.gh.bmd.jrt.common.InvocationException;
import com.gh.bmd.jrt.log.Log;
import com.gh.bmd.jrt.log.Log.LogLevel;
import com.gh.bmd.jrt.log.NullLog;
import com.gh.bmd.jrt.runner.Runners;
import com.gh.bmd.jrt.time.TimeDuration;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.gh.bmd.jrt.builder.RoutineConfiguration.builder;
import static com.gh.bmd.jrt.builder.RoutineConfiguration.withReadTimeout;
import static com.gh.bmd.jrt.time.TimeDuration.INFINITY;
import static com.gh.bmd.jrt.time.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Object routine builder unit tests.
 * <p/>
 * Created by davide on 3/27/15.
 */
public class ObjectRoutineBuilderTest {

    @Test
    public void testAsyncInputProxyRoutine() {

        final TimeDuration timeout = seconds(1);
        final Sum sum = new Sum();
        final SumItf sumAsync = JRoutine.on(sum)
                                        .withConfiguration(withReadTimeout(timeout))
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

    @Test
    public void testAsyncOutputProxyRoutine() {

        final TimeDuration timeout = seconds(1);
        final Count count = new Count();
        final CountItf countAsync = JRoutine.on(count)
                                            .withConfiguration(withReadTimeout(timeout))
                                            .buildProxy(CountItf.class);
        assertThat(countAsync.count(3).readAll()).containsExactly(0, 1, 2);
        assertThat(countAsync.count1(3).readAll()).containsExactly(new int[]{0, 1, 2});
        assertThat(countAsync.count2(2).readAll()).containsExactly(0, 1);
        assertThat(countAsync.countList(3).readAll()).containsExactly(0, 1, 2);
        assertThat(countAsync.countList1(3).readAll()).containsExactly(0, 1, 2);
    }

    @Test
    public void testBoundMethod() throws NoSuchMethodException {

        final TimeDuration timeout = seconds(1);
        final TestClass test = new TestClass();
        final RoutineConfiguration configuration =
                builder().withSyncRunner(Runners.sequentialRunner())
                         .withAsyncRunner(Runners.poolRunner())
                         .withMaxInvocations(1)
                         .withCoreInvocations(1)
                         .withAvailableTimeout(1, TimeUnit.SECONDS)
                         .onReadTimeout(TimeoutAction.EXIT)
                         .withLogLevel(LogLevel.DEBUG)
                         .withLog(new NullLog())
                         .buildConfiguration();
        final Routine<Object, Object> routine =
                JRoutine.on(test).withConfiguration(configuration).boundMethod(TestClass.GET);

        assertThat(routine.callSync().afterMax(timeout).readAll()).containsExactly(-77L);
    }

    @Test
    public void testConfigurationWarnings() {

        final TestClass test = new TestClass();
        final CountLog countLog = new CountLog();
        final RoutineConfiguration configuration = builder().withInputOrder(OrderType.NONE)
                                                            .withInputSize(3)
                                                            .withInputTimeout(seconds(1))
                                                            .withOutputOrder(OrderType.NONE)
                                                            .withOutputSize(3)
                                                            .withOutputTimeout(seconds(1))
                                                            .withLogLevel(LogLevel.DEBUG)
                                                            .withLog(countLog)
                                                            .buildConfiguration();
        JRoutine.on(test).withConfiguration(configuration).boundMethod(TestClass.GET);
        assertThat(countLog.getWrnCount()).isEqualTo(6);

        final Square square = new Square();
        JRoutine.on(square).withConfiguration(configuration).buildProxy(SquareItf.class).compute(3);
        assertThat(countLog.getWrnCount()).isEqualTo(12);
    }

    @Test
    public void testDuplicateAnnotationError() {

        try {

            new DefaultObjectRoutineBuilder(new DuplicateAnnotation());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testException() throws NoSuchMethodException {

        final TimeDuration timeout = seconds(1);
        final TestClass test = new TestClass();
        final Routine<Object, Object> routine3 = JRoutine.on(test).boundMethod(TestClass.THROW);

        try {

            routine3.callSync(new IllegalArgumentException("test")).afterMax(timeout).readAll();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
            assertThat(e.getCause().getMessage()).isEqualTo("test");
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

        final StandaloneChannel<Integer> channel = JRoutine.standalone().buildChannel();

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
                    .withConfiguration(withReadTimeout(INFINITY))
                    .buildProxy(TestItf.class)
                    .throwException(null);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.on(test)
                    .withConfiguration(withReadTimeout(INFINITY))
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
        final RoutineConfiguration configuration2 = builder().withSyncRunner(Runners.queuedRunner())
                                                             .withAsyncRunner(Runners.poolRunner())
                                                             .withMaxInvocations(1)
                                                             .withAvailableTimeout(
                                                                     TimeDuration.ZERO)
                                                             .buildConfiguration();
        final Routine<Object, Object> routine2 = JRoutine.on(test)
                                                         .withConfiguration(configuration2)
                                                         .withShareGroup("test")
                                                         .method(TestClass.class.getMethod(
                                                                 "getLong"));

        assertThat(routine2.callSync().afterMax(timeout).readAll()).containsExactly(-77L);

    }

    @Test
    public void testMethodBySignature() throws NoSuchMethodException {

        final TimeDuration timeout = seconds(1);
        final TestClass test = new TestClass();
        final RoutineConfiguration configuration1 = builder().withSyncRunner(Runners.queuedRunner())
                                                             .withAsyncRunner(Runners.poolRunner())
                                                             .buildConfiguration();
        final Routine<Object, Object> routine1 =
                JRoutine.on(test).withConfiguration(configuration1).method("getLong");

        assertThat(routine1.callSync().afterMax(timeout).readAll()).containsExactly(-77L);
    }

    @Test
    public void testMissingBoundMethodError() {

        final TestClass test = new TestClass();

        try {

            new DefaultObjectRoutineBuilder(test).boundMethod("test");

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
                                .withConfiguration(withReadTimeout(INFINITY))
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

        final Inc inc = new Inc();
        final IncItf incItf = JRoutine.on(inc).buildProxy(ClassToken.tokenOf(IncItf.class));
        assertThat(incItf.inc(1, 2, 3, 4)).containsOnly(2, 3, 4, 5);
        assertThat(incItf.incIterable(1, 2, 3, 4)).containsOnly(2, 3, 4, 5);

    }

    @Test
    public void testRoutineCache() {

        final TestClass test = new TestClass();
        final NullLog nullLog = new NullLog();
        final RoutineConfiguration configuration1 =
                builder().withSyncRunner(Runners.sequentialRunner())
                         .withAsyncRunner(Runners.sharedRunner())
                         .withLogLevel(LogLevel.DEBUG)
                         .withLog(nullLog)
                         .buildConfiguration();
        final Routine<Object, Object> routine1 =
                JRoutine.on(test).withConfiguration(configuration1).boundMethod(TestClass.GET);

        assertThat(routine1.callSync().readAll()).containsExactly(-77L);

        final RoutineConfiguration configuration2 =
                builder().withSyncRunner(Runners.sequentialRunner())
                         .withAsyncRunner(Runners.sharedRunner())
                         .withLogLevel(LogLevel.DEBUG)
                         .withLog(nullLog)
                         .buildConfiguration();
        final Routine<Object, Object> routine2 =
                JRoutine.on(test).withConfiguration(configuration2).boundMethod(TestClass.GET);

        assertThat(routine2.callSync().readAll()).containsExactly(-77L);
        assertThat(routine1).isEqualTo(routine2);

        final RoutineConfiguration configuration3 = builder().withSyncRunner(Runners.queuedRunner())
                                                             .withAsyncRunner(
                                                                     Runners.sharedRunner())
                                                             .withLogLevel(LogLevel.DEBUG)
                                                             .withLog(nullLog)
                                                             .buildConfiguration();
        final Routine<Object, Object> routine3 =
                JRoutine.on(test).withConfiguration(configuration3).boundMethod(TestClass.GET);

        assertThat(routine3.callSync().readAll()).containsExactly(-77L);
        assertThat(routine1).isNotEqualTo(routine3);
        assertThat(routine2).isNotEqualTo(routine3);

        final RoutineConfiguration configuration4 = builder().withSyncRunner(Runners.queuedRunner())
                                                             .withAsyncRunner(
                                                                     Runners.sharedRunner())
                                                             .withLogLevel(LogLevel.WARNING)
                                                             .withLog(nullLog)
                                                             .buildConfiguration();
        final Routine<Object, Object> routine4 =
                JRoutine.on(test).withConfiguration(configuration4).boundMethod(TestClass.GET);

        assertThat(routine4.callSync().readAll()).containsExactly(-77L);
        assertThat(routine3).isNotEqualTo(routine4);

        final RoutineConfiguration configuration5 = builder().withSyncRunner(Runners.queuedRunner())
                                                             .withAsyncRunner(
                                                                     Runners.sharedRunner())
                                                             .withLogLevel(LogLevel.WARNING)
                                                             .withLog(new NullLog())
                                                             .buildConfiguration();
        final Routine<Object, Object> routine5 =
                JRoutine.on(test).withConfiguration(configuration5).boundMethod(TestClass.GET);

        assertThat(routine5.callSync().readAll()).containsExactly(-77L);
        assertThat(routine4).isNotEqualTo(routine5);
    }

    @Test
    public void testShareGroup() throws NoSuchMethodException {

        final TestClass2 test2 = new TestClass2();
        final ObjectRoutineBuilder builder =
                JRoutine.on(test2).withConfiguration(withReadTimeout(seconds(2)));

        long startTime = System.currentTimeMillis();

        OutputChannel<Object> getOne = builder.withShareGroup("1").method("getOne").callAsync();
        OutputChannel<Object> getTwo = builder.withShareGroup("2").method("getTwo").callAsync();

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

    public interface Itf {

        @Bind("a")
        int add0(char c);

        @Bind("a")
        int add1(@Pass(value = char.class, mode = ParamMode.OBJECT) OutputChannel<Character> c);

        @Bind("a")
        int add2(@Pass(value = char.class, mode = ParamMode.PARALLEL) OutputChannel<Character> c);

        @Bind("a")
        @Pass(value = int.class, mode = ParamMode.OBJECT)
        OutputChannel<Integer> add3(char c);

        @Bind("a")
        @Pass(value = int.class, mode = ParamMode.OBJECT)
        OutputChannel<Integer> add4(
                @Pass(value = char.class, mode = ParamMode.OBJECT) OutputChannel<Character> c);

        @Bind("a")
        @Pass(value = int.class, mode = ParamMode.OBJECT)
        OutputChannel<Integer> add5(
                @Pass(value = char.class, mode = ParamMode.PARALLEL) OutputChannel<Character> c);

        @Bind("aa")
        int[] addA00(char[] c);

        @Bind("aa")
        int[] addA01(@Pass(value = char[].class,
                mode = ParamMode.OBJECT) OutputChannel<char[]> c);

        @Bind("aa")
        int[] addA02(@Pass(value = char[].class,
                mode = ParamMode.COLLECTION) OutputChannel<Character> c);

        @Bind("aa")
        int[] addA03(@Pass(value = char[].class,
                mode = ParamMode.PARALLEL) OutputChannel<char[]> c);

        @Bind("aa")
        @Pass(value = int[].class, mode = ParamMode.OBJECT)
        OutputChannel<int[]> addA04(char[] c);

        @Bind("aa")
        @Pass(value = int[].class, mode = ParamMode.OBJECT)
        OutputChannel<int[]> addA05(
                @Pass(value = char[].class, mode = ParamMode.OBJECT) OutputChannel<char[]> c);

        @Bind("aa")
        @Pass(value = int[].class, mode = ParamMode.OBJECT)
        OutputChannel<int[]> addA06(@Pass(value = char[].class,
                mode = ParamMode.COLLECTION) OutputChannel<Character> c);

        @Bind("aa")
        @Pass(value = int[].class, mode = ParamMode.OBJECT)
        OutputChannel<int[]> addA07(@Pass(value = char[].class,
                mode = ParamMode.PARALLEL) OutputChannel<char[]> c);

        @Bind("aa")
        @Pass(value = int[].class, mode = ParamMode.COLLECTION)
        OutputChannel<Integer> addA08(char[] c);

        @Bind("aa")
        @Pass(value = int[].class, mode = ParamMode.COLLECTION)
        OutputChannel<Integer> addA09(
                @Pass(value = char[].class, mode = ParamMode.OBJECT) OutputChannel<char[]> c);

        @Bind("aa")
        @Pass(value = int[].class, mode = ParamMode.COLLECTION)
        OutputChannel<Integer> addA10(@Pass(value = char[].class,
                mode = ParamMode.COLLECTION) OutputChannel<Character> c);

        @Bind("aa")
        @Pass(value = int[].class, mode = ParamMode.COLLECTION)
        OutputChannel<Integer> addA11(@Pass(value = char[].class,
                mode = ParamMode.PARALLEL) OutputChannel<char[]> c);

        @Bind("aa")
        @Pass(value = int[].class, mode = ParamMode.PARALLEL)
        List<int[]> addA12(char[] c);

        @Bind("aa")
        @Pass(value = int[].class, mode = ParamMode.PARALLEL)
        List<int[]> addA13(
                @Pass(value = char[].class, mode = ParamMode.OBJECT) OutputChannel<char[]> c);

        @Bind("aa")
        @Pass(value = int[].class, mode = ParamMode.PARALLEL)
        List<int[]> addA14(@Pass(value = char[].class,
                mode = ParamMode.COLLECTION) OutputChannel<Character> c);

        @Bind("aa")
        @Pass(value = int[].class, mode = ParamMode.PARALLEL)
        List<int[]> addA15(@Pass(value = char[].class,
                mode = ParamMode.PARALLEL) OutputChannel<char[]> c);

        @Bind("aa")
        @Pass(value = int[].class, mode = ParamMode.PARALLEL)
        int[][] addA16(char[] c);

        @Bind("aa")
        @Pass(value = int[].class, mode = ParamMode.PARALLEL)
        int[][] addA17(
                @Pass(value = char[].class, mode = ParamMode.OBJECT) OutputChannel<char[]> c);

        @Bind("aa")
        @Pass(value = int[].class, mode = ParamMode.PARALLEL)
        int[][] addA18(@Pass(value = char[].class,
                mode = ParamMode.COLLECTION) OutputChannel<Character> c);

        @Bind("aa")
        @Pass(value = int[].class, mode = ParamMode.PARALLEL)
        int[][] addA19(@Pass(value = char[].class,
                mode = ParamMode.PARALLEL) OutputChannel<char[]> c);

        @Bind("al")
        List<Integer> addL00(List<Character> c);

        @Bind("al")
        List<Integer> addL01(@Pass(value = List.class,
                mode = ParamMode.OBJECT) OutputChannel<List<Character>> c);

        @Bind("al")
        List<Integer> addL02(@Pass(value = List.class,
                mode = ParamMode.COLLECTION) OutputChannel<Character> c);

        @Bind("al")
        List<Integer> addL03(@Pass(value = List.class,
                mode = ParamMode.PARALLEL) OutputChannel<List<Character>> c);

        @Bind("al")
        @Pass(value = List.class, mode = ParamMode.OBJECT)
        OutputChannel<List<Integer>> addL04(List<Character> c);

        @Bind("al")
        @Pass(value = List.class, mode = ParamMode.OBJECT)
        OutputChannel<List<Integer>> addL05(@Pass(value = List.class,
                mode = ParamMode.OBJECT) OutputChannel<List<Character>> c);

        @Bind("al")
        @Pass(value = List.class, mode = ParamMode.OBJECT)
        OutputChannel<List<Integer>> addL06(@Pass(value = List.class,
                mode = ParamMode.COLLECTION) OutputChannel<Character> c);

        @Bind("al")
        @Pass(value = List.class, mode = ParamMode.OBJECT)
        OutputChannel<List<Integer>> addL07(@Pass(value = List.class,
                mode = ParamMode.PARALLEL) OutputChannel<List<Character>> c);

        @Bind("al")
        @Pass(value = List.class, mode = ParamMode.COLLECTION)
        OutputChannel<Integer> addL08(List<Character> c);

        @Bind("al")
        @Pass(value = List.class, mode = ParamMode.COLLECTION)
        OutputChannel<Integer> addL09(@Pass(value = List.class,
                mode = ParamMode.OBJECT) OutputChannel<List<Character>> c);

        @Bind("al")
        @Pass(value = List.class, mode = ParamMode.COLLECTION)
        OutputChannel<Integer> addL10(@Pass(value = List.class,
                mode = ParamMode.COLLECTION) OutputChannel<Character> c);

        @Bind("al")
        @Pass(value = List.class, mode = ParamMode.COLLECTION)
        OutputChannel<Integer> addL11(@Pass(value = List.class,
                mode = ParamMode.PARALLEL) OutputChannel<List<Character>> c);

        @Bind("al")
        @Pass(value = List.class, mode = ParamMode.PARALLEL)
        List<List<Integer>> addL12(List<Character> c);

        @Bind("al")
        @Pass(value = List.class, mode = ParamMode.PARALLEL)
        List<List<Integer>> addL13(@Pass(value = List.class,
                mode = ParamMode.OBJECT) OutputChannel<List<Character>> c);

        @Bind("al")
        @Pass(value = List.class, mode = ParamMode.PARALLEL)
        List<List<Integer>> addL14(@Pass(value = List.class,
                mode = ParamMode.COLLECTION) OutputChannel<Character> c);

        @Bind("al")
        @Pass(value = List.class, mode = ParamMode.PARALLEL)
        List<List<Integer>> addL15(@Pass(value = List.class,
                mode = ParamMode.PARALLEL) OutputChannel<List<Character>> c);

        @Bind("al")
        @Pass(value = List.class, mode = ParamMode.PARALLEL)
        List[] addL16(List<Character> c);

        @Bind("al")
        @Pass(value = List.class, mode = ParamMode.PARALLEL)
        List[] addL17(@Pass(value = List.class,
                mode = ParamMode.OBJECT) OutputChannel<List<Character>> c);

        @Bind("al")
        @Pass(value = List.class, mode = ParamMode.PARALLEL)
        List[] addL18(@Pass(value = List.class,
                mode = ParamMode.COLLECTION) OutputChannel<Character> c);

        @Bind("al")
        @Pass(value = List.class, mode = ParamMode.PARALLEL)
        List[] addL19(@Pass(value = List.class,
                mode = ParamMode.PARALLEL) OutputChannel<List<Character>> c);

        @Bind("g")
        int get0();

        @Bind("s")
        void set0(int i);

        @Bind("g")
        @Pass(value = int.class, mode = ParamMode.OBJECT)
        OutputChannel<Integer> get1();

        @Bind("s")
        void set1(@Pass(value = int.class, mode = ParamMode.OBJECT) OutputChannel<Integer> i);

        @Bind("ga")
        int[] getA0();

        @Bind("sa")
        void setA0(int[] i);

        @Bind("ga")
        @Pass(value = int[].class, mode = ParamMode.COLLECTION)
        OutputChannel<Integer> getA1();

        @Bind("sa")
        void setA1(@Pass(value = int[].class, mode = ParamMode.OBJECT) OutputChannel<int[]> i);

        @Bind("ga")
        @Pass(value = int[].class, mode = ParamMode.PARALLEL)
        List<int[]> getA2();

        @Bind("sa")
        void setA2(
                @Pass(value = int[].class, mode = ParamMode.COLLECTION) OutputChannel<Integer> i);

        @Bind("ga")
        @Pass(value = int[].class, mode = ParamMode.PARALLEL)
        int[][] getA3();

        @Bind("sa")
        void setA3(@Pass(value = int[].class, mode = ParamMode.PARALLEL) OutputChannel<int[]> i);

        @Bind("gl")
        List<Integer> getL0();

        @Bind("sl")
        void setL0(List<Integer> i);

        @Bind("gl")
        @Pass(value = List.class, mode = ParamMode.COLLECTION)
        OutputChannel<Integer> getL1();

        @Bind("sl")
        void setL1(@Pass(value = List.class,
                mode = ParamMode.OBJECT) OutputChannel<List<Integer>> i);

        @Bind("gl")
        @Pass(value = List.class, mode = ParamMode.PARALLEL)
        List<List<Integer>> getL2();

        @Bind("sl")
        void setL2(@Pass(value = List.class, mode = ParamMode.COLLECTION) OutputChannel<Integer> i);

        @Bind("gl")
        @Pass(value = List.class, mode = ParamMode.PARALLEL)
        List[] getL3();

        @Bind("sl")
        void setL3(@Pass(value = List.class,
                mode = ParamMode.PARALLEL) OutputChannel<List<Integer>> i);

        @Bind("s")
        void set2(@Pass(value = int.class, mode = ParamMode.PARALLEL) OutputChannel<Integer> i);
    }

    private interface CountError {

        @Pass(int.class)
        String[] count(int length);

        @Bind("count")
        @Pass(value = int.class, mode = ParamMode.COLLECTION)
        OutputChannel<Integer> count1(int length);

        @Bind("count")
        @Pass(value = int.class, mode = ParamMode.PARALLEL)
        String[] count2(int length);

        @Pass(value = List.class, mode = ParamMode.OBJECT)
        List<Integer> countList(int length);

        @Bind("countList")
        @Pass(value = List.class, mode = ParamMode.COLLECTION)
        List<Integer> countList1(int length);

        @Bind("countList")
        @Pass(value = List.class, mode = ParamMode.PARALLEL)
        OutputChannel<Integer> countList2(int length);
    }

    private interface CountItf {

        @Pass(int[].class)
        OutputChannel<Integer> count(int length);

        @Bind("count")
        @Pass(value = int[].class, mode = ParamMode.OBJECT)
        OutputChannel<int[]> count1(int length);

        @Bind("count")
        @Pass(value = int[].class, mode = ParamMode.COLLECTION)
        OutputChannel<Integer> count2(int length);

        @Pass(List.class)
        OutputChannel<Integer> countList(int length);

        @Bind("countList")
        @Pass(value = List.class, mode = ParamMode.COLLECTION)
        OutputChannel<Integer> countList1(int length);
    }

    private interface IncItf {

        @Timeout(1000)
        @Pass(int.class)
        int[] inc(@Pass(int.class) int... i);

        @Timeout(1000)
        @Bind("inc")
        @Pass(int.class)
        Iterable<Integer> incIterable(@Pass(int.class) int... i);
    }

    private interface SquareItf {

        @Timeout(value = 1, unit = TimeUnit.SECONDS)
        int compute(int i);

        @Bind("compute")
        @Pass(value = int.class, mode = ParamMode.PARALLEL)
        @Timeout(1000)
        int[] compute1(int length);

        @Bind("compute")
        @Pass(value = int.class, mode = ParamMode.PARALLEL)
        @Timeout(1000)
        List<Integer> compute2(int length);

        @Bind("compute")
        @Timeout(1000)
        int computeAsync(@Pass(int.class) OutputChannel<Integer> i);

        @Share(Share.NONE)
        @Bind("compute")
        @Pass(int.class)
        OutputChannel<Integer> computeParallel1(@Pass(int.class) int... i);

        @Bind("compute")
        @Pass(int.class)
        OutputChannel<Integer> computeParallel2(@Pass(int.class) Integer... i);

        @Share(Share.NONE)
        @Bind("compute")
        @Pass(int.class)
        OutputChannel<Integer> computeParallel3(@Pass(int.class) List<Integer> i);

        @Share(Share.NONE)
        @Bind("compute")
        @Pass(int.class)
        OutputChannel<Integer> computeParallel4(
                @Pass(value = int.class, mode = ParamMode.PARALLEL) OutputChannel<Integer> i);
    }

    private interface SumError {

        int compute(int a, @Pass(int.class) int[] b);

        int compute(@Pass(int.class) String[] ints);

        int compute(@Pass(value = int.class, mode = ParamMode.OBJECT) int[] ints);

        int compute(@Pass(value = int.class, mode = ParamMode.COLLECTION) Iterable<Integer> ints);

        int compute(@Pass(value = int.class,
                mode = ParamMode.COLLECTION) OutputChannel<Integer> ints);

        int compute(int a,
                @Pass(value = int[].class, mode = ParamMode.COLLECTION) OutputChannel<Integer> b);

        int compute(@Pass(value = int.class, mode = ParamMode.PARALLEL) Object ints);

        int compute(@Pass(value = int.class, mode = ParamMode.PARALLEL) Object[] ints);

        int compute(String text, @Pass(value = int.class, mode = ParamMode.PARALLEL) int[] ints);
    }

    private interface SumItf {

        int compute(int a, @Pass(int.class) OutputChannel<Integer> b);

        int compute(@Pass(int[].class) OutputChannel<Integer> ints);

        @Bind("compute")
        int compute1(@Pass(value = int[].class, mode = ParamMode.OBJECT) OutputChannel<int[]> ints);

        @Bind("compute")
        int computeList(@Pass(List.class) OutputChannel<Integer> ints);

        @Bind("compute")
        int computeList1(@Pass(value = List.class,
                mode = ParamMode.COLLECTION) OutputChannel<Integer> ints);
    }

    private interface TestItf {

        void throwException(@Pass(int.class) RuntimeException ex);

        @Bind(TestClass.THROW)
        @Pass(int.class)
        void throwException1(RuntimeException ex);

        @Bind(TestClass.THROW)
        int throwException2(RuntimeException ex);
    }

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

            return new int[3];
        }

        @Bind("sa")
        public void setArray(int[] i) {

        }

        @Bind("gl")
        public List<Integer> getList() {

            return Arrays.asList(1, 2, 3);
        }

        @Bind("sl")
        public void setList(List<Integer> l) {

        }

        @Bind("s")
        public void set(int i) {

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

            TimeDuration.millis(500).sleepAtLeast();

            return 1;
        }

        public int getTwo() throws InterruptedException {

            TimeDuration.millis(500).sleepAtLeast();

            return 2;
        }
    }

}
