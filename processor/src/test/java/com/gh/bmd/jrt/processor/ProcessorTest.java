/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh.bmd.jrt.processor;

import com.gh.bmd.jrt.annotation.Pass;
import com.gh.bmd.jrt.annotation.Timeout;
import com.gh.bmd.jrt.annotation.Wrap;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.builder.RoutineConfiguration.OrderType;
import com.gh.bmd.jrt.builder.RoutineConfiguration.RunnerType;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.StandaloneChannel;
import com.gh.bmd.jrt.common.ClassToken;
import com.gh.bmd.jrt.log.Log;
import com.gh.bmd.jrt.log.Log.LogLevel;
import com.gh.bmd.jrt.log.NullLog;
import com.gh.bmd.jrt.routine.JRoutine;
import com.gh.bmd.jrt.runner.Runner;
import com.gh.bmd.jrt.runner.Runners;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.gh.bmd.jrt.builder.RoutineConfiguration.builder;
import static com.gh.bmd.jrt.builder.RoutineConfiguration.withSyncRunner;
import static com.gh.bmd.jrt.time.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Processor unit tests.
 * <p/>
 * Created by davide on 3/6/15.
 */
public class ProcessorTest {

    @Test
    public void testInterface() {

        final TestClass testClass = new TestClass();
        final TestInterfaceWrapper testWrapper = JRoutine.on(testClass)
                                                         .withConfiguration(withSyncRunner(
                                                                 RunnerType.SEQUENTIAL))
                                                         .buildWrapper(ClassToken.tokenOf(
                                                                 TestInterfaceWrapper.class));

        assertThat(testWrapper.getOne().readNext()).isEqualTo(1);
    }

    @Test
    public void testWrapper() {

        final NullLog log = new NullLog();
        final Runner runner = Runners.poolRunner();
        final TestClass testClass = new TestClass();
        final TestWrapper testWrapper = JRoutine.on(testClass)
                                                .withConfiguration(builder().withSyncRunner(
                                                        RunnerType.SEQUENTIAL)
                                                                            .withRunner(runner)
                                                                            .withLogLevel(
                                                                                    LogLevel.DEBUG)
                                                                            .withLog(log)
                                                                            .buildConfiguration())
                                                .buildWrapper(
                                                        ClassToken.tokenOf(TestWrapper.class));

        assertThat(testWrapper.getOne().readNext()).isEqualTo(1);
        assertThat(testWrapper.getString(1, 2, 3)).isIn("1", "2", "3");
        assertThat(testWrapper.getString(new HashSet<Integer>(Arrays.asList(1, 2, 3)))
                              .readAll()).containsOnly("1", "2", "3");
        assertThat(testWrapper.getString(Arrays.asList(1, 2, 3))).containsOnly("1", "2", "3");
        assertThat(testWrapper.getString((Iterable<Integer>) Arrays.asList(1, 2, 3))).containsOnly(
                "1", "2", "3");
        assertThat(
                testWrapper.getString((Collection<Integer>) Arrays.asList(1, 2, 3))).containsOnly(
                "1", "2", "3");

        final ArrayList<String> list = new ArrayList<String>();
        assertThat(testWrapper.getList(Collections.singletonList(list))).containsExactly(list);

        final StandaloneChannel<Integer> standaloneChannel = JRoutine.standalone().buildChannel();
        standaloneChannel.input().pass(3).close();
        assertThat(testWrapper.getString(standaloneChannel.output())).isEqualTo("3");

        assertThat(JRoutine.on(testClass)
                           .withConfiguration(builder().withSyncRunner(RunnerType.SEQUENTIAL)
                                                       .withRunner(runner)
                                                       .withLogLevel(LogLevel.DEBUG)
                                                       .withLog(log)
                                                       .buildConfiguration())
                           .buildWrapper(ClassToken.tokenOf(TestWrapper.class))).isSameAs(
                testWrapper);
    }

