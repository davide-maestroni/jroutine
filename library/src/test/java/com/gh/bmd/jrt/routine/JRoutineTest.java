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
import com.gh.bmd.jrt.annotation.Pass.PassingMode;
import com.gh.bmd.jrt.annotation.Share;
import com.gh.bmd.jrt.annotation.Timeout;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.builder.RoutineConfiguration.OrderType;
import com.gh.bmd.jrt.builder.RoutineConfiguration.RunnerType;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.StandaloneChannel;
import com.gh.bmd.jrt.channel.StandaloneChannel.StandaloneInput;
import com.gh.bmd.jrt.common.ClassToken;
import com.gh.bmd.jrt.common.InvocationException;
import com.gh.bmd.jrt.invocation.Invocations.Function0;
import com.gh.bmd.jrt.invocation.Invocations.Function1;
import com.gh.bmd.jrt.invocation.Invocations.Function2;
import com.gh.bmd.jrt.invocation.Invocations.Function3;
import com.gh.bmd.jrt.invocation.Invocations.Function4;
import com.gh.bmd.jrt.invocation.Invocations.FunctionN;
import com.gh.bmd.jrt.invocation.PassingInvocation;
import com.gh.bmd.jrt.log.Log;
import com.gh.bmd.jrt.log.Log.LogLevel;
import com.gh.bmd.jrt.log.NullLog;
import com.gh.bmd.jrt.runner.Runners;
import com.gh.bmd.jrt.time.TimeDuration;

import org.junit.Test;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.gh.bmd.jrt.builder.RoutineConfiguration.TimeoutAction;
import static com.gh.bmd.jrt.builder.RoutineConfiguration.builder;
import static com.gh.bmd.jrt.builder.RoutineConfiguration.withInputOrder;
import static com.gh.bmd.jrt.builder.RoutineConfiguration.withOutputOrder;
import static com.gh.bmd.jrt.builder.RoutineConfiguration.withReadTimeout;
import static com.gh.bmd.jrt.invocation.Invocations.factoryOf;
import static com.gh.bmd.jrt.time.TimeDuration.INFINITY;
import static com.gh.bmd.jrt.time.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Routine builder unit tests.
 * <p/>
 * Created by davide on 10/16/14.
 */
public class JRoutineTest {

