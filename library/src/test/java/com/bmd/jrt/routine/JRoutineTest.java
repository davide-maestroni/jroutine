/**
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
package com.bmd.jrt.routine;

import com.bmd.jrt.annotation.Bind;
import com.bmd.jrt.annotation.Pass;
import com.bmd.jrt.annotation.Pass.PassingMode;
import com.bmd.jrt.annotation.Share;
import com.bmd.jrt.annotation.Timeout;
import com.bmd.jrt.builder.RoutineBuilder.RunnerType;
import com.bmd.jrt.builder.RoutineBuilder.TimeoutAction;
import com.bmd.jrt.builder.RoutineChannelBuilder.OrderBy;
import com.bmd.jrt.builder.RoutineConfiguration;
import com.bmd.jrt.builder.RoutineConfigurationBuilder;
import com.bmd.jrt.channel.OutputChannel;
import com.bmd.jrt.channel.StandaloneChannel;
import com.bmd.jrt.channel.StandaloneChannel.StandaloneInput;
import com.bmd.jrt.common.ClassToken;
import com.bmd.jrt.common.InvocationException;
import com.bmd.jrt.invocation.PassingInvocation;
import com.bmd.jrt.log.Log.LogLevel;
import com.bmd.jrt.log.NullLog;
import com.bmd.jrt.runner.Runners;
import com.bmd.jrt.time.TimeDuration;

import junit.framework.TestCase;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.bmd.jrt.time.TimeDuration.INFINITY;
import static com.bmd.jrt.time.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Routine builder unit tests.
 * <p/>
 * Created by davide on 10/16/14.
 */
public class JRoutineTest extends TestCase {

