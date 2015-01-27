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

import com.bmd.jrt.annotation.AsyncName;
import com.bmd.jrt.annotation.AsyncType;
import com.bmd.jrt.annotation.LockName;
import com.bmd.jrt.annotation.ParallelType;
import com.bmd.jrt.annotation.ResultTimeout;
import com.bmd.jrt.builder.RoutineBuilder.RunnerType;
import com.bmd.jrt.builder.RoutineChannelBuilder.DataOrder;
import com.bmd.jrt.builder.RoutineConfiguration;
import com.bmd.jrt.builder.RoutineConfigurationBuilder;
import com.bmd.jrt.channel.OutputChannel;
import com.bmd.jrt.channel.Tunnel;
import com.bmd.jrt.channel.Tunnel.TunnelInput;
import com.bmd.jrt.common.ClassToken;
import com.bmd.jrt.common.InvocationException;
import com.bmd.jrt.invocation.TunnelInvocation;
import com.bmd.jrt.log.Log.LogLevel;
import com.bmd.jrt.log.NullLog;
import com.bmd.jrt.runner.Runners;
import com.bmd.jrt.time.TimeDuration;

import junit.framework.TestCase;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

            new TunnelBuilder().dataOrder(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new TunnelBuilder().bufferTimeout(0, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new TunnelBuilder().logLevel(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new TunnelBuilder().bufferTimeout(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new TunnelBuilder().maxSize(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testClassRoutineBuilder() throws NoSuchMethodException {

        final TimeDuration timeout = seconds(1);
        final Routine<Object, Object> routine = JRoutine.on(TestStatic.class)
                                                        .syncRunner(RunnerType.SEQUENTIAL)
                                                        .runBy(Runners.poolRunner())
                                                        .logLevel(LogLevel.DEBUG)
                                                        .loggedWith(new NullLog())
                                                        .annotatedMethod(TestStatic.GET);

        assertThat(routine.callSync().afterMax(timeout).readAll()).containsExactly(-77L);

        final Routine<Object, Object> routine1 = JRoutine.on(TestStatic.class)
                                                         .syncRunner(RunnerType.QUEUED)
                                                         .runBy(Runners.poolRunner())
                                                         .maxRunning(1)
                                                         .availableTimeout(TimeDuration.ZERO)
                                                         .method("getLong");

        assertThat(routine1.callSync().afterMax(timeout).readAll()).containsExactly(-77L);

        final Routine<Object, Object> routine2 = JRoutine.on(TestStatic.class)
                                                         .syncRunner(RunnerType.QUEUED)
                                                         .runBy(Runners.poolRunner())
                                                         .maxRunning(1)
                                                         .maxRetained(0)
                                                         .availableTimeout(1, TimeUnit.SECONDS)
                                                         .lockName("test")
                                                         .method(TestStatic.class.getMethod(
                                                                 "getLong"));

        assertThat(routine2.callSync().afterMax(timeout).readAll()).containsExactly(-77L);

        final Routine<Object, Object> routine3 =
                JRoutine.on(TestStatic.class).annotatedMethod(TestStatic.THROW);

        try {

            routine3.callSync(new IllegalArgumentException("test")).afterMax(timeout).readAll();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
            assertThat(e.getCause().getMessage()).isEqualTo("test");
        }

        final ClassRoutineBuilder builder = JRoutine.on(TestStatic2.class);

        long startTime = System.currentTimeMillis();

        OutputChannel<Object> getOne = builder.lockName("1").method("getOne").callAsync();
        OutputChannel<Object> getTwo = builder.lockName("2").method("getTwo").callAsync();

        assertThat(getOne.afterMax(timeout).checkComplete()).isTrue();
        assertThat(getTwo.afterMax(timeout).checkComplete()).isTrue();
        assertThat(System.currentTimeMillis() - startTime).isLessThan(1000);

        startTime = System.currentTimeMillis();

        getOne = builder.method("getOne").callAsync();
        getTwo = builder.method("getTwo").callAsync();

        assertThat(getOne.afterMax(timeout).checkComplete()).isTrue();
        assertThat(getTwo.afterMax(timeout).checkComplete()).isTrue();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
    }

    public void testClassRoutineBuilderApply() {

        final RoutineConfiguration configuration =
                new RoutineConfigurationBuilder().runBy(Runners.queuedRunner())
                                                 .buildConfiguration();
        final Routine<Object, Object> routine = JRoutine.on(TestApply.class)
                                                        .runBy(Runners.sharedRunner())
                                                        .apply(configuration)
                                                        .annotatedMethod(TestApply.GET_STRING);

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

            new ClassRoutineBuilder(TestStatic.class).annotatedMethod("test");

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new ClassRoutineBuilder(TestStatic.class).availableTimeout(1, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new ClassRoutineBuilder(TestStatic.class).availableTimeout(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new ClassRoutineBuilder(TestStatic.class).logLevel(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new ClassRoutineBuilder(TestStatic.class).maxRunning(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new ClassRoutineBuilder(TestStatic.class).maxRetained(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new ClassRoutineBuilder(TestStatic.class).syncRunner(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testClassRoutineCache() {

        final NullLog nullLog = new NullLog();
        final Routine<Object, Object> routine1 = JRoutine.on(TestStatic.class)
                                                         .syncRunner(RunnerType.SEQUENTIAL)
                                                         .runBy(Runners.sharedRunner())
                                                         .logLevel(LogLevel.DEBUG)
                                                         .loggedWith(nullLog)
                                                         .annotatedMethod(TestStatic.GET);

        assertThat(routine1.callSync().readAll()).containsExactly(-77L);

        final Routine<Object, Object> routine2 = JRoutine.on(TestStatic.class)
                                                         .syncRunner(RunnerType.SEQUENTIAL)
                                                         .runBy(Runners.sharedRunner())
                                                         .logLevel(LogLevel.DEBUG)
                                                         .loggedWith(nullLog)
                                                         .annotatedMethod(TestStatic.GET);

        assertThat(routine2.callSync().readAll()).containsExactly(-77L);
        assertThat(routine1).isEqualTo(routine2);

        final Routine<Object, Object> routine3 = JRoutine.on(TestStatic.class)
                                                         .syncRunner(RunnerType.QUEUED)
                                                         .runBy(Runners.sharedRunner())
                                                         .logLevel(LogLevel.DEBUG)
                                                         .loggedWith(nullLog)
                                                         .annotatedMethod(TestStatic.GET);

        assertThat(routine3.callSync().readAll()).containsExactly(-77L);
        assertThat(routine1).isNotEqualTo(routine3);
        assertThat(routine2).isNotEqualTo(routine3);

        final Routine<Object, Object> routine4 = JRoutine.on(TestStatic.class)
                                                         .syncRunner(RunnerType.QUEUED)
                                                         .runBy(Runners.sharedRunner())
                                                         .logLevel(LogLevel.WARNING)
                                                         .loggedWith(nullLog)
                                                         .annotatedMethod(TestStatic.GET);

        assertThat(routine4.callSync().readAll()).containsExactly(-77L);
        assertThat(routine3).isNotEqualTo(routine4);

        final Routine<Object, Object> routine5 = JRoutine.on(TestStatic.class)
                                                         .syncRunner(RunnerType.QUEUED)
                                                         .runBy(Runners.sharedRunner())
                                                         .logLevel(LogLevel.WARNING)
                                                         .loggedWith(new NullLog())
                                                         .annotatedMethod(TestStatic.GET);

        assertThat(routine5.callSync().readAll()).containsExactly(-77L);
        assertThat(routine4).isNotEqualTo(routine5);
    }

    public void testObjectRoutineBuilder() throws NoSuchMethodException {

        final TimeDuration timeout = seconds(1);
        final Test test = new Test();
        final Routine<Object, Object> routine = JRoutine.on(test)
                                                        .syncRunner(RunnerType.SEQUENTIAL)
                                                        .runBy(Runners.poolRunner())
                                                        .maxRunning(1)
                                                        .maxRetained(1)
                                                        .availableTimeout(1, TimeUnit.SECONDS)
                                                        .logLevel(LogLevel.DEBUG)
                                                        .loggedWith(new NullLog())
                                                        .annotatedMethod(Test.GET);

        assertThat(routine.callSync().afterMax(timeout).readAll()).containsExactly(-77L);

        final Routine<Object, Object> routine1 = JRoutine.onWeak(test)
                                                         .syncRunner(RunnerType.QUEUED)
                                                         .runBy(Runners.poolRunner())
                                                         .method("getLong");

        assertThat(routine1.callSync().afterMax(timeout).readAll()).containsExactly(-77L);

        final Routine<Object, Object> routine2 = JRoutine.on(test)
                                                         .syncRunner(RunnerType.QUEUED)
                                                         .runBy(Runners.poolRunner())
                                                         .maxRunning(1)
                                                         .availableTimeout(TimeDuration.ZERO)
                                                         .lockName("test")
                                                         .method(Test.class.getMethod("getLong"));

        assertThat(routine2.callSync().afterMax(timeout).readAll()).containsExactly(-77L);

        final Routine<Object, Object> routine3 = JRoutine.onWeak(test).annotatedMethod(Test.THROW);

        try {

            routine3.callSync(new IllegalArgumentException("test")).afterMax(timeout).readAll();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
            assertThat(e.getCause().getMessage()).isEqualTo("test");
        }

        final ObjectRoutineBuilder builder = JRoutine.on(new Test2());

        long startTime = System.currentTimeMillis();

        OutputChannel<Object> getOne = builder.lockName("1").method("getOne").callAsync();
        OutputChannel<Object> getTwo = builder.lockName("2").method("getTwo").callAsync();

        assertThat(getOne.afterMax(timeout).checkComplete()).isTrue();
        assertThat(getTwo.afterMax(timeout).checkComplete()).isTrue();
        assertThat(System.currentTimeMillis() - startTime).isLessThan(1000);

        startTime = System.currentTimeMillis();

        getOne = builder.method("getOne").callAsync();
        getTwo = builder.method("getTwo").callAsync();

        assertThat(getOne.afterMax(timeout).checkComplete()).isTrue();
        assertThat(getTwo.afterMax(timeout).checkComplete()).isTrue();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
    }

    public void testObjectRoutineBuilderApply() {

        final RoutineConfiguration configuration =
                new RoutineConfigurationBuilder().runBy(Runners.queuedRunner())
                                                 .buildConfiguration();
        final TestApply testApply = new TestApply();
        final Routine<Object, Object> routine = JRoutine.on(testApply)
                                                        .runBy(Runners.sharedRunner())
                                                        .apply(configuration)
                                                        .annotatedMethod(TestApply.GET_STRING);

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

            new ObjectRoutineBuilder(test).annotatedMethod("test");

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new ObjectRoutineBuilder(test).availableTimeout(1, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new ObjectRoutineBuilder(test).availableTimeout(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new ObjectRoutineBuilder(test).logLevel(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new ObjectRoutineBuilder(test).maxRunning(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new ObjectRoutineBuilder(test).maxRetained(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new ObjectRoutineBuilder(test).syncRunner(null);

            fail();

        } catch (final NullPointerException ignored) {

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

            JRoutine.on(test).buildProxy(TestItf.class).throwException(null);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.on(test).buildProxy(TestItf.class).throwException1(null);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JRoutine.on(test).buildProxy(TestItf.class).throwException2(null);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testObjectRoutineCache() {

        final Test test = new Test();
        final NullLog nullLog = new NullLog();
        final Routine<Object, Object> routine1 = JRoutine.on(test)
                                                         .syncRunner(RunnerType.SEQUENTIAL)
                                                         .runBy(Runners.sharedRunner())
                                                         .logLevel(LogLevel.DEBUG)
                                                         .loggedWith(nullLog)
                                                         .annotatedMethod(Test.GET);

        assertThat(routine1.callSync().readAll()).containsExactly(-77L);

        final Routine<Object, Object> routine2 = JRoutine.on(test)
                                                         .syncRunner(RunnerType.SEQUENTIAL)
                                                         .runBy(Runners.sharedRunner())
                                                         .logLevel(LogLevel.DEBUG)
                                                         .loggedWith(nullLog)
                                                         .annotatedMethod(Test.GET);

        assertThat(routine2.callSync().readAll()).containsExactly(-77L);
        assertThat(routine1).isEqualTo(routine2);

        final Routine<Object, Object> routine3 = JRoutine.on(test)
                                                         .syncRunner(RunnerType.QUEUED)
                                                         .runBy(Runners.sharedRunner())
                                                         .logLevel(LogLevel.DEBUG)
                                                         .loggedWith(nullLog)
                                                         .annotatedMethod(Test.GET);

        assertThat(routine3.callSync().readAll()).containsExactly(-77L);
        assertThat(routine1).isNotEqualTo(routine3);
        assertThat(routine2).isNotEqualTo(routine3);

        final Routine<Object, Object> routine4 = JRoutine.on(test)
                                                         .syncRunner(RunnerType.QUEUED)
                                                         .runBy(Runners.sharedRunner())
                                                         .logLevel(LogLevel.WARNING)
                                                         .loggedWith(nullLog)
                                                         .annotatedMethod(Test.GET);

        assertThat(routine4.callSync().readAll()).containsExactly(-77L);
        assertThat(routine3).isNotEqualTo(routine4);

        final Routine<Object, Object> routine5 = JRoutine.on(test)
                                                         .syncRunner(RunnerType.QUEUED)
                                                         .runBy(Runners.sharedRunner())
                                                         .logLevel(LogLevel.WARNING)
                                                         .loggedWith(new NullLog())
                                                         .annotatedMethod(Test.GET);

        assertThat(routine5.callSync().readAll()).containsExactly(-77L);
        assertThat(routine4).isNotEqualTo(routine5);
    }

    public void testObjectRoutineParallel() {

        final TimeDuration timeout = seconds(1);
        final Square square = new Square();
        final SquareItf squareAsync = JRoutine.on(square).buildProxy(SquareItf.class);

        assertThat(squareAsync.compute(3)).isEqualTo(9);
        assertThat(squareAsync.computeParallel1(1, 2, 3).afterMax(timeout).readAll()).contains(1, 4,
                                                                                               9);
        assertThat(squareAsync.computeParallel2(1, 2, 3).afterMax(timeout).readAll()).contains(1, 4,
                                                                                               9);
        assertThat(squareAsync.computeParallel3(Arrays.asList(1, 2, 3)).afterMax(timeout).readAll())
                .contains(1, 4, 9);

        final Tunnel<Integer> tunnel = JRoutine.on().buildTunnel();

        tunnel.input().pass(1, 2, 3).close();
        assertThat(
                squareAsync.computeParallel4(tunnel.output()).afterMax(timeout).readAll()).contains(
                1, 4, 9);

        final TestInc testInc = new TestInc();
        final int[] inc =
                JRoutine.on(testInc).buildProxy(ClassToken.tokenOf(ITestInc.class)).inc(1, 2, 3, 4);
        assertThat(inc).containsOnly(2, 3, 4, 5);
    }

    public void testRoutineBuilder() {

        final Routine<String, String> routine =
                JRoutine.on(new ClassToken<TunnelInvocation<String>>() {})
                        .syncRunner(RunnerType.SEQUENTIAL)
                        .runBy(Runners.poolRunner())
                        .maxRetained(0)
                        .maxRunning(1)
                        .availableTimeout(1, TimeUnit.SECONDS)
                        .inputSize(2)
                        .inputTimeout(1, TimeUnit.SECONDS)
                        .outputSize(2)
                        .outputTimeout(1, TimeUnit.SECONDS)
                        .outputOrder(DataOrder.INSERTION)
                        .buildRoutine();

        assertThat(routine.callSync("test1", "test2").readAll()).containsExactly("test1", "test2");

        final Routine<String, String> routine1 =
                JRoutine.on(new ClassToken<TunnelInvocation<String>>() {})
                        .syncRunner(RunnerType.QUEUED)
                        .runBy(Runners.poolRunner())
                        .maxRetained(0)
                        .maxRunning(1)
                        .availableTimeout(TimeDuration.ZERO)
                        .inputSize(2)
                        .inputTimeout(TimeDuration.ZERO)
                        .outputSize(2)
                        .outputTimeout(TimeDuration.ZERO)
                        .outputOrder(DataOrder.INSERTION)
                        .buildRoutine();

        assertThat(routine1.callSync("test1", "test2").readAll()).containsExactly("test1", "test2");
    }

    public void testRoutineBuilderApply() {

        final RoutineConfiguration configuration =
                new RoutineConfigurationBuilder().runBy(Runners.queuedRunner())
                                                 .buildConfiguration();
        final Routine<Object, Object> routine = JRoutine.on(TunnelInvocation.tokenOf())
                                                        .runBy(Runners.sharedRunner())
                                                        .apply(configuration)
                                                        .buildRoutine();

        final OutputChannel<Object> channel =
                routine.invokeAsync().after(TimeDuration.millis(200)).pass("test").result();
        assertThat(channel.immediately().readAll()).containsExactly("test");
    }

    @SuppressWarnings("ConstantConditions")
    public void testRoutineBuilderError() {

        final ClassToken<TunnelInvocation<String>> token =
                new ClassToken<TunnelInvocation<String>>() {};

        try {

            new InvocationRoutineBuilder<String, String>(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new InvocationRoutineBuilder<String, String>(token).logLevel(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new InvocationRoutineBuilder<String, String>(token).withArgs((Object[]) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new InvocationRoutineBuilder<String, String>(token).maxRunning(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new InvocationRoutineBuilder<String, String>(token).maxRetained(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testTunnelBuilder() {

        final TimeDuration timeout = seconds(1);
        final Tunnel<Object> tunnel = JRoutine.on()
                                              .dataOrder(DataOrder.INSERTION)
                                              .delayRunner(Runners.sharedRunner())
                                              .maxSize(1)
                                              .bufferTimeout(1, TimeUnit.MILLISECONDS)
                                              .bufferTimeout(seconds(1))
                                              .logLevel(LogLevel.DEBUG)
                                              .loggedWith(new NullLog())
                                              .buildTunnel();
        tunnel.input().pass(-77L);
        assertThat(tunnel.output().afterMax(timeout).readNext()).isEqualTo(-77L);

        final Tunnel<Object> tunnel1 = JRoutine.on().buildTunnel();
        final TunnelInput<Object> input1 = tunnel1.input();

        input1.after(TimeDuration.millis(200)).pass(23).now().pass(-77L).close();
        assertThat(tunnel1.output().afterMax(timeout).readAll()).containsOnly(23, -77L);

        final Tunnel<Object> tunnel2 = JRoutine.on().dataOrder(DataOrder.INSERTION).buildTunnel();
        final TunnelInput<Object> input2 = tunnel2.input();

        input2.after(TimeDuration.millis(200)).pass(23).now().pass(-77L).close();
        assertThat(tunnel2.output().afterMax(timeout).readAll()).containsExactly(23, -77L);
    }

    public void testTunnnelBuilderApply() {

        final RoutineConfiguration configuration =
                new RoutineConfigurationBuilder().runBy(Runners.queuedRunner())
                                                 .buildConfiguration();
        final Tunnel<Object> tunnel = JRoutine.on()
                                              .delayRunner(Runners.sharedRunner())
                                              .apply(configuration)
                                              .buildTunnel();

        tunnel.input().after(TimeDuration.millis(200)).pass("test").close();
        assertThat(tunnel.output().immediately().readAll()).containsExactly("test");
    }

    private interface ITestInc {

        @ResultTimeout(1000)
        @AsyncType(int.class)
        public int[] inc(@ParallelType(int.class) int... i);
    }

    private static interface SquareItf {

        @ResultTimeout(value = 1, unit = TimeUnit.SECONDS)
        public int compute(int i);

        @LockName(LockName.NULL_LOCK)
        @AsyncName("compute")
        @AsyncType(int.class)
        public OutputChannel<Integer> computeParallel1(@ParallelType(int.class) int... i);

        @AsyncName("compute")
        @AsyncType(int.class)
        public OutputChannel<Integer> computeParallel2(@ParallelType(int.class) Integer... i);

        @LockName(LockName.NULL_LOCK)
        @AsyncName("compute")
        @AsyncType(int.class)
        public OutputChannel<Integer> computeParallel3(@ParallelType(int.class) List<Integer> i);

        @LockName(LockName.NULL_LOCK)
        @AsyncName("compute")
        @AsyncType(int.class)
        public OutputChannel<Integer> computeParallel4(
                @ParallelType(int.class) OutputChannel<Integer> i);
    }

    private static interface TestItf {

        public void throwException(@AsyncType(int.class) RuntimeException ex);

        @AsyncName(Test.THROW)
        @AsyncType(int.class)
        public void throwException1(RuntimeException ex);

        @AsyncName(Test.THROW)
        public int throwException2(RuntimeException ex);
    }

    private static class DuplicateAnnotation {

        public static final String GET = "get";

        @AsyncName(GET)
        public int getOne() {

            return 1;
        }

        @AsyncName(GET)
        public int getTwo() {

            return 2;
        }
    }

    private static class DuplicateAnnotationStatic {

        public static final String GET = "get";

        @AsyncName(GET)
        public static int getOne() {

            return 1;
        }

        @AsyncName(GET)
        public static int getTwo() {

            return 2;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    private static class Square {

        public int compute(final int i) {

            return i * i;
        }
    }

    private static class Test {

        public static final String GET = "get";

        public static final String THROW = "throw";

        @AsyncName(GET)
        public long getLong() {

            return -77;

        }

        @AsyncName(THROW)
        public void throwException(final RuntimeException ex) {

            throw ex;
        }
    }

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

        @AsyncName(GET_STRING)
        @LockName(LockName.NULL_LOCK)
        public static String getStringStatic(final String string) {

            return string;
        }

        @AsyncName(GET_STRING)
        @LockName(LockName.NULL_LOCK)
        public String getString(final String string) {

            return string;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    private static class TestInc {

        public int inc(final int i) {

            return i + 1;
        }
    }

    private static class TestStatic {

        public static final String GET = "get";

        public static final String THROW = "throw";

        @AsyncName(GET)
        public static long getLong() {

            return -77;
        }

        @AsyncName(THROW)
        public static void throwException(final RuntimeException ex) {

            throw ex;
        }
    }

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
