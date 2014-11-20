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
import com.bmd.jrt.builder.RoutineBuilder.ChannelDataOrder;
import com.bmd.jrt.builder.RoutineBuilder.SyncRunnerType;
import com.bmd.jrt.channel.IOChannel;
import com.bmd.jrt.channel.OutputChannel;
import com.bmd.jrt.log.Log.LogLevel;
import com.bmd.jrt.log.NullLog;
import com.bmd.jrt.runner.Runners;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Java routine processor unit tests.
 * <p/>
 * Created by davide on 11/18/14.
 */
public class ProcessorTest extends TestCase {

    public void testWrapper() {

        final TestInterface testInterface = JavaRoutine.on(new TestClass())
                                                       .syncRunner(SyncRunnerType.SEQUENTIAL)
                                                       .runBy(Runners.poolRunner())
                                                       .inputMaxSize(3)
                                                       .inputTimeout(1, TimeUnit.SECONDS)
                                                       .outputMaxSize(2)
                                                       .outputTimeout(1, TimeUnit.SECONDS)
                                                       .outputOrder(ChannelDataOrder.INSERTION)
                                                       .logLevel(LogLevel.DEBUG)
                                                       .loggedWith(new NullLog())
                                                       .wrappedAs(TestInterface.class);

        assertThat(testInterface.getOne().readFirst()).isEqualTo(1);
        assertThat(testInterface.getString(1, 2, 3)).isIn("1", "2", "3");
        assertThat(testInterface.getString(new HashSet<Integer>(Arrays.asList(1, 2, 3)))
                                .readAll()).containsOnly("1", "2", "3");

        final IOChannel<Integer> channel = JavaRoutine.io().buildChannel();
        channel.input().pass(3).close();
        assertThat(testInterface.getString(channel.output())).isEqualTo("3");
    }

    @AsyncClass(TestClass.class)
    public interface TestInterface {

        @AsyncType(int.class)
        public OutputChannel<Integer> getOne();

        public String getString(@AsyncType(int.class) OutputChannel<Integer> i);

        public String getString(@ParallelType(int.class) int... i);

        @AsyncType(int.class)
        public OutputChannel<String> getString(@ParallelType(int.class) HashSet<Integer> i);
    }

    public static class TestClass {

        public int getOne() {

            return 1;
        }

        public String getString(final int i) {

            return Integer.toString(i);
        }
    }
}
