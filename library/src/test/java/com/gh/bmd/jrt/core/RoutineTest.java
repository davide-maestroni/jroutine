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

import com.gh.bmd.jrt.annotation.Alias;
import com.gh.bmd.jrt.annotation.Input;
import com.gh.bmd.jrt.annotation.Output;
import com.gh.bmd.jrt.annotation.Timeout;
import com.gh.bmd.jrt.builder.InvocationConfiguration;
import com.gh.bmd.jrt.builder.InvocationConfiguration.AgingPriority;
import com.gh.bmd.jrt.builder.InvocationConfiguration.OrderType;
import com.gh.bmd.jrt.builder.InvocationConfiguration.TimeoutActionType;
import com.gh.bmd.jrt.channel.AbortException;
import com.gh.bmd.jrt.channel.DeadlockException;
import com.gh.bmd.jrt.channel.InputChannel;
import com.gh.bmd.jrt.channel.InputDeadlockException;
import com.gh.bmd.jrt.channel.InvocationChannel;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.OutputConsumer;
import com.gh.bmd.jrt.channel.OutputDeadlockException;
import com.gh.bmd.jrt.channel.ReadDeadlockException;
import com.gh.bmd.jrt.channel.ResultChannel;
import com.gh.bmd.jrt.channel.TemplateOutputConsumer;
import com.gh.bmd.jrt.core.DefaultExecution.InputIterator;
import com.gh.bmd.jrt.core.DefaultInvocationChannel.InvocationManager;
import com.gh.bmd.jrt.core.DefaultResultChannel.AbortHandler;
import com.gh.bmd.jrt.invocation.DelegatingInvocation;
import com.gh.bmd.jrt.invocation.FilterInvocation;
import com.gh.bmd.jrt.invocation.FunctionInvocation;
import com.gh.bmd.jrt.invocation.Invocation;
import com.gh.bmd.jrt.invocation.InvocationDeadlockException;
import com.gh.bmd.jrt.invocation.InvocationException;
import com.gh.bmd.jrt.invocation.InvocationFactory;
import com.gh.bmd.jrt.invocation.InvocationInterruptedException;
import com.gh.bmd.jrt.invocation.PassingInvocation;
import com.gh.bmd.jrt.invocation.ProcedureInvocation;
import com.gh.bmd.jrt.invocation.TemplateInvocation;
import com.gh.bmd.jrt.log.Log.LogLevel;
import com.gh.bmd.jrt.log.Logger;
import com.gh.bmd.jrt.log.NullLog;
import com.gh.bmd.jrt.routine.Routine;
import com.gh.bmd.jrt.runner.Execution;
import com.gh.bmd.jrt.runner.Runner;
import com.gh.bmd.jrt.runner.Runners;
import com.gh.bmd.jrt.util.ClassToken;
import com.gh.bmd.jrt.util.TimeDuration;

import org.junit.Test;

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

import static com.gh.bmd.jrt.builder.InvocationConfiguration.builder;
import static com.gh.bmd.jrt.invocation.Invocations.factoryOf;
import static com.gh.bmd.jrt.util.TimeDuration.INFINITY;
import static com.gh.bmd.jrt.util.TimeDuration.millis;
import static com.gh.bmd.jrt.util.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Routine unit tests.
 * <p/>
 * Created by davide-maestroni on 9/9/14.
 */
public class RoutineTest {

    @Test
    public void testAbort() throws InterruptedException {

        final TimeDuration timeout = seconds(1);
        final Routine<String, String> routine =
                JRoutine.on(factoryOf(DelayedInvocation.class, millis(100))).buildRoutine();

        final InvocationChannel<String, String> inputChannel = routine.invokeAsync().pass("test1");
        final OutputChannel<String> outputChannel = inputChannel.result();

        assertThat(inputChannel.isOpen()).isFalse();
        assertThat(inputChannel.abort(new IllegalArgumentException("test1"))).isFalse();
        assertThat(inputChannel.isOpen()).isFalse();
        assertThat(outputChannel.afterMax(timeout).next()).isEqualTo("test1");

        final InvocationChannel<String, String> inputChannel1 = routine.invokeAsync().pass("test1");
        final OutputChannel<String> outputChannel1 = inputChannel1.result();

        assertThat(inputChannel1.isOpen()).isFalse();
        assertThat(inputChannel1.abort()).isFalse();
        assertThat(inputChannel1.isOpen()).isFalse();
        assertThat(outputChannel1.isOpen()).isTrue();
        assertThat(outputChannel1.afterMax(timeout).next()).isEqualTo("test1");
        assertThat(outputChannel1.isOpen()).isFalse();

        final OutputChannel<String> channel = routine.callAsync("test2");
        assertThat(channel.isOpen()).isTrue();
        assertThat(channel.abort(new IllegalArgumentException("test2"))).isTrue();
        assertThat(channel.abort()).isFalse();
        assertThat(channel.isOpen()).isTrue();

        try {

            channel.afterMax(timeout).all();

            fail();

        } catch (final AbortException ex) {

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

            channel1.afterMax(timeout).all();

            fail();

        } catch (final AbortException ex) {

            assertThat(ex.getCause()).isNull();
        }

        assertThat(channel1.checkComplete()).isTrue();
        assertThat(channel1.isOpen()).isFalse();

        try {

            JRoutine.on(new AbortInvocation())
                    .invokeAsync()
                    .after(millis(10))
                    .pass("test_abort")
                    .result()
                    .afterMax(timeout)
                    .next();

            fail();

        } catch (final AbortException ex) {

            assertThat(ex.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
            assertThat(ex.getCause().getMessage()).isEqualTo("test_abort");
        }

        try {

            JRoutine.on(new AbortInvocation2())
                    .invokeAsync()
                    .after(millis(10))
                    .pass("test_abort")
                    .result()
                    .afterMax(timeout)
                    .next();

            fail();

        } catch (final AbortException ex) {

            assertThat(ex.getCause()).isNull();
        }

        final Semaphore semaphore = new Semaphore(0);
        final AtomicBoolean isFailed = new AtomicBoolean(false);
        assertThat(JRoutine.on(new CloseInvocation(semaphore, isFailed))
                           .withInvocation()
                           .withLogLevel(LogLevel.SILENT)
                           .set()
                           .callAsync("test")
                           .afterMax(timeout)
                           .all()).isEmpty();
        semaphore.tryAcquire(1, 1, TimeUnit.SECONDS);
        assertThat(isFailed.get()).isFalse();
    }

    @Test
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

        final Routine<String, String> routine =
                JRoutine.on(factoryOf(abortInvocation, this, abortReason, semaphore))
                        .buildRoutine();

        final InvocationChannel<String, String> channel = routine.invokeAsync();
        final IllegalArgumentException exception = new IllegalArgumentException();
        channel.after(millis(100)).abort(exception);

        semaphore.tryAcquire(1, TimeUnit.SECONDS);

        assertThat(abortReason.get().getCause()).isEqualTo(exception);

        final InvocationChannel<String, String> channel1 = routine.invokeAsync();
        final IllegalAccessError exception1 = new IllegalAccessError();
        channel1.now().abort(exception1);

        semaphore.tryAcquire(1, TimeUnit.SECONDS);

        assertThat(abortReason.get().getCause()).isEqualTo(exception1);
    }

    @Test
    public void testAgingPriority() {

        final TestRunner runner = new TestRunner();
        final Routine<Object, Object> routine1 = JRoutine.on(PassingInvocation.factoryOf())
                                                         .withInvocation()
                                                         .withAsyncRunner(runner)
                                                         .withPriority(
                                                                 AgingPriority.NORMAL_PRIORITY)
                                                         .set()
                                                         .buildRoutine();
        final Routine<Object, Object> routine2 = JRoutine.on(PassingInvocation.factoryOf())
                                                         .withInvocation()
                                                         .withAsyncRunner(runner)
                                                         .withPriority(AgingPriority.HIGH_PRIORITY)
                                                         .set()
                                                         .buildRoutine();
        final OutputChannel<Object> output1 = routine1.callAsync("test1").eventuallyExit();
        final InvocationChannel<Object, Object> input2 = routine2.invokeAsync();

        for (int i = 0; i < AgingPriority.HIGH_PRIORITY - 1; i++) {

            input2.pass("test2");
            runner.run(1);
            assertThat(output1.all()).isEmpty();
        }

        final OutputChannel<Object> output2 = input2.pass("test2").result();
        runner.run(1);
        assertThat(output1.all()).containsExactly("test1");
        runner.run(Integer.MAX_VALUE);
        final List<Object> result2 = output2.all();
        assertThat(result2).hasSize(AgingPriority.HIGH_PRIORITY);
        assertThat(result2).containsOnly("test2");
    }

