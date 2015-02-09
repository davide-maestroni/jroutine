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
import com.bmd.jrt.annotation.Timeout;
import com.bmd.jrt.annotation.Wrap;
import com.bmd.jrt.builder.RoutineBuilder.RunnerType;
import com.bmd.jrt.channel.OutputChannel;
import com.bmd.jrt.channel.StandaloneChannel;
import com.bmd.jrt.common.ClassToken;
import com.bmd.jrt.log.Log.LogLevel;
import com.bmd.jrt.log.NullLog;
import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.runner.Runners;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Java routine processor unit tests.
 * <p/>
 * Created by davide on 11/18/14.
 */
public class RoutineProcessorTest extends TestCase {

    public void testInterface() {

        final TestClass testClass = new TestClass();
        final TestInterfaceWrapper testWrapper = JRoutine.on(testClass)
                                                         .withSyncRunner(RunnerType.SEQUENTIAL)
                                                         .buildWrapper(ClassToken.tokenOf(
                                                                 TestInterfaceWrapper.class));

        assertThat(testWrapper.getOne().readNext()).isEqualTo(1);
    }

    @SuppressWarnings("unchecked")
    public void testWrapper() {

        final NullLog log = new NullLog();
        final Runner runner = Runners.poolRunner();
        final TestClass testClass = new TestClass();
        final TestWrapper testWrapper = JRoutine.on(testClass)
                                                .withSyncRunner(RunnerType.SEQUENTIAL)
                                                .withRunner(runner)
                                                .withLogLevel(LogLevel.DEBUG)
                                                .withLog(log)
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
        assertThat(testWrapper.getList(Arrays.asList(list))).containsExactly(list);

        final StandaloneChannel<Integer> standaloneChannel = JRoutine.on().buildChannel();
        standaloneChannel.input().pass(3).close();
        assertThat(testWrapper.getString(standaloneChannel.output())).isEqualTo("3");

        assertThat(JRoutine.on(testClass)
                           .withSyncRunner(RunnerType.SEQUENTIAL)
                           .withRunner(runner)
                           .withLogLevel(LogLevel.DEBUG)
                           .withLog(log)
                           .buildWrapper(ClassToken.tokenOf(TestWrapper.class))).isSameAs(
                testWrapper);
    }

    @SuppressWarnings("UnusedDeclaration")
    public interface TestClassInterface {

        public int getOne();
    }

    @Wrap(TestClassInterface.class)
    public interface TestInterfaceWrapper {

        @Timeout(300)
        @Async(int.class)
        public OutputChannel<Integer> getOne();
    }

    @Wrap(TestClass.class)
    public interface TestWrapper {

        @Timeout(300)
        @Async(List.class)
        public Iterable<Iterable> getList(@Async(List.class) List<? extends List<String>> i);

        @Timeout(300)
        @Async(int.class)
        public OutputChannel<Integer> getOne();

        @Timeout(300)
        public String getString(@Async(int.class) int... i);

        @Timeout(300)
        @Async(String.class)
        public OutputChannel<String> getString(@Async(int.class) HashSet<Integer> i);

        @Timeout(300)
        @Async(String.class)
        public List<String> getString(@Async(int.class) List<Integer> i);

        @Timeout(300)
        @Async(String.class)
        public Iterable<String> getString(@Async(int.class) Iterable<Integer> i);

        @Timeout(300)
        @Async(String.class)
        public String[] getString(@Async(int.class) Collection<Integer> i);

        @Timeout(300)
        public String getString(@Async(int.class) OutputChannel<Integer> i);
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
}
