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

import com.gh.bmd.jrt.builder.RoutineConfiguration.OrderType;
import com.gh.bmd.jrt.builder.RoutineConfiguration.TimeoutActionType;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.ReadDeadlockException;
import com.gh.bmd.jrt.channel.StandaloneChannel;
import com.gh.bmd.jrt.channel.StandaloneChannel.StandaloneInput;
import com.gh.bmd.jrt.channel.StandaloneChannel.StandaloneOutput;
import com.gh.bmd.jrt.common.AbortException;
import com.gh.bmd.jrt.common.InvocationException;
import com.gh.bmd.jrt.invocation.PassingInvocation;
import com.gh.bmd.jrt.log.Log;
import com.gh.bmd.jrt.log.Log.LogLevel;
import com.gh.bmd.jrt.log.NullLog;
import com.gh.bmd.jrt.runner.Runners;
import com.gh.bmd.jrt.time.TimeDuration;

import org.junit.Test;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.gh.bmd.jrt.time.TimeDuration.millis;
import static com.gh.bmd.jrt.time.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Standalone channel unit tests.
 * <p/>
 * Created by davide on 10/26/14.
 */
public class StandaloneChannelTest {

    @Test
    public void testAbort() {

        final TimeDuration timeout = seconds(1);
        final StandaloneChannel<String> standaloneChannel = JRoutine.standalone().buildChannel();
        final OutputChannel<String> outputChannel =
                JRoutine.on(PassingInvocation.<String>factoryOf())
                        .callAsync(standaloneChannel.output());

        standaloneChannel.input().abort(new IllegalStateException());

        try {

            outputChannel.afterMax(timeout).readNext();

            fail();

        } catch (final InvocationException ex) {

            assertThat(ex.getCause()).isExactlyInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    public void testAbortDelay() {

        final StandaloneChannel<String> standaloneChannel = JRoutine.standalone().buildChannel();
        standaloneChannel.input().after(TimeDuration.days(1)).pass("test").close();

        final StandaloneOutput<String> output = standaloneChannel.output();
        assertThat(output.immediately().eventuallyExit().readAll()).isEmpty();

        final ArrayList<String> results = new ArrayList<String>();
        output.afterMax(10, TimeUnit.MILLISECONDS).readAllInto(results);
        assertThat(results).isEmpty();
        assertThat(output.immediately().eventuallyExit().checkComplete()).isFalse();
        assertThat(output.abort()).isTrue();

        try {

            output.readNext();

            fail();

        } catch (final InvocationException ignored) {

        }

        assertThat(output.isOpen()).isFalse();
    }

    @Test
    public void testAllIntoTimeout() {

        final StandaloneChannel<String> standaloneChannel = JRoutine.standalone().buildChannel();
        standaloneChannel.input().after(TimeDuration.seconds(3)).pass("test").close();

        final StandaloneOutput<String> output = standaloneChannel.output();
        assertThat(output.immediately().eventuallyExit().readAll()).isEmpty();

        output.eventuallyDeadlock();

        try {

            output.readAllInto(new ArrayList<String>());

            fail();

        } catch (final ReadDeadlockException ignored) {

        }

        assertThat(output.checkComplete()).isFalse();
    }

    @Test
    public void testAllIntoTimeout2() {

        final StandaloneChannel<String> standaloneChannel = JRoutine.standalone().buildChannel();
        standaloneChannel.input().after(TimeDuration.seconds(3)).pass("test").close();

        final StandaloneOutput<String> output = standaloneChannel.output();
        assertThat(output.immediately().eventuallyExit().readAll()).isEmpty();

        output.eventuallyDeadlock().afterMax(TimeDuration.millis(10));

        try {

            output.readAllInto(new ArrayList<String>());

            fail();

        } catch (final ReadDeadlockException ignored) {

        }

        assertThat(output.checkComplete()).isFalse();
    }

    @Test
    public void testAllTimeout() {

        final StandaloneChannel<String> standaloneChannel = JRoutine.standalone().buildChannel();
        standaloneChannel.input().after(TimeDuration.seconds(3)).pass("test").close();

        final StandaloneOutput<String> output = standaloneChannel.output();
        assertThat(output.immediately().eventuallyExit().readAll()).isEmpty();

        output.eventuallyDeadlock();

        try {

            output.readAll();

            fail();

        } catch (final ReadDeadlockException ignored) {

        }

        assertThat(output.checkComplete()).isFalse();
    }

    @Test
    public void testAllTimeout2() {

        final StandaloneChannel<String> standaloneChannel = JRoutine.standalone().buildChannel();
        standaloneChannel.input().after(TimeDuration.seconds(3)).pass("test").close();

        final StandaloneOutput<String> output = standaloneChannel.output();
        assertThat(output.immediately().eventuallyExit().readAll()).isEmpty();

        output.eventuallyDeadlock().afterMax(TimeDuration.millis(10));

        try {

            output.readAll();

            fail();

        } catch (final ReadDeadlockException ignored) {

        }

        assertThat(output.checkComplete()).isFalse();
    }

    @Test
    public void testAsynchronousInput() {

        final TimeDuration timeout = seconds(1);
        final StandaloneChannel<String> standaloneChannel = JRoutine.standalone().buildChannel();

        new Thread() {

            @Override
            public void run() {

                try {

                    Thread.sleep(500);

                } catch (final InterruptedException ignored) {

                } finally {

                    standaloneChannel.input().pass("test").close();
                }
            }
        }.start();

        final OutputChannel<String> outputChannel =
                JRoutine.on(PassingInvocation.<String>factoryOf())
                        .callAsync(standaloneChannel.output());
        assertThat(outputChannel.afterMax(timeout).readNext()).isEqualTo("test");
        assertThat(outputChannel.checkComplete()).isTrue();
    }

    @Test
    public void testAsynchronousInput2() {

        final TimeDuration timeout = seconds(1);
        final StandaloneChannel<String> standaloneChannel1 = JRoutine.standalone()
                                                                     .routineConfiguration()
                                                                     .withOutputOrder(
                                                                             OrderType
                                                                                     .PASSING_ORDER)
                                                                     .apply()
                                                                     .buildChannel();

        new Thread() {

            @Override
            public void run() {

                standaloneChannel1.input()
                                  .after(1, TimeUnit.MILLISECONDS)
                                  .after(TimeDuration.millis(200))
                                  .pass("test1", "test2")
                                  .pass(Collections.singleton("test3"))
                                  .close();
            }
        }.start();

        final OutputChannel<String> outputChannel1 =
                JRoutine.on(PassingInvocation.<String>factoryOf())
                        .callAsync(standaloneChannel1.output());
        assertThat(outputChannel1.afterMax(timeout).readAll()).containsExactly("test1", "test2",
                                                                               "test3");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testConfigurationErrors() {

        try {

            new DefaultStandaloneChannelBuilder().apply(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testConfigurationWarnings() {

        final CountLog countLog = new CountLog();
        JRoutine.standalone()
                .routineConfiguration()
                .withFactoryArgs()
                .withSyncRunner(Runners.sequentialRunner())
                .withMaxInvocations(3)
                .withCoreInvocations(3)
                .withAvailableTimeout(seconds(1))
                .withInputOrder(OrderType.NONE)
                .withInputSize(3)
                .withInputTimeout(seconds(1))
                .withLogLevel(LogLevel.DEBUG)
                .withLog(countLog)
                .apply()
                .buildChannel();
        assertThat(countLog.getWrnCount()).isEqualTo(8);
    }

    @Test
    public void testHasNextIteratorTimeout() {

        final StandaloneChannel<String> standaloneChannel = JRoutine.standalone().buildChannel();
        standaloneChannel.input().after(TimeDuration.seconds(3)).pass("test").close();

        final StandaloneOutput<String> output = standaloneChannel.output();
        assertThat(output.immediately().eventuallyExit().readAll()).isEmpty();

        output.eventuallyDeadlock();

        try {

            output.iterator().hasNext();

            fail();

        } catch (final ReadDeadlockException ignored) {

        }

        assertThat(output.checkComplete()).isFalse();
    }

    @Test
    public void testHasNextIteratorTimeout2() {

        final StandaloneChannel<String> standaloneChannel = JRoutine.standalone().buildChannel();
        standaloneChannel.input().after(TimeDuration.seconds(3)).pass("test").close();

        final StandaloneOutput<String> output = standaloneChannel.output();
        assertThat(output.immediately().eventuallyExit().readAll()).isEmpty();

        output.eventuallyDeadlock().afterMax(TimeDuration.millis(10));

        try {

            output.iterator().hasNext();

            fail();

        } catch (final ReadDeadlockException ignored) {

        }

        assertThat(output.checkComplete()).isFalse();
    }

    @Test
    public void testNextIteratorTimeout() {

        final StandaloneChannel<String> standaloneChannel = JRoutine.standalone().buildChannel();
        standaloneChannel.input().after(TimeDuration.seconds(3)).pass("test").close();

        final StandaloneOutput<String> output = standaloneChannel.output();
        assertThat(output.immediately().eventuallyExit().readAll()).isEmpty();

        output.eventuallyDeadlock();

        try {

            output.iterator().next();

            fail();

        } catch (final ReadDeadlockException ignored) {

        }

        assertThat(output.checkComplete()).isFalse();
    }

    @Test
    public void testNextIteratorTimeout2() {

        final StandaloneChannel<String> standaloneChannel = JRoutine.standalone().buildChannel();
        standaloneChannel.input().after(TimeDuration.seconds(3)).pass("test").close();

        final StandaloneOutput<String> output = standaloneChannel.output();
        assertThat(output.immediately().eventuallyExit().readAll()).isEmpty();

        output.eventuallyDeadlock().afterMax(TimeDuration.millis(10));

        try {

            output.iterator().next();

            fail();

        } catch (final ReadDeadlockException ignored) {

        }

        assertThat(output.checkComplete()).isFalse();
    }

    @Test
    public void testNextTimeout() {

        final StandaloneChannel<String> standaloneChannel = JRoutine.standalone().buildChannel();
        standaloneChannel.input().after(TimeDuration.seconds(3)).pass("test").close();

        final StandaloneOutput<String> output = standaloneChannel.output();
        assertThat(output.immediately().eventuallyExit().readAll()).isEmpty();

        output.eventuallyDeadlock();

        try {

            output.readNext();

            fail();

        } catch (final ReadDeadlockException ignored) {

        }

        assertThat(output.checkComplete()).isFalse();
    }

    @Test
    public void testNextTimeout2() {

        final StandaloneChannel<String> standaloneChannel = JRoutine.standalone().buildChannel();
        standaloneChannel.input().after(TimeDuration.seconds(3)).pass("test").close();

        final StandaloneOutput<String> output = standaloneChannel.output();
        assertThat(output.immediately().eventuallyExit().readAll()).isEmpty();

        output.eventuallyDeadlock().afterMax(TimeDuration.millis(10));

        try {

            output.readNext();

            fail();

        } catch (final ReadDeadlockException ignored) {

        }

        assertThat(output.checkComplete()).isFalse();
    }

    @Test
    public void testOrderType() {

        final TimeDuration timeout = seconds(1);
        final StandaloneChannel<Object> channel = JRoutine.standalone()
                                                          .routineConfiguration()
                                                          .withOutputOrder(OrderType.PASSING_ORDER)
                                                          .withAsyncRunner(Runners.sharedRunner())
                                                          .withOutputSize(1)
                                                          .withOutputTimeout(1,
                                                                             TimeUnit.MILLISECONDS)
                                                          .withOutputTimeout(seconds(1))
                                                          .withLogLevel(LogLevel.DEBUG)
                                                          .withLog(new NullLog())
                                                          .apply()
                                                          .buildChannel();
        channel.input().pass(-77L);
        assertThat(channel.output().afterMax(timeout).readNext()).isEqualTo(-77L);

        final StandaloneChannel<Object> standaloneChannel1 = JRoutine.standalone().buildChannel();
        final StandaloneInput<Object> input1 = standaloneChannel1.input();

        input1.after(TimeDuration.millis(200)).pass(23).now().pass(-77L).close();
        assertThat(standaloneChannel1.output().afterMax(timeout).readAll()).containsOnly(23, -77L);

        final StandaloneChannel<Object> standaloneChannel2 = JRoutine.standalone()
                                                                     .routineConfiguration()
                                                                     .withOutputOrder(
                                                                             OrderType
                                                                                     .PASSING_ORDER)
                                                                     .apply()
                                                                     .buildChannel();
        final StandaloneInput<Object> input2 = standaloneChannel2.input();

        input2.after(TimeDuration.millis(200)).pass(23).now().pass(-77L).close();
        assertThat(standaloneChannel2.output().afterMax(timeout).readAll()).containsExactly(23,
                                                                                            -77L);
    }

    @Test
    public void testPartialOut() {

        final StandaloneChannel<String> standaloneChannel = JRoutine.standalone().buildChannel();

        new Thread() {

            @Override
            public void run() {

                standaloneChannel.input().pass("test");
            }
        }.start();

        final long startTime = System.currentTimeMillis();

        final OutputChannel<String> outputChannel =
                JRoutine.on(PassingInvocation.<String>factoryOf())
                        .callAsync(standaloneChannel.output())
                        .eventuallyExit();
        assertThat(outputChannel.afterMax(TimeDuration.millis(500)).readAll()).containsExactly(
                "test");

        assertThat(System.currentTimeMillis() - startTime).isLessThan(2000);

        assertThat(outputChannel.immediately().checkComplete()).isFalse();
        standaloneChannel.input().close();
        assertThat(outputChannel.afterMax(TimeDuration.millis(500)).checkComplete()).isTrue();
    }

    @Test
    public void testReadFirst() throws InterruptedException {

        final TimeDuration timeout = seconds(1);
        final StandaloneChannel<String> standaloneChannel = JRoutine.standalone().buildChannel();

        new WeakThread(standaloneChannel).start();

        final OutputChannel<String> outputChannel =
                JRoutine.on(PassingInvocation.<String>factoryOf())
                        .callAsync(standaloneChannel.output());
        assertThat(outputChannel.afterMax(timeout).readNext()).isEqualTo("test");
    }

    @Test
    public void testReadTimeout() {

        final StandaloneChannel<Object> channel1 = JRoutine.standalone()
                                                           .routineConfiguration()
                                                           .withReadTimeout(millis(10))
                                                           .onReadTimeout(TimeoutActionType.EXIT)
                                                           .apply()
                                                           .buildChannel();

        assertThat(channel1.output().readAll()).isEmpty();
    }

    @Test
    public void testReadTimeout2() {

        final StandaloneChannel<Object> channel2 = JRoutine.standalone()
                                                           .routineConfiguration()
                                                           .withReadTimeout(millis(10))
                                                           .onReadTimeout(TimeoutActionType.ABORT)
                                                           .apply()
                                                           .buildChannel();

        try {

            channel2.output().readAll();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @Test
    public void testReadTimeout3() {

        final StandaloneChannel<Object> channel3 = JRoutine.standalone()
                                                           .routineConfiguration()
                                                           .withReadTimeout(millis(10))
                                                           .onReadTimeout(
                                                                   TimeoutActionType.DEADLOCK)
                                                           .apply()
                                                           .buildChannel();

        try {

            channel3.output().readAll();

            fail();

        } catch (final ReadDeadlockException ignored) {

        }
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

    private static class WeakThread extends Thread {

        private final WeakReference<StandaloneChannel<String>> mChannelRef;

        public WeakThread(final StandaloneChannel<String> standaloneChannel) {

            mChannelRef = new WeakReference<StandaloneChannel<String>>(standaloneChannel);
        }

        @Override
        public void run() {

            try {

                Thread.sleep(500);

            } catch (final InterruptedException ignored) {

            } finally {

                final StandaloneChannel<String> standaloneChannel = mChannelRef.get();

                if (standaloneChannel != null) {

                    standaloneChannel.input().pass("test");
                }
            }
        }
    }
}
