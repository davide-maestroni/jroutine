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

import com.bmd.jrt.annotation.Async;
import com.bmd.jrt.annotation.AsyncOverride;
import com.bmd.jrt.channel.IOChannel;
import com.bmd.jrt.channel.InputChannel;
import com.bmd.jrt.channel.OutputChannel;
import com.bmd.jrt.channel.ResultChannel;
import com.bmd.jrt.common.ClassToken;
import com.bmd.jrt.invocation.BasicInvocation;
import com.bmd.jrt.log.Log.LogLevel;
import com.bmd.jrt.log.NullLog;
import com.bmd.jrt.runner.RunnerWrapper;
import com.bmd.jrt.runner.Runners;
import com.bmd.jrt.time.TimeDuration;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Routine builder unit tests.
 * <p/>
 * Created by davide on 10/16/14.
 */
public class JavaRoutineTest extends TestCase {

    public void testChannelBuilder() {

        final IOChannel<Object> channel = JavaRoutine.io()
                                                     .orderedInput()
                                                     .logLevel(LogLevel.DEBUG)
                                                     .loggedWith(new NullLog())
                                                     .buildChannel();
        channel.input().pass(-77L);
        assertThat(channel.output().readFirst()).isEqualTo(-77L);

        final IOChannel<Object> channel1 = JavaRoutine.io().buildChannel();
        final InputChannel<Object> input1 = channel1.input();

        input1.after(TimeDuration.millis(200)).pass(23).now().pass(-77L);
        channel1.close();
        assertThat(channel1.output().readAll()).containsOnly(23, -77L);

        final IOChannel<Object> channel2 = JavaRoutine.io().orderedInput().buildChannel();
        final InputChannel<Object> input2 = channel2.input();

        input2.after(TimeDuration.millis(200)).pass(23).now().pass(-77L);
        channel2.close();
        assertThat(channel2.output().readAll()).containsExactly(23, -77L);
    }

