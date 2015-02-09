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
import com.bmd.jrt.annotation.Bind;
import com.bmd.jrt.annotation.Timeout;
import com.bmd.jrt.builder.InputDeadlockException;
import com.bmd.jrt.builder.OutputDeadlockException;
import com.bmd.jrt.builder.RoutineBuilder.TimeoutAction;
import com.bmd.jrt.builder.RoutineChannelBuilder.OrderBy;
import com.bmd.jrt.builder.RoutineConfiguration;
import com.bmd.jrt.builder.RoutineConfigurationBuilder;
import com.bmd.jrt.channel.OutputChannel;
import com.bmd.jrt.channel.OutputConsumer;
import com.bmd.jrt.channel.ParameterChannel;
import com.bmd.jrt.channel.ReadDeadlockException;
import com.bmd.jrt.channel.ResultChannel;
import com.bmd.jrt.channel.TemplateOutputConsumer;
import com.bmd.jrt.common.AbortException;
import com.bmd.jrt.common.ClassToken;
import com.bmd.jrt.common.InvocationException;
import com.bmd.jrt.invocation.Invocation;
import com.bmd.jrt.invocation.PassingInvocation;
import com.bmd.jrt.invocation.SingleCallInvocation;
import com.bmd.jrt.invocation.TemplateInvocation;
import com.bmd.jrt.log.Log.LogLevel;
import com.bmd.jrt.log.Logger;
import com.bmd.jrt.log.NullLog;
import com.bmd.jrt.routine.DefaultExecution.InputIterator;
import com.bmd.jrt.routine.DefaultParameterChannel.InvocationManager;
import com.bmd.jrt.routine.DefaultResultChannel.AbortHandler;
import com.bmd.jrt.runner.Runners;
import com.bmd.jrt.time.TimeDuration;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.bmd.jrt.common.ClassToken.tokenOf;
import static com.bmd.jrt.routine.JRoutine.on;
import static com.bmd.jrt.time.TimeDuration.INFINITY;
import static com.bmd.jrt.time.TimeDuration.millis;
import static com.bmd.jrt.time.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Routine unit tests.
 * <p/>
 * Created by davide on 9/9/14.
 */
public class RoutineTest extends TestCase {

