/*
 * Copyright 2016 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dm.jrt.core;

import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.ExecutionTimeoutException;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.common.TimeoutException;
import com.github.dm.jrt.core.config.InvocationConfiguration.OrderType;
import com.github.dm.jrt.core.config.InvocationConfiguration.TimeoutActionType;
import com.github.dm.jrt.core.invocation.PassingInvocation;
import com.github.dm.jrt.core.log.Log;
import com.github.dm.jrt.core.log.Log.Level;
import com.github.dm.jrt.core.log.NullLog;
import com.github.dm.jrt.core.runner.Runners;
import com.github.dm.jrt.core.util.TimeDuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.core.util.TimeDuration.millis;
import static com.github.dm.jrt.core.util.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * I/O channel unit tests.
 * <p/>
 * Created by davide-maestroni on 10/26/2014.
 */
public class IOChannelTest {

    @Test
    @SuppressWarnings({"ConstantConditions", "ThrowableResultOfMethodCallIgnored"})
    public void testAbort() {

        final TimeDuration timeout = seconds(1);
        final IOChannel<String> ioChannel = JRoutineCore.io().buildChannel();
        ioChannel.abort(new IllegalStateException());

        try {

            ioChannel.afterMax(timeout).throwError();

        } catch (final AbortException ex) {

            assertThat(ex.getCause()).isExactlyInstanceOf(IllegalStateException.class);
        }

        assertThat(ioChannel.getError().getCause()).isExactlyInstanceOf(
                IllegalStateException.class);
    }

    @Test
    public void testAbortDelay() {

        final IOChannel<String> ioChannel = JRoutineCore.io().buildChannel();
        ioChannel.after(seconds(3)).pass("test");

        assertThat(ioChannel.immediately().eventuallyExit().all()).isEmpty();

        final ArrayList<String> results = new ArrayList<String>();
        ioChannel.afterMax(10, TimeUnit.MILLISECONDS).allInto(results);
        assertThat(results).isEmpty();
        assertThat(ioChannel.immediately().eventuallyExit().hasCompleted()).isFalse();
        assertThat(ioChannel.now().abort()).isTrue();

        try {

            ioChannel.next();

            fail();

        } catch (final AbortException ignored) {

        }

        assertThat(ioChannel.isOpen()).isFalse();
    }

    @Test
    public void testAllIntoTimeout() {

        final IOChannel<String> ioChannel = JRoutineCore.io().buildChannel();
        ioChannel.after(seconds(3)).pass("test").close();

        assertThat(ioChannel.immediately().eventuallyExit().all()).isEmpty();

        ioChannel.eventuallyAbort().eventuallyThrow();

        try {

            ioChannel.allInto(new ArrayList<String>());

            fail();

        } catch (final ExecutionTimeoutException ignored) {

        }

        assertThat(ioChannel.hasCompleted()).isFalse();
    }

    @Test
    public void testAllIntoTimeout2() {

        final IOChannel<String> ioChannel = JRoutineCore.io().buildChannel();
        ioChannel.after(seconds(3)).pass("test").close();

        assertThat(ioChannel.immediately().eventuallyExit().all()).isEmpty();

        ioChannel.eventuallyThrow().afterMax(millis(10));

        try {

            ioChannel.allInto(new ArrayList<String>());

            fail();

        } catch (final ExecutionTimeoutException ignored) {

        }

        assertThat(ioChannel.hasCompleted()).isFalse();
    }

    @Test
    public void testAllTimeout() {

        final IOChannel<String> ioChannel = JRoutineCore.io().buildChannel();
        ioChannel.after(seconds(3)).pass("test").close();

        assertThat(ioChannel.immediately().eventuallyExit().all()).isEmpty();

        ioChannel.eventuallyThrow();

        try {

            ioChannel.all();

            fail();

        } catch (final ExecutionTimeoutException ignored) {

        }

        assertThat(ioChannel.hasCompleted()).isFalse();
    }