    @SuppressWarnings("ConstantConditions")
    public void testChannelBuilderError() {

        try {

            new IOChannelBuilder().loggedWith(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new IOChannelBuilder().logLevel(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testClassRoutineBuilder() throws NoSuchMethodException {

        final Routine<Object, Object> routine = JavaRoutine.on(TestStatic.class)
                                                           .sequential()
                                                           .runBy(Runners.poolRunner())
                                                           .maxInputSize(2)
                                                           .inputTimeout(1, TimeUnit.SECONDS)
                                                           .maxOutputSize(2)
                                                           .outputTimeout(1, TimeUnit.SECONDS)
                                                           .orderedOutput()
                                                           .logLevel(LogLevel.DEBUG)
                                                           .loggedWith(new NullLog())
                                                           .asyncMethod(TestStatic.GET);

        assertThat(routine.call()).containsExactly(-77L);

        final Routine<Object, Object> routine1 = JavaRoutine.on(TestStatic.class)
                                                            .queued()
                                                            .runBy(Runners.poolRunner())
                                                            .maxRunning(1)
                                                            .availableTimeout(TimeDuration.ZERO)
                                                            .maxInputSize(2)
                                                            .inputTimeout(TimeDuration.ZERO)
                                                            .maxOutputSize(2)
                                                            .outputTimeout(TimeDuration.ZERO)
                                                            .orderedOutput()
                                                            .method("getLong");

        assertThat(routine1.call()).containsExactly(-77L);

        final Routine<Object, Object> routine2 = JavaRoutine.on(TestStatic.class)
                                                            .queued()
                                                            .runBy(Runners.poolRunner())
                                                            .maxRunning(1)
                                                            .maxRetained(0)
                                                            .availableTimeout(1, TimeUnit.SECONDS)
                                                            .lockId("test")
                                                            .method(TestStatic.class.getMethod(
                                                                    "getLong"));

        assertThat(routine2.call()).containsExactly(-77L);

        final Routine<Object, Object> routine3 =
                JavaRoutine.on(TestStatic.class).asyncMethod(TestStatic.THROW);

        try {

            routine3.call(new IllegalArgumentException("test"));

            fail();

        } catch (final RoutineInvocationException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
            assertThat(e.getCause().getMessage()).isEqualTo("test");
        }

        final ClassRoutineBuilder builder = JavaRoutine.on(TestStatic2.class);

        long startTime = System.currentTimeMillis();

        OutputChannel<Object> getOne = builder.method("getOne").runAsync();
        OutputChannel<Object> getTwo = builder.method("getTwo").runAsync();

        assertThat(getOne.isComplete()).isTrue();
        assertThat(getTwo.isComplete()).isTrue();
        assertThat(System.currentTimeMillis() - startTime).isLessThan(1000);

        startTime = System.currentTimeMillis();

        getOne = builder.lockId("test").method("getOne").runAsync();
        getTwo = builder.lockId("test").method("getTwo").runAsync();

        assertThat(getOne.isComplete()).isTrue();
        assertThat(getTwo.isComplete()).isTrue();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
    }

    @SuppressWarnings("ConstantConditions")
    public void testClassRoutineBuilderError() {

        try {

            new ClassRoutineBuilder(null);

            fail();

        } catch (final NullPointerException ignored) {

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

            new ClassRoutineBuilder(TestStatic.class).asyncMethod("test");

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new ClassRoutineBuilder(TestStatic.class).availableTimeout(1, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new ClassRoutineBuilder(TestStatic.class).availableTimeout(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new ClassRoutineBuilder(TestStatic.class).availableTimeout(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new ClassRoutineBuilder(TestStatic.class).inputTimeout(1, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new ClassRoutineBuilder(TestStatic.class).inputTimeout(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new ClassRoutineBuilder(TestStatic.class).inputTimeout(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new ClassRoutineBuilder(TestStatic.class).maxInputSize(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new ClassRoutineBuilder(TestStatic.class).outputTimeout(1, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new ClassRoutineBuilder(TestStatic.class).outputTimeout(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new ClassRoutineBuilder(TestStatic.class).outputTimeout(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new ClassRoutineBuilder(TestStatic.class).maxOutputSize(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new ClassRoutineBuilder(TestStatic.class).logLevel(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new ClassRoutineBuilder(TestStatic.class).loggedWith(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new ClassRoutineBuilder(TestStatic.class).runBy(null);

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
    }

    public void testClassRoutineCache() {

        final NullLog nullLog = new NullLog();
        final Routine<Object, Object> routine1 = JavaRoutine.on(TestStatic.class)
                                                            .sequential()
                                                            .runBy(Runners.sharedRunner())
                                                            .logLevel(LogLevel.DEBUG)
                                                            .loggedWith(nullLog)
                                                            .asyncMethod(TestStatic.GET);

        assertThat(routine1.call()).containsExactly(-77L);

        final Routine<Object, Object> routine2 = JavaRoutine.on(TestStatic.class)
                                                            .sequential()
                                                            .runBy(Runners.sharedRunner())
                                                            .logLevel(LogLevel.DEBUG)
                                                            .loggedWith(nullLog)
                                                            .asyncMethod(TestStatic.GET);

        assertThat(routine2.call()).containsExactly(-77L);
        assertThat(routine1).isEqualTo(routine2);

        final Routine<Object, Object> routine3 = JavaRoutine.on(TestStatic.class)
                                                            .queued()
                                                            .runBy(Runners.sharedRunner())
                                                            .logLevel(LogLevel.DEBUG)
                                                            .loggedWith(nullLog)
                                                            .asyncMethod(TestStatic.GET);

        assertThat(routine3.call()).containsExactly(-77L);
        assertThat(routine1).isNotEqualTo(routine3);
        assertThat(routine2).isNotEqualTo(routine3);

        final Routine<Object, Object> routine4 = JavaRoutine.on(TestStatic.class)
                                                            .queued()
                                                            .runBy(Runners.sharedRunner())
                                                            .logLevel(LogLevel.WARNING)
                                                            .loggedWith(nullLog)
                                                            .asyncMethod(TestStatic.GET);

        assertThat(routine4.call()).containsExactly(-77L);
        assertThat(routine3).isNotEqualTo(routine4);

        final Routine<Object, Object> routine5 = JavaRoutine.on(TestStatic.class)
                                                            .queued()
                                                            .runBy(Runners.sharedRunner())
                                                            .logLevel(LogLevel.WARNING)
                                                            .loggedWith(new NullLog())
                                                            .asyncMethod(TestStatic.GET);

        assertThat(routine5.call()).containsExactly(-77L);
        assertThat(routine4).isNotEqualTo(routine5);
    }

    public void testObjectRoutineBuilder() throws NoSuchMethodException {

        final Routine<Object, Object> routine = JavaRoutine.on(new Test())
                                                           .sequential()
                                                           .runBy(Runners.poolRunner())
                                                           .maxRunning(1)
                                                           .maxRetained(1)
                                                           .availableTimeout(1, TimeUnit.SECONDS)
                                                           .maxInputSize(2)
                                                           .inputTimeout(1, TimeUnit.SECONDS)
                                                           .orderedInput()
                                                           .maxOutputSize(2)
                                                           .outputTimeout(1, TimeUnit.SECONDS)
                                                           .orderedOutput()
                                                           .logLevel(LogLevel.DEBUG)
                                                           .loggedWith(new NullLog())
                                                           .asyncMethod(Test.GET);

        assertThat(routine.call()).containsExactly(-77L);

        final Routine<Object, Object> routine1 =
                JavaRoutine.on(new Test()).queued().runBy(Runners.poolRunner()).method("getLong");

        assertThat(routine1.call()).containsExactly(-77L);

        final Routine<Object, Object> routine2 = JavaRoutine.on(new Test())
                                                            .queued()
                                                            .runBy(Runners.poolRunner())
                                                            .maxRunning(1)
                                                            .availableTimeout(TimeDuration.ZERO)
                                                            .maxInputSize(2)
                                                            .inputTimeout(TimeDuration.ZERO)
                                                            .orderedInput()
                                                            .maxOutputSize(2)
                                                            .outputTimeout(TimeDuration.ZERO)
                                                            .orderedOutput()
                                                            .lockId("test")
                                                            .method(Test.class.getMethod(
                                                                    "getLong"));

        assertThat(routine2.call()).containsExactly(-77L);

        final Routine<Object, Object> routine3 = JavaRoutine.on(new Test()).asyncMethod(Test.THROW);

        try {

            routine3.call(new IllegalArgumentException("test"));

            fail();

        } catch (final RoutineInvocationException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
            assertThat(e.getCause().getMessage()).isEqualTo("test");
        }

        final ObjectRoutineBuilder builder = JavaRoutine.on(new Test2());

        long startTime = System.currentTimeMillis();

        OutputChannel<Object> getOne = builder.method("getOne").runAsync();
        OutputChannel<Object> getTwo = builder.method("getTwo").runAsync();

        assertThat(getOne.isComplete()).isTrue();
        assertThat(getTwo.isComplete()).isTrue();
        assertThat(System.currentTimeMillis() - startTime).isLessThan(1000);

        startTime = System.currentTimeMillis();

        getOne = builder.lockId("test").method("getOne").runAsync();
        getTwo = builder.lockId("test").method("getTwo").runAsync();

        assertThat(getOne.isComplete()).isTrue();
        assertThat(getTwo.isComplete()).isTrue();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(1000);
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

            new ObjectRoutineBuilder(test).asyncMethod("test");

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new ObjectRoutineBuilder(test).availableTimeout(1, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new ObjectRoutineBuilder(test).availableTimeout(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new ObjectRoutineBuilder(test).availableTimeout(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new ObjectRoutineBuilder(test).inputTimeout(1, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new ObjectRoutineBuilder(test).inputTimeout(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new ObjectRoutineBuilder(test).inputTimeout(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new ObjectRoutineBuilder(test).maxInputSize(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new ObjectRoutineBuilder(test).outputTimeout(1, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new ObjectRoutineBuilder(test).outputTimeout(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new ObjectRoutineBuilder(test).outputTimeout(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new ObjectRoutineBuilder(test).maxOutputSize(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new ObjectRoutineBuilder(test).logLevel(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new ObjectRoutineBuilder(test).loggedWith(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new ObjectRoutineBuilder(test).runBy(null);

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

            new ObjectRoutineBuilder(test).as((Class<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new ObjectRoutineBuilder(test).as((ClassToken<?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new ObjectRoutineBuilder(test).as(Test.class);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new ObjectRoutineBuilder(test).as(ClassToken.tokenOf(Test.class));

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JavaRoutine.on(test).as(TestItf.class).throwException(null);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JavaRoutine.on(test).as(TestItf.class).throwException1(null);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JavaRoutine.on(test).as(TestItf.class).throwException2(null);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testObjectRoutineCache() {

        final Test test = new Test();
        final NullLog nullLog = new NullLog();
        final Routine<Object, Object> routine1 = JavaRoutine.on(test)
                                                            .sequential()
                                                            .runBy(Runners.sharedRunner())
                                                            .logLevel(LogLevel.DEBUG)
                                                            .loggedWith(nullLog)
                                                            .asyncMethod(Test.GET);

        assertThat(routine1.call()).containsExactly(-77L);

        final Routine<Object, Object> routine2 = JavaRoutine.on(test)
                                                            .sequential()
                                                            .runBy(Runners.sharedRunner())
                                                            .logLevel(LogLevel.DEBUG)
                                                            .loggedWith(nullLog)
                                                            .asyncMethod(Test.GET);

        assertThat(routine2.call()).containsExactly(-77L);
        assertThat(routine1).isEqualTo(routine2);

        final Routine<Object, Object> routine3 = JavaRoutine.on(test)
                                                            .queued()
                                                            .runBy(Runners.sharedRunner())
                                                            .logLevel(LogLevel.DEBUG)
                                                            .loggedWith(nullLog)
                                                            .asyncMethod(Test.GET);

        assertThat(routine3.call()).containsExactly(-77L);
        assertThat(routine1).isNotEqualTo(routine3);
        assertThat(routine2).isNotEqualTo(routine3);

        final Routine<Object, Object> routine4 = JavaRoutine.on(test)
                                                            .queued()
                                                            .runBy(Runners.sharedRunner())
                                                            .logLevel(LogLevel.WARNING)
                                                            .loggedWith(nullLog)
                                                            .asyncMethod(Test.GET);

        assertThat(routine4.call()).containsExactly(-77L);
        assertThat(routine3).isNotEqualTo(routine4);

        final Routine<Object, Object> routine5 = JavaRoutine.on(test)
                                                            .queued()
                                                            .runBy(Runners.sharedRunner())
                                                            .logLevel(LogLevel.WARNING)
                                                            .loggedWith(new NullLog())
                                                            .asyncMethod(Test.GET);

        assertThat(routine5.call()).containsExactly(-77L);
        assertThat(routine4).isNotEqualTo(routine5);
    }

    public void testObjectRoutineParallel() {

        final Square square = new Square();
        final SquareItf squareAsync = JavaRoutine.on(square).as(SquareItf.class);

        assertThat(squareAsync.compute(3)).isEqualTo(9);
        assertThat(squareAsync.computeParallel1(1, 2, 3).readAll()).contains(1, 4, 9);
        assertThat(squareAsync.computeParallel2(1, 2, 3).readAll()).contains(1, 4, 9);
        assertThat(squareAsync.computeParallel3(Arrays.asList(1, 2, 3)).readAll()).contains(1, 4,
                                                                                            9);

        final IOChannel<Integer> channel = JavaRoutine.io().buildChannel();

        channel.input().pass(1, 2, 3);
        channel.close();
        assertThat(squareAsync.computeParallel4(channel.output()).readAll()).contains(1, 4, 9);
    }

    public void testRoutineBuilder() {

        final Routine<String, String> routine =
                JavaRoutine.on(ClassToken.tokenOf(PassThroughInvocation.class))
                           .sequential()
                           .runBy(Runners.poolRunner())
                           .maxRetained(0)
                           .maxRunning(1)
                           .availableTimeout(1, TimeUnit.SECONDS)
                           .maxInputSize(2)
                           .inputTimeout(1, TimeUnit.SECONDS)
                           .maxOutputSize(2)
                           .outputTimeout(1, TimeUnit.SECONDS)
                           .orderedOutput()
                           .buildRoutine();

        assertThat(routine.call("test1", "test2")).containsExactly("test1", "test2");

        final Routine<String, String> routine1 =
                JavaRoutine.on(ClassToken.tokenOf(PassThroughInvocation.class))
                           .queued()
                           .runBy(Runners.poolRunner())
                           .maxRetained(0)
                           .maxRunning(1)
                           .availableTimeout(TimeDuration.ZERO)
                           .maxInputSize(2)
                           .inputTimeout(TimeDuration.ZERO)
                           .maxOutputSize(2)
                           .outputTimeout(TimeDuration.ZERO)
                           .orderedOutput()
                           .buildRoutine();

        assertThat(routine1.call("test1", "test2")).containsExactly("test1", "test2");
    }

    @SuppressWarnings("ConstantConditions")
    public void testRoutineBuilderError() {

        try {

            new InvocationRoutineBuilder<String, String>(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new InvocationRoutineBuilder<String, String>(
                    ClassToken.tokenOf(PassThroughInvocation.class)).availableTimeout(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new InvocationRoutineBuilder<String, String>(
                    ClassToken.tokenOf(PassThroughInvocation.class)).logLevel(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new InvocationRoutineBuilder<String, String>(
                    ClassToken.tokenOf(PassThroughInvocation.class)).loggedWith(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new InvocationRoutineBuilder<String, String>(
                    ClassToken.tokenOf(PassThroughInvocation.class)).runBy(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new InvocationRoutineBuilder<String, String>(
                    ClassToken.tokenOf(PassThroughInvocation.class)).withArgs((Object[]) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new InvocationRoutineBuilder<String, String>(
                    ClassToken.tokenOf(PassThroughInvocation.class)).maxRunning(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new InvocationRoutineBuilder<String, String>(
                    ClassToken.tokenOf(PassThroughInvocation.class)).maxRetained(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    private static interface SquareItf {

        public int compute(int i);

        @Async(value = "compute", lockId = Async.UNLOCKED)
        @AsyncOverride(value = int.class, parallel = true, result = true)
        public OutputChannel<Integer> computeParallel1(int... i);

        @Async(value = "compute")
        @AsyncOverride(value = int.class, parallel = true, result = true)
        public OutputChannel<Integer> computeParallel2(Integer... i);

        @Async(value = "compute", lockId = Async.UNLOCKED)
        @AsyncOverride(value = int.class, parallel = true, result = true)
        public OutputChannel<Integer> computeParallel3(List<Integer> i);

        @Async(value = "compute", lockId = Async.UNLOCKED)
        @AsyncOverride(value = int.class, parallel = true, result = true)
        public OutputChannel<Integer> computeParallel4(OutputChannel<Integer> i);
    }

    private static interface TestItf {

        @AsyncOverride({RuntimeException.class, int.class})
        public void throwException(RuntimeException ex);

        @Async(Test.THROW)
        @AsyncOverride({int.class})
        public void throwException1(RuntimeException ex);

        @Async(Test.THROW)
        public int throwException2(RuntimeException ex);
    }

    private static class DuplicateAnnotation {

        public static final String GET = "get";

        @Async(value = GET)
        public int getOne() {

            return 1;
        }

        @Async(value = GET)
        public int getTwo() {

            return 2;
        }
    }

    private static class DuplicateAnnotationStatic {

        public static final String GET = "get";

        @Async(GET)
        public static int getOne() {

            return 1;
        }

        @Async(GET)
        public static int getTwo() {

            return 2;
        }
    }

    private static class MyRunner extends RunnerWrapper {

        public MyRunner() {

            super(Runners.queuedRunner());
        }
    }

    private static class PassThroughInvocation extends BasicInvocation<String, String> {

        @Override
        public void onInput(final String s, @Nonnull final ResultChannel<String> results) {

            results.pass(s);
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

        @Async(value = GET)
        public long getLong() {

            return -77;
        }

        @Async(value = THROW, log = NullLog.class, logLevel = LogLevel.DEBUG, sequential = false,
               runner = MyRunner.class)
        public void throwException(final RuntimeException ex) {

            throw ex;
        }
    }

    private static class Test2 {

        @Async(lockId = "1")
        public int getOne() throws InterruptedException {

            TimeDuration.millis(500).sleepAtLeast();

            return 1;
        }

        @Async(lockId = "2")
        public int getTwo() throws InterruptedException {

            TimeDuration.millis(500).sleepAtLeast();

            return 2;
        }
    }

    private static class TestStatic {

        public static final String GET = "get";

        public static final String THROW = "throw";

        @Async(GET)
        public static long getLong() {

            return -77;
        }

        @Async(value = THROW, log = NullLog.class, logLevel = LogLevel.DEBUG, sequential = false,
               runner = MyRunner.class)
        public static void throwException(final RuntimeException ex) {

            throw ex;
        }
    }

    private static class TestStatic2 {

        @Async(lockId = "1")
        public static int getOne() throws InterruptedException {

            TimeDuration.millis(500).sleepAtLeast();

            return 1;
        }

        @Async(lockId = "2")
        public static int getTwo() throws InterruptedException {

            TimeDuration.millis(500).sleepAtLeast();

            return 2;
        }
    }
}