    @Test
    public void testBind() {

        final TestOutputConsumer consumer = new TestOutputConsumer();
        final OutputChannel<Object> channel1 = JRoutine.on(PassingInvocation.factoryOf())
                                                       .invokeAsync()
                                                       .after(seconds(1))
                                                       .pass("test1")
                                                       .result();

        channel1.passTo(consumer);
        assertThat(channel1.isBound()).isTrue();
        assertThat(consumer.isOutput()).isFalse();

        final OutputChannel<Object> channel2 =
                JRoutine.on(PassingInvocation.factoryOf()).invokeSync().pass("test2").result();

        channel2.passTo(consumer);
        assertThat(channel1.isBound()).isTrue();
        assertThat(channel2.isBound()).isTrue();
        assertThat(consumer.isOutput()).isTrue();
        assertThat(consumer.getOutput()).isEqualTo("test2");
    }

    @Test
    public void testCalls() {

        final TimeDuration timeout = seconds(1);
        final Routine<String, String> routine =
                JRoutine.on(PassingInvocation.<String>factoryOf()).buildRoutine();

        assertThat(routine.callSync().afterMax(timeout).all()).isEmpty();
        assertThat(routine.callSync(Arrays.asList("test1", "test2"))
                          .afterMax(timeout)
                          .all()).containsExactly("test1", "test2");
        assertThat(routine.callSync(routine.callSync("test1", "test2"))
                          .afterMax(timeout)
                          .all()).containsExactly("test1", "test2");
        assertThat(routine.callSync("test1").afterMax(timeout).all()).containsExactly("test1");
        assertThat(routine.callSync("test1", "test2").afterMax(timeout).all()).containsExactly(
                "test1", "test2");
        assertThat(routine.callAsync().afterMax(timeout).all()).isEmpty();
        assertThat(routine.callAsync(Arrays.asList("test1", "test2"))
                          .afterMax(timeout)
                          .all()).containsExactly("test1", "test2");
        assertThat(routine.callAsync(routine.callSync("test1", "test2"))
                          .afterMax(timeout)
                          .all()).containsExactly("test1", "test2");
        assertThat(routine.callAsync("test1").afterMax(timeout).all()).containsExactly("test1");
        assertThat(routine.callAsync("test1", "test2").afterMax(timeout).all()).containsExactly(
                "test1", "test2");
        assertThat(routine.callParallel().afterMax(timeout).all()).isEmpty();
        assertThat(routine.callParallel(Arrays.asList("test1", "test2"))
                          .afterMax(timeout)
                          .all()).containsOnly("test1", "test2");
        assertThat(routine.callParallel(routine.callSync("test1", "test2")).afterMax(timeout).all())
                .containsOnly("test1", "test2");
        assertThat(routine.callParallel("test1").afterMax(timeout).all()).containsOnly("test1");
        assertThat(routine.callParallel("test1", "test2").afterMax(timeout).all()).containsOnly(
                "test1", "test2");

        assertThat(routine.invokeSync().pass().result().all()).isEmpty();
        assertThat(routine.invokeSync()
                          .pass(Arrays.asList("test1", "test2"))
                          .result()
                          .afterMax(timeout)
                          .all()).containsExactly("test1", "test2");
        assertThat(routine.invokeSync()
                          .pass(routine.callSync("test1", "test2"))
                          .result()
                          .afterMax(timeout)
                          .all()).containsExactly("test1", "test2");
        assertThat(routine.invokeSync()
                          .pass("test1")
                          .result()
                          .afterMax(timeout)
                          .all()).containsExactly("test1");
        assertThat(routine.invokeSync()
                          .pass("test1", "test2")
                          .result()
                          .afterMax(timeout)
                          .all()).containsExactly("test1", "test2");
        assertThat(routine.invokeAsync().pass().result().afterMax(timeout).all()).isEmpty();
        assertThat(routine.invokeAsync()
                          .pass(Arrays.asList("test1", "test2"))
                          .result()
                          .afterMax(timeout)
                          .all()).containsExactly("test1", "test2");
        assertThat(routine.invokeAsync()
                          .pass(routine.callSync("test1", "test2"))
                          .result()
                          .afterMax(timeout)
                          .all()).containsExactly("test1", "test2");
        assertThat(routine.invokeAsync()
                          .pass("test1")
                          .result()
                          .afterMax(timeout)
                          .all()).containsExactly("test1");
        assertThat(routine.invokeAsync()
                          .pass("test1", "test2")
                          .result()
                          .afterMax(timeout)
                          .all()).containsExactly("test1", "test2");
        assertThat(routine.invokeParallel().pass().result().afterMax(timeout).all()).isEmpty();
        assertThat(routine.invokeParallel()
                          .pass(Arrays.asList("test1", "test2"))
                          .result()
                          .afterMax(timeout)
                          .all()).containsOnly("test1", "test2");
        assertThat(routine.invokeParallel()
                          .pass(routine.callSync("test1", "test2"))
                          .result()
                          .afterMax(timeout)
                          .all()).containsOnly("test1", "test2");
        assertThat(routine.invokeParallel()
                          .pass("test1")
                          .result()
                          .afterMax(timeout)
                          .all()).containsOnly("test1");
        assertThat(routine.invokeParallel().pass("test1", "test2").result().afterMax(timeout).all())
                .containsOnly("test1", "test2");
    }

