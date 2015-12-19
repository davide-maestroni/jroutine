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
package com.github.dm.jrt.core;

import com.github.dm.jrt.builder.InvocationConfiguration.OrderType;
import com.github.dm.jrt.builder.InvocationConfiguration.TimeoutActionType;
import com.github.dm.jrt.channel.AbortException;
import com.github.dm.jrt.channel.Channel.OutputChannel;
import com.github.dm.jrt.channel.ExecutionTimeoutException;
import com.github.dm.jrt.channel.IOChannel;
import com.github.dm.jrt.channel.InvocationChannel;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.channel.StreamingIOChannel;
import com.github.dm.jrt.channel.TimeoutException;
import com.github.dm.jrt.invocation.FilterInvocation;
import com.github.dm.jrt.invocation.InvocationFactory;
import com.github.dm.jrt.invocation.PassingInvocation;
import com.github.dm.jrt.log.Log;
import com.github.dm.jrt.log.Log.Level;
import com.github.dm.jrt.log.NullLog;
import com.github.dm.jrt.routine.Routine;
import com.github.dm.jrt.util.TimeDuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.core.Channels.asyncIo;
import static com.github.dm.jrt.core.Channels.parallelIo;
import static com.github.dm.jrt.core.Channels.syncIo;
import static com.github.dm.jrt.util.TimeDuration.millis;
import static com.github.dm.jrt.util.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Streaming channel unit tests.
 * <p/>
 * Created by davide-maestroni on 09/25/2015.
 */
public class StreamingIOChannelTest {

