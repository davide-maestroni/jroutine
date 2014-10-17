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
import com.bmd.jrt.channel.ResultChannel;
import com.bmd.jrt.common.ClassToken;
import com.bmd.jrt.execution.BasicExecution;
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

        final Routine<Object, Object> routine = JavaRoutine.on(Test.class)
                                                           .sequential()
                                                           .runBy(Runners.pool())
                                                           .logLevel(LogLevel.DEBUG)
                                                           .loggedWith(new NullLog())
                                                           .method(Test.NAME);

        assertThat(routine.call()).containsExactly(-77L);

        final Routine<Object, Object> routine1 =
                JavaRoutine.on(Test.class).queued().runBy(Runners.pool()).classMethod("getLong");

        assertThat(routine1.call()).containsExactly(-77L);

        final Routine<Object, Object> routine2 = JavaRoutine.on(Test.class)
                                                            .queued()
                                                            .runBy(Runners.pool())
                                                            .parallelGroup("test")
                                                            .classMethod(Test.class.getMethod(
                                                                    "getLong"));

        assertThat(routine2.call()).containsExactly(-77L);

        final Catch testCatch = new Catch() {

            @Override
            public void exception(@Nonnull final RoutineInvocationException ex) {

                assertThat(ex.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
            }
        };

        final Routine<Object, Object> routine3 =
                JavaRoutine.on(Test.class).withinTry(testCatch).method(Test.THROW);

        assertThat(routine3.call(new IllegalArgumentException())).isEmpty();
    }

    @SuppressWarnings("ConstantConditions")
    public void testClassRoutineBuilderError() {

        try {

            new ClassRoutineBuilder(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new ClassRoutineBuilder(DuplicateAnnotation.class);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new ClassRoutineBuilder(Test.class).classMethod("test");

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new ClassRoutineBuilder(Test.class).method("test");

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new ClassRoutineBuilder(Test.class).logLevel(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new ClassRoutineBuilder(Test.class).loggedWith(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new ClassRoutineBuilder(Test.class).runBy(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new ClassRoutineBuilder(Test.class).withinTry(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testRoutineBuilder() {

        final Routine<String, String> routine =
                JavaRoutine.on(ClassToken.tokenOf(PassThroughExecution.class))
                           .sequential()
                           .runBy(Runners.pool())
                           .maxRetained(0)
                           .maxRunning(1)
                           .availableTimeout(1, TimeUnit.SECONDS)
                           .buildRoutine();

        assertThat(routine.call("test1", "test2")).containsExactly("test1", "test2");

        final Routine<String, String> routine1 =
                JavaRoutine.on(ClassToken.tokenOf(PassThroughExecution.class))
                           .queued()
                           .runBy(Runners.pool())
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
                    ClassToken.tokenOf(PassThroughExecution.class)).availableTimeout(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new RoutineBuilder<String, String>(
                    ClassToken.tokenOf(PassThroughExecution.class)).logLevel(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new RoutineBuilder<String, String>(
                    ClassToken.tokenOf(PassThroughExecution.class)).loggedWith(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new RoutineBuilder<String, String>(
                    ClassToken.tokenOf(PassThroughExecution.class)).runBy(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new RoutineBuilder<String, String>(
                    ClassToken.tokenOf(PassThroughExecution.class)).withArgs((Object[]) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new RoutineBuilder<String, String>(
                    ClassToken.tokenOf(PassThroughExecution.class)).maxRunning(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new RoutineBuilder<String, String>(
                    ClassToken.tokenOf(PassThroughExecution.class)).maxRetained(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    private static class DuplicateAnnotation {

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

            super(Runners.queued());
        }
    }

    private static class PassThroughExecution extends BasicExecution<String, String> {

        @Override
        public void onInput(final String s, @Nonnull final ResultChannel<String> results) {

            results.pass(s);
        }
    }

    private static class Test {

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
}