    @Test
    public void testChainedRoutine() {

        final TimeDuration timeout = seconds(1);
        final FunctionInvocation<Integer, Integer> execSum =
                new FunctionInvocation<Integer, Integer>() {

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
                JRoutine.on(factoryOf(execSum, this)).buildRoutine();
        final Routine<Integer, Integer> squareRoutine =
                JRoutine.on(new SquareInvocation()).buildRoutine();

        assertThat(sumRoutine.callSync(squareRoutine.callSync(1, 2, 3, 4))
                             .afterMax(timeout)
                             .all()).containsExactly(30);
        assertThat(sumRoutine.callAsync(squareRoutine.callSync(1, 2, 3, 4)).afterMax(timeout).all())
                .containsExactly(30);

        assertThat(sumRoutine.callSync(squareRoutine.callAsync(1, 2, 3, 4)).afterMax(timeout).all())
                .containsExactly(30);
        assertThat(sumRoutine.callAsync(squareRoutine.callAsync(1, 2, 3, 4))
                             .afterMax(timeout)
                             .all()).containsExactly(30);

        assertThat(
                sumRoutine.callSync(squareRoutine.callParallel(1, 2, 3, 4)).afterMax(timeout).all())
                .containsExactly(30);
        assertThat(sumRoutine.callAsync(squareRoutine.callParallel(1, 2, 3, 4))
                             .afterMax(timeout)
                             .all()).containsExactly(30);
    }

    @Test
    public void testComposedRoutine() {

        final TimeDuration timeout = seconds(1);
        final FunctionInvocation<Integer, Integer> execSum =
                new FunctionInvocation<Integer, Integer>() {

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
                JRoutine.on(factoryOf(execSum, this)).buildRoutine();
        final Routine<Integer, Integer> squareRoutine =
                JRoutine.on(new SquareInvocation()).buildRoutine();

        final TemplateInvocation<Integer, Integer> invokeSquareSum =
                new TemplateInvocation<Integer, Integer>() {

                    private InvocationChannel<Integer, Integer> mChannel;

                    @Override
                    public void onAbort(final Throwable reason) {

                        mChannel.abort(reason);
                    }

                    @Override
                    public void onInitialize() {

                        mChannel = sumRoutine.invokeAsync();
                    }

                    @Override
                    public void onInput(final Integer integer,
                            @Nonnull final ResultChannel<Integer> result) {

                        squareRoutine.callAsync(integer).passTo(mChannel);
                    }

                    @Override
                    public void onResult(@Nonnull final ResultChannel<Integer> result) {

                        result.pass(mChannel.result());
                    }
                };

        final Routine<Integer, Integer> squareSumRoutine =
                JRoutine.on(factoryOf(invokeSquareSum, this, sumRoutine, squareRoutine))
                        .buildRoutine();

        assertThat(squareSumRoutine.callSync(1, 2, 3, 4).afterMax(timeout).all()).containsExactly(
                30);
        assertThat(squareSumRoutine.callAsync(1, 2, 3, 4).afterMax(timeout).all()).containsExactly(
                30);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testConfigurationErrors() {

        try {

            new DefaultRoutineBuilder<Object, Object>(
                    PassingInvocation.factoryOf()).setConfiguration(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testDeadlockOnAll() {

        final Routine<Object, Object> routine2 = JRoutine.on(new AllInvocation()).buildRoutine();

        try {

            routine2.callAsync("test").eventually().all();

            fail();

        } catch (final DeadlockException ignored) {

        }
    }

    @Test
    public void testDeadlockOnCheckComplete() {

        final Routine<Object, Object> routine1 =
                JRoutine.on(new CheckCompleteInvocation()).buildRoutine();

        try {

            routine1.callAsync("test").eventually().all();

            fail();

        } catch (final DeadlockException ignored) {

        }
    }

    @Test
    public void testDeadlockOnHasNext() {

        final Routine<Object, Object> routine3 =
                JRoutine.on(new HasNextInvocation()).buildRoutine();

        try {

            routine3.callAsync("test").eventually().all();

            fail();

        } catch (final DeadlockException ignored) {

        }
    }

    @Test
    public void testDeadlockOnNext() {

        final Routine<Object, Object> routine4 = JRoutine.on(new NextInvocation()).buildRoutine();

        try {

            routine4.callAsync("test").eventually().all();

            fail();

        } catch (final DeadlockException ignored) {

        }
    }

    @Test
    public void testDelay() {

        long startTime = System.currentTimeMillis();

        final InvocationChannel<String, String> channel =
                JRoutine.on(factoryOf(DelayedInvocation.class, millis(10))).invokeAsync();
        channel.after(100, TimeUnit.MILLISECONDS).pass("test1");
        channel.after(millis(10).nanosTime()).pass("test2");
        channel.after(millis(10).microsTime()).pass("test3", "test4");
        channel.after(millis(10)).pass((String[]) null);
        channel.now().pass((List<String>) null).pass((OutputChannel<String>) null);
        assertThat(channel.result().afterMax(3, TimeUnit.SECONDS).all()).containsOnly("test1",
                                                                                      "test2",
                                                                                      "test3",
                                                                                      "test4");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(110);

        startTime = System.currentTimeMillis();

        final InvocationChannel<String, String> channel1 =
                JRoutine.on(factoryOf(ClassToken.tokenOf(DelayedInvocation.class), millis(10)))
                        .withInvocation()
                        .withInputOrder(OrderType.PASS_ORDER)
                        .withOutputOrder(OrderType.PASS_ORDER)
                        .set()
                        .invokeAsync();
        channel1.after(100, TimeUnit.MILLISECONDS).pass("test1");
        channel1.after(millis(10).nanosTime()).pass("test2");
        channel1.after(millis(10).microsTime()).pass(Arrays.asList("test3", "test4"));
        channel1.after(millis(10)).pass((String[]) null);
        channel1.now().pass((List<String>) null).pass((OutputChannel<String>) null);
        assertThat(channel1.result().afterMax(seconds(7000)).all()).containsExactly("test1",
                                                                                    "test2",
                                                                                    "test3",
                                                                                    "test4");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(110);

        startTime = System.currentTimeMillis();

        final InvocationChannel<String, String> channel2 =
                JRoutine.on(factoryOf(DelayedListInvocation.class, millis(10), 2)).invokeAsync();
        channel2.after(100, TimeUnit.MILLISECONDS).pass("test1");
        channel2.after(millis(10).nanosTime()).pass("test2");
        channel2.after(millis(10).microsTime()).pass("test3", "test4");
        channel2.after(millis(10)).pass((String[]) null);
        channel2.now().pass((List<String>) null).pass((OutputChannel<String>) null);
        assertThat(channel2.result().afterMax(3, TimeUnit.SECONDS).all()).containsOnly("test1",
                                                                                       "test2",
                                                                                       "test3",
                                                                                       "test4");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(110);

        startTime = System.currentTimeMillis();

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.PASS_ORDER)
                                                               .withOutputOrder(
                                                                       OrderType.PASS_ORDER)
                                                               .set();
        final InvocationChannel<String, String> channel3 =
                JRoutine.on(factoryOf(DelayedListInvocation.class, millis(10), 2))
                        .withInvocation()
                        .with(configuration)
                        .set()
                        .invokeAsync();
        channel3.after(100, TimeUnit.MILLISECONDS).pass("test1");
        channel3.after(millis(10).nanosTime()).pass("test2");
        channel3.after(millis(10).microsTime()).pass("test3", "test4");
        channel3.after(millis(10)).pass((String[]) null);
        channel3.now().pass((List<String>) null).pass((OutputChannel<String>) null);
        assertThat(channel3.result().afterMax(3, TimeUnit.SECONDS).all()).containsExactly("test1",
                                                                                          "test2",
                                                                                          "test3",
                                                                                          "test4");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(110);

        startTime = System.currentTimeMillis();

        final InvocationChannel<String, String> channel4 =
                JRoutine.on(factoryOf(DelayedListInvocation.class, TimeDuration.ZERO, 2))
                        .invokeAsync();
        channel4.after(100, TimeUnit.MILLISECONDS).pass("test1");
        channel4.after(millis(10).nanosTime()).pass("test2");
        channel4.after(millis(10).microsTime()).pass("test3", "test4");
        channel4.after(millis(10)).pass((String[]) null);
        channel4.now().pass((List<String>) null).pass((OutputChannel<String>) null);
        assertThat(channel4.result().afterMax(3, TimeUnit.SECONDS).all()).containsOnly("test1",
                                                                                       "test2",
                                                                                       "test3",
                                                                                       "test4");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(100);

        startTime = System.currentTimeMillis();

        final InvocationChannel<String, String> channel5 =
                JRoutine.on(factoryOf(DelayedListInvocation.class, TimeDuration.ZERO, 2))
                        .withInvocation()
                        .with(configuration)
                        .set()
                        .invokeAsync();
        channel5.after(100, TimeUnit.MILLISECONDS).pass("test1");
        channel5.after(millis(10).nanosTime()).pass("test2");
        channel5.after(millis(10).microsTime()).pass("test3", "test4");
        channel5.after(millis(10)).pass((String[]) null);
        channel5.now().pass((List<String>) null).pass((OutputChannel<String>) null);
        assertThat(channel5.result().afterMax(3, TimeUnit.SECONDS).all()).containsExactly("test1",
                                                                                          "test2",
                                                                                          "test3",
                                                                                          "test4");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(100);

        startTime = System.currentTimeMillis();

        final InvocationChannel<String, String> channel6 =
                JRoutine.on(factoryOf(DelayedChannelInvocation.class, millis(10))).invokeAsync();
        channel6.after(100, TimeUnit.MILLISECONDS).pass("test1");
        channel6.after(millis(10).nanosTime()).pass("test2");
        channel6.after(millis(10).microsTime()).pass("test3", "test4");
        channel6.after(millis(10)).pass((String[]) null);
        channel6.now().pass((List<String>) null).pass((OutputChannel<String>) null);
        assertThat(channel6.result().afterMax(3, TimeUnit.SECONDS).all()).containsOnly("test1",
                                                                                       "test2",
                                                                                       "test3",
                                                                                       "test4");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(110);

        startTime = System.currentTimeMillis();

        final InvocationChannel<String, String> channel7 =
                JRoutine.on(factoryOf(DelayedChannelInvocation.class, millis(10)))
                        .withInvocation()
                        .with(configuration)
                        .set()
                        .invokeAsync();
        channel7.after(100, TimeUnit.MILLISECONDS).pass("test1");
        channel7.after(millis(10).nanosTime()).pass("test2");
        channel7.after(millis(10).microsTime()).pass("test3", "test4");
        channel7.after(millis(10)).pass((String[]) null);
        channel7.now().pass((List<String>) null).pass((OutputChannel<String>) null);
        assertThat(channel7.result().afterMax(3, TimeUnit.SECONDS).all()).containsExactly("test1",
                                                                                          "test2",
                                                                                          "test3",
                                                                                          "test4");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(110);

        startTime = System.currentTimeMillis();

        final InvocationChannel<String, String> channel8 =
                JRoutine.on(factoryOf(DelayedChannelInvocation.class, TimeDuration.ZERO))
                        .invokeAsync();
        channel8.after(100, TimeUnit.MILLISECONDS).pass("test1");
        channel8.after(millis(10).nanosTime()).pass("test2");
        channel8.after(millis(10).microsTime()).pass("test3", "test4");
        channel8.after(millis(10)).pass((String[]) null);
        channel8.now().pass((List<String>) null).pass((OutputChannel<String>) null);
        assertThat(channel8.result().afterMax(3, TimeUnit.SECONDS).all()).containsOnly("test1",
                                                                                       "test2",
                                                                                       "test3",
                                                                                       "test4");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(100);

        startTime = System.currentTimeMillis();

        final InvocationChannel<String, String> channel9 =
                JRoutine.on(factoryOf(DelayedChannelInvocation.class, TimeDuration.ZERO))
                        .withInvocation()
                        .with(configuration)
                        .set()
                        .invokeAsync();
        channel9.after(100, TimeUnit.MILLISECONDS).pass("test1");
        channel9.after(millis(10).nanosTime()).pass("test2");
        channel9.after(millis(10).microsTime()).pass("test3", "test4");
        channel9.after(millis(10)).pass((String[]) null);
        channel9.now().pass((List<String>) null).pass((OutputChannel<String>) null);
        assertThat(channel9.result().afterMax(3, TimeUnit.SECONDS).all()).containsExactly("test1",
                                                                                          "test2",
                                                                                          "test3",
                                                                                          "test4");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(100);
    }

    @Test
    public void testDelayedAbort() throws InterruptedException {

        final TimeDuration timeout = seconds(1);
        final Routine<String, String> passingRoutine =
                JRoutine.on(PassingInvocation.<String>factoryOf()).buildRoutine();

        final InvocationChannel<String, String> channel1 = passingRoutine.invokeAsync();
        channel1.after(seconds(2)).abort();
        assertThat(channel1.now().pass("test").result().afterMax(timeout).next()).isEqualTo("test");

        final InvocationChannel<String, String> channel2 = passingRoutine.invokeAsync();
        channel2.after(millis(100)).abort();

        try {

            channel2.after(millis(200)).pass("test").result().afterMax(timeout).next();

            fail();

        } catch (final AbortException ignored) {

        }

        final Routine<String, String> abortRoutine =
                JRoutine.on(factoryOf(DelayedAbortInvocation.class, millis(200))).buildRoutine();

        assertThat(abortRoutine.callAsync("test").afterMax(timeout).next()).isEqualTo("test");

        try {

            final OutputChannel<String> channel = abortRoutine.callAsync("test");

            millis(500).sleepAtLeast();

            channel.afterMax(timeout).next();

            fail();

        } catch (final AbortException ignored) {

        }
    }

    @Test
    public void testDelayedBind() {

        final TimeDuration timeout = seconds(1);
        final Routine<Object, Object> routine1 =
                JRoutine.on(PassingInvocation.factoryOf()).buildRoutine();
        final Routine<Object, Object> routine2 =
                JRoutine.on(PassingInvocation.factoryOf()).buildRoutine();

        final long startTime = System.currentTimeMillis();

        assertThat(routine1.invokeAsync()
                           .after(millis(500))
                           .pass(routine2.callAsync("test"))
                           .result()
                           .afterMax(timeout)
                           .next()).isEqualTo("test");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(500);
    }

    @Test
    public void testDelegation() {

        final TimeDuration timeout = seconds(1);
        final Routine<Object, Object> routine1 =
                JRoutine.on(PassingInvocation.factoryOf()).buildRoutine();
        final Routine<Object, Object> routine2 =
                JRoutine.on(DelegatingInvocation.factoryWith(routine1)).buildRoutine();

        assertThat(routine2.callAsync("test1").afterMax(timeout).all()).containsExactly("test1");

        final InvocationChannel<Object, Object> channel =
                routine2.invokeAsync().after(timeout).pass("test2");
        channel.now().abort(new IllegalArgumentException());

        try {

            channel.result().next();

        } catch (final AbortException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
        }

        final Routine<String, String> routine3 =
                JRoutine.on(factoryOf(TestDestroy.class)).buildRoutine();
        final Routine<String, String> routine4 =
                JRoutine.on(DelegatingInvocation.factoryWith(routine3)).buildRoutine();

        assertThat(routine4.callAsync("test4").afterMax(timeout).all()).containsExactly("test4");
        routine4.purge();
        assertThat(TestDestroy.getInstanceCount()).isZero();
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testDelegationError() {

        try {

            DelegatingInvocation.factoryWith(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testDestroyAsync() {

        final TimeDuration timeout = seconds(1);
        final Routine<String, String> routine1 = JRoutine.on(factoryOf(TestDestroy.class))
                                                         .withInvocation()
                                                         .withCoreInvocations(0)
                                                         .set()
                                                         .buildRoutine();
        assertThat(routine1.callSync("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(
                routine1.callAsync("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(routine1.callParallel("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .all()).containsOnly("1", "2", "3", "4", "5");
        assertThat(TestDestroy.getInstanceCount()).isZero();

        final Routine<String, String> routine2 = JRoutine.on(factoryOf(TestDiscardException.class))
                                                         .withInvocation()
                                                         .withCoreInvocations(0)
                                                         .set()
                                                         .buildRoutine();
        assertThat(routine2.callSync("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(
                routine2.callAsync("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(routine2.callParallel("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .all()).containsOnly("1", "2", "3", "4", "5");
        assertThat(TestDestroy.getInstanceCount()).isZero();

        final Routine<String, String> routine3 =
                JRoutine.on(factoryOf(TestDestroyDiscard.class)).buildRoutine();
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
                JRoutine.on(factoryOf(TestDestroyDiscardException.class)).buildRoutine();
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
                JRoutine.on(factoryOf(TestDestroy.class)).buildRoutine();
        assertThat(routine5.callSync("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(
                routine5.callAsync("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(routine5.callParallel("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .all()).containsOnly("1", "2", "3", "4", "5");
        routine5.purge();
        assertThat(TestDestroy.getInstanceCount()).isZero();

        final Routine<String, String> routine6 =
                JRoutine.on(factoryOf(TestDestroyException.class)).buildRoutine();

        assertThat(routine6.callSync("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(
                routine6.callAsync("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(routine6.callParallel("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .all()).containsOnly("1", "2", "3", "4", "5");
        routine6.purge();
        assertThat(TestDestroy.getInstanceCount()).isZero();
    }

    @Test
    public void testDestroySync() {

        final TimeDuration timeout = seconds(1);
        final Routine<String, String> routine1 = JRoutine.on(factoryOf(TestDestroy.class))
                                                         .withInvocation()
                                                         .withCoreInvocations(0)
                                                         .set()
                                                         .buildRoutine();
        assertThat(
                routine1.callAsync("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(routine1.callParallel("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .all()).containsOnly("1", "2", "3", "4", "5");
        assertThat(routine1.callSync("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(TestDestroy.getInstanceCount()).isZero();

        final Routine<String, String> routine2 = JRoutine.on(factoryOf(TestDiscardException.class))
                                                         .withInvocation()
                                                         .withCoreInvocations(0)
                                                         .set()
                                                         .buildRoutine();
        assertThat(
                routine2.callAsync("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(routine2.callParallel("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .all()).containsOnly("1", "2", "3", "4", "5");
        assertThat(routine2.callSync("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(TestDestroy.getInstanceCount()).isZero();

        final Routine<String, String> routine3 =
                JRoutine.on(factoryOf(TestDestroyDiscard.class)).buildRoutine();
        assertThat(routine3.callParallel("1", "2", "3", "4", "5").afterMax(timeout).checkComplete())
                .isTrue();
        assertThat(routine3.callAsync("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .checkComplete()).isTrue();
        assertThat(routine3.callSync("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .checkComplete()).isTrue();
        assertThat(TestDestroy.getInstanceCount()).isZero();

        final Routine<String, String> routine4 =
                JRoutine.on(factoryOf(TestDestroyDiscardException.class)).buildRoutine();
        assertThat(routine4.callParallel("1", "2", "3", "4", "5").afterMax(timeout).checkComplete())
                .isTrue();
        assertThat(routine4.callAsync("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .checkComplete()).isTrue();
        assertThat(routine4.callSync("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .checkComplete()).isTrue();
        assertThat(TestDestroy.getInstanceCount()).isZero();

        final Routine<String, String> routine5 =
                JRoutine.on(factoryOf(TestDestroy.class)).buildRoutine();
        assertThat(
                routine5.callAsync("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(routine5.callParallel("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .all()).containsOnly("1", "2", "3", "4", "5");
        assertThat(routine5.callSync("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        routine5.purge();
        assertThat(TestDestroy.getInstanceCount()).isZero();

        final Routine<String, String> routine6 =
                JRoutine.on(factoryOf(TestDestroyException.class)).buildRoutine();

        assertThat(
                routine6.callAsync("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(routine6.callParallel("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .all()).containsOnly("1", "2", "3", "4", "5");
        assertThat(routine6.callSync("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        routine6.purge();
        assertThat(TestDestroy.getInstanceCount()).isZero();
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testError() {

        try {

            JRoutine.on(factoryOf(ConstructorException.class))
                    .withInvocation()
                    .withLogLevel(LogLevel.SILENT)
                    .set()
                    .callSync()
                    .all();

            fail();

        } catch (final InvocationException ignored) {

        }

        try {

            new AbstractRoutine<Object, Object>(null) {

                @Nonnull
                @Override
                protected Invocation<Object, Object> newInvocation(
                        @Nonnull final InvocationType type) {

                    return new ConstructorException();
                }
            };

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultRoutine<Object, Object>(null, new InvocationFactory<Object, Object>() {

                @Nonnull
                public Invocation<Object, Object> newInvocation() {

                    return null;
                }
            });

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultRoutine<Object, Object>(InvocationConfiguration.DEFAULT_CONFIGURATION, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        final Logger logger = Logger.newLogger(null, null, this);

        try {

            final DefaultResultChannel<Object> channel =
                    new DefaultResultChannel<Object>(InvocationConfiguration.DEFAULT_CONFIGURATION,
                                                     new TestAbortHandler(),
                                                     Runners.sequentialRunner(), logger);

            new DefaultExecution<Object, Object>(null, new TestInputIterator(), channel, logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final DefaultResultChannel<Object> channel =
                    new DefaultResultChannel<Object>(InvocationConfiguration.DEFAULT_CONFIGURATION,
                                                     new TestAbortHandler(),
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

            final DefaultResultChannel<Object> channel =
                    new DefaultResultChannel<Object>(InvocationConfiguration.DEFAULT_CONFIGURATION,
                                                     new TestAbortHandler(),
                                                     Runners.sequentialRunner(), logger);

            new DefaultExecution<Object, Object>(new TestInvocationManager(),
                                                 new TestInputIterator(), channel, null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
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

    @Test
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

    @Test
    public void testErrorOnInit() {

        final TemplateInvocation<String, String> exceptionOnInit =
                new TemplateInvocation<String, String>() {

                    @Override
                    public void onInitialize() {

                        throw new NullPointerException("test1");
                    }
                };

        final Routine<String, String> exceptionRoutine =
                JRoutine.on(factoryOf(exceptionOnInit, this)).buildRoutine();

        testException(exceptionRoutine, "test", "test1");

        final Routine<String, String> passingRoutine =
                JRoutine.on(PassingInvocation.<String>factoryOf()).buildRoutine();

        testChained(passingRoutine, exceptionRoutine, "test", "test1");
        testChained(exceptionRoutine, passingRoutine, "test", "test1");
    }

    @Test
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
                JRoutine.on(factoryOf(exceptionOnInput, this)).buildRoutine();

        testException(exceptionRoutine, "test2", "test2");

        final Routine<String, String> passingRoutine =
                JRoutine.on(PassingInvocation.<String>factoryOf()).buildRoutine();

        testChained(passingRoutine, exceptionRoutine, "test2", "test2");
        testChained(exceptionRoutine, passingRoutine, "test2", "test2");
    }

    @Test
    public void testErrorOnResult() {

        final TemplateInvocation<String, String> exceptionOnResult =
                new TemplateInvocation<String, String>() {

                    @Override
                    public void onResult(@Nonnull final ResultChannel<String> result) {

                        throw new NullPointerException("test3");
                    }
                };

        final Routine<String, String> exceptionRoutine =
                JRoutine.on(factoryOf(exceptionOnResult, this)).buildRoutine();

        testException(exceptionRoutine, "test", "test3");

        final Routine<String, String> passingRoutine =
                JRoutine.on(PassingInvocation.<String>factoryOf()).buildRoutine();

        testChained(passingRoutine, exceptionRoutine, "test", "test3");
        testChained(exceptionRoutine, passingRoutine, "test", "test3");
    }

    @Test
    public void testFactoryError() {

        try {

            final FilterInvocation<Object, Object> anonymousFactory =
                    new FilterInvocation<Object, Object>() {

                        public void onInput(final Object o,
                                @Nonnull final ResultChannel<Object> result) {

                        }
                    };

            JRoutine.on(anonymousFactory);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testInitInvocationException() {

        final ExceptionRoutine routine =
                new ExceptionRoutine(InvocationConfiguration.DEFAULT_CONFIGURATION);

        try {

            routine.callAsync().afterMax(seconds(1)).all();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    public void testInitInvocationNull() {

        final NullRoutine routine = new NullRoutine(InvocationConfiguration.DEFAULT_CONFIGURATION);

        try {

            routine.callAsync().afterMax(seconds(1)).all();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(NullPointerException.class);
        }
    }

    @Test
    public void testInputDeadlock() {

        try {

            JRoutine.on(PassingInvocation.<String>factoryOf())
                    .withInvocation()
                    .withInputMaxSize(1)
                    .withInputTimeout(TimeDuration.ZERO)
                    .set()
                    .callAsync("test1", "test2")
                    .all();

            fail();

        } catch (final DeadlockException ignored) {

        }
    }

    @Test
    public void testInputTimeout() {

        try {

            JRoutine.on(new SlowFilterInvocation<String>(millis(100)))
                    .withInvocation()
                    .withInputMaxSize(1)
                    .withInputTimeout(TimeDuration.ZERO)
                    .set()
                    .invokeAsync()
                    .pass("test1")
                    .pass("test2")
                    .result()
                    .all();

            fail();

        } catch (final InputDeadlockException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testInvocationChannelError() {

        final Logger logger = Logger.newLogger(new NullLog(), LogLevel.DEBUG, this);

        try {

            new DefaultInvocationChannel<Object, Object>(
                    InvocationConfiguration.DEFAULT_CONFIGURATION, null, Runners.sharedRunner(),
                    logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultInvocationChannel<Object, Object>(
                    InvocationConfiguration.DEFAULT_CONFIGURATION, new TestInvocationManager(),
                    null, logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultInvocationChannel<Object, Object>(
                    InvocationConfiguration.DEFAULT_CONFIGURATION, new TestInvocationManager(),
                    Runners.sharedRunner(), null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultInvocationChannel<Object, Object>(null, new TestInvocationManager(),
                                                         Runners.sharedRunner(), logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final DefaultInvocationChannel<Object, Object> channel =
                    new DefaultInvocationChannel<Object, Object>(
                            InvocationConfiguration.DEFAULT_CONFIGURATION,
                            new TestInvocationManager(), Runners.sharedRunner(), logger);

            channel.result();
            channel.pass("test");

            fail();

        } catch (final IllegalStateException ignored) {

        }

        try {

            final DefaultInvocationChannel<Object, Object> channel =
                    new DefaultInvocationChannel<Object, Object>(
                            InvocationConfiguration.DEFAULT_CONFIGURATION,
                            new TestInvocationManager(), Runners.sharedRunner(), logger);

            channel.after(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final DefaultInvocationChannel<Object, Object> channel =
                    new DefaultInvocationChannel<Object, Object>(
                            InvocationConfiguration.DEFAULT_CONFIGURATION,
                            new TestInvocationManager(), Runners.sharedRunner(), logger);

            channel.after(1, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final DefaultInvocationChannel<Object, Object> channel =
                    new DefaultInvocationChannel<Object, Object>(
                            InvocationConfiguration.DEFAULT_CONFIGURATION,
                            new TestInvocationManager(), Runners.sharedRunner(), logger);

            channel.after(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testInvocationLifecycle() throws InterruptedException {

        final OutputChannel<String> outputChannel =
                JRoutine.on(factoryOf(TestLifecycle.class)).callAsync("test");

        Thread.sleep(500);

        outputChannel.abort();
        outputChannel.afterMax(INFINITY).checkComplete();
        assertThat(TestLifecycle.sIsError).isFalse();
    }

    @Test
    public void testInvocationNotAvailable() throws InterruptedException {

        final Routine<Void, Void> routine = JRoutine.on(new SleepInvocation())
                                                    .withInvocation()
                                                    .withMaxInvocations(1)
                                                    .set()
                                                    .buildRoutine();

        routine.callAsync();
        millis(100).sleepAtLeast();

        try {

            routine.callAsync().eventually().next();

            fail();

        } catch (final InvocationDeadlockException ignored) {

        }
    }

    @Test
    public void testMethod() throws NoSuchMethodException {

        final TimeDuration timeout = seconds(1);
        final TestClass test = new TestClass();
        assertThat(JRoutine.on(test)
                           .method(TestClass.class.getMethod("getOne"))
                           .callSync()
                           .afterMax(timeout)
                           .all()).containsExactly(1);
        assertThat(JRoutine.on(test)
                           .method("getOne")
                           .callSync()
                           .afterMax(timeout)
                           .all()).containsExactly(1);
        assertThat(JRoutine.on(test)
                           .aliasMethod(TestClass.GET)
                           .callSync()
                           .afterMax(timeout)
                           .all()).containsExactly(1);
        assertThat(JRoutine.on(TestClass.class)
                           .aliasMethod(TestClass.GET)
                           .callSync(3)
                           .afterMax(timeout)
                           .all()).containsExactly(3);
        assertThat(JRoutine.on(TestClass.class)
                           .aliasMethod("get")
                           .callAsync(-3)
                           .afterMax(timeout)
                           .all()).containsExactly(-3);
        assertThat(JRoutine.on(TestClass.class)
                           .method("get", int.class)
                           .callParallel(17)
                           .afterMax(timeout)
                           .all()).containsExactly(17);

        assertThat(JRoutine.on(test).buildProxy(TestInterface.class).getInt(2)).isEqualTo(2);

        try {

            JRoutine.on(TestClass.class).aliasMethod("get").callAsync().afterMax(timeout).all();

            fail();

        } catch (final InvocationException ignored) {

        }

        try {

            JRoutine.on(TestClass.class).aliasMethod("take");

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        assertThat(JRoutine.on(test)
                           .withInvocation()
                           .withReadTimeout(timeout)
                           .set()
                           .buildProxy(TestInterfaceAsync.class)
                           .take(77)).isEqualTo(77);
        assertThat(JRoutine.on(test)
                           .buildProxy(TestInterfaceAsync.class)
                           .getOne()
                           .afterMax(timeout)
                           .next()).isEqualTo(1);

        final TestInterfaceAsync testInterfaceAsync = JRoutine.on(test)
                                                              .withInvocation()
                                                              .withReadTimeout(1, TimeUnit.SECONDS)
                                                              .set()
                                                              .buildProxy(TestInterfaceAsync.class);
        assertThat(testInterfaceAsync.getInt(testInterfaceAsync.getOne())).isEqualTo(1);
    }

    @Test
    public void testOutputDeadlock() {

        final Routine<String, String> routine =
                JRoutine.on(factoryOf(new FunctionInvocation<String, String>() {

                    @Override
                    public void onCall(@Nonnull final List<? extends String> strings,
                            @Nonnull final ResultChannel<String> result) {

                        result.pass(strings);
                    }
                }, this))
                        .withInvocation()
                        .withOutputMaxSize(1)
                        .withOutputTimeout(TimeDuration.ZERO)
                        .set()
                        .buildRoutine();

        try {

            routine.callAsync("test1", "test2").eventually().all();

            fail();

        } catch (final DeadlockException ignored) {

        }
    }

    @Test
    public void testOutputTimeout() {

        final Routine<String, String> routine =
                JRoutine.on(new SlowFilterInvocation<String>(millis(100)))
                        .withInvocation()
                        .withOutputMaxSize(1)
                        .withOutputTimeout(TimeDuration.ZERO)
                        .set()
                        .buildRoutine();

        try {

            final OutputChannel<String> outputChannel =
                    routine.invokeAsync().pass("test1").pass("test2").result().eventually();
            outputChannel.checkComplete();
            outputChannel.all();

            fail();

        } catch (final OutputDeadlockException ignored) {

        }
    }

    @Test
    public void testPartialOut() {

        final TemplateInvocation<String, String> invocation =
                new TemplateInvocation<String, String>() {

                    @Override
                    public void onInput(final String s,
                            @Nonnull final ResultChannel<String> result) {

                        result.now().pass(s).after(seconds(2)).abort();
                    }
                };

        assertThat(JRoutine.on(factoryOf(invocation, this))
                           .callAsync("test")
                           .afterMax(millis(500))
                           .all()).containsExactly("test");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testResultChannelError() {

        final Logger logger = Logger.newLogger(new NullLog(), LogLevel.DEBUG, this);

        try {

            new DefaultResultChannel<Object>(InvocationConfiguration.DEFAULT_CONFIGURATION, null,
                                             Runners.sharedRunner(), logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultResultChannel<Object>(InvocationConfiguration.DEFAULT_CONFIGURATION,
                                             new TestAbortHandler(), null, logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultResultChannel<Object>(InvocationConfiguration.DEFAULT_CONFIGURATION,
                                             new TestAbortHandler(), Runners.sharedRunner(), null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultResultChannel<Object>(InvocationConfiguration.DEFAULT_CONFIGURATION,
                                             new TestAbortHandler(), Runners.sharedRunner(), logger)
                    .after(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultResultChannel<Object>(InvocationConfiguration.DEFAULT_CONFIGURATION,
                                             new TestAbortHandler(), Runners.sharedRunner(), logger)
                    .after(0, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final DefaultResultChannel<Object> channel =
                    new DefaultResultChannel<Object>(InvocationConfiguration.DEFAULT_CONFIGURATION,
                                                     new TestAbortHandler(), Runners.sharedRunner(),
                                                     logger);

            channel.after(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        final OutputChannel<String> channel =
                JRoutine.on(factoryOf(DelayedInvocation.class, TimeDuration.ZERO))
                        .withInvocation()
                        .withLogLevel(LogLevel.SILENT)
                        .set()
                        .callSync();

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

            channel.passTo((InputChannel<String>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            channel.passTo((OutputConsumer<String>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            channel.allInto(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        final TemplateOutputConsumer<String> consumer = new TemplateOutputConsumer<String>() {};

        try {

            channel.passTo(consumer).passTo(consumer);

            fail();

        } catch (final IllegalStateException ignored) {

        }

        try {

            channel.iterator();

            fail();

        } catch (final IllegalStateException ignored) {

        }

        final Routine<String, String> routine1 =
                JRoutine.on(factoryOf(DelayedInvocation.class, millis(100)))
                        .withInvocation()
                        .withLogLevel(LogLevel.SILENT)
                        .set()
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

    @Test
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
                JRoutine.on(factoryOf(execSquare, this)).buildRoutine();

        assertThat(squareRoutine.callSync(1, 2, 3, 4).afterMax(timeout).all()).containsExactly(1, 4,
                                                                                               9,
                                                                                               16);
        assertThat(squareRoutine.callAsync(1, 2, 3, 4).afterMax(timeout).all()).containsExactly(1,
                                                                                                4,
                                                                                                9,
                                                                                                16);
        assertThat(squareRoutine.callParallel(1, 2, 3, 4).afterMax(timeout).all()).containsOnly(1,
                                                                                                4,
                                                                                                9,
                                                                                                16);
    }

    @Test
    public void testRoutineBuilder() {

        assertThat(JRoutine.on(factoryOf(new ClassToken<PassingInvocation<String>>() {}))
                           .withInvocation()
                           .withSyncRunner(Runners.sequentialRunner())
                           .withAsyncRunner(Runners.poolRunner())
                           .withCoreInvocations(0)
                           .withMaxInvocations(1)
                           .withAvailInstanceTimeout(1, TimeUnit.SECONDS)
                           .withInputMaxSize(2)
                           .withInputTimeout(1, TimeUnit.SECONDS)
                           .withOutputMaxSize(2)
                           .withOutputTimeout(1, TimeUnit.SECONDS)
                           .withOutputOrder(OrderType.PASS_ORDER)
                           .set()
                           .callSync("test1", "test2")
                           .all()).containsExactly("test1", "test2");

        assertThat(JRoutine.on(factoryOf(new ClassToken<PassingInvocation<String>>() {}))
                           .withInvocation()
                           .withSyncRunner(Runners.queuedRunner())
                           .withAsyncRunner(Runners.poolRunner())
                           .withCoreInvocations(0)
                           .withMaxInvocations(1)
                           .withAvailInstanceTimeout(TimeDuration.ZERO)
                           .withInputMaxSize(2)
                           .withInputTimeout(TimeDuration.ZERO)
                           .withOutputMaxSize(2)
                           .withOutputTimeout(TimeDuration.ZERO)
                           .withOutputOrder(OrderType.PASS_ORDER)
                           .set()
                           .callSync("test1", "test2")
                           .all()).containsExactly("test1", "test2");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testRoutineBuilderError() {

        try {

            new DefaultRoutineBuilder<String, String>(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testRoutineFunction() {

        final TimeDuration timeout = seconds(1);
        final FunctionInvocation<Integer, Integer> execSum =
                new FunctionInvocation<Integer, Integer>() {

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
                JRoutine.on(factoryOf(execSum, this)).buildRoutine();

        assertThat(sumRoutine.callSync(1, 2, 3, 4).afterMax(timeout).all()).containsExactly(10);
        assertThat(sumRoutine.callAsync(1, 2, 3, 4).afterMax(timeout).all()).containsExactly(10);
    }

    @Test
    public void testTimeoutActions() {

        final Routine<String, String> routine1 =
                JRoutine.on(factoryOf(DelayedInvocation.class, seconds(1)))
                        .withInvocation()
                        .withReadTimeoutAction(TimeoutActionType.ABORT)
                        .set()
                        .buildRoutine();

        try {

            routine1.callAsync("test1").next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            routine1.callAsync("test1").all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            final ArrayList<String> results = new ArrayList<String>();
            routine1.callAsync("test1").allInto(results);

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
                JRoutine.on(factoryOf(DelayedInvocation.class, seconds(1)))
                        .withInvocation()
                        .withReadTimeoutAction(TimeoutActionType.ABORT)
                        .withReadTimeout(millis(10))
                        .set()
                        .buildRoutine();

        try {

            routine2.callAsync("test1").next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            routine2.callAsync("test1").all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            final ArrayList<String> results = new ArrayList<String>();
            routine2.callAsync("test1").allInto(results);

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
                JRoutine.on(factoryOf(DelayedInvocation.class, seconds(1)))
                        .withInvocation()
                        .withReadTimeoutAction(TimeoutActionType.DEADLOCK)
                        .set()
                        .buildRoutine();
        final OutputChannel<String> channel3 = routine3.callAsync("test1");

        try {

            channel3.next();

            fail();

        } catch (final ReadDeadlockException ignored) {

        }

        try {

            channel3.all();

            fail();

        } catch (final ReadDeadlockException ignored) {

        }

        try {

            final ArrayList<String> results = new ArrayList<String>();
            channel3.allInto(results);

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

            channel3.next();

            fail();

        } catch (final ReadDeadlockException ignored) {

        }

        try {

            channel3.all();

            fail();

        } catch (final ReadDeadlockException ignored) {

        }

        try {

            final ArrayList<String> results = new ArrayList<String>();
            channel3.allInto(results);

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
                JRoutine.on(factoryOf(DelayedInvocation.class, seconds(1)))
                        .withInvocation()
                        .withReadTimeoutAction(TimeoutActionType.EXIT)
                        .set()
                        .buildRoutine();
        final OutputChannel<String> channel4 = routine4.callAsync("test1");

        try {

            channel4.next();

            fail();

        } catch (final NoSuchElementException ignored) {

        }

        assertThat(channel4.all()).isEmpty();

        final ArrayList<String> results = new ArrayList<String>();
        channel4.allInto(results);
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

            channel4.next();

            fail();

        } catch (final NoSuchElementException ignored) {

        }

        assertThat(channel4.all()).isEmpty();

        results.clear();
        channel4.allInto(results);
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

            before.callSync(after.callSync(input)).afterMax(timeout).all();

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

            before.callAsync(after.callSync(input)).afterMax(timeout).all();

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

            before.callParallel(after.callSync(input)).afterMax(timeout).all();

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

            before.invokeSync().pass(after.callSync(input)).result().afterMax(timeout).all();

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

            before.invokeAsync().pass(after.callSync(input)).result().afterMax(timeout).all();

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

            before.invokeParallel().pass(after.callSync(input)).result().afterMax(timeout).all();

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

            before.callSync(after.callAsync(input)).afterMax(timeout).all();

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

            before.callAsync(after.callAsync(input)).afterMax(timeout).all();

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

            before.invokeSync().pass(after.callAsync(input)).result().afterMax(timeout).all();

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

            before.invokeAsync().pass(after.callAsync(input)).result().afterMax(timeout).all();

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

            before.callSync(after.callParallel(input)).afterMax(timeout).all();

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

            before.callAsync(after.callParallel(input)).afterMax(timeout).all();

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

            before.invokeSync().pass(after.callParallel(input)).result().afterMax(timeout).all();

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

            before.invokeAsync().pass(after.callParallel(input)).result().afterMax(timeout).all();

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
                JRoutine.on(factoryOf(DelayedInvocation.class, TimeDuration.ZERO)).buildRoutine();

        assertThat(routine.callSync(input)
                          .passTo(consumer)
                          .afterMax(timeout)
                          .checkComplete()).isTrue();
        assertThat(routine.callAsync(input)
                          .passTo(consumer)
                          .afterMax(timeout)
                          .checkComplete()).isTrue();
        assertThat(routine.callParallel(input)
                          .passTo(consumer)
                          .afterMax(timeout)
                          .checkComplete()).isTrue();
        assertThat(routine.invokeSync()
                          .pass(input)
                          .result()
                          .passTo(consumer)
                          .afterMax(timeout)
                          .checkComplete()).isTrue();
        assertThat(routine.invokeAsync()
                          .pass(input)
                          .result()
                          .passTo(consumer)
                          .afterMax(timeout)
                          .checkComplete()).isTrue();
        assertThat(routine.invokeParallel()
                          .pass(input)
                          .result()
                          .passTo(consumer)
                          .afterMax(timeout)
                          .checkComplete()).isTrue();
    }

    private void testException(final Routine<String, String> routine, final String input,
            final String expected) {

        final TimeDuration timeout = seconds(1);

        try {

            routine.callSync(input).afterMax(timeout).all();

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

            routine.callAsync(input).afterMax(timeout).all();

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

            routine.callParallel(input).afterMax(timeout).all();

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

            routine.invokeSync().pass(input).result().afterMax(timeout).all();

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

            routine.invokeAsync().pass(input).result().afterMax(timeout).all();

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

            routine.invokeParallel().pass(input).result().afterMax(timeout).all();

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
        int getInt(int i);
    }

    private interface TestInterfaceAsync {

        int getInt(@Input(int.class) OutputChannel<Integer> i);

        @Output
        OutputChannel<Integer> getOne();

        @Alias(value = "getInt")
        int take(int i);
    }

    private static class AbortInvocation extends FilterInvocation<String, String> {

        public void onInput(final String s, @Nonnull final ResultChannel<String> result) {

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
    }

    private static class AbortInvocation2 extends FilterInvocation<String, String> {

        public void onInput(final String s, @Nonnull final ResultChannel<String> result) {

            assertThat(result.abort()).isTrue();
            assertThat(result.abort(new IllegalArgumentException(s))).isFalse();
        }
    }

    private static class AllInvocation extends FilterInvocation<Object, Object> {

        public void onInput(final Object o, @Nonnull final ResultChannel<Object> result) {

            JRoutine.on(factoryOf(DelayedInvocation.class, millis(100)))
                    .callAsync("test")
                    .eventually()
                    .all();
        }
    }

    private static class CheckCompleteInvocation extends FilterInvocation<Object, Object> {

        public void onInput(final Object o, @Nonnull final ResultChannel<Object> result) {

            JRoutine.on(factoryOf(DelayedInvocation.class, millis(100)))
                    .callAsync("test")
                    .eventually()
                    .checkComplete();
        }
    }

    private static class CloseInvocation extends FilterInvocation<String, String> {

        private final AtomicBoolean mIsFailed;

        private final Semaphore mSemaphore;

        private CloseInvocation(@Nonnull final Semaphore semaphore,
                @Nonnull final AtomicBoolean isFailed) {

            mSemaphore = semaphore;
            mIsFailed = isFailed;
        }

        public void onInput(final String s, @Nonnull final ResultChannel<String> result) {

            new Thread() {

                @Override
                public void run() {

                    super.run();

                    try {

                        Thread.sleep(100);

                        try {

                            result.pass(s);

                            mIsFailed.set(true);

                        } catch (final IllegalStateException ignored) {

                        }

                        mSemaphore.release();

                    } catch (final InterruptedException ignored) {

                    }
                }

            }.start();
        }
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
            mRoutine = JRoutine.on(factoryOf(DelayedInvocation.class, TimeDuration.ZERO))
                               .buildRoutine();
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

    private static class ExceptionRoutine extends AbstractRoutine<Object, Object> {

        protected ExceptionRoutine(@Nonnull final InvocationConfiguration configuration) {

            super(configuration);
        }

        @Nonnull
        @Override
        protected Invocation<Object, Object> newInvocation(@Nonnull final InvocationType type) {

            throw new IllegalStateException();
        }
    }

    private static class HasNextInvocation extends FilterInvocation<Object, Object> {

        public void onInput(final Object o, @Nonnull final ResultChannel<Object> result) {

            JRoutine.on(factoryOf(DelayedInvocation.class, millis(100)))
                    .callAsync("test")
                    .eventually()
                    .iterator()
                    .hasNext();
        }
    }

    private static class NextInvocation extends FilterInvocation<Object, Object> {

        public void onInput(final Object o, @Nonnull final ResultChannel<Object> result) {

            JRoutine.on(factoryOf(DelayedInvocation.class, millis(100)))
                    .callAsync("test")
                    .eventually()
                    .iterator()
                    .next();
        }
    }

    private static class NullRoutine extends AbstractRoutine<Object, Object> {

        protected NullRoutine(@Nonnull final InvocationConfiguration configuration) {

            super(configuration);
        }

        @Nonnull
        @Override
        @SuppressWarnings("ConstantConditions")
        protected Invocation<Object, Object> newInvocation(@Nonnull final InvocationType type) {

            return null;
        }
    }

    private static class SleepInvocation extends ProcedureInvocation<Void> {

        public void onResult(@Nonnull final ResultChannel<Void> result) {

            try {

                seconds(1).sleepAtLeast();

            } catch (final InterruptedException e) {

                throw new InvocationInterruptedException(e);
            }
        }
    }

    private static class SlowFilterInvocation<DATA> extends FilterInvocation<DATA, DATA> {

        private final TimeDuration mDelay;

        private SlowFilterInvocation(@Nonnull final TimeDuration delay) {

            mDelay = delay;
        }

        public void onInput(final DATA data, @Nonnull final ResultChannel<DATA> result) {

            try {

                mDelay.sleepAtLeast();

            } catch (final InterruptedException e) {

                throw new InvocationInterruptedException(e);
            }

            result.pass(data);
        }
    }

    private static class SquareInvocation extends FilterInvocation<Integer, Integer> {

        public void onInput(final Integer integer, @Nonnull final ResultChannel<Integer> result) {

            final int input = integer;
            result.pass(input * input);
        }
    }

    private static class TestAbortHandler implements AbortHandler {

        public void onAbort(@Nullable final Throwable reason, final long delay,
                @Nonnull final TimeUnit timeUnit) {

        }
    }

    @SuppressWarnings("unused")
    private static class TestClass implements TestInterface {

        public static final String GET = "get";

        @Alias(GET)
        public static int get(final int i) {

            return i;
        }

        public int getInt(final int i) {

            return i;
        }

        @Alias(GET)
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
        public void onTerminate() {

            super.onTerminate();
            throw new IllegalArgumentException("test");
        }
    }

    private static class TestDestroyDiscardException extends TestDestroyException {

        @Override
        public void onTerminate() {

            super.onTerminate();
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
        public void onTerminate() {

            super.onTerminate();
            throw new IllegalArgumentException("test");
        }
    }

    private static class TestInputIterator implements InputIterator<Object> {

        @Nullable
        public Throwable getAbortException() {

            return null;
        }

        public boolean hasInput() {

            return false;
        }

        public boolean isAborting() {

            return false;
        }

        public Object nextInput() {

            return null;
        }

        public void onAbortComplete() {

        }

        public boolean onConsumeComplete() {

            return false;
        }

        public void onConsumeStart() {

        }

        public void onInvocationComplete() {

        }
    }

    private static class TestInvocationManager implements InvocationManager<Object, Object> {

        @Nonnull
        public Invocation<Object, Object> create() {

            return new TemplateInvocation<Object, Object>() {};
        }

        public void discard(@Nonnull final Invocation<Object, Object> invocation) {

        }

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
        public void onInitialize() {

            sActive = true;
        }

        @Override
        public void onTerminate() {

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

    private static class TestRunner implements Runner {

        private final ArrayList<Execution> mExecutions = new ArrayList<Execution>();

        public boolean isRunnerThread() {

            return false;
        }

        public void run(@Nonnull final Execution execution, final long delay,
                @Nonnull final TimeUnit timeUnit) {

            mExecutions.add(execution);
        }

        private void run(int count) {

            final Iterator<Execution> iterator = mExecutions.iterator();

            while (iterator.hasNext() && (count-- > 0)) {

                final Execution execution = iterator.next();
                iterator.remove();
                execution.run();
            }
        }
    }
}