    @Test
    public void testAbort() {

        final TimeDuration timeout = seconds(1);
        final StreamingIOChannel<String, String> streamingChannel =
                asyncIo(JRoutine.on(PassingInvocation.<String>factoryOf()));
        final InvocationChannel<String, String> invocationChannel =
                JRoutine.on(PassingInvocation.<String>factoryOf()).asyncInvoke();
        final OutputChannel<String> outputChannel =
                streamingChannel.passTo(invocationChannel).result();

        streamingChannel.abort(new IllegalStateException());

        try {

            outputChannel.afterMax(timeout).next();

            fail();

        } catch (final AbortException ex) {

            assertThat(ex.getCause()).isExactlyInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    public void testAbortDelay() {

        final StreamingIOChannel<String, String> streamingChannel =
                asyncIo(JRoutine.on(PassingInvocation.<String>factoryOf()));
        streamingChannel.after(TimeDuration.days(1)).pass("test");

        assertThat(streamingChannel.immediately().eventuallyExit().all()).isEmpty();

        final ArrayList<String> results = new ArrayList<String>();
        streamingChannel.afterMax(10, TimeUnit.MILLISECONDS).allInto(results);
        assertThat(results).isEmpty();
        assertThat(streamingChannel.immediately().eventuallyExit().checkComplete()).isFalse();
        assertThat(streamingChannel.now().abort()).isTrue();

        try {

            streamingChannel.afterMax(seconds(1)).next();

            fail();

        } catch (final AbortException ignored) {

        }

        assertThat(streamingChannel.isOpen()).isFalse();
    }

    @Test
    public void testAllIntoTimeout() {

        final StreamingIOChannel<String, String> streamingChannel =
                asyncIo(JRoutine.on(PassingInvocation.<String>factoryOf()));
        streamingChannel.after(TimeDuration.seconds(3)).pass("test").close();

        assertThat(streamingChannel.immediately().eventuallyExit().all()).isEmpty();

        streamingChannel.eventuallyAbort().eventuallyThrow();

        try {

            streamingChannel.allInto(new ArrayList<String>());

            fail();

        } catch (final ExecutionTimeoutException ignored) {

        }

        assertThat(streamingChannel.checkComplete()).isFalse();
    }

    @Test
    public void testAllIntoTimeout2() {

        final StreamingIOChannel<String, String> streamingChannel =
                asyncIo(JRoutine.on(PassingInvocation.<String>factoryOf()));
        streamingChannel.after(TimeDuration.seconds(3)).pass("test").close();

        assertThat(streamingChannel.immediately().eventuallyExit().all()).isEmpty();

        streamingChannel.eventuallyThrow().afterMax(TimeDuration.millis(10));

        try {

            streamingChannel.allInto(new ArrayList<String>());

            fail();

        } catch (final ExecutionTimeoutException ignored) {

        }

        assertThat(streamingChannel.checkComplete()).isFalse();
    }

    @Test
    public void testAllTimeout() {

        final StreamingIOChannel<String, String> streamingChannel =
                asyncIo(JRoutine.on(PassingInvocation.<String>factoryOf()));
        streamingChannel.after(TimeDuration.seconds(3)).pass("test").close();

        assertThat(streamingChannel.immediately().eventuallyExit().all()).isEmpty();

        streamingChannel.eventuallyThrow();

        try {

            streamingChannel.all();

            fail();

        } catch (final ExecutionTimeoutException ignored) {

        }

        assertThat(streamingChannel.checkComplete()).isFalse();
    }

    @Test
    public void testAllTimeout2() {

        final StreamingIOChannel<String, String> streamingChannel =
                asyncIo(JRoutine.on(PassingInvocation.<String>factoryOf()));
        streamingChannel.after(TimeDuration.seconds(3)).pass("test").close();

        assertThat(streamingChannel.immediately().eventuallyExit().all()).isEmpty();

        streamingChannel.eventuallyThrow().afterMax(TimeDuration.millis(10));

        try {

            streamingChannel.all();

            fail();

        } catch (final ExecutionTimeoutException ignored) {

        }

        assertThat(streamingChannel.checkComplete()).isFalse();
    }

    @Test
    public void testAppend() {

        final Routine<String, String> doubleString = JRoutine.on(new DoubleString()).buildRoutine();
        assertThat(asyncIo(doubleString).pass("test").afterMax(seconds(10)).next()).isEqualTo(
                "testtest");
        final Routine<String, Integer> stringLength =
                JRoutine.on(new StringLength()).buildRoutine();
        assertThat(asyncIo(stringLength).pass("test").afterMax(seconds(10)).next()).isEqualTo(4);
        assertThat(asyncIo(doubleString).concat(asyncIo(stringLength))
                                        .pass("test")
                                        .afterMax(seconds(10))
                                        .next()).isEqualTo(8);
    }

    @Test
    public void testAsynchronousInput() {

        final TimeDuration timeout = seconds(1);
        final StreamingIOChannel<String, String> streamingChannel =
                asyncIo(JRoutine.on(PassingInvocation.<String>factoryOf()));

        new Thread() {

            @Override
            public void run() {

                try {

                    Thread.sleep(500);

                } catch (final InterruptedException ignored) {

                } finally {

                    streamingChannel.pass("test").close();
                }
            }
        }.start();

        final OutputChannel<String> outputChannel =
                JRoutine.on(PassingInvocation.<String>factoryOf()).asyncCall(streamingChannel);
        assertThat(outputChannel.afterMax(timeout).next()).isEqualTo("test");
        assertThat(outputChannel.checkComplete()).isTrue();
    }

    @Test
    public void testAsynchronousInput2() {

        final TimeDuration timeout = seconds(1);
        final StreamingIOChannel<String, String> streamingChannel =
                asyncIo(JRoutine.on(PassingInvocation.<String>factoryOf())
                                .invocations()
                                .withInputOrder(OrderType.BY_CALL)
                                .set());

        new Thread() {

            @Override
            public void run() {

                streamingChannel.after(1, TimeUnit.MILLISECONDS)
                                .after(TimeDuration.millis(200))
                                .orderByCall()
                                .pass("test1", "test2")
                                .pass(Collections.singleton("test3"))
                                .close();
            }
        }.start();

        final OutputChannel<String> outputChannel1 =
                JRoutine.on(PassingInvocation.<String>factoryOf()).asyncCall(streamingChannel);
        assertThat(outputChannel1.afterMax(timeout).all()).containsExactly("test1", "test2",
                                                                           "test3");
    }

    @Test
    public void testEmpty() {

        final StreamingIOChannel<String, String> streamingChannel =
                syncIo(JRoutine.on(PassingInvocation.<String>factoryOf()));
        assertThat(streamingChannel.isEmpty()).isTrue();
        assertThat(streamingChannel.pass("test").isEmpty()).isFalse();
        streamingChannel.next();
        assertThat(streamingChannel.isEmpty()).isTrue();
        assertThat(streamingChannel.after(millis(100)).pass("test").isEmpty()).isTrue();
    }

    @Test
    public void testEmptyAbort() {

        final StreamingIOChannel<String, String> streamingChannel =
                syncIo(JRoutine.on(PassingInvocation.<String>factoryOf()));
        assertThat(streamingChannel.isEmpty()).isTrue();
        assertThat(streamingChannel.pass("test").isEmpty()).isFalse();
        assertThat(streamingChannel.abort()).isTrue();
        assertThat(streamingChannel.isEmpty()).isFalse();
    }

    @Test
    public void testHasNextIteratorTimeout() {

        final StreamingIOChannel<String, String> streamingChannel =
                parallelIo(JRoutine.on(PassingInvocation.<String>factoryOf()));
        streamingChannel.after(TimeDuration.seconds(3)).pass("test").close();

        assertThat(streamingChannel.immediately().eventuallyExit().all()).isEmpty();

        streamingChannel.eventuallyThrow();

        try {

            streamingChannel.iterator().hasNext();

            fail();

        } catch (final ExecutionTimeoutException ignored) {

        }

        assertThat(streamingChannel.checkComplete()).isFalse();
    }

    @Test
    public void testHasNextIteratorTimeout2() {

        final StreamingIOChannel<String, String> streamingChannel =
                asyncIo(JRoutine.on(PassingInvocation.<String>factoryOf()));
        streamingChannel.after(TimeDuration.seconds(3)).pass("test").close();

        assertThat(streamingChannel.immediately().eventuallyExit().all()).isEmpty();

        streamingChannel.eventuallyThrow().afterMax(TimeDuration.millis(10));

        try {

            streamingChannel.iterator().hasNext();

            fail();

        } catch (final ExecutionTimeoutException ignored) {

        }

        assertThat(streamingChannel.checkComplete()).isFalse();
    }

    @Test
    public void testNext() {

        final InvocationFactory<String, String> factory = PassingInvocation.factoryOf();
        assertThat(asyncIo(JRoutine.on(factory)).pass("test1", "test2", "test3", "test4")
                                                .close()
                                                .afterMax(seconds(3))
                                                .next(2)).containsExactly("test1", "test2");

        assertThat(asyncIo(JRoutine.on(factory)).pass("test1")
                                                .close()
                                                .afterMax(seconds(3))
                                                .eventuallyExit()
                                                .next(2)).containsExactly("test1");

        try {

            asyncIo(JRoutine.on(factory)).pass("test1")
                                         .close()
                                         .afterMax(seconds(3))
                                         .eventuallyAbort()
                                         .next(2);

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            asyncIo(JRoutine.on(factory)).pass("test1")
                                         .close()
                                         .afterMax(seconds(3))
                                         .eventuallyAbort(new IllegalStateException())
                                         .next(2);

            fail();

        } catch (final AbortException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(IllegalStateException.class);
        }

        try {

            asyncIo(JRoutine.on(factory)).pass("test1")
                                         .close()
                                         .afterMax(seconds(3))
                                         .eventuallyThrow()
                                         .next(2);

            fail();

        } catch (final TimeoutException ignored) {

        }
    }

    @Test
    public void testNextIteratorTimeout() {

        final StreamingIOChannel<String, String> streamingChannel =
                asyncIo(JRoutine.on(PassingInvocation.<String>factoryOf()));
        streamingChannel.after(TimeDuration.seconds(3)).pass("test").close();

        assertThat(streamingChannel.immediately().eventuallyExit().all()).isEmpty();

        streamingChannel.eventuallyThrow();

        try {

            streamingChannel.iterator().next();

            fail();

        } catch (final ExecutionTimeoutException ignored) {

        }

        assertThat(streamingChannel.checkComplete()).isFalse();
    }

    @Test
    public void testNextIteratorTimeout2() {

        final StreamingIOChannel<String, String> streamingChannel =
                asyncIo(JRoutine.on(PassingInvocation.<String>factoryOf()));
        streamingChannel.after(TimeDuration.seconds(3)).pass("test").close();

        assertThat(streamingChannel.immediately().eventuallyExit().all()).isEmpty();

        streamingChannel.eventuallyThrow().afterMax(TimeDuration.millis(10));

        try {

            streamingChannel.iterator().next();

            fail();

        } catch (final ExecutionTimeoutException ignored) {

        }

        assertThat(streamingChannel.checkComplete()).isFalse();
    }

    @Test
    public void testNextTimeout() {

        final StreamingIOChannel<String, String> streamingChannel =
                asyncIo(JRoutine.on(PassingInvocation.<String>factoryOf()));
        streamingChannel.after(TimeDuration.seconds(3)).pass("test").close();

        assertThat(streamingChannel.immediately().eventuallyExit().all()).isEmpty();

        streamingChannel.eventuallyThrow();

        try {

            streamingChannel.next();

            fail();

        } catch (final ExecutionTimeoutException ignored) {

        }

        assertThat(streamingChannel.checkComplete()).isFalse();
    }

    @Test
    public void testNextTimeout2() {

        final StreamingIOChannel<String, String> streamingChannel =
                asyncIo(JRoutine.on(PassingInvocation.<String>factoryOf()));
        streamingChannel.after(TimeDuration.seconds(3)).pass("test").close();

        assertThat(streamingChannel.immediately().eventuallyExit().all()).isEmpty();

        streamingChannel.eventuallyThrow().afterMax(TimeDuration.millis(10));

        try {

            streamingChannel.next();

            fail();

        } catch (final ExecutionTimeoutException ignored) {

        }

        assertThat(streamingChannel.checkComplete()).isFalse();
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testNullPointerErrors() {

        try {

            new DefaultStreamingIOChannel<Object, Object>(null, JRoutine.io().buildChannel());

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultStreamingIOChannel<Object, Object>(JRoutine.io().buildChannel(), null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testOrderType() {

        final TimeDuration timeout = seconds(1);
        final StreamingIOChannel<Object, Object> channel =
                asyncIo(JRoutine.on(PassingInvocation.factoryOf())
                                .invocations()
                                .withInputOrder(OrderType.BY_CALL)
                                .withInputMaxSize(1)
                                .withInputTimeout(1, TimeUnit.MILLISECONDS)
                                .withInputTimeout(seconds(1))
                                .withLogLevel(Level.DEBUG)
                                .withLog(new NullLog())
                                .set());
        channel.pass(-77L);
        assertThat(channel.afterMax(timeout).next()).isEqualTo(-77L);

        final StreamingIOChannel<Object, Object> channel1 =
                asyncIo(JRoutine.on(PassingInvocation.factoryOf()));
        channel1.after(TimeDuration.millis(200)).pass(23).now().pass(-77L).close();
        assertThat(channel1.afterMax(timeout).all()).containsOnly(23, -77L);

        final StreamingIOChannel<Object, Object> channel2 =
                asyncIo(JRoutine.on(PassingInvocation.factoryOf()));
        channel2.orderByChance().orderByDelay().orderByCall();
        channel2.after(TimeDuration.millis(200)).pass(23).now().pass(-77L).close();
        assertThat(channel2.afterMax(timeout).all()).containsExactly(23, -77L);
    }

    @Test
    public void testPartialOut() {

        final StreamingIOChannel<String, String> streamingChannel =
                asyncIo(JRoutine.on(PassingInvocation.<String>factoryOf()));

        new Thread() {

            @Override
            public void run() {

                streamingChannel.pass("test");
            }
        }.start();

        final long startTime = System.currentTimeMillis();

        final OutputChannel<String> outputChannel =
                JRoutine.on(PassingInvocation.<String>factoryOf())
                        .asyncCall(streamingChannel)
                        .eventuallyExit();
        assertThat(outputChannel.afterMax(TimeDuration.millis(500)).all()).containsExactly("test");

        assertThat(System.currentTimeMillis() - startTime).isLessThan(2000);

        assertThat(outputChannel.immediately().checkComplete()).isFalse();
        streamingChannel.close();
        assertThat(streamingChannel.isOpen()).isFalse();
        assertThat(outputChannel.afterMax(TimeDuration.millis(500)).checkComplete()).isTrue();
    }

    @Test
    public void testPassTimeout() {

        final StreamingIOChannel<Object, Object> streamingChannel =
                asyncIo(JRoutine.on(PassingInvocation.factoryOf())
                                .invocations()
                                .withReadTimeout(millis(10))
                                .withReadTimeoutAction(TimeoutActionType.EXIT)
                                .set());

        assertThat(streamingChannel.all()).isEmpty();
    }

    @Test
    public void testPassTimeout2() {

        final StreamingIOChannel<Object, Object> streamingChannel =
                asyncIo(JRoutine.on(PassingInvocation.factoryOf())
                                .invocations()
                                .withReadTimeout(millis(10))
                                .withReadTimeoutAction(TimeoutActionType.ABORT)
                                .set());

        try {

            streamingChannel.all();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @Test
    public void testPassTimeout3() {

        final StreamingIOChannel<Object, Object> streamingChannel =
                asyncIo(JRoutine.on(PassingInvocation.factoryOf())
                                .invocations()
                                .withReadTimeout(millis(10))
                                .withReadTimeoutAction(TimeoutActionType.THROW)
                                .set());

        try {

            streamingChannel.all();

            fail();

        } catch (final ExecutionTimeoutException ignored) {

        }
    }

    @Test
    public void testPendingInputs() throws InterruptedException {

        final StreamingIOChannel<String, String> channel =
                asyncIo(JRoutine.on(PassingInvocation.<String>factoryOf()));
        assertThat(channel.isOpen()).isTrue();
        channel.pass("test");
        assertThat(channel.isOpen()).isTrue();
        channel.after(millis(500)).pass("test");
        assertThat(channel.isOpen()).isTrue();
        assertThat(channel.isOpen()).isTrue();
        final IOChannel<String, String> ioChannel = JRoutine.io().buildChannel();
        channel.pass(ioChannel);
        assertThat(channel.isOpen()).isTrue();
        channel.close();
        assertThat(channel.isOpen()).isFalse();
        ioChannel.close();
        assertThat(channel.isOpen()).isFalse();
    }

    @Test
    public void testPendingInputsAbort() throws InterruptedException {

        final StreamingIOChannel<String, String> channel =
                asyncIo(JRoutine.on(PassingInvocation.<String>factoryOf()));
        assertThat(channel.isOpen()).isTrue();
        channel.pass("test");
        assertThat(channel.isOpen()).isTrue();
        channel.after(millis(500)).pass("test");
        assertThat(channel.isOpen()).isTrue();
        final IOChannel<String, String> ioChannel = JRoutine.io().buildChannel();
        channel.pass(ioChannel);
        assertThat(channel.isOpen()).isTrue();
        channel.now().abort();
        assertThat(channel.isOpen()).isFalse();
    }

    @Test
    public void testPrepend() {

        final Routine<String, String> doubleString = JRoutine.on(new DoubleString()).buildRoutine();
        assertThat(asyncIo(doubleString).pass("test").afterMax(seconds(10)).next()).isEqualTo(
                "testtest");
        final Routine<String, Integer> stringLength =
                JRoutine.on(new StringLength()).buildRoutine();
        assertThat(asyncIo(stringLength).pass("test").afterMax(seconds(10)).next()).isEqualTo(4);
        assertThat(asyncIo(stringLength).combine(asyncIo(doubleString))
                                        .pass("test")
                                        .afterMax(seconds(10))
                                        .next()).isEqualTo(8);
    }

    @Test
    public void testReadFirst() throws InterruptedException {

        final TimeDuration timeout = seconds(1);
        final StreamingIOChannel<String, String> streamingChannel =
                asyncIo(JRoutine.on(PassingInvocation.<String>factoryOf()));

        new WeakThread(streamingChannel).start();

        final OutputChannel<String> outputChannel =
                JRoutine.on(PassingInvocation.<String>factoryOf()).asyncCall(streamingChannel);
        assertThat(outputChannel.afterMax(timeout).next()).isEqualTo("test");
    }

    @Test
    public void testSkip() {

        final InvocationFactory<String, String> factory = PassingInvocation.factoryOf();
        assertThat(asyncIo(JRoutine.on(factory)).pass("test1", "test2", "test3", "test4")
                                                .close()
                                                .afterMax(seconds(3))
                                                .skip(2)
                                                .all()).containsExactly("test3", "test4");

        assertThat(asyncIo(JRoutine.on(factory)).pass("test1")
                                                .close()
                                                .afterMax(seconds(3))
                                                .eventuallyExit()
                                                .skip(2)
                                                .all()).isEmpty();

        try {

            asyncIo(JRoutine.on(factory)).pass("test1")
                                         .close()
                                         .afterMax(seconds(3))
                                         .eventuallyAbort()
                                         .skip(2);

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            asyncIo(JRoutine.on(factory)).pass("test1")
                                         .close()
                                         .afterMax(seconds(3))
                                         .eventuallyAbort(new IllegalStateException())
                                         .skip(2);

            fail();

        } catch (final AbortException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(IllegalStateException.class);
        }

        try {

            asyncIo(JRoutine.on(factory)).pass("test1")
                                         .close()
                                         .afterMax(seconds(3))
                                         .eventuallyThrow()
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

    private static class DoubleString extends FilterInvocation<String, String> {

        public void onInput(final String input, @NotNull final ResultChannel<String> result) {

            result.pass(input + input);
        }
    }

    private static class StringLength extends FilterInvocation<String, Integer> {

        public void onInput(final String input, @NotNull final ResultChannel<Integer> result) {

            result.pass(input.length());
        }
    }

    private static class WeakThread extends Thread {

        private final WeakReference<StreamingIOChannel<String, String>> mChannelRef;

        public WeakThread(final StreamingIOChannel<String, String> ioChannel) {

            mChannelRef = new WeakReference<StreamingIOChannel<String, String>>(ioChannel);
        }

        @Override
        public void run() {

            try {

                Thread.sleep(500);

            } catch (final InterruptedException ignored) {

            } finally {

                final StreamingIOChannel<String, String> streamingChannel = mChannelRef.get();

                if (streamingChannel != null) {

                    streamingChannel.pass("test");
                }
            }
        }
    }
}