    @SuppressWarnings("ConstantConditions")
    public void testChannelBuilderError() {

        try {

            new StandaloneChannelBuilder().withBufferTimeout(0, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new StandaloneChannelBuilder().withBufferTimeout(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new StandaloneChannelBuilder().withMaxSize(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testClassRoutineBuilder() throws NoSuchMethodException {

        final TimeDuration timeout = seconds(1);
        final Routine<Object, Object> routine = JRoutine.on(TestStatic.class)
                                                        .withSyncRunner(RunnerType.SEQUENTIAL)
                                                        .withRunner(Runners.poolRunner())
                                                        .withLogLevel(LogLevel.DEBUG)
                                                        .withLog(new NullLog())
                                                        .boundMethod(TestStatic.GET);

        assertThat(routine.callSync().afterMax(timeout).readAll()).containsExactly(-77L);

        final Routine<Object, Object> routine1 = JRoutine.on(TestStatic.class)
                                                         .withSyncRunner(RunnerType.QUEUED)
                                                         .withRunner(Runners.poolRunner())
                                                         .withMaxInvocations(1)
                                                         .withAvailableTimeout(TimeDuration.ZERO)
                                                         .method("getLong");

        assertThat(routine1.callSync().afterMax(timeout).readAll()).containsExactly(-77L);

        final Routine<Object, Object> routine2 = JRoutine.on(TestStatic.class)
                                                         .withSyncRunner(RunnerType.QUEUED)
                                                         .withRunner(Runners.poolRunner())
                                                         .withMaxInvocations(1)
                                                         .withCoreInvocations(0)
                                                         .withAvailableTimeout(1, TimeUnit.SECONDS)
                                                         .shareGroup("test")
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
                JRoutine.on(TestStatic2.class).withReadTimeout(seconds(2));

        long startTime = System.currentTimeMillis();

        OutputChannel<Object> getOne = builder.shareGroup("1").method("getOne").callAsync();
        OutputChannel<Object> getTwo = builder.shareGroup("2").method("getTwo").callAsync();

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

    public void testClassRoutineBuilderApply() {

        final RoutineConfiguration configuration =
                new RoutineConfigurationBuilder().withRunner(Runners.queuedRunner())
                                                 .buildConfiguration();
        final Routine<Object, Object> routine = JRoutine.on(TestApply.class)
                                                        .withRunner(Runners.sharedRunner())
                                                        .apply(configuration)
                                                        .boundMethod(TestApply.GET_STRING);

        final OutputChannel<Object> channel =
                routine.invokeAsync().after(TimeDuration.millis(200)).pass("test").result();
        assertThat(channel.immediately().readAll()).containsExactly("test");
    }

    @SuppressWarnings("ConstantConditions")
    public void testClassRoutineBuilderError() {

        try {

            new ClassRoutineBuilder((Object) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new ClassRoutineBuilder((WeakReference<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new ClassRoutineBuilder(TestItf.class);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new ClassRoutineBuilder(DuplicateAnnotationStatic.class);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new ClassRoutineBuilder(TestStatic.class).method("test");

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new ClassRoutineBuilder(TestStatic.class).boundMethod("test");

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new ClassRoutineBuilder(TestStatic.class).withAvailableTimeout(1, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new ClassRoutineBuilder(TestStatic.class).withAvailableTimeout(-1,
                                                                           TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new ClassRoutineBuilder(TestStatic.class).withMaxInvocations(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new ClassRoutineBuilder(TestStatic.class).withCoreInvocations(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testClassRoutineCache() {

        final NullLog nullLog = new NullLog();
        final Routine<Object, Object> routine1 = JRoutine.on(TestStatic.class)
                                                         .withSyncRunner(RunnerType.SEQUENTIAL)
                                                         .withRunner(Runners.sharedRunner())
                                                         .withLogLevel(LogLevel.DEBUG)
                                                         .withLog(nullLog)
                                                         .boundMethod(TestStatic.GET);

        assertThat(routine1.callSync().readAll()).containsExactly(-77L);

        final Routine<Object, Object> routine2 = JRoutine.on(TestStatic.class)
                                                         .withSyncRunner(RunnerType.SEQUENTIAL)
                                                         .withRunner(Runners.sharedRunner())
                                                         .withLogLevel(LogLevel.DEBUG)
                                                         .withLog(nullLog)
                                                         .boundMethod(TestStatic.GET);

        assertThat(routine2.callSync().readAll()).containsExactly(-77L);
        assertThat(routine1).isEqualTo(routine2);

        final Routine<Object, Object> routine3 = JRoutine.on(TestStatic.class)
                                                         .withSyncRunner(RunnerType.QUEUED)
                                                         .withRunner(Runners.sharedRunner())
                                                         .withLogLevel(LogLevel.DEBUG)
                                                         .withLog(nullLog)
                                                         .boundMethod(TestStatic.GET);

        assertThat(routine3.callSync().readAll()).containsExactly(-77L);
        assertThat(routine1).isNotEqualTo(routine3);
        assertThat(routine2).isNotEqualTo(routine3);

        final Routine<Object, Object> routine4 = JRoutine.on(TestStatic.class)
                                                         .withSyncRunner(RunnerType.QUEUED)
                                                         .withRunner(Runners.sharedRunner())
                                                         .withLogLevel(LogLevel.WARNING)
                                                         .withLog(nullLog)
                                                         .boundMethod(TestStatic.GET);

        assertThat(routine4.callSync().readAll()).containsExactly(-77L);
        assertThat(routine3).isNotEqualTo(routine4);

        final Routine<Object, Object> routine5 = JRoutine.on(TestStatic.class)
                                                         .withSyncRunner(RunnerType.QUEUED)
                                                         .withRunner(Runners.sharedRunner())
                                                         .withLogLevel(LogLevel.WARNING)
                                                         .withLog(new NullLog())
                                                         .boundMethod(TestStatic.GET);

        assertThat(routine5.callSync().readAll()).containsExactly(-77L);
        assertThat(routine4).isNotEqualTo(routine5);
    }

    public void testObjectRoutineBuilder() throws NoSuchMethodException {

        final TimeDuration timeout = seconds(1);
        final Test test = new Test();
        final Routine<Object, Object> routine = JRoutine.on(test)
                                                        .withSyncRunner(RunnerType.SEQUENTIAL)
                                                        .withRunner(Runners.poolRunner())
                                                        .withMaxInvocations(1)
                                                        .withCoreInvocations(1)
                                                        .withAvailableTimeout(1, TimeUnit.SECONDS)
                                                        .onReadTimeout(TimeoutAction.EXIT)
                                                        .withLogLevel(LogLevel.DEBUG)
                                                        .withLog(new NullLog())
                                                        .boundMethod(Test.GET);

        assertThat(routine.callSync().afterMax(timeout).readAll()).containsExactly(-77L);

        final Routine<Object, Object> routine1 = JRoutine.onWeak(test)
                                                         .withSyncRunner(RunnerType.QUEUED)
                                                         .withRunner(Runners.poolRunner())
                                                         .method("getLong");

        assertThat(routine1.callSync().afterMax(timeout).readAll()).containsExactly(-77L);

        final Routine<Object, Object> routine2 = JRoutine.on(test)
                                                         .withSyncRunner(RunnerType.QUEUED)
                                                         .withRunner(Runners.poolRunner())
                                                         .withMaxInvocations(1)
                                                         .withAvailableTimeout(TimeDuration.ZERO)
                                                         .shareGroup("test")
                                                         .method(Test.class.getMethod("getLong"));

        assertThat(routine2.callSync().afterMax(timeout).readAll()).containsExactly(-77L);

        final Routine<Object, Object> routine3 = JRoutine.onWeak(test).boundMethod(Test.THROW);

        try {

            routine3.callSync(new IllegalArgumentException("test")).afterMax(timeout).readAll();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
            assertThat(e.getCause().getMessage()).isEqualTo("test");
        }

        final ObjectRoutineBuilder builder = JRoutine.on(new Test2()).withReadTimeout(seconds(2));

        long startTime = System.currentTimeMillis();

        OutputChannel<Object> getOne = builder.shareGroup("1").method("getOne").callAsync();
        OutputChannel<Object> getTwo = builder.shareGroup("2").method("getTwo").callAsync();

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

    public void testObjectRoutineBuilderApply() {

        final RoutineConfiguration configuration =
                new RoutineConfigurationBuilder().withRunner(Runners.queuedRunner())
                                                 .buildConfiguration();
        final TestApply testApply = new TestApply();
        final Routine<Object, Object> routine = JRoutine.on(testApply)
                                                        .withRunner(Runners.sharedRunner())
                                                        .apply(configuration)
                                                        .boundMethod(TestApply.GET_STRING);

        final OutputChannel<Object> channel =
                routine.invokeAsync().after(TimeDuration.millis(200)).pass("test").result();
        assertThat(channel.immediately().readAll()).containsExactly("test");
    }

    @SuppressWarnings("ConstantConditions")
    public void testObjectRoutineBuilderError() {

        try {

            new ObjectRoutineBuilder(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new ObjectRoutineBuilder(new DuplicateAnnotation());

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        final Test test = new Test();

        try {

            new ObjectRoutineBuilder(test).method("test");

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new ObjectRoutineBuilder(test).boundMethod("test");

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new ObjectRoutineBuilder(test).withAvailableTimeout(1, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new ObjectRoutineBuilder(test).withAvailableTimeout(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new ObjectRoutineBuilder(test).withMaxInvocations(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new ObjectRoutineBuilder(test).withCoreInvocations(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new ObjectRoutineBuilder(test).buildProxy((Class<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new ObjectRoutineBuilder(test).buildProxy((ClassToken<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new ObjectRoutineBuilder(test).buildProxy(Test.class);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new ObjectRoutineBuilder(test).buildProxy(ClassToken.tokenOf(Test.class));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new ObjectRoutineBuilder(test).buildWrapper((Class<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new ObjectRoutineBuilder(test).buildWrapper((ClassToken<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new ObjectRoutineBuilder(test).buildWrapper(Test.class);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new ObjectRoutineBuilder(test).buildWrapper(ClassToken.tokenOf(Test.class));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.on(test)
                    .withReadTimeout(INFINITY)
                    .buildProxy(TestItf.class)
                    .throwException(null);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.on(test)
                    .withReadTimeout(INFINITY)
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

        final StandaloneChannel<Integer> channel = JRoutine.on().buildChannel();

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

    public void testObjectRoutineCache() {

        final Test test = new Test();
        final NullLog nullLog = new NullLog();
        final Routine<Object, Object> routine1 = JRoutine.on(test)
                                                         .withSyncRunner(RunnerType.SEQUENTIAL)
                                                         .withRunner(Runners.sharedRunner())
                                                         .withLogLevel(LogLevel.DEBUG)
                                                         .withLog(nullLog)
                                                         .boundMethod(Test.GET);

        assertThat(routine1.callSync().readAll()).containsExactly(-77L);

        final Routine<Object, Object> routine2 = JRoutine.on(test)
                                                         .withSyncRunner(RunnerType.SEQUENTIAL)
                                                         .withRunner(Runners.sharedRunner())
                                                         .withLogLevel(LogLevel.DEBUG)
                                                         .withLog(nullLog)
                                                         .boundMethod(Test.GET);

        assertThat(routine2.callSync().readAll()).containsExactly(-77L);
        assertThat(routine1).isEqualTo(routine2);

        final Routine<Object, Object> routine3 = JRoutine.on(test)
                                                         .withSyncRunner(RunnerType.QUEUED)
                                                         .withRunner(Runners.sharedRunner())
                                                         .withLogLevel(LogLevel.DEBUG)
                                                         .withLog(nullLog)
                                                         .boundMethod(Test.GET);

        assertThat(routine3.callSync().readAll()).containsExactly(-77L);
        assertThat(routine1).isNotEqualTo(routine3);
        assertThat(routine2).isNotEqualTo(routine3);

        final Routine<Object, Object> routine4 = JRoutine.on(test)
                                                         .withSyncRunner(RunnerType.QUEUED)
                                                         .withRunner(Runners.sharedRunner())
                                                         .withLogLevel(LogLevel.WARNING)
                                                         .withLog(nullLog)
                                                         .boundMethod(Test.GET);

        assertThat(routine4.callSync().readAll()).containsExactly(-77L);
        assertThat(routine3).isNotEqualTo(routine4);

        final Routine<Object, Object> routine5 = JRoutine.on(test)
                                                         .withSyncRunner(RunnerType.QUEUED)
                                                         .withRunner(Runners.sharedRunner())
                                                         .withLogLevel(LogLevel.WARNING)
                                                         .withLog(new NullLog())
                                                         .boundMethod(Test.GET);

        assertThat(routine5.callSync().readAll()).containsExactly(-77L);
        assertThat(routine4).isNotEqualTo(routine5);
    }

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

        final StandaloneChannel<Integer> channel1 = JRoutine.on().buildChannel();
        channel1.input().pass(4).close();
        assertThat(squareAsync.computeAsync(channel1.output())).isEqualTo(16);

        final StandaloneChannel<Integer> channel2 = JRoutine.on().buildChannel();
        channel2.input().pass(1, 2, 3).close();
        assertThat(squareAsync.computeParallel4(channel2.output())
                              .afterMax(timeout)
                              .readAll()).contains(1, 4, 9);

        final Inc inc = new Inc();
        final IncItf incItf = JRoutine.on(inc).buildProxy(ClassToken.tokenOf(IncItf.class));
        assertThat(incItf.inc(1, 2, 3, 4)).containsOnly(2, 3, 4, 5);
        assertThat(incItf.incIterable(1, 2, 3, 4)).containsOnly(2, 3, 4, 5);

        final Sum sum = new Sum();
        final SumItf sumAsync = JRoutine.on(sum).withReadTimeout(timeout).buildProxy(SumItf.class);
        final StandaloneChannel<Integer> channel3 = JRoutine.on().buildChannel();
        channel3.input().pass(7).close();
        assertThat(sumAsync.compute(3, channel3.output())).isEqualTo(10);

        final StandaloneChannel<Integer> channel4 = JRoutine.on().buildChannel();
        channel4.input().pass(1, 2, 3, 4).close();
        assertThat(sumAsync.compute(channel4.output())).isEqualTo(10);

        final StandaloneChannel<int[]> channel5 = JRoutine.on().buildChannel();
        channel5.input().pass(new int[]{1, 2, 3, 4}).close();
        assertThat(sumAsync.compute1(channel5.output())).isEqualTo(10);

        final StandaloneChannel<Integer> channel6 = JRoutine.on().buildChannel();
        channel6.input().pass(1, 2, 3, 4).close();
        assertThat(sumAsync.computeList(channel6.output())).isEqualTo(10);

        final StandaloneChannel<Integer> channel7 = JRoutine.on().buildChannel();
        channel7.input().pass(1, 2, 3, 4).close();
        assertThat(sumAsync.computeList1(channel7.output())).isEqualTo(10);

        final Count count = new Count();
        final CountItf countAsync =
                JRoutine.on(count).withReadTimeout(timeout).buildProxy(CountItf.class);
        assertThat(countAsync.count(3).readAll()).containsExactly(0, 1, 2);
        assertThat(countAsync.count1(3).readAll()).containsExactly(new int[]{0, 1, 2});
        assertThat(countAsync.count2(2).readAll()).containsExactly(0, 1);
        assertThat(countAsync.countList(3).readAll()).containsExactly(0, 1, 2);
        assertThat(countAsync.countList1(3).readAll()).containsExactly(0, 1, 2);
    }

    public void testRoutineBuilder() {

        final Routine<String, String> routine =
                JRoutine.on(new ClassToken<PassingInvocation<String>>() {})
                        .withSyncRunner(RunnerType.SEQUENTIAL)
                        .withRunner(Runners.poolRunner())
                        .withCoreInvocations(0)
                        .withMaxInvocations(1)
                        .withAvailableTimeout(1, TimeUnit.SECONDS)
                        .withInputSize(2)
                        .withInputTimeout(1, TimeUnit.SECONDS)
                        .withOutputSize(2)
                        .withOutputTimeout(1, TimeUnit.SECONDS)
                        .withOutputOrder(OrderBy.INSERTION)
                        .buildRoutine();

        assertThat(routine.callSync("test1", "test2").readAll()).containsExactly("test1", "test2");

        final Routine<String, String> routine1 =
                JRoutine.on(new ClassToken<PassingInvocation<String>>() {})
                        .withSyncRunner(RunnerType.QUEUED)
                        .withRunner(Runners.poolRunner())
                        .withCoreInvocations(0)
                        .withMaxInvocations(1)
                        .withAvailableTimeout(TimeDuration.ZERO)
                        .withInputSize(2)
                        .withInputTimeout(TimeDuration.ZERO)
                        .withOutputSize(2)
                        .withOutputTimeout(TimeDuration.ZERO)
                        .withOutputOrder(OrderBy.INSERTION)
                        .buildRoutine();

        assertThat(routine1.callSync("test1", "test2").readAll()).containsExactly("test1", "test2");
    }

    public void testRoutineBuilderApply() {

        final RoutineConfiguration configuration =
                new RoutineConfigurationBuilder().withRunner(Runners.queuedRunner())
                                                 .buildConfiguration();
        final Routine<Object, Object> routine = JRoutine.on(PassingInvocation.tokenOf())
                                                        .withRunner(Runners.sharedRunner())
                                                        .apply(configuration)
                                                        .buildRoutine();

        final OutputChannel<Object> channel =
                routine.invokeAsync().after(TimeDuration.millis(200)).pass("test").result();
        assertThat(channel.immediately().readAll()).containsExactly("test");
    }

    @SuppressWarnings("ConstantConditions")
    public void testRoutineBuilderError() {

        final ClassToken<PassingInvocation<String>> token =
                new ClassToken<PassingInvocation<String>>() {};

        try {

            new InvocationRoutineBuilder<String, String>(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new InvocationRoutineBuilder<String, String>(token).withArgs((Object[]) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new InvocationRoutineBuilder<String, String>(token).withMaxInvocations(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new InvocationRoutineBuilder<String, String>(token).withCoreInvocations(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testStandaloneChannelBuilder() {

        final TimeDuration timeout = seconds(1);
        final StandaloneChannel<Object> channel = JRoutine.on()
                                                          .withDataOrder(OrderBy.INSERTION)
                                                          .withRunner(Runners.sharedRunner())
                                                          .withMaxSize(1)
                                                          .withBufferTimeout(1,
                                                                             TimeUnit.MILLISECONDS)
                                                          .withBufferTimeout(seconds(1))
                                                          .withLogLevel(LogLevel.DEBUG)
                                                          .withLog(new NullLog())
                                                          .buildChannel();
        channel.input().pass(-77L);
        assertThat(channel.output().afterMax(timeout).readNext()).isEqualTo(-77L);

        final StandaloneChannel<Object> standaloneChannel1 = JRoutine.on().buildChannel();
        final StandaloneInput<Object> input1 = standaloneChannel1.input();

        input1.after(TimeDuration.millis(200)).pass(23).now().pass(-77L).close();
        assertThat(standaloneChannel1.output().afterMax(timeout).readAll()).containsOnly(23, -77L);

        final StandaloneChannel<Object> standaloneChannel2 =
                JRoutine.on().withDataOrder(OrderBy.INSERTION).buildChannel();
        final StandaloneInput<Object> input2 = standaloneChannel2.input();

        input2.after(TimeDuration.millis(200)).pass(23).now().pass(-77L).close();
        assertThat(standaloneChannel2.output().afterMax(timeout).readAll()).containsExactly(23,
                                                                                            -77L);
    }

    public void testStandaloneChannelBuilderApply() {

        final RoutineConfiguration configuration =
                new RoutineConfigurationBuilder().withRunner(Runners.queuedRunner())
                                                 .buildConfiguration();
        final StandaloneChannel<Object> channel = JRoutine.on()
                                                          .withRunner(Runners.sharedRunner())
                                                          .apply(configuration)
                                                          .buildChannel();

        channel.input().after(TimeDuration.millis(200)).pass("test").close();
        assertThat(channel.output().immediately().readAll()).containsExactly("test");
    }

    private static interface CountError {

        @Pass(int.class)
        public String[] count(int length);

        @Bind("count")
        @Pass(value = int.class, mode = PassingMode.COLLECTION)
        public OutputChannel<Integer> count1(int length);

        @Bind("count")
        @Pass(value = int.class, mode = PassingMode.PARALLEL)
        public String[] count2(int length);

        @Pass(value = List.class, mode = PassingMode.OBJECT)
        public List<Integer> countList(int length);

        @Bind("countList")
        @Pass(value = List.class, mode = PassingMode.COLLECTION)
        public List<Integer> countList1(int length);

        @Bind("countList")
        @Pass(value = List.class, mode = PassingMode.PARALLEL)
        public OutputChannel<Integer> countList2(int length);
    }

    private static interface CountItf {

        @Pass(int[].class)
        public OutputChannel<Integer> count(int length);

        @Bind("count")
        @Pass(value = int[].class, mode = PassingMode.OBJECT)
        public OutputChannel<int[]> count1(int length);

        @Bind("count")
        @Pass(value = int[].class, mode = PassingMode.COLLECTION)
        public OutputChannel<Integer> count2(int length);

        @Pass(List.class)
        public OutputChannel<Integer> countList(int length);

        @Bind("countList")
        @Pass(value = List.class, mode = PassingMode.COLLECTION)
        public OutputChannel<Integer> countList1(int length);
    }

    private interface IncItf {

        @Timeout(1000)
        @Pass(int.class)
        public int[] inc(@Pass(int.class) int... i);

        @Timeout(1000)
        @Bind("inc")
        @Pass(int.class)
        public Iterable<Integer> incIterable(@Pass(int.class) int... i);
    }

    private static interface SquareItf {

        @Timeout(value = 1, unit = TimeUnit.SECONDS)
        public int compute(int i);

        @Bind("compute")
        @Pass(value = int.class, mode = PassingMode.PARALLEL)
        @Timeout(1000)
        public int[] compute1(int length);

        @Bind("compute")
        @Pass(value = int.class, mode = PassingMode.PARALLEL)
        @Timeout(1000)
        public List<Integer> compute2(int length);

        @Bind("compute")
        @Timeout(1000)
        public int computeAsync(@Pass(int.class) OutputChannel<Integer> i);

        @Share(Share.NONE)
        @Bind("compute")
        @Pass(int.class)
        public OutputChannel<Integer> computeParallel1(@Pass(int.class) int... i);

        @Bind("compute")
        @Pass(int.class)
        public OutputChannel<Integer> computeParallel2(@Pass(int.class) Integer... i);

        @Share(Share.NONE)
        @Bind("compute")
        @Pass(int.class)
        public OutputChannel<Integer> computeParallel3(@Pass(int.class) List<Integer> i);

        @Share(Share.NONE)
        @Bind("compute")
        @Pass(int.class)
        public OutputChannel<Integer> computeParallel4(
                @Pass(value = int.class, mode = PassingMode.PARALLEL) OutputChannel<Integer> i);
    }

    private static interface SumError {

        public int compute(int a, @Pass(int.class) int[] b);

        public int compute(@Pass(int.class) String[] ints);

        public int compute(@Pass(value = int.class, mode = PassingMode.OBJECT) int[] ints);

        public int compute(
                @Pass(value = int.class, mode = PassingMode.COLLECTION) Iterable<Integer> ints);

        public int compute(@Pass(value = int.class,
                                 mode = PassingMode.COLLECTION) OutputChannel<Integer> ints);

        public int compute(int a,
                @Pass(value = int[].class, mode = PassingMode.COLLECTION) OutputChannel<Integer> b);

        public int compute(@Pass(value = int.class, mode = PassingMode.PARALLEL) Object ints);

        public int compute(@Pass(value = int.class, mode = PassingMode.PARALLEL) Object[] ints);

        public int compute(String text,
                @Pass(value = int.class, mode = PassingMode.PARALLEL) int[] ints);
    }

    private static interface SumItf {

        public int compute(int a, @Pass(int.class) OutputChannel<Integer> b);

        public int compute(@Pass(int[].class) OutputChannel<Integer> ints);

        @Bind("compute")
        public int compute1(
                @Pass(value = int[].class, mode = PassingMode.OBJECT) OutputChannel<int[]> ints);

        @Bind("compute")
        public int computeList(@Pass(List.class) OutputChannel<Integer> ints);

        @Bind("compute")
        public int computeList1(@Pass(value = List.class,
                                      mode = PassingMode.COLLECTION) OutputChannel<Integer> ints);
    }

    private static interface TestItf {

        public void throwException(@Pass(int.class) RuntimeException ex);

        @Bind(Test.THROW)
        @Pass(int.class)
        public void throwException1(RuntimeException ex);

        @Bind(Test.THROW)
        public int throwException2(RuntimeException ex);
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

    private static class Test {

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
    private static class Test2 {

        public int getOne() throws InterruptedException {

            TimeDuration.millis(500).sleepAtLeast();

            return 1;
        }

        public int getTwo() throws InterruptedException {

            TimeDuration.millis(500).sleepAtLeast();

            return 2;
        }
    }

    private static class TestApply {

        public static final String GET_STRING = "get_string";

        @Bind(GET_STRING)
        @Share(Share.NONE)
        public static String getStringStatic(final String string) {

            return string;
        }

        @Bind(GET_STRING)
        @Share(Share.NONE)
        public String getString(final String string) {

            return string;
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