    @Test
    public void testWrapperBuilder() {

        final NullLog log = new NullLog();
        final Runner runner = Runners.poolRunner();
        final TestClass testClass = new TestClass();
        final TestWrapper testWrapper = JRoutine_TestWrapper.on(testClass)
                                                            .withConfiguration(
                                                                    builder().withSyncRunner(
                                                                            RunnerType.SEQUENTIAL)
                                                                             .withRunner(runner)
                                                                             .withLogLevel(
                                                                                     LogLevel.DEBUG)
                                                                             .withLog(log)
                                                                             .buildConfiguration())
                                                            .buildWrapper();

        assertThat(testWrapper.getOne().readNext()).isEqualTo(1);
        assertThat(testWrapper.getString(1, 2, 3)).isIn("1", "2", "3");
        assertThat(testWrapper.getString(new HashSet<Integer>(Arrays.asList(1, 2, 3)))
                              .readAll()).containsOnly("1", "2", "3");
        assertThat(testWrapper.getString(Arrays.asList(1, 2, 3))).containsOnly("1", "2", "3");
        assertThat(testWrapper.getString((Iterable<Integer>) Arrays.asList(1, 2, 3))).containsOnly(
                "1", "2", "3");
        assertThat(
                testWrapper.getString((Collection<Integer>) Arrays.asList(1, 2, 3))).containsOnly(
                "1", "2", "3");

        final ArrayList<String> list = new ArrayList<String>();
        assertThat(testWrapper.getList(Collections.singletonList(list))).containsExactly(list);

        final StandaloneChannel<Integer> standaloneChannel = JRoutine.standalone().buildChannel();
        standaloneChannel.input().pass(3).close();
        assertThat(testWrapper.getString(standaloneChannel.output())).isEqualTo("3");

        assertThat(JRoutine.on(testClass)
                           .withConfiguration(builder().withSyncRunner(RunnerType.SEQUENTIAL)
                                                       .withRunner(runner)
                                                       .withLogLevel(LogLevel.DEBUG)
                                                       .withLog(log)
                                                       .buildConfiguration())
                           .buildWrapper(ClassToken.tokenOf(TestWrapper.class))).isSameAs(
                testWrapper);
    }

    @Test
    public void testWrapperBuilderWarnings() {

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
        final TestClass testClass = new TestClass();
        JRoutine.on(testClass)
                .withConfiguration(configuration)
                .buildWrapper(TestWrapper.class)
                .getOne();
        assertThat(countLog.getWrnCount()).isEqualTo(6);
    }

    @SuppressWarnings("UnusedDeclaration")
    public interface TestClassInterface {

        int getOne();
    }

    @Wrap(TestClassInterface.class)
    public interface TestInterfaceWrapper {

        @Timeout(300)
        @Pass(int.class)
        OutputChannel<Integer> getOne();
    }

    @Wrap(TestClass.class)
    public interface TestWrapper {

        @Timeout(300)
        @Pass(List.class)
        Iterable<Iterable> getList(@Pass(List.class) List<? extends List<String>> i);

        @Timeout(300)
        @Pass(int.class)
        OutputChannel<Integer> getOne();

        @Timeout(300)
        String getString(@Pass(int.class) int... i);

        @Timeout(300)
        @Pass(String.class)
        OutputChannel<String> getString(@Pass(int.class) HashSet<Integer> i);

        @Timeout(300)
        @Pass(String.class)
        List<String> getString(@Pass(int.class) List<Integer> i);

        @Timeout(300)
        @Pass(String.class)
        Iterable<String> getString(@Pass(int.class) Iterable<Integer> i);

        @Timeout(300)
        @Pass(String.class)
        String[] getString(@Pass(int.class) Collection<Integer> i);

        @Timeout(300)
        String getString(@Pass(int.class) OutputChannel<Integer> i);
    }

    @SuppressWarnings("UnusedDeclaration")
    public static class TestClass implements TestClassInterface {

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
}
