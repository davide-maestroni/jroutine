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
import com.gh.bmd.jrt.channel.TransportChannel;
import com.gh.bmd.jrt.channel.TransportChannel.TransportInput;
import com.gh.bmd.jrt.channel.TransportChannel.TransportOutput;
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
 * Transport channel unit tests.
 * <p/>
 * Created by davide-maestroni on 10/26/14.
 */
public class TransportChannelTest {

    @Test
    public void testAbort() {

        final TimeDuration timeout = seconds(1);
        final TransportChannel<String> transportChannel = JRoutine.transport().buildChannel();
        final OutputChannel<String> outputChannel =
                JRoutine.on(PassingInvocation.<String>factoryOf())
                        .callAsync(transportChannel.output());

        transportChannel.input().abort(new IllegalStateException());

        try {

            outputChannel.afterMax(timeout).readNext();

            fail();

        } catch (final InvocationException ex) {

            assertThat(ex.getCause()).isExactlyInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    public void testAbortDelay() {

        final TransportChannel<String> transportChannel = JRoutine.transport().buildChannel();
        transportChannel.input().after(TimeDuration.days(1)).pass("test").close();

        final TransportOutput<String> output = transportChannel.output();
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

        final TransportChannel<String> transportChannel = JRoutine.transport().buildChannel();
        transportChannel.input().after(TimeDuration.seconds(3)).pass("test").close();

        final TransportOutput<String> output = transportChannel.output();
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

        final TransportChannel<String> transportChannel = JRoutine.transport().buildChannel();
        transportChannel.input().after(TimeDuration.seconds(3)).pass("test").close();

        final TransportOutput<String> output = transportChannel.output();
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

        final TransportChannel<String> transportChannel = JRoutine.transport().buildChannel();
        transportChannel.input().after(TimeDuration.seconds(3)).pass("test").close();

        final TransportOutput<String> output = transportChannel.output();
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

        final TransportChannel<String> transportChannel = JRoutine.transport().buildChannel();
        transportChannel.input().after(TimeDuration.seconds(3)).pass("test").close();

        final TransportOutput<String> output = transportChannel.output();
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
        final TransportChannel<String> transportChannel = JRoutine.transport().buildChannel();

        new Thread() {

            @Override
            public void run() {

                try {

                    Thread.sleep(500);

                } catch (final InterruptedException ignored) {

                } finally {

                    transportChannel.input().pass("test").close();
                }
            }
        }.start();

        final OutputChannel<String> outputChannel =
                JRoutine.on(PassingInvocation.<String>factoryOf())
                        .callAsync(transportChannel.output());
        assertThat(outputChannel.afterMax(timeout).readNext()).isEqualTo("test");
        assertThat(outputChannel.checkComplete()).isTrue();
    }

    @Test
    public void testAsynchronousInput2() {

        final TimeDuration timeout = seconds(1);
        final TransportChannel<String> transportChannel1 = JRoutine.transport()
                                                                   .withRoutine()
                                                                   .withOutputOrder(
                                                                           OrderType.PASS_ORDER)
                                                                   .set()
                                                                   .buildChannel();

        new Thread() {

            @Override
            public void run() {

                transportChannel1.input()
                                 .after(1, TimeUnit.MILLISECONDS)
                                 .after(TimeDuration.millis(200))
                                 .pass("test1", "test2")
                                 .pass(Collections.singleton("test3"))
                                 .close();
            }
        }.start();

        final OutputChannel<String> outputChannel1 =
                JRoutine.on(PassingInvocation.<String>factoryOf())
                        .callAsync(transportChannel1.output());
        assertThat(outputChannel1.afterMax(timeout).readAll()).containsExactly("test1", "test2",
                                                                               "test3");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testConfigurationErrors() {

        try {

            new DefaultTransportChannelBuilder().setConfiguration(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testConfigurationWarnings() {

        final CountLog countLog = new CountLog();
        JRoutine.transport()
                .withRoutine()
                .withFactoryArgs()
                .withSyncRunner(Runners.sequentialRunner())
                .withMaxInvocations(3)
                .withCoreInvocations(3)
                .withAvailInvocationTimeout(seconds(1))
                .withInputOrder(OrderType.NONE)
                .withInputMaxSize(3)
                .withInputTimeout(seconds(1))
                .withLogLevel(LogLevel.DEBUG)
                .withLog(countLog)
                .set()
                .buildChannel();
        assertThat(countLog.getWrnCount()).isEqualTo(8);
    }

    @Test
    public void testHasNextIteratorTimeout() {

        final TransportChannel<String> transportChannel = JRoutine.transport().buildChannel();
        transportChannel.input().after(TimeDuration.seconds(3)).pass("test").close();

        final TransportOutput<String> output = transportChannel.output();
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

        final TransportChannel<String> transportChannel = JRoutine.transport().buildChannel();
        transportChannel.input().after(TimeDuration.seconds(3)).pass("test").close();

        final TransportOutput<String> output = transportChannel.output();
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

        final TransportChannel<String> transportChannel = JRoutine.transport().buildChannel();
        transportChannel.input().after(TimeDuration.seconds(3)).pass("test").close();

        final TransportOutput<String> output = transportChannel.output();
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

        final TransportChannel<String> transportChannel = JRoutine.transport().buildChannel();
        transportChannel.input().after(TimeDuration.seconds(3)).pass("test").close();

        final TransportOutput<String> output = transportChannel.output();
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

        final TransportChannel<String> transportChannel = JRoutine.transport().buildChannel();
        transportChannel.input().after(TimeDuration.seconds(3)).pass("test").close();

        final TransportOutput<String> output = transportChannel.output();
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

        final TransportChannel<String> transportChannel = JRoutine.transport().buildChannel();
        transportChannel.input().after(TimeDuration.seconds(3)).pass("test").close();

        final TransportOutput<String> output = transportChannel.output();
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
        final TransportChannel<Object> channel = JRoutine.transport()
                                                         .withRoutine()
                                                         .withOutputOrder(OrderType.PASS_ORDER)
                                                         .withAsyncRunner(Runners.sharedRunner())
                                                         .withOutputMaxSize(1)
                                                         .withOutputTimeout(1,
                                                                            TimeUnit.MILLISECONDS)
                                                         .withOutputTimeout(seconds(1))
                                                         .withLogLevel(LogLevel.DEBUG)
                                                         .withLog(new NullLog())
                                                         .set()
                                                         .buildChannel();
        channel.input().pass(-77L);
        assertThat(channel.output().afterMax(timeout).readNext()).isEqualTo(-77L);

        final TransportChannel<Object> transportChannel1 = JRoutine.transport().buildChannel();
        final TransportInput<Object> input1 = transportChannel1.input();

        input1.after(TimeDuration.millis(200)).pass(23).now().pass(-77L).close();
        assertThat(transportChannel1.output().afterMax(timeout).readAll()).containsOnly(23, -77L);

        final TransportChannel<Object> transportChannel2 = JRoutine.transport()
                                                                   .withRoutine()
                                                                   .withOutputOrder(
                                                                           OrderType.PASS_ORDER)
                                                                   .set()
                                                                   .buildChannel();
        final TransportInput<Object> input2 = transportChannel2.input();

        input2.after(TimeDuration.millis(200)).pass(23).now().pass(-77L).close();
        assertThat(transportChannel2.output().afterMax(timeout).readAll()).containsExactly(23,
                                                                                           -77L);
    }

    @Test
    public void testPartialOut() {

        final TransportChannel<String> transportChannel = JRoutine.transport().buildChannel();

        new Thread() {

            @Override
            public void run() {

                transportChannel.input().pass("test");
            }
        }.start();

        final long startTime = System.currentTimeMillis();

        final OutputChannel<String> outputChannel =
                JRoutine.on(PassingInvocation.<String>factoryOf())
                        .callAsync(transportChannel.output())
                        .eventuallyExit();
        assertThat(outputChannel.afterMax(TimeDuration.millis(500)).readAll()).containsExactly(
                "test");

        assertThat(System.currentTimeMillis() - startTime).isLessThan(2000);

        assertThat(outputChannel.immediately().checkComplete()).isFalse();
        transportChannel.input().close();
        assertThat(outputChannel.afterMax(TimeDuration.millis(500)).checkComplete()).isTrue();
    }

    @Test
    public void testReadFirst() throws InterruptedException {

        final TimeDuration timeout = seconds(1);
        final TransportChannel<String> transportChannel = JRoutine.transport().buildChannel();

        new WeakThread(transportChannel).start();

        final OutputChannel<String> outputChannel =
                JRoutine.on(PassingInvocation.<String>factoryOf())
                        .callAsync(transportChannel.output());
        assertThat(outputChannel.afterMax(timeout).readNext()).isEqualTo("test");
    }

    @Test
    public void testReadTimeout() {

        final TransportChannel<Object> channel1 = JRoutine.transport()
                                                          .withRoutine()
                                                          .withReadTimeout(millis(10))
                                                          .withReadTimeoutAction(
                                                                  TimeoutActionType.EXIT)
                                                          .set()
                                                          .buildChannel();

        assertThat(channel1.output().readAll()).isEmpty();
    }

    @Test
    public void testReadTimeout2() {

        final TransportChannel<Object> channel2 = JRoutine.transport()
                                                          .withRoutine()
                                                          .withReadTimeout(millis(10))
                                                          .withReadTimeoutAction(
                                                                  TimeoutActionType.ABORT)
                                                          .set()
                                                          .buildChannel();

        try {

            channel2.output().readAll();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @Test
    public void testReadTimeout3() {

        final TransportChannel<Object> channel3 = JRoutine.transport()
                                                          .withRoutine()
                                                          .withReadTimeout(millis(10))
                                                          .withReadTimeoutAction(
                                                                  TimeoutActionType.DEADLOCK)
                                                          .set()
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

        private final WeakReference<TransportChannel<String>> mChannelRef;

        public WeakThread(final TransportChannel<String> transportChannel) {

            mChannelRef = new WeakReference<TransportChannel<String>>(transportChannel);
        }

        @Override
        public void run() {

            try {

                Thread.sleep(500);

            } catch (final InterruptedException ignored) {

            } finally {

                final TransportChannel<String> transportChannel = mChannelRef.get();

                if (transportChannel != null) {

                    transportChannel.input().pass("test");
                }
            }
        }
    }
}
