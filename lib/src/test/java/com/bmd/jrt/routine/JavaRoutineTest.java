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
import com.bmd.jrt.annotation.AsyncParameters;
import com.bmd.jrt.channel.OutputChannel;
import com.bmd.jrt.channel.ResultChannel;
import com.bmd.jrt.common.ClassToken;
import com.bmd.jrt.invocation.BasicInvocation;
import com.bmd.jrt.log.Log.LogLevel;
import com.bmd.jrt.log.NullLog;
import com.bmd.jrt.routine.ClassRoutineBuilder.Catch;
import com.bmd.jrt.runner.RunnerDecorator;
import com.bmd.jrt.runner.Runners;
import com.bmd.jrt.time.TimeDuration;

import junit.framework.TestCase;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Routine builder unit tests.
 * <p/>
 * Created by davide on 10/16/14.
 */
public class JavaRoutineTest extends TestCase {

    public void testClassRoutineBuilder() throws NoSuchMethodException {

        final Routine<Object, Object> routine = JavaRoutine.on(TestStatic.class)
                                                           .sequential()
                                                           .runBy(Runners.poolRunner())
                                                           .logLevel(LogLevel.DEBUG)
                                                           .loggedWith(new NullLog())
                                                           .asyncMethod(TestStatic.NAME);

        assertThat(routine.call()).containsExactly(-77L);

        final Routine<Object, Object> routine1 = JavaRoutine.on(TestStatic.class)
                                                            .queued()
                                                            .runBy(Runners.poolRunner())
                                                            .method("getLong");

        assertThat(routine1.call()).containsExactly(-77L);

        final Routine<Object, Object> routine2 = JavaRoutine.on(TestStatic.class)
                                                            .queued()
                                                            .runBy(Runners.poolRunner())
                                                            .parallelId("test")
                                                            .method(TestStatic.class.getMethod(
                                                                    "getLong"));

        assertThat(routine2.call()).containsExactly(-77L);

        final Catch testCatch = new Catch() {

            @Override
            public void exception(@Nonnull final RoutineInvocationException ex) {

                assertThat(ex.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
            }
        };

        final Routine<Object, Object> routine3 =
                JavaRoutine.on(TestStatic.class).withinTry(testCatch).asyncMethod(TestStatic.THROW);

        assertThat(routine3.call(new IllegalArgumentException())).isEmpty();

        final ClassRoutineBuilder builder = JavaRoutine.on(TestStatic2.class);

        long startTime = System.currentTimeMillis();

        OutputChannel<Object> getOne = builder.method("getOne").runAsync();
        OutputChannel<Object> getTwo = builder.method("getTwo").runAsync();

        assertThat(getOne.waitComplete()).isTrue();
        assertThat(getTwo.waitComplete()).isTrue();
        assertThat(System.currentTimeMillis() - startTime).isLessThan(1000);

        startTime = System.currentTimeMillis();

        getOne = builder.parallelId("test").method("getOne").runAsync();
        getTwo = builder.parallelId("test").method("getTwo").runAsync();

        assertThat(getOne.waitComplete()).isTrue();
        assertThat(getTwo.waitComplete()).isTrue();
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

            new ClassRoutineBuilder(TestStatic.class).withinTry(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testObjectRoutineBuilder() throws NoSuchMethodException {

        final Routine<Object, Object> routine = JavaRoutine.on(new Test())
                                                           .sequential()
                                                           .runBy(Runners.poolRunner())
                                                           .logLevel(LogLevel.DEBUG)
                                                           .loggedWith(new NullLog())
                                                           .asyncMethod(Test.NAME);

        assertThat(routine.call()).containsExactly(-77L);

        final Routine<Object, Object> routine1 =
                JavaRoutine.on(new Test()).queued().runBy(Runners.poolRunner()).method("getLong");

        assertThat(routine1.call()).containsExactly(-77L);

        final Routine<Object, Object> routine2 = JavaRoutine.on(new Test())
                                                            .queued()
                                                            .runBy(Runners.poolRunner())
                                                            .parallelId("test")
                                                            .method(Test.class.getMethod(
                                                                    "getLong"));

        assertThat(routine2.call()).containsExactly(-77L);

        final Catch testCatch = new Catch() {

            @Override
            public void exception(@Nonnull final RoutineInvocationException ex) {

                assertThat(ex.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
            }
        };

        final Routine<Object, Object> routine3 =
                JavaRoutine.on(new Test()).withinTry(testCatch).asyncMethod(Test.THROW);

        assertThat(routine3.call(new IllegalArgumentException())).isEmpty();

        final ObjectRoutineBuilder builder = JavaRoutine.on(new Test2());

        long startTime = System.currentTimeMillis();

        OutputChannel<Object> getOne = builder.method("getOne").runAsync();
        OutputChannel<Object> getTwo = builder.method("getTwo").runAsync();

        assertThat(getOne.waitComplete()).isTrue();
        assertThat(getTwo.waitComplete()).isTrue();
        assertThat(System.currentTimeMillis() - startTime).isLessThan(1000);

        startTime = System.currentTimeMillis();

        getOne = builder.parallelId("test").method("getOne").runAsync();
        getTwo = builder.parallelId("test").method("getTwo").runAsync();

        assertThat(getOne.waitComplete()).isTrue();
        assertThat(getTwo.waitComplete()).isTrue();
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

        try {

            new ObjectRoutineBuilder(new Test()).method("test");

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new ObjectRoutineBuilder(new Test()).asyncMethod("test");

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new ObjectRoutineBuilder(new Test()).logLevel(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new ObjectRoutineBuilder(new Test()).loggedWith(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new ObjectRoutineBuilder(new Test()).runBy(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new ObjectRoutineBuilder(new Test()).withinTry(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new ObjectRoutineBuilder(new Test()).as(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new ObjectRoutineBuilder(new Test()).as(Test.class);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new ObjectRoutineBuilder(new Test()).as(Test.class);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JavaRoutine.on(new Test()).as(TestItf.class).throwException(null);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JavaRoutine.on(new Test()).as(TestItf.class).throwException1(null);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            JavaRoutine.on(new Test()).as(TestItf.class).throwException2(null);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testRoutineBuilder() {

        final Routine<String, String> routine =
                JavaRoutine.on(ClassToken.tokenOf(PassThroughInvocation.class))
                           .sequential()
                           .runBy(Runners.poolRunner())
                           .maxRetained(0)
                           .maxRunning(1)
                           .availableTimeout(1, TimeUnit.SECONDS)
                           .buildRoutine();

        assertThat(routine.call("test1", "test2")).containsExactly("test1", "test2");

        final Routine<String, String> routine1 =
                JavaRoutine.on(ClassToken.tokenOf(PassThroughInvocation.class))
                           .queued()
                           .runBy(Runners.poolRunner())
                           .maxRetained(0)
                           .maxRunning(1)
                           .availableTimeout(TimeDuration.ZERO)
                           .buildRoutine();

        assertThat(routine1.call("test1", "test2")).containsExactly("test1", "test2");
    }

    @SuppressWarnings("ConstantConditions")
    public void testRoutineBuilderError() {

        try {

            new RoutineBuilder<String, String>(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new RoutineBuilder<String, String>(
                    ClassToken.tokenOf(PassThroughInvocation.class)).availableTimeout(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new RoutineBuilder<String, String>(
                    ClassToken.tokenOf(PassThroughInvocation.class)).logLevel(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new RoutineBuilder<String, String>(
                    ClassToken.tokenOf(PassThroughInvocation.class)).loggedWith(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new RoutineBuilder<String, String>(
                    ClassToken.tokenOf(PassThroughInvocation.class)).runBy(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new RoutineBuilder<String, String>(
                    ClassToken.tokenOf(PassThroughInvocation.class)).withArgs((Object[]) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new RoutineBuilder<String, String>(
                    ClassToken.tokenOf(PassThroughInvocation.class)).maxRunning(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new RoutineBuilder<String, String>(
                    ClassToken.tokenOf(PassThroughInvocation.class)).maxRetained(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    private static interface TestItf {

        @AsyncParameters({RuntimeException.class, int.class})
        public void throwException(RuntimeException ex);

        @Async(name = Test.THROW)
        @AsyncParameters({int.class})
        public void throwException1(RuntimeException ex);

        @Async(name = Test.THROW)
        public int throwException2(RuntimeException ex);
    }

    private static class DuplicateAnnotation {

        public static final String NAME = "get";

        @Async(name = NAME)
        public int getOne() {

            return 1;
        }

        @Async(name = NAME)
        public int getTwo() {

            return 2;
        }
    }

    private static class DuplicateAnnotationStatic {

        public static final String NAME = "get";

        @Async(name = NAME)
        public static int getOne() {

            return 1;
        }

        @Async(name = NAME)
        public static int getTwo() {

            return 2;
        }
    }

    private static class MyRunner extends RunnerDecorator {

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

    private static class Test {

        public static final String NAME = "get";

        public static final String THROW = "throw";

        @Async(name = NAME)
        public long getLong() {

            return -77;
        }

        @Async(name = THROW, log = NullLog.class, logLevel = LogLevel.DEBUG, sequential = false,
               runner = MyRunner.class)
        public void throwException(final RuntimeException ex) {

            throw ex;
        }
    }

    private static class Test2 {

        @Async(parallelId = "1")
        public int getOne() throws InterruptedException {

            TimeDuration.millis(500).sleepAtLeast();

            return 1;
        }

        @Async(parallelId = "2")
        public int getTwo() throws InterruptedException {

            TimeDuration.millis(500).sleepAtLeast();

            return 2;
        }
    }

    private static class TestStatic {

        public static final String NAME = "get";

        public static final String THROW = "throw";

        @Async(name = NAME)
        public static long getLong() {

            return -77;
        }

        @Async(name = THROW, log = NullLog.class, logLevel = LogLevel.DEBUG, sequential = false,
               runner = MyRunner.class)
        public static void throwException(final RuntimeException ex) {

            throw ex;
        }
    }

    private static class TestStatic2 {

        @Async(parallelId = "1")
        public static int getOne() throws InterruptedException {

            TimeDuration.millis(500).sleepAtLeast();

            return 1;
        }

        @Async(parallelId = "2")
        public static int getTwo() throws InterruptedException {

            TimeDuration.millis(500).sleepAtLeast();

            return 2;
        }
    }
}