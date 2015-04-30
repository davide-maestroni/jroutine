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

import com.gh.bmd.jrt.annotation.Bind;
import com.gh.bmd.jrt.annotation.Pass;
import com.gh.bmd.jrt.builder.ClassRoutineBuilder;
import com.gh.bmd.jrt.builder.ProxyConfiguration;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.builder.RoutineConfiguration.OrderType;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.common.InvocationException;
import com.gh.bmd.jrt.log.Log;
import com.gh.bmd.jrt.log.Log.LogLevel;
import com.gh.bmd.jrt.log.NullLog;
import com.gh.bmd.jrt.routine.Routine;
import com.gh.bmd.jrt.runner.Runners;
import com.gh.bmd.jrt.time.TimeDuration;

import org.junit.Test;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.gh.bmd.jrt.time.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Class routine builder unit tests.
 * <p/>
 * Created by davide on 3/26/15.
 */
public class ClassRoutineBuilderTest {

    @Test
    public void testBoundMethod() throws NoSuchMethodException {

        final TimeDuration timeout = seconds(1);
        final Routine<Object, Object> routine = JRoutine.on(TestStatic.class)
                                                        .withConfiguration()
                                                        .withSyncRunner(Runners.sequentialRunner())
                                                        .withAsyncRunner(Runners.poolRunner())
                                                        .withLogLevel(LogLevel.DEBUG)
                                                        .withLog(new NullLog())
                                                        .apply()
                                                        .boundMethod(TestStatic.GET);

        assertThat(routine.callSync().afterMax(timeout).readAll()).containsExactly(-77L);
    }

