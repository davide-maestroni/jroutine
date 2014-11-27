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

import com.bmd.jrt.annotation.AsyncClass;
import com.bmd.jrt.annotation.AsyncType;
import com.bmd.jrt.annotation.ParallelType;
import com.bmd.jrt.builder.RoutineBuilder.DataOrder;
import com.bmd.jrt.builder.RoutineBuilder.RunnerType;
import com.bmd.jrt.channel.IOChannel;
import com.bmd.jrt.channel.OutputChannel;
import com.bmd.jrt.log.Log.LogLevel;
import com.bmd.jrt.log.NullLog;
import com.bmd.jrt.runner.Runners;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Java routine processor unit tests.
 * <p/>
 * Created by davide on 11/18/14.
 */
public class RoutineProcessorTest extends TestCase {

    @SuppressWarnings("unchecked")
    public void testWrapper() {

        final TestInterface testInterface = JavaRoutine.on(new TestClass())
                                                       .syncRunner(RunnerType.SEQUENTIAL)
                                                       .runBy(Runners.poolRunner())
                                                       .inputMaxSize(3)
                                                       .inputTimeout(1, TimeUnit.SECONDS)
                                                       .outputMaxSize(3)
                                                       .outputTimeout(2, TimeUnit.SECONDS)
                                                       .outputOrder(DataOrder.INSERTION)
                                                       .logLevel(LogLevel.DEBUG)
                                                       .loggedWith(new NullLog())
                                                       .as(TestInterface.class);

        assertThat(testInterface.getOne().readFirst()).isEqualTo(1);
        assertThat(testInterface.getString(1, 2, 3)).isIn("1", "2", "3");
        assertThat(testInterface.getString(new HashSet<Integer>(Arrays.asList(1, 2, 3)))
                                .readAll()).containsOnly("1", "2", "3");
        assertThat(testInterface.getString(Arrays.asList(1, 2, 3))).containsOnly("1", "2", "3");
        assertThat(
                testInterface.getString((Iterable<Integer>) Arrays.asList(1, 2, 3))).containsOnly(
                "1", "2", "3");
        assertThat(
                testInterface.getString((Collection<Integer>) Arrays.asList(1, 2, 3))).containsOnly(
                "1", "2", "3");

        final ArrayList<String> list = new ArrayList<String>();
        assertThat(testInterface.getList(Arrays.asList(list))).containsExactly(list);

        final IOChannel<Integer> channel = JavaRoutine.io().buildChannel();
        channel.input().pass(3).close();
        assertThat(testInterface.getString(channel.output())).isEqualTo("3");
    }

    @AsyncClass(TestClass.class)
    public interface TestInterface {

        @AsyncType(List.class)
        public Iterable<Iterable> getList(@ParallelType(List.class) List<? extends List<String>> i);

        @AsyncType(int.class)
        public OutputChannel<Integer> getOne();

        public String getString(@ParallelType(int.class) int... i);

        @AsyncType(int.class)
        public OutputChannel<String> getString(@ParallelType(int.class) HashSet<Integer> i);

        @AsyncType(int.class)
        public List<String> getString(@ParallelType(int.class) List<Integer> i);

        @AsyncType(int.class)
        public Iterable<String> getString(@ParallelType(int.class) Iterable<Integer> i);

        @AsyncType(int.class)
        public String[] getString(@ParallelType(int.class) Collection<Integer> i);

        public String getString(@AsyncType(int.class) OutputChannel<Integer> i);
    }

    @SuppressWarnings("UnusedDeclaration")
    public static class TestClass {

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