    public void testAbort() throws InterruptedException {

        final TimeDuration timeout = seconds(1);
        final Routine<String, String> routine =
                on(tokenOf(DelayedInvocation.class)).withArgs(TimeDuration.millis(100))
                                                    .buildRoutine();

        final ParameterChannel<String, String> inputChannel = routine.invokeAsync().pass("test1");
        final OutputChannel<String> outputChannel = inputChannel.result();

        assertThat(inputChannel.isOpen()).isFalse();
        assertThat(inputChannel.abort(new IllegalArgumentException("test1"))).isFalse();
        assertThat(inputChannel.isOpen()).isFalse();
        assertThat(outputChannel.afterMax(timeout).readNext()).isEqualTo("test1");

        final ParameterChannel<String, String> inputChannel1 = routine.invokeAsync().pass("test1");
        final OutputChannel<String> outputChannel1 = inputChannel1.result();

        assertThat(inputChannel1.isOpen()).isFalse();
        assertThat(inputChannel1.abort()).isFalse();
        assertThat(inputChannel1.isOpen()).isFalse();
        assertThat(outputChannel1.isOpen()).isTrue();
        assertThat(outputChannel1.afterMax(timeout).readNext()).isEqualTo("test1");
        assertThat(outputChannel1.isOpen()).isFalse();

        final OutputChannel<String> channel = routine.callAsync("test2");
        assertThat(channel.isOpen()).isTrue();
        assertThat(channel.abort(new IllegalArgumentException("test2"))).isTrue();
        assertThat(channel.abort()).isFalse();
        assertThat(channel.isOpen()).isTrue();

        try {

            channel.afterMax(timeout).readAll();

            fail();

        } catch (final InvocationException ex) {

            assertThat(ex.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
            assertThat(ex.getCause().getMessage()).isEqualTo("test2");
        }

        assertThat(channel.checkComplete()).isTrue();
        assertThat(channel.isOpen()).isFalse();


        final OutputChannel<String> channel1 = routine.callAsync("test2");
        assertThat(channel1.isOpen()).isTrue();
        assertThat(channel1.abort()).isTrue();
        assertThat(channel1.abort(new IllegalArgumentException("test2"))).isFalse();
        assertThat(channel1.isOpen()).isTrue();

        try {

            channel1.afterMax(timeout).readAll();

            fail();

        } catch (final InvocationException ex) {

            assertThat(ex.getCause()).isNull();
        }

        assertThat(channel1.checkComplete()).isTrue();
        assertThat(channel1.isOpen()).isFalse();


        final Invocation<String, String> abortInvocation =
                new TemplateInvocation<String, String>() {

                    @Override
                    public void onInput(final String s,
                            @Nonnull final ResultChannel<String> result) {

                        assertThat(result.isOpen()).isTrue();
                        assertThat(result.abort(new IllegalArgumentException(s))).isTrue();
                        assertThat(result.abort()).isFalse();
                        assertThat(result.isOpen()).isFalse();

                        try {

                            result.pass(s);

                            fail();

                        } catch (final InvocationException ignored) {

                        }
                    }
                };

        final Routine<String, String> routine1 =
                on(ClassToken.tokenOf(abortInvocation)).withArgs(this).buildRoutine();

        try {

            routine1.invokeAsync()
                    .after(TimeDuration.millis(10))
                    .pass("test_abort")
                    .result()
                    .afterMax(timeout)
                    .readNext();

            fail();

        } catch (final InvocationException ex) {

            assertThat(ex.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
            assertThat(ex.getCause().getMessage()).isEqualTo("test_abort");
        }

        final Invocation<String, String> abortInvocation2 =
                new TemplateInvocation<String, String>() {

                    @Override
                    public void onInput(final String s,
                            @Nonnull final ResultChannel<String> result) {

                        assertThat(result.abort()).isTrue();
                        assertThat(result.abort(new IllegalArgumentException(s))).isFalse();
                    }
                };

        final Routine<String, String> routine2 =
                on(ClassToken.tokenOf(abortInvocation2)).withArgs(this).buildRoutine();

        try {

            routine2.invokeAsync()
                    .after(TimeDuration.millis(10))
                    .pass("test_abort")
                    .result()
                    .afterMax(timeout)
                    .readNext();

            fail();

        } catch (final InvocationException ex) {

            assertThat(ex.getCause()).isNull();
        }

        final AtomicBoolean isFailed = new AtomicBoolean(false);
        final Semaphore semaphore = new Semaphore(0);
        final Invocation<String, String> closeInvocation =
                new TemplateInvocation<String, String>() {

                    @Override
                    public void onInput(final String s,
                            @Nonnull final ResultChannel<String> result) {

                        new Thread() {

                            @Override
                            public void run() {

                                super.run();

                                try {

                                    Thread.sleep(100);

                                    try {

                                        result.pass(s);

                                        isFailed.set(true);

                                    } catch (final IllegalStateException ignored) {

                                    }

                                    semaphore.release();

                                } catch (final InterruptedException ignored) {

                                }
                            }

                        }.start();
                    }
                };

        final Routine<String, String> routine3 =
                on(ClassToken.tokenOf(closeInvocation)).withLogLevel(LogLevel.SILENT)
                                                       .withArgs(this, isFailed, semaphore)
                                                       .buildRoutine();

        assertThat(routine3.callAsync("test").afterMax(timeout).readAll()).isEmpty();
        semaphore.tryAcquire(1, 1, TimeUnit.SECONDS);
        assertThat(isFailed.get()).isFalse();
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void testAbortInput() throws InterruptedException {

        final Semaphore semaphore = new Semaphore(0);
        final AtomicReference<Throwable> abortReason = new AtomicReference<Throwable>();

        final TemplateInvocation<String, String> abortInvocation =
                new TemplateInvocation<String, String>() {

                    @Override
                    public void onAbort(@Nullable final Throwable reason) {

                        abortReason.set(reason);
                        semaphore.release();
                    }
                };

        final Routine<String, String> routine = JRoutine.on(tokenOf(abortInvocation))
                                                        .withArgs(this, abortReason, semaphore)
                                                        .buildRoutine();

        final ParameterChannel<String, String> channel = routine.invokeAsync();
        final IllegalArgumentException exception = new IllegalArgumentException();
        channel.after(TimeDuration.millis(100)).abort(exception);

        semaphore.tryAcquire(1, TimeUnit.SECONDS);

        assertThat(abortReason.get()).isEqualTo(exception);

        final ParameterChannel<String, String> channel1 = routine.invokeAsync();
        final IllegalAccessError exception1 = new IllegalAccessError();
        channel1.now().abort(exception1);

        semaphore.tryAcquire(1, TimeUnit.SECONDS);

        assertThat(abortReason.get().getCause()).isEqualTo(exception1);
    }

    public void testBind() {

        final TestOutputConsumer consumer = new TestOutputConsumer();
        final OutputChannel<Object> channel1 =
                JRoutine.on(new ClassToken<PassingInvocation<Object>>() {})
                        .buildRoutine()
                        .invokeAsync()
                        .after(seconds(1))
                        .pass("test1")
                        .result();

        channel1.bind(consumer);
        assertThat(channel1.isBound()).isTrue();
        assertThat(consumer.isOutput()).isFalse();

        channel1.unbind(null);
        assertThat(channel1.isBound()).isTrue();
        assertThat(consumer.isOutput()).isFalse();

        channel1.unbind(new TestOutputConsumer());
        assertThat(channel1.isBound()).isTrue();
        assertThat(consumer.isOutput()).isFalse();

        channel1.unbind(consumer);
        assertThat(channel1.isBound()).isFalse();
        assertThat(consumer.isOutput()).isFalse();

        final OutputChannel<Object> channel2 =
                JRoutine.on(new ClassToken<PassingInvocation<Object>>() {})
                        .buildRoutine()
                        .invokeSync()
                        .pass("test2")
                        .result();

        channel2.bind(consumer);
        assertThat(channel1.isBound()).isFalse();
        assertThat(channel2.isBound()).isTrue();
        assertThat(consumer.isOutput()).isTrue();
        assertThat(consumer.getOutput()).isEqualTo("test2");
    }

    public void testCalls() {

        final TimeDuration timeout = seconds(1);
        final Routine<String, String> routine =
                JRoutine.on(new ClassToken<PassingInvocation<String>>() {}).buildRoutine();

        assertThat(routine.callSync().afterMax(timeout).readAll()).isEmpty();
        assertThat(routine.callSync(Arrays.asList("test1", "test2"))
                          .afterMax(timeout)
                          .readAll()).containsExactly("test1", "test2");
        assertThat(routine.callSync(routine.callSync("test1", "test2")).afterMax(timeout).readAll())
                .containsExactly("test1", "test2");
        assertThat(routine.callSync("test1").afterMax(timeout).readAll()).containsExactly("test1");
        assertThat(routine.callSync("test1", "test2").afterMax(timeout).readAll()).containsExactly(
                "test1", "test2");
        assertThat(routine.callAsync().afterMax(timeout).readAll()).isEmpty();
        assertThat(routine.callAsync(Arrays.asList("test1", "test2"))
                          .afterMax(timeout)
                          .readAll()).containsExactly("test1", "test2");
        assertThat(routine.callAsync(routine.callSync("test1", "test2"))
                          .afterMax(timeout)
                          .readAll()).containsExactly("test1", "test2");
        assertThat(routine.callAsync("test1").afterMax(timeout).readAll()).containsExactly("test1");
        assertThat(routine.callAsync("test1", "test2").afterMax(timeout).readAll()).containsExactly(
                "test1", "test2");
        assertThat(routine.callParallel().afterMax(timeout).readAll()).isEmpty();
        assertThat(routine.callParallel(Arrays.asList("test1", "test2"))
                          .afterMax(timeout)
                          .readAll()).containsOnly("test1", "test2");
        assertThat(routine.callParallel(routine.callSync("test1", "test2"))
                          .afterMax(timeout)
                          .readAll()).containsOnly("test1", "test2");
        assertThat(routine.callParallel("test1").afterMax(timeout).readAll()).containsOnly("test1");
        assertThat(routine.callParallel("test1", "test2").afterMax(timeout).readAll()).containsOnly(
                "test1", "test2");

        assertThat(routine.invokeSync().pass().result().readAll()).isEmpty();
        assertThat(routine.invokeSync()
                          .pass(Arrays.asList("test1", "test2"))
                          .result()
                          .afterMax(timeout)
                          .readAll()).containsExactly("test1", "test2");
        assertThat(routine.invokeSync()
                          .pass(routine.callSync("test1", "test2"))
                          .result()
                          .afterMax(timeout)
                          .readAll()).containsExactly("test1", "test2");
        assertThat(routine.invokeSync()
                          .pass("test1")
                          .result()
                          .afterMax(timeout)
                          .readAll()).containsExactly("test1");
        assertThat(routine.invokeSync().pass("test1", "test2").result().afterMax(timeout).readAll())
                .containsExactly("test1", "test2");
        assertThat(routine.invokeAsync().pass().result().afterMax(timeout).readAll()).isEmpty();
        assertThat(routine.invokeAsync()
                          .pass(Arrays.asList("test1", "test2"))
                          .result()
                          .afterMax(timeout)
                          .readAll()).containsExactly("test1", "test2");
        assertThat(routine.invokeAsync()
                          .pass(routine.callSync("test1", "test2"))
                          .result()
                          .afterMax(timeout)
                          .readAll()).containsExactly("test1", "test2");
        assertThat(routine.invokeAsync()
                          .pass("test1")
                          .result()
                          .afterMax(timeout)
                          .readAll()).containsExactly("test1");
        assertThat(routine.invokeAsync()
                          .pass("test1", "test2")
                          .result()
                          .afterMax(timeout)
                          .readAll()).containsExactly("test1", "test2");
        assertThat(routine.invokeParallel().pass().result().afterMax(timeout).readAll()).isEmpty();
        assertThat(routine.invokeParallel()
                          .pass(Arrays.asList("test1", "test2"))
                          .result()
                          .afterMax(timeout)
                          .readAll()).containsOnly("test1", "test2");
        assertThat(routine.invokeParallel()
                          .pass(routine.callSync("test1", "test2"))
                          .result()
                          .afterMax(timeout)
                          .readAll()).containsOnly("test1", "test2");
        assertThat(routine.invokeParallel()
                          .pass("test1")
                          .result()
                          .afterMax(timeout)
                          .readAll()).containsOnly("test1");
        assertThat(routine.invokeParallel()
                          .pass("test1", "test2")
                          .result()
                          .afterMax(timeout)
                          .readAll()).containsOnly("test1", "test2");
    }

    public void testChainedRoutine() {

        final TimeDuration timeout = seconds(1);
        final SingleCallInvocation<Integer, Integer> execSum =
                new SingleCallInvocation<Integer, Integer>() {

                    @Override
                    public void onCall(@Nonnull final List<? extends Integer> integers,
                            @Nonnull final ResultChannel<Integer> result) {

                        int sum = 0;

                        for (final Integer integer : integers) {

                            sum += integer;
                        }

                        result.pass(sum);
                    }
                };

        final Routine<Integer, Integer> sumRoutine =
                on(ClassToken.tokenOf(execSum)).withArgs(this).buildRoutine();

        final TemplateInvocation<Integer, Integer> invokeSquare =
                new TemplateInvocation<Integer, Integer>() {

                    @Override
                    public void onInput(final Integer integer,
                            @Nonnull final ResultChannel<Integer> result) {

                        final int input = integer;

                        result.pass(input * input);
                    }
                };

        final Routine<Integer, Integer> squareRoutine =
                on(ClassToken.tokenOf(invokeSquare)).withArgs(this).buildRoutine();

        assertThat(
                sumRoutine.callSync(squareRoutine.callSync(1, 2, 3, 4)).afterMax(timeout).readAll())
                .containsExactly(30);
        assertThat(sumRoutine.callAsync(squareRoutine.callSync(1, 2, 3, 4))
                             .afterMax(timeout)
                             .readAll()).containsExactly(30);

        assertThat(sumRoutine.callSync(squareRoutine.callAsync(1, 2, 3, 4))
                             .afterMax(timeout)
                             .readAll()).containsExactly(30);
        assertThat(sumRoutine.callAsync(squareRoutine.callAsync(1, 2, 3, 4))
                             .afterMax(timeout)
                             .readAll()).containsExactly(30);

        assertThat(sumRoutine.callSync(squareRoutine.callParallel(1, 2, 3, 4))
                             .afterMax(timeout)
                             .readAll()).containsExactly(30);
        assertThat(sumRoutine.callAsync(squareRoutine.callParallel(1, 2, 3, 4))
                             .afterMax(timeout)
                             .readAll()).containsExactly(30);
    }

    public void testComposedRoutine() {

        final TimeDuration timeout = seconds(1);
        final SingleCallInvocation<Integer, Integer> execSum =
                new SingleCallInvocation<Integer, Integer>() {

                    @Override
                    public void onCall(@Nonnull final List<? extends Integer> integers,
                            @Nonnull final ResultChannel<Integer> result) {

                        int sum = 0;

                        for (final Integer integer : integers) {

                            sum += integer;
                        }

                        result.pass(sum);
                    }
                };

        final Routine<Integer, Integer> sumRoutine =
                on(ClassToken.tokenOf(execSum)).withArgs(this).buildRoutine();

        final TemplateInvocation<Integer, Integer> invokeSquare =
                new TemplateInvocation<Integer, Integer>() {

                    @Override
                    public void onInput(final Integer integer,
                            @Nonnull final ResultChannel<Integer> result) {

                        final int input = integer;

                        result.pass(input * input);
                    }
                };

        final Routine<Integer, Integer> squareRoutine =
                on(ClassToken.tokenOf(invokeSquare)).withArgs(this).buildRoutine();

        final TemplateInvocation<Integer, Integer> invokeSquareSum =
                new TemplateInvocation<Integer, Integer>() {

                    private ParameterChannel<Integer, Integer> mChannel;

                    @Override
                    public void onAbort(final Throwable reason) {

                        mChannel.abort(reason);
                    }

                    @Override
                    public void onInit() {

                        mChannel = sumRoutine.invokeAsync();
                    }

                    @Override
                    public void onInput(final Integer integer,
                            @Nonnull final ResultChannel<Integer> result) {

                        mChannel.pass(squareRoutine.callAsync(integer));
                    }

                    @Override
                    public void onResult(@Nonnull final ResultChannel<Integer> result) {

                        result.pass(mChannel.result());
                    }
                };

        final Routine<Integer, Integer> squareSumRoutine =
                on(ClassToken.tokenOf(invokeSquareSum)).withArgs(this, sumRoutine, squareRoutine)
                                                       .buildRoutine();

        assertThat(
                squareSumRoutine.callSync(1, 2, 3, 4).afterMax(timeout).readAll()).containsExactly(
                30);
        assertThat(
                squareSumRoutine.callAsync(1, 2, 3, 4).afterMax(timeout).readAll()).containsExactly(
                30);
    }

    public void testDelay() {

        final Routine<String, String> routine = JRoutine.on(tokenOf(DelayedInvocation.class))
                                                        .withArgs(TimeDuration.millis(10))
                                                        .buildRoutine();

        long startTime = System.currentTimeMillis();

        final ParameterChannel<String, String> channel = routine.invokeAsync();
        channel.after(100, TimeUnit.MILLISECONDS).pass("test1");
        channel.after(TimeDuration.millis(10).nanosTime()).pass("test2");
        channel.after(TimeDuration.millis(10).microsTime()).pass("test3", "test4");
        channel.after(TimeDuration.millis(10)).pass((String[]) null);
        channel.now().pass((List<String>) null).pass((OutputChannel<String>) null);
        assertThat(channel.result().afterMax(3, TimeUnit.SECONDS).readAll()).containsOnly("test1",
                                                                                          "test2",
                                                                                          "test3",
                                                                                          "test4");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(110);

        final Routine<String, String> routine1 = JRoutine.on(tokenOf(DelayedInvocation.class))
                                                         .withInputOrder(OrderBy.INSERTION)
                                                         .withOutputOrder(OrderBy.INSERTION)
                                                         .withArgs(TimeDuration.millis(10))
                                                         .buildRoutine();

        startTime = System.currentTimeMillis();

        final ParameterChannel<String, String> channel1 = routine1.invokeAsync();
        channel1.after(100, TimeUnit.MILLISECONDS).pass("test1");
        channel1.after(TimeDuration.millis(10).nanosTime()).pass("test2");
        channel1.after(TimeDuration.millis(10).microsTime()).pass(Arrays.asList("test3", "test4"));
        channel1.after(TimeDuration.millis(10)).pass((String[]) null);
        channel1.now().pass((List<String>) null).pass((OutputChannel<String>) null);
        assertThat(
                channel1.result().afterMax(TimeDuration.seconds(7000)).readAll()).containsExactly(
                "test1", "test2", "test3", "test4");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(110);

        final Routine<String, String> routine2 = JRoutine.on(tokenOf(DelayedListInvocation.class))
                                                         .withArgs(TimeDuration.millis(10), 2)
                                                         .buildRoutine();

        startTime = System.currentTimeMillis();

        final ParameterChannel<String, String> channel2 = routine2.invokeAsync();
        channel2.after(100, TimeUnit.MILLISECONDS).pass("test1");
        channel2.after(TimeDuration.millis(10).nanosTime()).pass("test2");
        channel2.after(TimeDuration.millis(10).microsTime()).pass("test3", "test4");
        channel2.after(TimeDuration.millis(10)).pass((String[]) null);
        channel2.now().pass((List<String>) null).pass((OutputChannel<String>) null);
        assertThat(channel2.result().afterMax(3, TimeUnit.SECONDS).readAll()).containsOnly("test1",
                                                                                           "test2",
                                                                                           "test3",
                                                                                           "test4");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(110);

        final Routine<String, String> routine3 = JRoutine.on(tokenOf(DelayedListInvocation.class))
                                                         .withInputOrder(OrderBy.INSERTION)
                                                         .withOutputOrder(OrderBy.INSERTION)
                                                         .withArgs(TimeDuration.millis(10), 2)
                                                         .buildRoutine();

        startTime = System.currentTimeMillis();

        final ParameterChannel<String, String> channel3 = routine3.invokeAsync();
        channel3.after(100, TimeUnit.MILLISECONDS).pass("test1");
        channel3.after(TimeDuration.millis(10).nanosTime()).pass("test2");
        channel3.after(TimeDuration.millis(10).microsTime()).pass("test3", "test4");
        channel3.after(TimeDuration.millis(10)).pass((String[]) null);
        channel3.now().pass((List<String>) null).pass((OutputChannel<String>) null);
        assertThat(channel3.result().afterMax(3, TimeUnit.SECONDS).readAll()).containsExactly(
                "test1", "test2", "test3", "test4");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(110);

        final Routine<String, String> routine4 = JRoutine.on(tokenOf(DelayedListInvocation.class))
                                                         .withArgs(TimeDuration.ZERO, 2)
                                                         .buildRoutine();

        startTime = System.currentTimeMillis();

        final ParameterChannel<String, String> channel4 = routine4.invokeAsync();
        channel4.after(100, TimeUnit.MILLISECONDS).pass("test1");
        channel4.after(TimeDuration.millis(10).nanosTime()).pass("test2");
        channel4.after(TimeDuration.millis(10).microsTime()).pass("test3", "test4");
        channel4.after(TimeDuration.millis(10)).pass((String[]) null);
        channel4.now().pass((List<String>) null).pass((OutputChannel<String>) null);
        assertThat(channel4.result().afterMax(3, TimeUnit.SECONDS).readAll()).containsOnly("test1",
                                                                                           "test2",
                                                                                           "test3",
                                                                                           "test4");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(100);

        final Routine<String, String> routine5 = JRoutine.on(tokenOf(DelayedListInvocation.class))
                                                         .withInputOrder(OrderBy.INSERTION)
                                                         .withOutputOrder(OrderBy.INSERTION)
                                                         .withArgs(TimeDuration.ZERO, 2)
                                                         .buildRoutine();

        startTime = System.currentTimeMillis();

        final ParameterChannel<String, String> channel5 = routine5.invokeAsync();
        channel5.after(100, TimeUnit.MILLISECONDS).pass("test1");
        channel5.after(TimeDuration.millis(10).nanosTime()).pass("test2");
        channel5.after(TimeDuration.millis(10).microsTime()).pass("test3", "test4");
        channel5.after(TimeDuration.millis(10)).pass((String[]) null);
        channel5.now().pass((List<String>) null).pass((OutputChannel<String>) null);
        assertThat(channel5.result().afterMax(3, TimeUnit.SECONDS).readAll()).containsExactly(
                "test1", "test2", "test3", "test4");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(100);

        final Routine<String, String> routine6 =
                JRoutine.on(tokenOf(DelayedChannelInvocation.class))
                        .withArgs(TimeDuration.millis(10))
                        .buildRoutine();

        startTime = System.currentTimeMillis();

        final ParameterChannel<String, String> channel6 = routine6.invokeAsync();
        channel6.after(100, TimeUnit.MILLISECONDS).pass("test1");
        channel6.after(TimeDuration.millis(10).nanosTime()).pass("test2");
        channel6.after(TimeDuration.millis(10).microsTime()).pass("test3", "test4");
        channel6.after(TimeDuration.millis(10)).pass((String[]) null);
        channel6.now().pass((List<String>) null).pass((OutputChannel<String>) null);
        assertThat(channel6.result().afterMax(3, TimeUnit.SECONDS).readAll()).containsOnly("test1",
                                                                                           "test2",
                                                                                           "test3",
                                                                                           "test4");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(110);

        final Routine<String, String> routine7 =
                JRoutine.on(tokenOf(DelayedChannelInvocation.class))
                        .withInputOrder(OrderBy.INSERTION)
                        .withOutputOrder(OrderBy.INSERTION)
                        .withArgs(TimeDuration.millis(10))
                        .buildRoutine();

        startTime = System.currentTimeMillis();

        final ParameterChannel<String, String> channel7 = routine7.invokeAsync();
        channel7.after(100, TimeUnit.MILLISECONDS).pass("test1");
        channel7.after(TimeDuration.millis(10).nanosTime()).pass("test2");
        channel7.after(TimeDuration.millis(10).microsTime()).pass("test3", "test4");
        channel7.after(TimeDuration.millis(10)).pass((String[]) null);
        channel7.now().pass((List<String>) null).pass((OutputChannel<String>) null);
        assertThat(channel7.result().afterMax(3, TimeUnit.SECONDS).readAll()).containsExactly(
                "test1", "test2", "test3", "test4");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(110);

        final Routine<String, String> routine8 =
                JRoutine.on(tokenOf(DelayedChannelInvocation.class))
                        .withArgs(TimeDuration.ZERO)
                        .buildRoutine();

        startTime = System.currentTimeMillis();

        final ParameterChannel<String, String> channel8 = routine8.invokeAsync();
        channel8.after(100, TimeUnit.MILLISECONDS).pass("test1");
        channel8.after(TimeDuration.millis(10).nanosTime()).pass("test2");
        channel8.after(TimeDuration.millis(10).microsTime()).pass("test3", "test4");
        channel8.after(TimeDuration.millis(10)).pass((String[]) null);
        channel8.now().pass((List<String>) null).pass((OutputChannel<String>) null);
        assertThat(channel8.result().afterMax(3, TimeUnit.SECONDS).readAll()).containsOnly("test1",
                                                                                           "test2",
                                                                                           "test3",
                                                                                           "test4");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(100);

        final Routine<String, String> routine9 =
                JRoutine.on(tokenOf(DelayedChannelInvocation.class))
                        .withInputOrder(OrderBy.INSERTION)
                        .withOutputOrder(OrderBy.INSERTION)
                        .withArgs(TimeDuration.ZERO)
                        .buildRoutine();

        startTime = System.currentTimeMillis();

        final ParameterChannel<String, String> channel9 = routine9.invokeAsync();
        channel9.after(100, TimeUnit.MILLISECONDS).pass("test1");
        channel9.after(TimeDuration.millis(10).nanosTime()).pass("test2");
        channel9.after(TimeDuration.millis(10).microsTime()).pass("test3", "test4");
        channel9.after(TimeDuration.millis(10)).pass((String[]) null);
        channel9.now().pass((List<String>) null).pass((OutputChannel<String>) null);
        assertThat(channel9.result().afterMax(3, TimeUnit.SECONDS).readAll()).containsExactly(
                "test1", "test2", "test3", "test4");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(100);
    }

    public void testDelayedAbort() throws InterruptedException {

        final TimeDuration timeout = seconds(1);
        final Routine<String, String> passingRoutine =
                JRoutine.on(new ClassToken<PassingInvocation<String>>() {}).buildRoutine();

        final ParameterChannel<String, String> channel1 = passingRoutine.invokeAsync();
        channel1.after(TimeDuration.seconds(2)).abort();
        assertThat(channel1.now().pass("test").result().afterMax(timeout).readNext()).isEqualTo(
                "test");

        final ParameterChannel<String, String> channel2 = passingRoutine.invokeAsync();
        channel2.after(TimeDuration.millis(100)).abort();

        try {

            channel2.after(TimeDuration.millis(200))
                    .pass("test")
                    .result()
                    .afterMax(timeout)
                    .readNext();

            fail();

        } catch (final InvocationException ignored) {

        }

        final Routine<String, String> abortRoutine =
                JRoutine.on(tokenOf(DelayedAbortInvocation.class))
                        .withArgs(TimeDuration.millis(200))
                        .buildRoutine();

        assertThat(abortRoutine.callAsync("test").afterMax(timeout).readNext()).isEqualTo("test");

        try {

            final OutputChannel<String> channel = abortRoutine.callAsync("test");

            TimeDuration.millis(500).sleepAtLeast();

            channel.afterMax(timeout).readNext();

            fail();

        } catch (final InvocationException ignored) {

        }
    }

    public void testDelayedBind() {

        final TimeDuration timeout = seconds(1);
        final Routine<Object, Object> routine1 =
                JRoutine.on(new ClassToken<PassingInvocation<Object>>() {}).buildRoutine();
        final Routine<Object, Object> routine2 =
                JRoutine.on(new ClassToken<PassingInvocation<Object>>() {}).buildRoutine();

        final long startTime = System.currentTimeMillis();

        assertThat(routine1.invokeAsync()
                           .after(TimeDuration.millis(500))
                           .pass(routine2.callAsync("test"))
                           .result()
                           .afterMax(timeout)
                           .readNext()).isEqualTo("test");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(500);
    }

    public void testDestroy() {

        final TimeDuration timeout = seconds(1);
        final Routine<String, String> routine1 =
                JRoutine.on(tokenOf(TestDestroy.class)).withCoreInvocations(0).buildRoutine();
        assertThat(routine1.callSync("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .readAll()).containsOnly("1", "2", "3", "4", "5");
        assertThat(routine1.callAsync("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .readAll()).containsOnly("1", "2", "3", "4", "5");
        assertThat(routine1.callParallel("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .readAll()).containsOnly("1", "2", "3", "4", "5");
        assertThat(TestDestroy.getInstanceCount()).isZero();

        final Routine<String, String> routine2 = JRoutine.on(tokenOf(TestDiscardException.class))
                                                         .withCoreInvocations(0)
                                                         .buildRoutine();
        assertThat(routine2.callSync("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .readAll()).containsOnly("1", "2", "3", "4", "5");
        assertThat(routine2.callAsync("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .readAll()).containsOnly("1", "2", "3", "4", "5");
        assertThat(routine2.callParallel("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .readAll()).containsOnly("1", "2", "3", "4", "5");
        assertThat(TestDestroy.getInstanceCount()).isZero();

        final Routine<String, String> routine3 =
                JRoutine.on(tokenOf(TestDestroyDiscard.class)).buildRoutine();
        assertThat(routine3.callSync("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .checkComplete()).isTrue();
        assertThat(routine3.callParallel("1", "2", "3", "4", "5").afterMax(timeout).checkComplete())
                .isTrue();
        assertThat(routine3.callAsync("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .checkComplete()).isTrue();
        assertThat(TestDestroy.getInstanceCount()).isZero();

        final Routine<String, String> routine4 =
                JRoutine.on(tokenOf(TestDestroyDiscardException.class)).buildRoutine();
        assertThat(routine4.callSync("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .checkComplete()).isTrue();
        assertThat(routine4.callParallel("1", "2", "3", "4", "5").afterMax(timeout).checkComplete())
                .isTrue();
        assertThat(routine4.callAsync("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .checkComplete()).isTrue();
        assertThat(TestDestroy.getInstanceCount()).isZero();

        final Routine<String, String> routine5 =
                JRoutine.on(tokenOf(TestDestroy.class)).buildRoutine();
        assertThat(routine5.callSync("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .readAll()).containsOnly("1", "2", "3", "4", "5");
        assertThat(routine5.callAsync("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .readAll()).containsOnly("1", "2", "3", "4", "5");
        assertThat(routine5.callParallel("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .readAll()).containsOnly("1", "2", "3", "4", "5");
        routine5.purge();
        assertThat(TestDestroy.getInstanceCount()).isZero();

        final Routine<String, String> routine6 =
                JRoutine.on(tokenOf(TestDestroyException.class)).buildRoutine();

        assertThat(routine6.callSync("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .readAll()).containsOnly("1", "2", "3", "4", "5");
        assertThat(routine6.callAsync("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .readAll()).containsOnly("1", "2", "3", "4", "5");
        assertThat(routine6.callParallel("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .readAll()).containsOnly("1", "2", "3", "4", "5");
        routine6.purge();
        assertThat(TestDestroy.getInstanceCount()).isZero();
    }

    @SuppressWarnings("ConstantConditions")
    public void testError() {

        try {

            on(tokenOf(ConstructorException.class)).withLogLevel(LogLevel.SILENT)
                                                   .buildRoutine()
                                                   .callSync()
                                                   .readAll();

            fail();

        } catch (final InvocationException ignored) {

        }

        try {

            new AbstractRoutine<Object, Object>(null) {

                @Override
                @Nonnull
                protected Invocation<Object, Object> createInvocation(final boolean async) {

                    return new ConstructorException();
                }
            };

            fail();

        } catch (final NullPointerException ignored) {

        }

        final Logger logger = Logger.createLogger(null, null, this);

        try {

            final DefaultResultChannel<Object> channel = new DefaultResultChannel<Object>(
                    new RoutineConfigurationBuilder().buildConfiguration(), new TestAbortHandler(),
                    Runners.sequentialRunner(), logger);

            new DefaultExecution<Object, Object>(null, new TestInputIterator(), channel, logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final DefaultResultChannel<Object> channel = new DefaultResultChannel<Object>(
                    new RoutineConfigurationBuilder().buildConfiguration(), new TestAbortHandler(),
                    Runners.sequentialRunner(), logger);

            new DefaultExecution<Object, Object>(new TestInvocationManager(), null, channel,
                                                 logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultExecution<Object, Object>(new TestInvocationManager(),
                                                 new TestInputIterator(), null, logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final DefaultResultChannel<Object> channel = new DefaultResultChannel<Object>(
                    new RoutineConfigurationBuilder().buildConfiguration(), new TestAbortHandler(),
                    Runners.sequentialRunner(), logger);

            new DefaultExecution<Object, Object>(new TestInvocationManager(),
                                                 new TestInputIterator(), channel, null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testErrorConsumerOnComplete() {

        final TemplateOutputConsumer<String> exceptionConsumer =
                new TemplateOutputConsumer<String>() {

                    @Override
                    public void onComplete() {

                        throw new NullPointerException("test2");
                    }
                };

        testConsumer(exceptionConsumer);
    }

    public void testErrorConsumerOnOutput() {

        final TemplateOutputConsumer<String> exceptionConsumer =
                new TemplateOutputConsumer<String>() {

                    @Override
                    public void onOutput(final String output) {

                        throw new NullPointerException(output);
                    }
                };

        testConsumer(exceptionConsumer);
    }

    public void testErrorOnInit() {

        final TemplateInvocation<String, String> exceptionOnInit =
                new TemplateInvocation<String, String>() {

                    @Override
                    public void onInit() {

                        throw new NullPointerException("test1");
                    }
                };

        final Routine<String, String> exceptionRoutine =
                on(ClassToken.tokenOf(exceptionOnInit)).withArgs(this).buildRoutine();

        testException(exceptionRoutine, "test", "test1");

        final Routine<String, String> passingRoutine =
                JRoutine.on(new ClassToken<PassingInvocation<String>>() {}).buildRoutine();

        testChained(passingRoutine, exceptionRoutine, "test", "test1");
        testChained(exceptionRoutine, passingRoutine, "test", "test1");
    }

    public void testErrorOnInput() {

        final TemplateInvocation<String, String> exceptionOnInput =
                new TemplateInvocation<String, String>() {

                    @Override
                    public void onInput(final String s,
                            @Nonnull final ResultChannel<String> result) {

                        throw new NullPointerException(s);
                    }
                };

        final Routine<String, String> exceptionRoutine =
                on(ClassToken.tokenOf(exceptionOnInput)).withArgs(this).buildRoutine();

        testException(exceptionRoutine, "test2", "test2");

        final Routine<String, String> passingRoutine =
                JRoutine.on(new ClassToken<PassingInvocation<String>>() {}).buildRoutine();

        testChained(passingRoutine, exceptionRoutine, "test2", "test2");
        testChained(exceptionRoutine, passingRoutine, "test2", "test2");
    }

    public void testErrorOnResult() {

        final TemplateInvocation<String, String> exceptionOnResult =
                new TemplateInvocation<String, String>() {

                    @Override
                    public void onResult(@Nonnull final ResultChannel<String> result) {

                        throw new NullPointerException("test3");
                    }
                };

        final Routine<String, String> exceptionRoutine =
                on(ClassToken.tokenOf(exceptionOnResult)).withArgs(this).buildRoutine();

        testException(exceptionRoutine, "test", "test3");

        final Routine<String, String> passingRoutine =
                JRoutine.on(new ClassToken<PassingInvocation<String>>() {}).buildRoutine();

        testChained(passingRoutine, exceptionRoutine, "test", "test3");
        testChained(exceptionRoutine, passingRoutine, "test", "test3");
    }

    public void testInputTimeout() {

        final Routine<String, String> routine =
                JRoutine.on(new ClassToken<PassingInvocation<String>>() {})
                        .withInputSize(1)
                        .withInputTimeout(TimeDuration.ZERO)
                        .buildRoutine();

        try {

            routine.callAsync("test1", "test2").readAll();

            fail();

        } catch (final InputDeadlockException ignored) {

        }
    }

    public void testInvocationLifecycle() throws InterruptedException {

        final Routine<String, String> routine =
                JRoutine.on(tokenOf(TestLifecycle.class)).buildRoutine();
        final OutputChannel<String> outputChannel = routine.callAsync("test");

        Thread.sleep(500);

        outputChannel.abort();
        outputChannel.afterMax(INFINITY).checkComplete();
        assertThat(TestLifecycle.sIsError).isFalse();
    }

    public void testMethod() throws NoSuchMethodException {

        final TimeDuration timeout = seconds(1);
        final TestClass testClass = new TestClass();
        assertThat(on(testClass).method(TestClass.class.getMethod("getOne"))
                                .callSync()
                                .afterMax(timeout)
                                .readAll()).containsExactly(1);
        assertThat(on(testClass).method("getOne")
                                .callSync()
                                .afterMax(timeout)
                                .readAll()).containsExactly(1);
        assertThat(on(testClass).boundMethod(TestClass.GET)
                                .callSync()
                                .afterMax(timeout)
                                .readAll()).containsExactly(1);
        assertThat(on(TestClass.class).boundMethod(TestClass.GET)
                                      .callSync(3)
                                      .afterMax(timeout)
                                      .readAll()).containsExactly(3);
        assertThat(on(TestClass.class).boundMethod("get").callAsync(-3).afterMax(timeout).readAll())
                .containsExactly(-3);
        assertThat(on(TestClass.class).method("get", int.class)
                                      .callParallel(17)
                                      .afterMax(timeout)
                                      .readAll()).containsExactly(17);

        assertThat(on(testClass).buildProxy(TestInterface.class).getInt(2)).isEqualTo(2);

        try {

            on(TestClass.class).boundMethod("get").callAsync().afterMax(timeout).readAll();

            fail();

        } catch (final InvocationException ignored) {

        }

        try {

            on(TestClass.class).boundMethod("take");

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        assertThat(on(testClass).withReadTimeout(timeout)
                                .buildProxy(TestInterfaceAsync.class)
                                .take(77)).isEqualTo(77);
        assertThat(on(testClass).buildProxy(TestInterfaceAsync.class)
                                .getOne()
                                .afterMax(timeout)
                                .readNext()).isEqualTo(1);

        final TestInterfaceAsync testInterfaceAsync =
                on(testClass).withReadTimeout(1, TimeUnit.SECONDS)
                             .buildProxy(TestInterfaceAsync.class);
        assertThat(testInterfaceAsync.getInt(testInterfaceAsync.getOne())).isEqualTo(1);
    }

    public void testOutputTimeout() {

        final Routine<String, String> routine =
                JRoutine.on(tokenOf(new SingleCallInvocation<String, String>() {

                    @Override
                    public void onCall(@Nonnull final List<? extends String> strings,
                            @Nonnull final ResultChannel<String> result) {

                        result.pass(strings);
                    }
                }))
                        .withOutputSize(1)
                        .withOutputTimeout(TimeDuration.ZERO)
                        .withArgs(this)
                        .buildRoutine();

        try {

            routine.callAsync("test1", "test2").eventually().readAll();

            fail();

        } catch (final OutputDeadlockException ignored) {

        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testParameterChannelError() {

        final Logger logger = Logger.createLogger(new NullLog(), LogLevel.DEBUG, this);

        try {

            final RoutineConfiguration configuration =
                    new RoutineConfigurationBuilder().buildConfiguration();

            new DefaultParameterChannel<Object, Object>(configuration, null, Runners.sharedRunner(),
                                                        logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final RoutineConfiguration configuration =
                    new RoutineConfigurationBuilder().buildConfiguration();

            new DefaultParameterChannel<Object, Object>(configuration, new TestInvocationManager(),
                                                        null, logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final RoutineConfiguration configuration =
                    new RoutineConfigurationBuilder().buildConfiguration();

            new DefaultParameterChannel<Object, Object>(configuration, new TestInvocationManager(),
                                                        Runners.sharedRunner(), null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultParameterChannel<Object, Object>(null, new TestInvocationManager(),
                                                        Runners.sharedRunner(), logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final RoutineConfiguration configuration =
                    new RoutineConfigurationBuilder().buildConfiguration();

            final DefaultParameterChannel<Object, Object> channel =
                    new DefaultParameterChannel<Object, Object>(configuration,
                                                                new TestInvocationManager(),
                                                                Runners.sharedRunner(), logger);

            channel.result();
            channel.pass("test");

            fail();

        } catch (final IllegalStateException ignored) {

        }

        try {

            final RoutineConfiguration configuration =
                    new RoutineConfigurationBuilder().buildConfiguration();

            final DefaultParameterChannel<Object, Object> channel =
                    new DefaultParameterChannel<Object, Object>(configuration,
                                                                new TestInvocationManager(),
                                                                Runners.sharedRunner(), logger);

            channel.after(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final RoutineConfiguration configuration =
                    new RoutineConfigurationBuilder().buildConfiguration();

            final DefaultParameterChannel<Object, Object> channel =
                    new DefaultParameterChannel<Object, Object>(configuration,
                                                                new TestInvocationManager(),
                                                                Runners.sharedRunner(), logger);

            channel.after(1, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final RoutineConfiguration configuration =
                    new RoutineConfigurationBuilder().buildConfiguration();

            final DefaultParameterChannel<Object, Object> channel =
                    new DefaultParameterChannel<Object, Object>(configuration,
                                                                new TestInvocationManager(),
                                                                Runners.sharedRunner(), logger);

            channel.after(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testPartialOut() {

        final TemplateInvocation<String, String> invocation =
                new TemplateInvocation<String, String>() {

                    @Override
                    public void onInput(final String s,
                            @Nonnull final ResultChannel<String> result) {

                        result.now().pass(s).after(TimeDuration.seconds(2)).abort();
                    }
                };

        final Routine<String, String> routine =
                JRoutine.on(tokenOf(invocation)).withArgs(this).buildRoutine();
        assertThat(routine.callAsync("test")
                          .afterMax(TimeDuration.millis(500))
                          .readAll()).containsExactly("test");
    }

    @SuppressWarnings("ConstantConditions")
    public void testResultChannelError() {

        final Logger logger = Logger.createLogger(new NullLog(), LogLevel.DEBUG, this);

        try {

            final RoutineConfiguration configuration =
                    new RoutineConfigurationBuilder().buildConfiguration();

            new DefaultResultChannel<Object>(configuration, null, Runners.sharedRunner(), logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final RoutineConfiguration configuration =
                    new RoutineConfigurationBuilder().buildConfiguration();

            new DefaultResultChannel<Object>(configuration, new TestAbortHandler(), null, logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final RoutineConfiguration configuration =
                    new RoutineConfigurationBuilder().buildConfiguration();

            new DefaultResultChannel<Object>(configuration, new TestAbortHandler(),
                                             Runners.sharedRunner(), null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final RoutineConfiguration configuration =
                    new RoutineConfigurationBuilder().buildConfiguration();

            new DefaultResultChannel<Object>(configuration, new TestAbortHandler(),
                                             Runners.sharedRunner(), logger).after(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final RoutineConfiguration configuration =
                    new RoutineConfigurationBuilder().buildConfiguration();

            new DefaultResultChannel<Object>(configuration, new TestAbortHandler(),
                                             Runners.sharedRunner(), logger).after(0, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final RoutineConfiguration configuration =
                    new RoutineConfigurationBuilder().buildConfiguration();

            final DefaultResultChannel<Object> channel =
                    new DefaultResultChannel<Object>(configuration, new TestAbortHandler(),
                                                     Runners.sharedRunner(), logger);

            channel.after(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        final Routine<String, String> routine = JRoutine.on(tokenOf(DelayedInvocation.class))
                                                        .withLogLevel(LogLevel.SILENT)
                                                        .withArgs(TimeDuration.ZERO)
                                                        .buildRoutine();
        final OutputChannel<String> channel = routine.callSync();

        try {

            channel.afterMax(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            channel.afterMax(0, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            channel.afterMax(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            channel.bind(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            channel.readAllInto(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        final TemplateOutputConsumer<String> consumer = new TemplateOutputConsumer<String>() {};

        try {

            channel.bind(consumer).bind(consumer);

            fail();

        } catch (final IllegalStateException ignored) {

        }

        try {

            channel.iterator();

            fail();

        } catch (final IllegalStateException ignored) {

        }

        final Routine<String, String> routine1 = JRoutine.on(tokenOf(DelayedInvocation.class))
                                                         .withLogLevel(LogLevel.SILENT)
                                                         .withArgs(millis(100))
                                                         .buildRoutine();
        final Iterator<String> iterator =
                routine1.callSync("test").afterMax(millis(500)).eventuallyExit().iterator();

        assertThat(iterator.next()).isEqualTo("test");
        iterator.remove();

        try {

            iterator.remove();

            fail();

        } catch (final IllegalStateException ignored) {

        }

        try {

            iterator.next();

            fail();

        } catch (final NoSuchElementException ignored) {

        }

        try {

            routine1.callSync().immediately().eventuallyExit().iterator().next();

            fail();

        } catch (final NoSuchElementException ignored) {

        }

        try {

            routine1.callAsync("test").immediately().iterator().next();

            fail();

        } catch (final ReadDeadlockException ignored) {

        }
    }

    public void testRoutine() {

        final TimeDuration timeout = seconds(1);
        final TemplateInvocation<Integer, Integer> execSquare =
                new TemplateInvocation<Integer, Integer>() {

                    @Override
                    public void onInput(final Integer integer,
                            @Nonnull final ResultChannel<Integer> result) {

                        final int input = integer;

                        result.pass(input * input);
                    }
                };

        final Routine<Integer, Integer> squareRoutine =
                on(ClassToken.tokenOf(execSquare)).withArgs(this).buildRoutine();

        assertThat(squareRoutine.callSync(1, 2, 3, 4).afterMax(timeout).readAll()).containsExactly(
                1, 4, 9, 16);
        assertThat(squareRoutine.callAsync(1, 2, 3, 4).afterMax(timeout).readAll()).containsExactly(
                1, 4, 9, 16);
        assertThat(squareRoutine.callParallel(1, 2, 3, 4).afterMax(timeout).readAll()).containsOnly(
                1, 4, 9, 16);
    }

    public void testRoutineFunction() {

        final TimeDuration timeout = seconds(1);
        final SingleCallInvocation<Integer, Integer> execSum =
                new SingleCallInvocation<Integer, Integer>() {

                    @Override
                    public void onCall(@Nonnull final List<? extends Integer> integers,
                            @Nonnull final ResultChannel<Integer> result) {

                        int sum = 0;

                        for (final Integer integer : integers) {

                            sum += integer;
                        }

                        result.pass(sum);
                    }
                };

        final Routine<Integer, Integer> sumRoutine =
                on(ClassToken.tokenOf(execSum)).withArgs(this).buildRoutine();

        assertThat(sumRoutine.callSync(1, 2, 3, 4).afterMax(timeout).readAll()).containsExactly(10);
        assertThat(sumRoutine.callAsync(1, 2, 3, 4).afterMax(timeout).readAll()).containsExactly(
                10);
    }

    public void testTimeoutActions() {

        final Routine<String, String> routine1 =
                on(tokenOf(DelayedInvocation.class)).onReadTimeout(TimeoutAction.ABORT)
                                                    .withArgs(seconds(1))
                                                    .buildRoutine();

        try {

            routine1.callAsync("test1").readNext();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            routine1.callAsync("test1").readAll();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            final ArrayList<String> results = new ArrayList<String>();
            routine1.callAsync("test1").readAllInto(results);

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            routine1.callAsync("test1").iterator().hasNext();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            routine1.callAsync("test1").iterator().next();

            fail();

        } catch (final AbortException ignored) {

        }

        assertThat(routine1.callAsync("test1").checkComplete()).isFalse();

        final Routine<String, String> routine2 =
                on(tokenOf(DelayedInvocation.class)).onReadTimeout(TimeoutAction.ABORT)
                                                    .withReadTimeout(millis(10))
                                                    .withArgs(seconds(1))
                                                    .buildRoutine();

        try {

            routine2.callAsync("test1").readNext();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            routine2.callAsync("test1").readAll();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            final ArrayList<String> results = new ArrayList<String>();
            routine2.callAsync("test1").readAllInto(results);

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            routine2.callAsync("test1").iterator().hasNext();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            routine2.callAsync("test1").iterator().next();

            fail();

        } catch (final AbortException ignored) {

        }

        assertThat(routine2.callAsync("test1").checkComplete()).isFalse();

        final Routine<String, String> routine3 =
                on(tokenOf(DelayedInvocation.class)).onReadTimeout(TimeoutAction.DEADLOCK)
                                                    .withArgs(seconds(1))
                                                    .buildRoutine();
        final OutputChannel<String> channel3 = routine3.callAsync("test1");

        try {

            channel3.readNext();

            fail();

        } catch (final ReadDeadlockException ignored) {

        }

        try {

            channel3.readAll();

            fail();

        } catch (final ReadDeadlockException ignored) {

        }

        try {

            final ArrayList<String> results = new ArrayList<String>();
            channel3.readAllInto(results);

            fail();

        } catch (final ReadDeadlockException ignored) {

        }

        try {

            channel3.iterator().hasNext();

            fail();

        } catch (final ReadDeadlockException ignored) {

        }

        try {

            channel3.iterator().next();

            fail();

        } catch (final ReadDeadlockException ignored) {

        }

        assertThat(channel3.checkComplete()).isFalse();

        channel3.afterMax(millis(10));

        try {

            channel3.readNext();

            fail();

        } catch (final ReadDeadlockException ignored) {

        }

        try {

            channel3.readAll();

            fail();

        } catch (final ReadDeadlockException ignored) {

        }

        try {

            final ArrayList<String> results = new ArrayList<String>();
            channel3.readAllInto(results);

            fail();

        } catch (final ReadDeadlockException ignored) {

        }

        try {

            channel3.iterator().hasNext();

            fail();

        } catch (final ReadDeadlockException ignored) {

        }

        try {

            channel3.iterator().next();

            fail();

        } catch (final ReadDeadlockException ignored) {

        }

        assertThat(channel3.checkComplete()).isFalse();

        final Routine<String, String> routine4 =
                on(tokenOf(DelayedInvocation.class)).onReadTimeout(TimeoutAction.EXIT)
                                                    .withArgs(seconds(1))
                                                    .buildRoutine();
        final OutputChannel<String> channel4 = routine4.callAsync("test1");

        try {

            channel4.readNext();

            fail();

        } catch (final NoSuchElementException ignored) {

        }

        assertThat(channel4.readAll()).isEmpty();

        final ArrayList<String> results = new ArrayList<String>();
        channel4.readAllInto(results);
        assertThat(results).isEmpty();

        assertThat(channel4.iterator().hasNext()).isFalse();

        try {

            channel4.iterator().next();

            fail();

        } catch (final NoSuchElementException ignored) {

        }

        assertThat(channel4.checkComplete()).isFalse();

        channel4.afterMax(millis(10));

        try {

            channel4.readNext();

            fail();

        } catch (final NoSuchElementException ignored) {

        }

        assertThat(channel4.readAll()).isEmpty();

        results.clear();
        channel4.readAllInto(results);
        assertThat(results).isEmpty();

        assertThat(channel4.iterator().hasNext()).isFalse();

        try {

            channel4.iterator().next();

            fail();

        } catch (final NoSuchElementException ignored) {

        }

        assertThat(channel4.checkComplete()).isFalse();
    }

    private void testChained(final Routine<String, String> before,
            final Routine<String, String> after, final String input, final String expected) {

        final TimeDuration timeout = seconds(1);

        try {

            before.callSync(after.callSync(input)).afterMax(timeout).readAll();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.callSync(after.callSync(input)).afterMax(timeout)) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.callAsync(after.callSync(input)).afterMax(timeout).readAll();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.callAsync(after.callSync(input)).afterMax(timeout)) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.callParallel(after.callSync(input)).afterMax(timeout).readAll();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.callParallel(after.callSync(input)).afterMax(timeout)) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.invokeSync().pass(after.callSync(input)).result().afterMax(timeout).readAll();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.invokeSync()
                                        .pass(after.callSync(input))
                                        .result()
                                        .afterMax(timeout)) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.invokeAsync().pass(after.callSync(input)).result().afterMax(timeout).readAll();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.invokeAsync()
                                        .pass(after.callSync(input))
                                        .result()
                                        .afterMax(timeout)) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.invokeParallel()
                  .pass(after.callSync(input))
                  .result()
                  .afterMax(timeout)
                  .readAll();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.invokeParallel()
                                        .pass(after.callSync(input))
                                        .result()
                                        .afterMax(timeout)) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.callSync(after.callAsync(input)).afterMax(timeout).readAll();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.callSync(after.callAsync(input)).afterMax(timeout)) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.callAsync(after.callAsync(input)).afterMax(timeout).readAll();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.callAsync(after.callAsync(input)).afterMax(timeout)) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.invokeSync().pass(after.callAsync(input)).result().afterMax(timeout).readAll();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.invokeSync()
                                        .pass(after.callAsync(input))
                                        .result()
                                        .afterMax(timeout)) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.invokeAsync().pass(after.callAsync(input)).result().afterMax(timeout).readAll();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.invokeAsync()
                                        .pass(after.callAsync(input))
                                        .result()
                                        .afterMax(timeout)) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.callSync(after.callParallel(input)).afterMax(timeout).readAll();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.callSync(after.callParallel(input)).afterMax(timeout)) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.callAsync(after.callParallel(input)).afterMax(timeout).readAll();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.callAsync(after.callParallel(input)).afterMax(timeout)) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.invokeSync()
                  .pass(after.callParallel(input))
                  .result()
                  .afterMax(timeout)
                  .readAll();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.invokeSync()
                                        .pass(after.callParallel(input))
                                        .result()
                                        .afterMax(timeout)) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.invokeAsync()
                  .pass(after.callParallel(input))
                  .result()
                  .afterMax(timeout)
                  .readAll();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.invokeAsync()
                                        .pass(after.callParallel(input))
                                        .result()
                                        .afterMax(timeout)) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }
    }

    private void testConsumer(final OutputConsumer<String> consumer) {

        final TimeDuration timeout = seconds(1);
        final String input = "test";
        final Routine<String, String> routine =
                on(tokenOf(DelayedInvocation.class)).withArgs(TimeDuration.ZERO).buildRoutine();

        assertThat(
                routine.callSync(input).bind(consumer).afterMax(timeout).checkComplete()).isTrue();
        assertThat(
                routine.callAsync(input).bind(consumer).afterMax(timeout).checkComplete()).isTrue();
        assertThat(routine.callParallel(input)
                          .bind(consumer)
                          .afterMax(timeout)
                          .checkComplete()).isTrue();
        assertThat(routine.invokeSync()
                          .pass(input)
                          .result()
                          .bind(consumer)
                          .afterMax(timeout)
                          .checkComplete()).isTrue();
        assertThat(routine.invokeAsync()
                          .pass(input)
                          .result()
                          .bind(consumer)
                          .afterMax(timeout)
                          .checkComplete()).isTrue();
        assertThat(routine.invokeParallel()
                          .pass(input)
                          .result()
                          .bind(consumer)
                          .afterMax(timeout)
                          .checkComplete()).isTrue();
    }

    private void testException(final Routine<String, String> routine, final String input,
            final String expected) {

        final TimeDuration timeout = seconds(1);

        try {

            routine.callSync(input).afterMax(timeout).readAll();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : routine.callSync(input).afterMax(timeout)) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            routine.callAsync(input).afterMax(timeout).readAll();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : routine.callAsync(input).afterMax(timeout)) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            routine.callParallel(input).afterMax(timeout).readAll();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : routine.callParallel(input).afterMax(timeout)) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            routine.invokeSync().pass(input).result().afterMax(timeout).readAll();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : routine.invokeSync().pass(input).result().afterMax(timeout)) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            routine.invokeAsync().pass(input).result().afterMax(timeout).readAll();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : routine.invokeAsync().pass(input).result().afterMax(timeout)) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            routine.invokeParallel().pass(input).result().afterMax(timeout).readAll();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : routine.invokeParallel().pass(input).result().afterMax(timeout)) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }
    }

    private interface TestInterface {

        @Timeout(value = 1, unit = TimeUnit.SECONDS)
        public int getInt(int i);
    }

    private interface TestInterfaceAsync {

        public int getInt(@Async(int.class) OutputChannel<Integer> i);

        @Async(int.class)
        public OutputChannel<Integer> getOne();

        @Bind(value = "getInt")
        public int take(int i);
    }

    private static class ConstructorException extends TemplateInvocation<Object, Object> {

        public ConstructorException() {

            throw new IllegalStateException();
        }
    }

    private static class DelayedAbortInvocation extends TemplateInvocation<String, String> {

        private final TimeDuration mDelay;

        public DelayedAbortInvocation(final TimeDuration delay) {

            mDelay = delay;
        }

        @Override
        public void onInput(final String s, @Nonnull final ResultChannel<String> result) {

            result.now().pass(s).after(mDelay).abort();
        }
    }

    private static class DelayedChannelInvocation extends TemplateInvocation<String, String> {

        private final TimeDuration mDelay;

        private final Routine<String, String> mRoutine;

        private boolean mFlag;

        public DelayedChannelInvocation(final TimeDuration delay) {

            mDelay = delay;
            mRoutine =
                    on(tokenOf(DelayedInvocation.class)).withArgs(TimeDuration.ZERO).buildRoutine();
        }

        @Override
        public void onInput(final String s, @Nonnull final ResultChannel<String> result) {

            if (mFlag) {

                result.after(mDelay).pass((OutputChannel<String>) null);

            } else {

                result.after(mDelay.time, mDelay.unit).pass((OutputChannel<String>) null);
            }

            result.pass(mRoutine.callAsync(s));

            mFlag = !mFlag;
        }
    }

    private static class DelayedInvocation extends TemplateInvocation<String, String> {

        private final TimeDuration mDelay;

        private boolean mFlag;

        public DelayedInvocation(final TimeDuration delay) {

            mDelay = delay;
        }

        @Override
        public void onInput(final String s, @Nonnull final ResultChannel<String> result) {

            if (mFlag) {

                result.after(mDelay);

            } else {

                result.after(mDelay.time, mDelay.unit);
            }

            result.pass(s);

            mFlag = !mFlag;
        }
    }

    private static class DelayedListInvocation extends TemplateInvocation<String, String> {

        private final int mCount;

        private final TimeDuration mDelay;

        private final ArrayList<String> mList;

        private boolean mFlag;

        public DelayedListInvocation(final TimeDuration delay, final int listCount) {

            mDelay = delay;
            mCount = listCount;
            mList = new ArrayList<String>(listCount);
        }

        @Override
        public void onInput(final String s, @Nonnull final ResultChannel<String> result) {

            final ArrayList<String> list = mList;
            list.add(s);

            if (list.size() >= mCount) {

                if (mFlag) {

                    result.after(mDelay).pass((String[]) null).pass(list);

                } else {

                    result.after(mDelay.time, mDelay.unit)
                          .pass((List<String>) null)
                          .pass(list.toArray(new String[list.size()]));
                }

                result.now();
                list.clear();

                mFlag = !mFlag;
            }
        }

        @Override
        public void onResult(@Nonnull final ResultChannel<String> result) {

            final ArrayList<String> list = mList;
            result.after(mDelay).pass(list);
            list.clear();
        }
    }

    private static class TestAbortHandler implements AbortHandler {

        @Override
        public void onAbort(@Nullable final Throwable reason, final long delay,
                @Nonnull final TimeUnit timeUnit) {

        }
    }

    private static class TestClass implements TestInterface {

        public static final String GET = "get";

        @Bind(GET)
        public static int get(final int i) {

            return i;
        }

        @Override
        public int getInt(final int i) {

            return i;
        }

        @Bind(GET)
        public int getOne() {

            return 1;
        }
    }

    private static class TestDestroy extends TemplateInvocation<String, String> {

        private static final AtomicInteger sInstanceCount = new AtomicInteger();

        public TestDestroy() {

            sInstanceCount.incrementAndGet();
        }

        public static int getInstanceCount() {

            return sInstanceCount.get();
        }

        @Override
        public void onInput(final String input, @Nonnull final ResultChannel<String> result) {

            result.after(millis(100)).pass(input);
        }

        @Override
        public void onDestroy() {

            synchronized (sInstanceCount) {

                sInstanceCount.decrementAndGet();
                sInstanceCount.notifyAll();
            }
        }
    }

    private static class TestDestroyDiscard extends TestDestroy {

        @Override
        public void onDestroy() {

            super.onDestroy();
            throw new IllegalArgumentException("test");
        }

        @Override
        public void onReturn() {

            super.onReturn();
            throw new IllegalArgumentException("test");
        }
    }

    private static class TestDestroyDiscardException extends TestDestroyException {

        @Override
        public void onReturn() {

            super.onReturn();
            throw new IllegalArgumentException("test");
        }
    }

    private static class TestDestroyException extends TestDestroy {

        @Override
        public void onDestroy() {

            super.onDestroy();
            throw new IllegalArgumentException("test");
        }
    }

    private static class TestDiscardException extends TestDestroy {

        @Override
        public void onReturn() {

            super.onReturn();
            throw new IllegalArgumentException("test");
        }
    }

    private static class TestInputIterator implements InputIterator<Object> {

        @Nullable
        @Override
        public Throwable getAbortException() {

            return null;
        }

        @Override
        public boolean hasInput() {

            return false;
        }

        @Override
        public boolean isAborting() {

            return false;
        }

        @Override
        public Object nextInput() {

            return null;
        }

        @Override
        public void onAbortComplete() {

        }

        @Override
        public boolean onConsumeComplete() {

            return false;
        }

        @Override
        public void onConsumeStart() {

        }

        @Override
        public void onInvocationComplete() {

        }
    }

    private static class TestInvocationManager implements InvocationManager<Object, Object> {

        @Override
        @Nonnull
        public Invocation<Object, Object> create() {

            return new TemplateInvocation<Object, Object>() {};
        }

        @Override
        public void discard(@Nonnull final Invocation<Object, Object> invocation) {

        }

        @Override
        public void recycle(@Nonnull final Invocation<Object, Object> invocation) {

        }
    }

    private static class TestLifecycle extends TemplateInvocation<String, String> {

        private static boolean sActive;

        private static boolean sIsError;

        @Override
        public void onInput(final String input, @Nonnull final ResultChannel<String> result) {

            result.after(millis(1000)).pass(input);
        }

        @Override
        public void onAbort(@Nullable final Throwable reason) {

            if (!sActive) {

                sIsError = true;
            }
        }

        @Override
        public void onInit() {

            sActive = true;
        }


        @Override
        public void onReturn() {

            sActive = false;
        }
    }

    private static class TestOutputConsumer extends TemplateOutputConsumer<Object> {

        private boolean mIsOutput;

        private Object mOutput;

        public Object getOutput() {

            return mOutput;
        }

        public boolean isOutput() {

            return mIsOutput;
        }

        @Override
        public void onOutput(final Object o) {

            mIsOutput = true;
            mOutput = o;
        }
    }
}
