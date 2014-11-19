package com.bmd.jrt.routine;

import com.bmd.jrt.annotation.AsyncOverride;
import com.bmd.jrt.annotation.AsyncWrapper;
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
        channel.input().pass(3);
        channel.close();
        assertThat(testInterface.getString(channel.output())).isEqualTo("3");
    }

    @AsyncWrapper(TestClass.class)
    public interface TestInterface {

        @AsyncOverride(result = true)
        public OutputChannel<Integer> getOne();

        @AsyncOverride(int.class)
        public String getString(OutputChannel<Integer> i);

        @AsyncOverride(parallel = true, value = int.class)
        public String getString(int... i);

        @AsyncOverride(parallel = true, value = int.class, result = true)
        public OutputChannel<String> getString(HashSet<Integer> i);
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