    @Test
    public void testBoundMethodError() {

        try {

            new DefaultClassRoutineBuilder(TestStatic.class).boundMethod("test");

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testConfigurationErrors() {

        try {

            new DefaultClassRoutineBuilder(TestStatic.class).apply((RoutineConfiguration) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultClassRoutineBuilder(TestStatic.class).apply((ProxyConfiguration) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testConfigurationWarnings() {

        final CountLog countLog = new CountLog();
        JRoutine.on(TestStatic.class)
                .withConfiguration()
                .withFactoryArgs()
                .withInputOrder(OrderType.NONE)
                .withInputMaxSize(3)
                .withInputTimeout(seconds(1))
                .withOutputOrder(OrderType.NONE)
                .withOutputMaxSize(3)
                .withOutputTimeout(seconds(1))
                .withLogLevel(LogLevel.DEBUG)
                .withLog(countLog)
                .apply()
                .boundMethod(TestStatic.GET);
        assertThat(countLog.getWrnCount()).isEqualTo(7);
    }

    @Test
    public void testDuplicateAnnotationError() {

        try {

            new DefaultClassRoutineBuilder(DuplicateAnnotationStatic.class);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testException() throws NoSuchMethodException {

        final TimeDuration timeout = seconds(1);

        final Routine<Object, Object> routine3 =
                JRoutine.on(TestStatic.class).boundMethod(TestStatic.THROW);

        try {

            routine3.callSync(new IllegalArgumentException("test")).afterMax(timeout).readAll();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
            assertThat(e.getCause().getMessage()).isEqualTo("test");
        }
    }

    @Test
    public void testInterfaceError() {

        try {

            new DefaultClassRoutineBuilder(TestItf.class);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testMethod() throws NoSuchMethodException {

        final TimeDuration timeout = seconds(1);
        final Routine<Object, Object> routine2 = JRoutine.on(TestStatic.class)
                                                         .withConfiguration()
                                                         .withSyncRunner(Runners.queuedRunner())
                                                         .withAsyncRunner(Runners.poolRunner())
                                                         .withMaxInvocations(1)
                                                         .withCoreInvocations(0)
                                                         .withAvailableTimeout(1, TimeUnit.SECONDS)
                                                         .apply()
                                                         .withProxy()
                                                         .withShareGroup("test")
                                                         .apply()
                                                         .method(TestStatic.class.getMethod(
                                                                 "getLong"));

        assertThat(routine2.callSync().afterMax(timeout).readAll()).containsExactly(-77L);
    }

    @Test
    public void testMethodBySignature() throws NoSuchMethodException {

        final TimeDuration timeout = seconds(1);
        final Routine<Object, Object> routine1 = JRoutine.on(TestStatic.class)
                                                         .withConfiguration()
                                                         .withSyncRunner(Runners.queuedRunner())
                                                         .withAsyncRunner(Runners.poolRunner())
                                                         .withMaxInvocations(1)
                                                         .withAvailableTimeout(TimeDuration.ZERO)
                                                         .apply()
                                                         .method("getLong");

        assertThat(routine1.callSync().afterMax(timeout).readAll()).containsExactly(-77L);

    }

    @Test
    public void testMethodError() {

        try {

            new DefaultClassRoutineBuilder(TestStatic.class).method("test");

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testNullError() {

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
    }

    @Test
    public void testRoutineCache() {

        final NullLog nullLog = new NullLog();
        final Routine<Object, Object> routine1 = JRoutine.on(TestStatic.class)
                                                         .withConfiguration()
                                                         .withSyncRunner(Runners.sequentialRunner())
                                                         .withAsyncRunner(Runners.sharedRunner())
                                                         .withLogLevel(LogLevel.DEBUG)
                                                         .withLog(nullLog)
                                                         .apply()
                                                         .boundMethod(TestStatic.GET);

        assertThat(routine1.callSync().readAll()).containsExactly(-77L);

        final Routine<Object, Object> routine2 = JRoutine.on(TestStatic.class)
                                                         .withConfiguration()
                                                         .withSyncRunner(Runners.sequentialRunner())
                                                         .withAsyncRunner(Runners.sharedRunner())
                                                         .withLogLevel(LogLevel.DEBUG)
                                                         .withLog(nullLog)
                                                         .apply()
                                                         .boundMethod(TestStatic.GET);

        assertThat(routine2.callSync().readAll()).containsExactly(-77L);
        assertThat(routine1).isEqualTo(routine2);

        final Routine<Object, Object> routine3 = JRoutine.on(TestStatic.class)
                                                         .withConfiguration()
                                                         .withSyncRunner(Runners.queuedRunner())
                                                         .withAsyncRunner(Runners.sharedRunner())
                                                         .withLogLevel(LogLevel.DEBUG)
                                                         .withLog(nullLog)
                                                         .apply()
                                                         .boundMethod(TestStatic.GET);

        assertThat(routine3.callSync().readAll()).containsExactly(-77L);
        assertThat(routine1).isNotEqualTo(routine3);
        assertThat(routine2).isNotEqualTo(routine3);

        final Routine<Object, Object> routine4 = JRoutine.on(TestStatic.class)
                                                         .withConfiguration()
                                                         .withSyncRunner(Runners.queuedRunner())
                                                         .withAsyncRunner(Runners.sharedRunner())
                                                         .withLogLevel(LogLevel.WARNING)
                                                         .withLog(nullLog)
                                                         .apply()
                                                         .boundMethod(TestStatic.GET);

        assertThat(routine4.callSync().readAll()).containsExactly(-77L);
        assertThat(routine3).isNotEqualTo(routine4);

        final Routine<Object, Object> routine5 = JRoutine.on(TestStatic.class)
                                                         .withConfiguration()
                                                         .withSyncRunner(Runners.queuedRunner())
                                                         .withAsyncRunner(Runners.sharedRunner())
                                                         .withLogLevel(LogLevel.WARNING)
                                                         .withLog(new NullLog())
                                                         .apply()
                                                         .boundMethod(TestStatic.GET);

        assertThat(routine5.callSync().readAll()).containsExactly(-77L);
        assertThat(routine4).isNotEqualTo(routine5);
    }

    @Test
    public void testShareGroup() throws NoSuchMethodException {

        final ClassRoutineBuilder builder = JRoutine.on(TestStatic2.class)
                                                    .withConfiguration()
                                                    .withReadTimeout(seconds(2))
                                                    .apply();

        long startTime = System.currentTimeMillis();

        OutputChannel<Object> getOne =
                builder.withProxy().withShareGroup("1").apply().method("getOne").callAsync();
        OutputChannel<Object> getTwo =
                builder.withProxy().withShareGroup("2").apply().method("getTwo").callAsync();

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

    @SuppressWarnings("unused")
    private interface TestItf {

        void throwException(@Pass(int.class) RuntimeException ex);
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

    @SuppressWarnings("unused")
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

    @SuppressWarnings("unused")
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