    @Test
    public void testAllTimeout2() {

        final IOChannel<String> ioChannel = JRoutineCore.io().buildChannel();
        ioChannel.after(seconds(3)).pass("test").close();

        assertThat(ioChannel.immediately().eventuallyExit().all()).isEmpty();

        ioChannel.eventuallyThrow().afterMax(millis(10));

        try {

            ioChannel.all();

            fail();

        } catch (final ExecutionTimeoutException ignored) {

        }

        assertThat(ioChannel.hasCompleted()).isFalse();
    }

    @Test
    public void testAsynchronousInput() {

        final TimeDuration timeout = seconds(1);
        final IOChannel<String> ioChannel = JRoutineCore.io().buildChannel();

        new Thread() {

            @Override
            public void run() {

                try {

                    Thread.sleep(500);

                } catch (final InterruptedException ignored) {

                } finally {

                    ioChannel.pass("test").close();
                }
            }
        }.start();

        final OutputChannel<String> outputChannel =
                JRoutineCore.on(PassingInvocation.<String>factoryOf()).asyncCall(ioChannel);
        assertThat(outputChannel.afterMax(timeout).next()).isEqualTo("test");
        assertThat(outputChannel.hasCompleted()).isTrue();
    }

    @Test
    public void testAsynchronousInput2() {

        final TimeDuration timeout = seconds(1);
        final IOChannel<String> ioChannel1 = JRoutineCore.io()
                                                         .withChannels()
                                                         .withChannelOrder(OrderType.BY_CALL)
                                                         .setConfiguration()
                                                         .buildChannel();

        new Thread() {

            @Override
            public void run() {

                ioChannel1.after(1, TimeUnit.MILLISECONDS)
                          .after(millis(200))
                          .pass("test1", "test2")
                          .pass(Collections.singleton("test3"))
                          .close();
            }
        }.start();

        final OutputChannel<String> outputChannel1 =
                JRoutineCore.on(PassingInvocation.<String>factoryOf()).asyncCall(ioChannel1);
        assertThat(outputChannel1.afterMax(timeout).all()).containsExactly("test1", "test2",
                "test3");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testConfigurationErrors() {

        try {

            new DefaultIOChannelBuilder().setConfiguration(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testEmpty() {

        final IOChannel<String> ioChannel = JRoutineCore.io().buildChannel();
        assertThat(ioChannel.isEmpty()).isTrue();
        assertThat(ioChannel.pass("test").isEmpty()).isFalse();
        ioChannel.afterMax(seconds(1)).next();
        assertThat(ioChannel.isEmpty()).isTrue();
        assertThat(ioChannel.after(millis(100)).pass("test").isEmpty()).isTrue();
        assertThat(ioChannel.close().afterMax(seconds(10)).hasCompleted()).isTrue();
        assertThat(ioChannel.isEmpty()).isFalse();
    }

    @Test
    public void testEmptyAbort() {

        final IOChannel<Object> ioChannel = JRoutineCore.io().buildChannel();
        assertThat(ioChannel.isEmpty()).isTrue();
        assertThat(ioChannel.pass("test").isEmpty()).isFalse();
        assertThat(ioChannel.abort()).isTrue();
        assertThat(ioChannel.isEmpty()).isFalse();
    }

    @Test
    public void testHasNextIteratorTimeout() {

        final IOChannel<String> ioChannel = JRoutineCore.io().buildChannel();
        ioChannel.after(seconds(3)).pass("test").close();

        assertThat(ioChannel.immediately().eventuallyExit().all()).isEmpty();

        ioChannel.eventuallyThrow();

        try {

            ioChannel.iterator().hasNext();

            fail();

        } catch (final ExecutionTimeoutException ignored) {

        }

        assertThat(ioChannel.hasCompleted()).isFalse();
    }

    @Test
    public void testHasNextIteratorTimeout2() {

        final IOChannel<String> ioChannel = JRoutineCore.io().buildChannel();
        ioChannel.after(seconds(3)).pass("test").close();

        assertThat(ioChannel.immediately().eventuallyExit().all()).isEmpty();

        ioChannel.eventuallyThrow().afterMax(millis(10));

        try {

            ioChannel.iterator().hasNext();

            fail();

        } catch (final ExecutionTimeoutException ignored) {

        }

        assertThat(ioChannel.hasCompleted()).isFalse();
    }

    @Test
    public void testNextIteratorTimeout() {

        final IOChannel<String> ioChannel = JRoutineCore.io().buildChannel();
        ioChannel.after(seconds(3)).pass("test").close();

        assertThat(ioChannel.immediately().eventuallyExit().all()).isEmpty();

        ioChannel.eventuallyThrow();

        try {

            ioChannel.iterator().next();

            fail();

        } catch (final ExecutionTimeoutException ignored) {

        }

        assertThat(ioChannel.hasCompleted()).isFalse();
    }

    @Test
    public void testNextIteratorTimeout2() {

        final IOChannel<String> ioChannel = JRoutineCore.io().buildChannel();
        ioChannel.after(seconds(3)).pass("test").close();

        assertThat(ioChannel.immediately().eventuallyExit().all()).isEmpty();

        ioChannel.eventuallyThrow().afterMax(millis(10));

        try {

            ioChannel.iterator().next();

            fail();

        } catch (final ExecutionTimeoutException ignored) {

        }

        assertThat(ioChannel.hasCompleted()).isFalse();
    }

    @Test
    public void testNextList() {

        assertThat(JRoutineCore.io()
                               .buildChannel()
                               .pass("test1", "test2", "test3", "test4")
                               .close()
                               .afterMax(seconds(1))
                               .next(2)).containsExactly("test1", "test2");

        assertThat(JRoutineCore.io()
                               .buildChannel()
                               .pass("test1")
                               .close()
                               .eventuallyExit()
                               .afterMax(seconds(1))
                               .next(2)).containsExactly("test1");

        try {

            JRoutineCore.io()
                        .buildChannel()
                        .pass("test1")
                        .eventuallyAbort()
                        .afterMax(seconds(1))
                        .next(2);

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            JRoutineCore.io()
                        .buildChannel()
                        .pass("test1")
                        .eventuallyAbort(new IllegalStateException())
                        .afterMax(seconds(1))
                        .next(2);

            fail();

        } catch (final AbortException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(IllegalStateException.class);
        }

        try {

            JRoutineCore.io()
                        .buildChannel()
                        .pass("test1")
                        .eventuallyThrow()
                        .afterMax(seconds(1))
                        .next(2);

            fail();

        } catch (final TimeoutException ignored) {

        }
    }

    @Test
    public void testNextOr() {

        assertThat(JRoutineCore.io()
                               .buildChannel()
                               .pass("test1")
                               .afterMax(seconds(1))
                               .nextOr(2)).isEqualTo("test1");

        assertThat(JRoutineCore.io().buildChannel().eventuallyExit().afterMax(seconds(1)).nextOr(2))
                .isEqualTo(2);

        try {

            JRoutineCore.io()
                        .buildChannel()
                        .eventuallyAbort()
                        .afterMax(millis(100))
                        .nextOr("test2");

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            JRoutineCore.io()
                        .buildChannel()
                        .eventuallyAbort(new IllegalStateException())
                        .afterMax(millis(100))
                        .nextOr("test2");

            fail();

        } catch (final AbortException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(IllegalStateException.class);
        }

        try {

            JRoutineCore.io()
                        .buildChannel()
                        .eventuallyThrow()
                        .afterMax(millis(100))
                        .nextOr("test2");

            fail();

        } catch (final TimeoutException ignored) {

        }
    }

    @Test
    public void testNextTimeout() {

        final IOChannel<String> ioChannel = JRoutineCore.io().buildChannel();
        ioChannel.after(seconds(3)).pass("test").close();

        assertThat(ioChannel.immediately().eventuallyExit().all()).isEmpty();

        ioChannel.eventuallyThrow();

        try {

            ioChannel.next();

            fail();

        } catch (final ExecutionTimeoutException ignored) {

        }

        assertThat(ioChannel.hasCompleted()).isFalse();
    }

    @Test
    public void testNextTimeout2() {

        final IOChannel<String> ioChannel = JRoutineCore.io().buildChannel();
        ioChannel.after(seconds(3)).pass("test").close();

        assertThat(ioChannel.immediately().eventuallyExit().all()).isEmpty();

        ioChannel.eventuallyThrow().afterMax(millis(10));

        try {

            ioChannel.next();

            fail();

        } catch (final ExecutionTimeoutException ignored) {

        }

        assertThat(ioChannel.hasCompleted()).isFalse();
    }

    @Test
    public void testOf() {

        final IOChannel<Integer> channel = JRoutineCore.io().of(2);
        assertThat(channel.isOpen()).isFalse();
        assertThat(channel.afterMax(seconds(1)).all()).containsExactly(2);
        assertThat(JRoutineCore.io().of(-11, 73).afterMax(seconds(1)).all()).containsExactly(-11,
                73);
        assertThat(JRoutineCore.io()
                               .of(Arrays.asList(3, 12, -7))
                               .afterMax(seconds(1))
                               .all()).containsExactly(3, 12, -7);
    }

    @Test
    public void testOrderType() {

        final TimeDuration timeout = seconds(1);
        final IOChannel<Object> channel = JRoutineCore.io()
                                                      .withChannels()
                                                      .withChannelOrder(OrderType.BY_CALL)
                                                      .withRunner(Runners.sharedRunner())
                                                      .withChannelMaxSize(1)
                                                      .withChannelMaxDelay(1, TimeUnit.MILLISECONDS)
                                                      .withChannelMaxDelay(seconds(1))
                                                      .withLogLevel(Level.DEBUG)
                                                      .withLog(new NullLog())
                                                      .setConfiguration()
                                                      .buildChannel();
        channel.pass(-77L);
        assertThat(channel.afterMax(timeout).next()).isEqualTo(-77L);

        final IOChannel<Object> ioChannel1 = JRoutineCore.io().buildChannel();
        ioChannel1.after(millis(200)).pass(23).now().pass(-77L).close();
        assertThat(ioChannel1.afterMax(timeout).all()).containsOnly(23, -77L);

        final IOChannel<Object> ioChannel2 = JRoutineCore.io().buildChannel();
        ioChannel2.orderByDelay().orderByCall();
        ioChannel2.after(millis(200)).pass(23).now().pass(-77L).close();
        assertThat(ioChannel2.afterMax(timeout).all()).containsExactly(23, -77L);
    }

    @Test
    public void testPartialOut() {

        final IOChannel<String> ioChannel = JRoutineCore.io().buildChannel();

        new Thread() {

            @Override
            public void run() {

                ioChannel.pass("test");
            }
        }.start();

        final long startTime = System.currentTimeMillis();

        final OutputChannel<String> outputChannel =
                JRoutineCore.on(PassingInvocation.<String>factoryOf())
                            .asyncCall(ioChannel)
                            .eventuallyExit();
        assertThat(outputChannel.afterMax(millis(500)).all()).containsExactly("test");

        assertThat(System.currentTimeMillis() - startTime).isLessThan(2000);

        assertThat(outputChannel.immediately().hasCompleted()).isFalse();
        ioChannel.close();
        assertThat(ioChannel.isOpen()).isFalse();
        assertThat(outputChannel.afterMax(millis(500)).hasCompleted()).isTrue();
    }

    @Test
    public void testPassTimeout() {

        final IOChannel<Object> channel1 = JRoutineCore.io()
                                                       .withChannels()
                                                       .withReadTimeout(millis(10))
                                                       .withReadTimeoutAction(
                                                               TimeoutActionType.EXIT)
                                                       .setConfiguration()
                                                       .buildChannel();

        assertThat(channel1.all()).isEmpty();
    }

    @Test
    public void testPassTimeout2() {

        final IOChannel<Object> channel2 = JRoutineCore.io()
                                                       .withChannels()
                                                       .withReadTimeout(millis(10))
                                                       .withReadTimeoutAction(
                                                               TimeoutActionType.ABORT)
                                                       .setConfiguration()
                                                       .buildChannel();

        try {

            channel2.all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @Test
    public void testPassTimeout3() {

        final IOChannel<Object> channel3 = JRoutineCore.io()
                                                       .withChannels()
                                                       .withReadTimeout(millis(10))
                                                       .withReadTimeoutAction(
                                                               TimeoutActionType.THROW)
                                                       .setConfiguration()
                                                       .buildChannel();

        try {

            channel3.all();

            fail();

        } catch (final ExecutionTimeoutException ignored) {

        }
    }

    @Test
    public void testPendingInputs() throws InterruptedException {

        final IOChannel<Object> channel = JRoutineCore.io().buildChannel();
        assertThat(channel.isOpen()).isTrue();
        channel.pass("test");
        assertThat(channel.isOpen()).isTrue();
        channel.after(millis(500)).pass("test");
        assertThat(channel.isOpen()).isTrue();
        final IOChannel<Object> ioChannel = JRoutineCore.io().buildChannel();
        channel.pass(ioChannel);
        assertThat(channel.isOpen()).isTrue();
        channel.close();
        assertThat(channel.isOpen()).isFalse();
        ioChannel.close();
        assertThat(channel.isOpen()).isFalse();
    }

    @Test
    public void testPendingInputsAbort() throws InterruptedException {

        final IOChannel<Object> channel = JRoutineCore.io().buildChannel();
        assertThat(channel.isOpen()).isTrue();
        channel.pass("test");
        assertThat(channel.isOpen()).isTrue();
        channel.after(millis(500)).pass("test");
        assertThat(channel.isOpen()).isTrue();
        final IOChannel<Object> ioChannel = JRoutineCore.io().buildChannel();
        channel.pass(ioChannel);
        assertThat(channel.isOpen()).isTrue();
        channel.now().abort();
        assertThat(channel.isOpen()).isFalse();
    }

    @Test
    public void testReadFirst() throws InterruptedException {

        final TimeDuration timeout = seconds(1);
        final IOChannel<String> ioChannel = JRoutineCore.io().buildChannel();

        new WeakThread(ioChannel).start();

        final OutputChannel<String> outputChannel =
                JRoutineCore.on(PassingInvocation.<String>factoryOf()).asyncCall(ioChannel);
        assertThat(outputChannel.afterMax(timeout).next()).isEqualTo("test");
    }

    @Test
    public void testSkip() {

        assertThat(JRoutineCore.io()
                               .buildChannel()
                               .pass("test1", "test2", "test3", "test4")
                               .close()
                               .afterMax(seconds(1))
                               .skip(2)
                               .all()).containsExactly("test3", "test4");

        assertThat(JRoutineCore.io()
                               .buildChannel()
                               .pass("test1")
                               .close()
                               .eventuallyExit()
                               .afterMax(seconds(1))
                               .skip(2)
                               .all()).isEmpty();

        try {

            JRoutineCore.io()
                        .buildChannel()
                        .pass("test1")
                        .close()
                        .eventuallyAbort()
                        .afterMax(seconds(1))
                        .skip(2);

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            JRoutineCore.io()
                        .buildChannel()
                        .pass("test1")
                        .close()
                        .eventuallyAbort(new IllegalStateException())
                        .afterMax(seconds(1))
                        .skip(2);

            fail();

        } catch (final AbortException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(IllegalStateException.class);
        }

        try {

            JRoutineCore.io()
                        .buildChannel()
                        .pass("test1")
                        .close()
                        .eventuallyThrow()
                        .afterMax(seconds(1))
                        .skip(2);

            fail();

        } catch (final TimeoutException ignored) {

        }
    }

    @SuppressWarnings("unused")
    private static class CountLog implements Log {

        private int mDgbCount;

        private int mErrCount;

        private int mWrnCount;

        public void dbg(@NotNull final List<Object> contexts, @Nullable final String message,
                @Nullable final Throwable throwable) {

            ++mDgbCount;
        }

        public void err(@NotNull final List<Object> contexts, @Nullable final String message,
                @Nullable final Throwable throwable) {

            ++mErrCount;
        }

        public void wrn(@NotNull final List<Object> contexts, @Nullable final String message,
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

        private final WeakReference<IOChannel<String>> mChannelRef;

        public WeakThread(final IOChannel<String> ioChannel) {

            mChannelRef = new WeakReference<IOChannel<String>>(ioChannel);
        }

        @Override
        public void run() {

            try {

                Thread.sleep(500);

            } catch (final InterruptedException ignored) {

            } finally {

                final IOChannel<String> ioChannel = mChannelRef.get();

                if (ioChannel != null) {

                    ioChannel.pass("test");
                }
            }
        }
    }
}