    @Test
    public void testClassRoutineBuilder() throws NoSuchMethodException {

        final TimeDuration timeout = seconds(1);
        final RoutineConfiguration configuration = builder().withSyncRunner(RunnerType.SEQUENTIAL)
                                                            .withRunner(Runners.poolRunner())
                                                            .withLogLevel(LogLevel.DEBUG)
                                                            .withLog(new NullLog())
                                                            .buildConfiguration();
        final Routine<Object, Object> routine = JRoutine.on(TestStatic.class)
                                                        .withConfiguration(configuration)
                                                        .boundMethod(TestStatic.GET);

        assertThat(routine.callSync().afterMax(timeout).readAll()).containsExactly(-77L);

        final RoutineConfiguration configuration1 = builder().withSyncRunner(RunnerType.QUEUED)
                                                             .withRunner(Runners.poolRunner())
                                                             .withMaxInvocations(1)
                                                             .withAvailableTimeout(
                                                                     TimeDuration.ZERO)
                                                             .buildConfiguration();
        final Routine<Object, Object> routine1 =
                JRoutine.on(TestStatic.class).withConfiguration(configuration1).method("getLong");

        assertThat(routine1.callSync().afterMax(timeout).readAll()).containsExactly(-77L);

        final RoutineConfiguration configuration2 = builder().withSyncRunner(RunnerType.QUEUED)
                                                             .withRunner(Runners.poolRunner())
                                                             .withMaxInvocations(1)
                                                             .withCoreInvocations(0)
                                                             .withAvailableTimeout(1,
                                                                                   TimeUnit.SECONDS)
                                                             .buildConfiguration();
        final Routine<Object, Object> routine2 = JRoutine.on(TestStatic.class)
                                                         .withConfiguration(configuration2)
                                                         .withShareGroup("test")
                                                         .method(TestStatic.class.getMethod(
                                                                 "getLong"));

        assertThat(routine2.callSync().afterMax(timeout).readAll()).containsExactly(-77L);

        final Routine<Object, Object> routine3 =
                JRoutine.on(TestStatic.class).boundMethod(TestStatic.THROW);

        try {

            routine3.callSync(new IllegalArgumentException("test")).afterMax(timeout).readAll();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
            assertThat(e.getCause().getMessage()).isEqualTo("test");
        }

        final ClassRoutineBuilder builder =
                JRoutine.on(TestStatic2.class).withConfiguration(withReadTimeout(seconds(2)));

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

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testClassRoutineBuilderError() {

        try {

            new DefaultClassRoutineBuilder((Object) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultClassRoutineBuilder((WeakReference<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultClassRoutineBuilder(TestItf.class);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new DefaultClassRoutineBuilder(DuplicateAnnotationStatic.class);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new DefaultClassRoutineBuilder(TestStatic.class).method("test");

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new DefaultClassRoutineBuilder(TestStatic.class).boundMethod("test");

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testClassRoutineBuilderWarnings() {

        final CountLog countLog = new CountLog();
        final RoutineConfiguration configuration = builder().withInputOrder(OrderType.DELIVERY)
                                                            .withInputSize(3)
                                                            .withInputTimeout(seconds(1))
                                                            .withOutputOrder(OrderType.DELIVERY)
                                                            .withOutputSize(3)
                                                            .withOutputTimeout(seconds(1))
                                                            .withLogLevel(LogLevel.DEBUG)
                                                            .withLog(countLog)
                                                            .buildConfiguration();
        JRoutine.on(TestStatic.class).withConfiguration(configuration).boundMethod(TestStatic.GET);
        assertThat(countLog.getWrnCount()).isEqualTo(6);
    }

    @Test
    public void testClassRoutineCache() {

        final NullLog nullLog = new NullLog();
        final RoutineConfiguration configuration1 = builder().withSyncRunner(RunnerType.SEQUENTIAL)
                                                             .withRunner(Runners.sharedRunner())
                                                             .withLogLevel(LogLevel.DEBUG)
                                                             .withLog(nullLog)
                                                             .buildConfiguration();
        final Routine<Object, Object> routine1 = JRoutine.on(TestStatic.class)
                                                         .withConfiguration(configuration1)
                                                         .boundMethod(TestStatic.GET);

        assertThat(routine1.callSync().readAll()).containsExactly(-77L);

        final RoutineConfiguration configuration2 = builder().withSyncRunner(RunnerType.SEQUENTIAL)
                                                             .withRunner(Runners.sharedRunner())
                                                             .withLogLevel(LogLevel.DEBUG)
                                                             .withLog(nullLog)
                                                             .buildConfiguration();
        final Routine<Object, Object> routine2 = JRoutine.on(TestStatic.class)
                                                         .withConfiguration(configuration2)
                                                         .boundMethod(TestStatic.GET);

        assertThat(routine2.callSync().readAll()).containsExactly(-77L);
        assertThat(routine1).isEqualTo(routine2);

        final RoutineConfiguration configuration3 = builder().withSyncRunner(RunnerType.QUEUED)
                                                             .withRunner(Runners.sharedRunner())
                                                             .withLogLevel(LogLevel.DEBUG)
                                                             .withLog(nullLog)
                                                             .buildConfiguration();
        final Routine<Object, Object> routine3 = JRoutine.on(TestStatic.class)
                                                         .withConfiguration(configuration3)
                                                         .boundMethod(TestStatic.GET);

        assertThat(routine3.callSync().readAll()).containsExactly(-77L);
        assertThat(routine1).isNotEqualTo(routine3);
        assertThat(routine2).isNotEqualTo(routine3);

        final RoutineConfiguration configuration4 = builder().withSyncRunner(RunnerType.QUEUED)
                                                             .withRunner(Runners.sharedRunner())
                                                             .withLogLevel(LogLevel.WARNING)
                                                             .withLog(nullLog)
                                                             .buildConfiguration();
        final Routine<Object, Object> routine4 = JRoutine.on(TestStatic.class)
                                                         .withConfiguration(configuration4)
                                                         .boundMethod(TestStatic.GET);

        assertThat(routine4.callSync().readAll()).containsExactly(-77L);
        assertThat(routine3).isNotEqualTo(routine4);

        final RoutineConfiguration configuration5 = builder().withSyncRunner(RunnerType.QUEUED)
                                                             .withRunner(Runners.sharedRunner())
                                                             .withLogLevel(LogLevel.WARNING)
                                                             .withLog(new NullLog())
                                                             .buildConfiguration();
        final Routine<Object, Object> routine5 = JRoutine.on(TestStatic.class)
                                                         .withConfiguration(configuration5)
                                                         .boundMethod(TestStatic.GET);

        assertThat(routine5.callSync().readAll()).containsExactly(-77L);
        assertThat(routine4).isNotEqualTo(routine5);
    }

    @Test
    public void testFunctionBuilder() {

        final Function0<String> function0 = new Function0<String>() {

            public String call() {

                return "test0";
            }
        };
        assertThat(JRoutine.onFunction(function0).callAsync().eventually().readNext()).isEqualTo(
                "test0");

        final Function1<String, String> function1 = new Function1<String, String>() {

            public String call(final String param1) {

                return param1;
            }
        };
        assertThat(JRoutine.onFunction(function1)
                           .callAsync("test1")
                           .eventually()
                           .readNext()).isEqualTo("test1");

        final Function2<String, String, String> function2 =
                new Function2<String, String, String>() {

                    public String call(final String param1, final String param2) {

                        return param1 + " " + param2;
                    }
                };
        assertThat(JRoutine.onFunction(function2)
                           .callAsync("test1", "test2")
                           .eventually()
                           .readNext()).isEqualTo("test1 test2");

        final Function3<String, String, String, String> function3 =
                new Function3<String, String, String, String>() {

                    public String call(final String param1, final String param2,
                            final String param3) {

                        return param1 + " " + param2 + " " + param3;
                    }
                };
        assertThat(JRoutine.onFunction(function3)
                           .callAsync("test1", "test2", "test3")
                           .eventually()
                           .readNext()).isEqualTo("test1 test2 test3");

        final Function4<String, String, String, String, String> function4 =
                new Function4<String, String, String, String, String>() {

                    public String call(final String param1, final String param2,
                            final String param3, final String param4) {

                        return param1 + " " + param2 + " " + param3 + " " + param4;
                    }
                };
        assertThat(JRoutine.onFunction(function4)
                           .callAsync("test1", "test2", "test3", "test4")
                           .eventually()
                           .readNext()).isEqualTo("test1 test2 test3 test4");

        final FunctionN<String, String> functionN = new FunctionN<String, String>() {

            public String call(@Nonnull final List<? extends String> strings) {

                final StringBuilder builder = new StringBuilder();

                for (final String string : strings) {

                    builder.append(string);
                }

                return builder.toString();
            }
        };
        assertThat(JRoutine.onFunction(functionN)
                           .callAsync("test1", "test2", "test3", "test4")
                           .eventually()
                           .readNext()).isEqualTo("test1test2test3test4");

        assertThat(JRoutine.onFunction(function4)
                           .withConfiguration(withInputOrder(OrderType.DELIVERY))
                           .callAsync("test1", "test2", "test3", "test4")
                           .eventually()
                           .readNext()).isEqualTo("test1 test2 test3 test4");

        assertThat(JRoutine.onFunction(function1).callSync("test0").readNext()).isEqualTo("test0");
        assertThat(JRoutine.onFunction(function1)
                           .callParallel("test1", "test2", "test3")
                           .eventually()
                           .readAll()).containsOnly("test1", "test2", "test3");
    }

    @Test
    public void testObjectRoutineBuilder() throws NoSuchMethodException {

        final TimeDuration timeout = seconds(1);
        final TestClass test = new TestClass();
        final RoutineConfiguration configuration = builder().withSyncRunner(RunnerType.SEQUENTIAL)
                                                            .withRunner(Runners.poolRunner())
                                                            .withMaxInvocations(1)
                                                            .withCoreInvocations(1)
                                                            .withAvailableTimeout(1,
                                                                                  TimeUnit.SECONDS)
                                                            .onReadTimeout(TimeoutAction.EXIT)
                                                            .withLogLevel(LogLevel.DEBUG)
                                                            .withLog(new NullLog())
                                                            .buildConfiguration();
        final Routine<Object, Object> routine =
                JRoutine.on(test).withConfiguration(configuration).boundMethod(TestClass.GET);

        assertThat(routine.callSync().afterMax(timeout).readAll()).containsExactly(-77L);

        final RoutineConfiguration configuration1 = builder().withSyncRunner(RunnerType.QUEUED)
                                                             .withRunner(Runners.poolRunner())
                                                             .buildConfiguration();
        final Routine<Object, Object> routine1 =
                JRoutine.on(test).withConfiguration(configuration1).method("getLong");

        assertThat(routine1.callSync().afterMax(timeout).readAll()).containsExactly(-77L);

        final RoutineConfiguration configuration2 = builder().withSyncRunner(RunnerType.QUEUED)
                                                             .withRunner(Runners.poolRunner())
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

        final Routine<Object, Object> routine3 = JRoutine.on(test).boundMethod(TestClass.THROW);

        try {

            routine3.callSync(new IllegalArgumentException("test")).afterMax(timeout).readAll();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
            assertThat(e.getCause().getMessage()).isEqualTo("test");
        }

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

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testObjectRoutineBuilderError() {

        try {

            new DefaultObjectRoutineBuilder(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultObjectRoutineBuilder(new DuplicateAnnotation());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        final TestClass test = new TestClass();

        try {

            new DefaultObjectRoutineBuilder(test).method("test");

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new DefaultObjectRoutineBuilder(test).boundMethod("test");

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

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
    public void testObjectRoutineBuilderWarnings() {

        final TestClass test = new TestClass();
        final CountLog countLog = new CountLog();
        final RoutineConfiguration configuration = builder().withInputOrder(OrderType.DELIVERY)
                                                            .withInputSize(3)
                                                            .withInputTimeout(seconds(1))
                                                            .withOutputOrder(OrderType.DELIVERY)
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
    public void testObjectRoutineCache() {

        final TestClass test = new TestClass();
        final NullLog nullLog = new NullLog();
        final RoutineConfiguration configuration1 = builder().withSyncRunner(RunnerType.SEQUENTIAL)
                                                             .withRunner(Runners.sharedRunner())
                                                             .withLogLevel(LogLevel.DEBUG)
                                                             .withLog(nullLog)
                                                             .buildConfiguration();
        final Routine<Object, Object> routine1 =
                JRoutine.on(test).withConfiguration(configuration1).boundMethod(TestClass.GET);

        assertThat(routine1.callSync().readAll()).containsExactly(-77L);

        final RoutineConfiguration configuration2 = builder().withSyncRunner(RunnerType.SEQUENTIAL)
                                                             .withRunner(Runners.sharedRunner())
                                                             .withLogLevel(LogLevel.DEBUG)
                                                             .withLog(nullLog)
                                                             .buildConfiguration();
        final Routine<Object, Object> routine2 =
                JRoutine.on(test).withConfiguration(configuration2).boundMethod(TestClass.GET);

        assertThat(routine2.callSync().readAll()).containsExactly(-77L);
        assertThat(routine1).isEqualTo(routine2);

        final RoutineConfiguration configuration3 = builder().withSyncRunner(RunnerType.QUEUED)
                                                             .withRunner(Runners.sharedRunner())
                                                             .withLogLevel(LogLevel.DEBUG)
                                                             .withLog(nullLog)
                                                             .buildConfiguration();
        final Routine<Object, Object> routine3 =
                JRoutine.on(test).withConfiguration(configuration3).boundMethod(TestClass.GET);

        assertThat(routine3.callSync().readAll()).containsExactly(-77L);
        assertThat(routine1).isNotEqualTo(routine3);
        assertThat(routine2).isNotEqualTo(routine3);

        final RoutineConfiguration configuration4 = builder().withSyncRunner(RunnerType.QUEUED)
                                                             .withRunner(Runners.sharedRunner())
                                                             .withLogLevel(LogLevel.WARNING)
                                                             .withLog(nullLog)
                                                             .buildConfiguration();
        final Routine<Object, Object> routine4 =
                JRoutine.on(test).withConfiguration(configuration4).boundMethod(TestClass.GET);

        assertThat(routine4.callSync().readAll()).containsExactly(-77L);
        assertThat(routine3).isNotEqualTo(routine4);

        final RoutineConfiguration configuration5 = builder().withSyncRunner(RunnerType.QUEUED)
                                                             .withRunner(Runners.sharedRunner())
                                                             .withLogLevel(LogLevel.WARNING)
                                                             .withLog(new NullLog())
                                                             .buildConfiguration();
        final Routine<Object, Object> routine5 =
                JRoutine.on(test).withConfiguration(configuration5).boundMethod(TestClass.GET);

        assertThat(routine5.callSync().readAll()).containsExactly(-77L);
        assertThat(routine4).isNotEqualTo(routine5);
    }

    @Test
    @SuppressWarnings("NullArgumentToVariableArgMethod")
    public void testObjectRoutineProxy() {

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
    public void testProcedureBuilder() throws InterruptedException {

        final Semaphore semaphore = new Semaphore(0);
        final Function0<Void> procedure0 = new Function0<Void>() {

            public Void call() {

                semaphore.release();
                return null;
            }
        };
        assertThat(JRoutine.onProcedure(procedure0).callAsync().eventually().readAll()).isEmpty();
        assertThat(semaphore.tryAcquire(0, TimeUnit.SECONDS)).isTrue();

        final Function1<String, Void> procedure1 = new Function1<String, Void>() {

            public Void call(final String param1) {

                semaphore.release();
                return null;
            }
        };
        assertThat(JRoutine.onProcedure(procedure1)
                           .callAsync("test1")
                           .eventually()
                           .readAll()).isEmpty();
        assertThat(semaphore.tryAcquire(0, TimeUnit.SECONDS)).isTrue();

        final Function2<String, String, Void> procedure2 = new Function2<String, String, Void>() {

            public Void call(final String param1, final String param2) {

                semaphore.release();
                return null;
            }
        };
        assertThat(
                JRoutine.onProcedure(procedure2).callAsync("test1", "test2").eventually().readAll())
                .isEmpty();
        assertThat(semaphore.tryAcquire(0, TimeUnit.SECONDS)).isTrue();

        final Function3<String, String, String, Void> procedure3 =
                new Function3<String, String, String, Void>() {

                    public Void call(final String param1, final String param2,
                            final String param3) {

                        semaphore.release();
                        return null;
                    }
                };
        assertThat(JRoutine.onProcedure(procedure3)
                           .callAsync("test1", "test2", "test3")
                           .eventually()
                           .readAll()).isEmpty();
        assertThat(semaphore.tryAcquire(0, TimeUnit.SECONDS)).isTrue();

        final Function4<String, String, String, String, Void> procedure4 =
                new Function4<String, String, String, String, Void>() {

                    public Void call(final String param1, final String param2, final String param3,
                            final String param4) {

                        semaphore.release();
                        return null;
                    }
                };
        assertThat(JRoutine.onProcedure(procedure4)
                           .callAsync("test1", "test2", "test3", "test4")
                           .eventually()
                           .readAll()).isEmpty();
        assertThat(semaphore.tryAcquire(0, TimeUnit.SECONDS)).isTrue();

        final FunctionN<String, Void> procedureN = new FunctionN<String, Void>() {

            public Void call(@Nonnull final List<? extends String> strings) {

                semaphore.release();
                return null;
            }
        };
        assertThat(JRoutine.onProcedure(procedureN)
                           .callAsync("test1", "test2", "test3", "test4")
                           .eventually()
                           .readAll()).isEmpty();
        assertThat(semaphore.tryAcquire(0, TimeUnit.SECONDS)).isTrue();

        assertThat(JRoutine.onProcedure(procedure1)
                           .callSync("test0")
                           .eventually()
                           .readAll()).isEmpty();
        assertThat(semaphore.tryAcquire(0, TimeUnit.SECONDS)).isTrue();

        assertThat(JRoutine.onProcedure(procedure1)
                           .callParallel("test0", "test1", "test2")
                           .eventually()
                           .readAll()).isEmpty();
        assertThat(semaphore.tryAcquire(0, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testRoutineBuilder() {

        final RoutineConfiguration configuration = builder().withSyncRunner(RunnerType.SEQUENTIAL)
                                                            .withRunner(Runners.poolRunner())
                                                            .withCoreInvocations(0)
                                                            .withMaxInvocations(1)
                                                            .withAvailableTimeout(1,
                                                                                  TimeUnit.SECONDS)
                                                            .withInputSize(2)
                                                            .withInputTimeout(1, TimeUnit.SECONDS)
                                                            .withOutputSize(2)
                                                            .withOutputTimeout(1, TimeUnit.SECONDS)
                                                            .withOutputOrder(OrderType.PASSING)
                                                            .buildConfiguration();

        assertThat(JRoutine.on(factoryOf(new ClassToken<PassingInvocation<String>>() {}))
                           .withConfiguration(configuration)
                           .callSync("test1", "test2")
                           .readAll()).containsExactly("test1", "test2");

        final RoutineConfiguration configuration1 = builder().withSyncRunner(RunnerType.QUEUED)
                                                             .withRunner(Runners.poolRunner())
                                                             .withCoreInvocations(0)
                                                             .withMaxInvocations(1)
                                                             .withAvailableTimeout(
                                                                     TimeDuration.ZERO)
                                                             .withInputSize(2)
                                                             .withInputTimeout(TimeDuration.ZERO)
                                                             .withOutputSize(2)
                                                             .withOutputTimeout(TimeDuration.ZERO)
                                                             .withOutputOrder(OrderType.PASSING)
                                                             .buildConfiguration();

        assertThat(JRoutine.on(factoryOf(new ClassToken<PassingInvocation<String>>() {}))
                           .withConfiguration(configuration1)
                           .callSync("test1", "test2")
                           .readAll()).containsExactly("test1", "test2");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testRoutineBuilderError() {

        try {

            new DefaultRoutineBuilder<String, String>(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.onFunction((Function0<Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.onFunction((Function1<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.onFunction((Function2<Object, Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.onFunction((Function3<Object, Object, Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.onFunction((Function4<Object, Object, Object, Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.onFunction((FunctionN<Object, Object>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.onProcedure((Function0<Void>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.onProcedure((Function1<Object, Void>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.onProcedure((Function2<Object, Object, Void>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.onProcedure((Function3<Object, Object, Object, Void>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.onProcedure((Function4<Object, Object, Object, Object, Void>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            JRoutine.onProcedure((FunctionN<Object, Void>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testStandaloneChannelBuilder() {

        final TimeDuration timeout = seconds(1);
        final RoutineConfiguration config = builder().withOutputOrder(OrderType.PASSING)
                                                     .withRunner(Runners.sharedRunner())
                                                     .withOutputSize(1)
                                                     .withOutputTimeout(1, TimeUnit.MILLISECONDS)
                                                     .withOutputTimeout(seconds(1))
                                                     .withLogLevel(LogLevel.DEBUG)
                                                     .withLog(new NullLog())
                                                     .buildConfiguration();
        final StandaloneChannel<Object> channel =
                JRoutine.standalone().withConfiguration(config).buildChannel();
        channel.input().pass(-77L);
        assertThat(channel.output().afterMax(timeout).readNext()).isEqualTo(-77L);

        final StandaloneChannel<Object> standaloneChannel1 = JRoutine.standalone().buildChannel();
        final StandaloneInput<Object> input1 = standaloneChannel1.input();

        input1.after(TimeDuration.millis(200)).pass(23).now().pass(-77L).close();
        assertThat(standaloneChannel1.output().afterMax(timeout).readAll()).containsOnly(23, -77L);

        final RoutineConfiguration config2 = withOutputOrder(OrderType.PASSING);
        final StandaloneChannel<Object> standaloneChannel2 =
                JRoutine.standalone().withConfiguration(config2).buildChannel();
        final StandaloneInput<Object> input2 = standaloneChannel2.input();

        input2.after(TimeDuration.millis(200)).pass(23).now().pass(-77L).close();
        assertThat(standaloneChannel2.output().afterMax(timeout).readAll()).containsExactly(23,
                                                                                            -77L);
    }

    @Test
    public void testStandaloneChannelBuilderWarnings() {

        final CountLog countLog = new CountLog();
        final RoutineConfiguration configuration = builder().withSyncRunner(RunnerType.SEQUENTIAL)
                                                            .withMaxInvocations(3)
                                                            .withCoreInvocations(3)
                                                            .withAvailableTimeout(seconds(1))
                                                            .withInputOrder(OrderType.DELIVERY)
                                                            .withInputSize(3)
                                                            .withInputTimeout(seconds(1))
                                                            .withLogLevel(LogLevel.DEBUG)
                                                            .withLog(countLog)
                                                            .buildConfiguration();
        JRoutine.standalone().withConfiguration(configuration).buildChannel();
        assertThat(countLog.getWrnCount()).isEqualTo(7);
    }

    private interface CountError {

        @Pass(int.class)
        String[] count(int length);

        @Bind("count")
        @Pass(value = int.class, mode = PassingMode.COLLECTION)
        OutputChannel<Integer> count1(int length);

        @Bind("count")
        @Pass(value = int.class, mode = PassingMode.PARALLEL)
        String[] count2(int length);

        @Pass(value = List.class, mode = PassingMode.OBJECT)
        List<Integer> countList(int length);

        @Bind("countList")
        @Pass(value = List.class, mode = PassingMode.COLLECTION)
        List<Integer> countList1(int length);

        @Bind("countList")
        @Pass(value = List.class, mode = PassingMode.PARALLEL)
        OutputChannel<Integer> countList2(int length);
    }

    private interface CountItf {

        @Pass(int[].class)
        OutputChannel<Integer> count(int length);

        @Bind("count")
        @Pass(value = int[].class, mode = PassingMode.OBJECT)
        OutputChannel<int[]> count1(int length);

        @Bind("count")
        @Pass(value = int[].class, mode = PassingMode.COLLECTION)
        OutputChannel<Integer> count2(int length);

        @Pass(List.class)
        OutputChannel<Integer> countList(int length);

        @Bind("countList")
        @Pass(value = List.class, mode = PassingMode.COLLECTION)
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
        @Pass(value = int.class, mode = PassingMode.PARALLEL)
        @Timeout(1000)
        int[] compute1(int length);

        @Bind("compute")
        @Pass(value = int.class, mode = PassingMode.PARALLEL)
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
                @Pass(value = int.class, mode = PassingMode.PARALLEL) OutputChannel<Integer> i);
    }

    private interface SumError {

        int compute(int a, @Pass(int.class) int[] b);

        int compute(@Pass(int.class) String[] ints);

        int compute(@Pass(value = int.class, mode = PassingMode.OBJECT) int[] ints);

        int compute(@Pass(value = int.class, mode = PassingMode.COLLECTION) Iterable<Integer> ints);

        int compute(@Pass(value = int.class,
                mode = PassingMode.COLLECTION) OutputChannel<Integer> ints);

        int compute(int a,
                @Pass(value = int[].class, mode = PassingMode.COLLECTION) OutputChannel<Integer> b);

        int compute(@Pass(value = int.class, mode = PassingMode.PARALLEL) Object ints);

        int compute(@Pass(value = int.class, mode = PassingMode.PARALLEL) Object[] ints);

        int compute(String text, @Pass(value = int.class, mode = PassingMode.PARALLEL) int[] ints);
    }

    private interface SumItf {

        int compute(int a, @Pass(int.class) OutputChannel<Integer> b);

        int compute(@Pass(int[].class) OutputChannel<Integer> ints);

        @Bind("compute")
        int compute1(
                @Pass(value = int[].class, mode = PassingMode.OBJECT) OutputChannel<int[]> ints);

        @Bind("compute")
        int computeList(@Pass(List.class) OutputChannel<Integer> ints);

        @Bind("compute")
        int computeList1(@Pass(value = List.class,
                mode = PassingMode.COLLECTION) OutputChannel<Integer> ints);
    }

    private interface TestItf {

        void throwException(@Pass(int.class) RuntimeException ex);

        @Bind(TestClass.THROW)
        @Pass(int.class)
        void throwException1(RuntimeException ex);

        @Bind(TestClass.THROW)
        int throwException2(RuntimeException ex);
    }

    @SuppressWarnings("UnusedDeclaration")
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

    @SuppressWarnings("UnusedDeclaration")
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

    private static class DuplicateAnnotationStatic {

        public static final String GET = "get";

        @Bind(GET)
        public static int getOne() {

            return 1;
        }

        @Bind(GET)
        public static int getTwo() {

            return 2;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    private static class Inc {

        public int inc(final int i) {

            return i + 1;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    private static class Square {

        public int compute(final int i) {

            return i * i;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
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

    @SuppressWarnings("UnusedDeclaration")
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

    private static class TestStatic {

        public static final String GET = "get";

        public static final String THROW = "throw";

        @Bind(GET)
        public static long getLong() {

            return -77;
        }

        @Bind(THROW)
        public static void throwException(final RuntimeException ex) {

            throw ex;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
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
}
