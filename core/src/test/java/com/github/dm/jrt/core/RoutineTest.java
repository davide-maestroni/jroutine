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

import com.github.dm.jrt.core.DefaultResultChannel.AbortHandler;
import com.github.dm.jrt.core.DelegatingInvocation.DelegationType;
import com.github.dm.jrt.core.InvocationExecution.InputIterator;
import com.github.dm.jrt.core.builder.InvocationConfiguration;
import com.github.dm.jrt.core.builder.InvocationConfiguration.AgingPriority;
import com.github.dm.jrt.core.builder.InvocationConfiguration.OrderType;
import com.github.dm.jrt.core.builder.InvocationConfiguration.TimeoutActionType;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel.InputChannel;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.ExecutionTimeoutException;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.channel.InputDeadlockException;
import com.github.dm.jrt.core.channel.InvocationChannel;
import com.github.dm.jrt.core.channel.OutputConsumer;
import com.github.dm.jrt.core.channel.OutputDeadlockException;
import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.channel.TemplateOutputConsumer;
import com.github.dm.jrt.core.common.DeadlockException;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.common.TimeoutException;
import com.github.dm.jrt.core.invocation.CommandInvocation;
import com.github.dm.jrt.core.invocation.FilterInvocation;
import com.github.dm.jrt.core.invocation.FunctionInvocation;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationDeadlockException;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.InvocationInterruptedException;
import com.github.dm.jrt.core.invocation.PassingInvocation;
import com.github.dm.jrt.core.invocation.TemplateInvocation;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.core.util.TimeDuration;
import com.github.dm.jrt.log.Log.Level;
import com.github.dm.jrt.log.Logger;
import com.github.dm.jrt.log.NullLog;
import com.github.dm.jrt.routine.Routine;
import com.github.dm.jrt.runner.Execution;
import com.github.dm.jrt.runner.Runner;
import com.github.dm.jrt.runner.Runners;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.dm.jrt.core.builder.InvocationConfiguration.builder;
import static com.github.dm.jrt.core.invocation.InvocationFactories.factoryOf;
import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.core.util.TimeDuration.INFINITY;
import static com.github.dm.jrt.core.util.TimeDuration.millis;
import static com.github.dm.jrt.core.util.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Routine unit tests.
 * <p/>
 * Created by davide-maestroni on 09/09/2014.
 */
public class RoutineTest {

    @Test
    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored", "ConstantConditions"})
    public void testAbort() throws InterruptedException {

        final TimeDuration timeout = seconds(1);
        final Routine<String, String> routine =
                JRoutineCore.on(factoryOf(DelayedInvocation.class, millis(100))).buildRoutine();

        final InvocationChannel<String, String> inputChannel = routine.asyncInvoke().pass("test1");
        final OutputChannel<String> outputChannel = inputChannel.result();

        assertThat(inputChannel.isOpen()).isFalse();
        assertThat(inputChannel.abort(new IllegalArgumentException("test1"))).isFalse();
        assertThat(inputChannel.isOpen()).isFalse();
        assertThat(outputChannel.afterMax(timeout).next()).isEqualTo("test1");

        final InvocationChannel<String, String> inputChannel1 =
                routine.asyncInvoke().after(millis(10)).pass("test1");
        final OutputChannel<String> outputChannel1 = inputChannel1.result();

        assertThat(inputChannel1.isOpen()).isFalse();
        assertThat(inputChannel1.abort()).isFalse();
        assertThat(inputChannel1.isOpen()).isFalse();
        assertThat(outputChannel1.isOpen()).isTrue();
        assertThat(outputChannel1.afterMax(timeout).hasNext()).isTrue();
        assertThat(outputChannel1.afterMax(timeout).all()).containsExactly("test1");
        assertThat(outputChannel1.isOpen()).isFalse();
        assertThat(outputChannel1.afterMax(timeout).hasNext()).isFalse();

        final OutputChannel<String> channel =
                routine.asyncInvoke().after(millis(10)).pass("test2").result();
        assertThat(channel.isOpen()).isTrue();
        assertThat(channel.abort(new IllegalArgumentException("test2"))).isTrue();
        assertThat(channel.abort()).isFalse();
        assertThat(channel.isOpen()).isFalse();

        try {

            channel.afterMax(timeout).all();

            fail();

        } catch (final AbortException ex) {

            assertThat(ex.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
            assertThat(ex.getCause().getMessage()).isEqualTo("test2");
        }

        try {

            channel.throwError();

        } catch (final AbortException ex) {

            assertThat(ex.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
            assertThat(ex.getCause().getMessage()).isEqualTo("test2");
        }

        final RoutineException error = channel.getError();
        assertThat(error.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(error.getCause().getMessage()).isEqualTo("test2");
        assertThat(channel.hasCompleted()).isTrue();
        assertThat(channel.isOpen()).isFalse();

        final OutputChannel<String> channel1 =
                routine.asyncInvoke().after(millis(10)).pass("test2").result();
        assertThat(channel1.isOpen()).isTrue();
        assertThat(channel1.abort()).isTrue();
        assertThat(channel1.abort(new IllegalArgumentException("test2"))).isFalse();
        assertThat(channel1.isOpen()).isFalse();

        try {

            channel1.afterMax(timeout).all();

            fail();

        } catch (final AbortException ex) {

            assertThat(ex.getCause()).isNull();
        }

        assertThat(channel1.hasCompleted()).isTrue();
        assertThat(channel1.isOpen()).isFalse();

        try {

            JRoutineCore.on(new AbortInvocation())
                        .asyncInvoke()
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

            JRoutineCore.on(new AbortInvocation2())
                        .asyncInvoke()
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
        assertThat(JRoutineCore.on(new CloseInvocation(semaphore, isFailed))
                               .withInvocations()
                               .withLogLevel(Level.SILENT)
                               .getConfigured()
                               .asyncCall("test")
                               .afterMax(timeout)
                               .all()).isEmpty();
        semaphore.tryAcquire(1, 1, TimeUnit.SECONDS);
        assertThat(isFailed.get()).isFalse();

        final InvocationChannel<Object, Object> channel2 =
                JRoutineCore.on(PassingInvocation.factoryOf()).asyncInvoke();
        channel2.after(millis(300)).abort(new IllegalArgumentException("test_abort"));
        try {
            channel2.result().afterMax(seconds(1)).throwError();

        } catch (final AbortException ex) {
            assertThat(ex.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
            assertThat(ex.getCause().getMessage()).isEqualTo("test_abort");
        }
    }

    @Test
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void testAbortInput() throws InterruptedException {

        final Semaphore semaphore = new Semaphore(0);
        final AtomicReference<Throwable> abortReason = new AtomicReference<Throwable>();

        final TemplateInvocation<String, String> abortInvocation =
                new TemplateInvocation<String, String>() {

                    @Override
                    public void onAbort(@NotNull final RoutineException reason) {

                        abortReason.set(reason);
                        semaphore.release();
                    }
                };

        final Routine<String, String> routine =
                JRoutineCore.on(factoryOf(abortInvocation, this, abortReason, semaphore))
                            .buildRoutine();

        final InvocationChannel<String, String> channel = routine.asyncInvoke();
        final IllegalArgumentException exception = new IllegalArgumentException();
        channel.after(millis(100)).abort(exception);

        semaphore.tryAcquire(1, TimeUnit.SECONDS);

        assertThat(abortReason.get().getCause()).isEqualTo(exception);

        final InvocationChannel<String, String> channel1 = routine.asyncInvoke();
        final IllegalAccessError exception1 = new IllegalAccessError();
        channel1.now().abort(exception1);

        semaphore.tryAcquire(1, TimeUnit.SECONDS);

        assertThat(abortReason.get().getCause()).isEqualTo(exception1);
    }

    @Test
    public void testAgingPriority() {

        final TestRunner runner = new TestRunner();
        final Routine<Object, Object> routine1 = JRoutineCore.on(PassingInvocation.factoryOf())
                                                             .withInvocations()
                                                             .withRunner(runner)
                                                             .withPriority(
                                                                     AgingPriority.NORMAL_PRIORITY)
                                                             .getConfigured()
                                                             .buildRoutine();
        final Routine<Object, Object> routine2 = JRoutineCore.on(PassingInvocation.factoryOf())
                                                             .withInvocations()
                                                             .withRunner(runner)
                                                             .withPriority(
                                                                     AgingPriority.HIGH_PRIORITY)
                                                             .getConfigured()
                                                             .buildRoutine();
        final OutputChannel<Object> output1 = routine1.asyncCall("test1").eventuallyExit();
        final InvocationChannel<Object, Object> input2 = routine2.asyncInvoke();

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
        final OutputChannel<Object> channel1 = JRoutineCore.on(PassingInvocation.factoryOf())
                                                           .asyncInvoke()
                                                           .after(seconds(1))
                                                           .pass("test1")
                                                           .result();

        channel1.bindTo(consumer);
        assertThat(channel1.isBound()).isTrue();
        assertThat(consumer.isOutput()).isFalse();

        final OutputChannel<Object> channel2 =
                JRoutineCore.on(PassingInvocation.factoryOf()).syncInvoke().pass("test2").result();

        channel2.bindTo(consumer);
        assertThat(channel1.isBound()).isTrue();
        assertThat(channel2.isBound()).isTrue();
        assertThat(consumer.isOutput()).isTrue();
        assertThat(consumer.getOutput()).isEqualTo("test2");
    }

    @Test
    public void testCalls() {

        final TimeDuration timeout = seconds(1);
        final Routine<String, String> routine =
                JRoutineCore.on(PassingInvocation.<String>factoryOf()).buildRoutine();

        assertThat(routine.syncCall().afterMax(timeout).all()).isEmpty();
        assertThat(routine.syncCall(Arrays.asList("test1", "test2"))
                          .afterMax(timeout)
                          .all()).containsExactly("test1", "test2");
        assertThat(routine.syncCall(routine.syncCall("test1", "test2"))
                          .afterMax(timeout)
                          .all()).containsExactly("test1", "test2");
        assertThat(routine.syncCall("test1").afterMax(timeout).all()).containsExactly("test1");
        assertThat(routine.syncCall("test1", "test2").afterMax(timeout).all()).containsExactly(
                "test1", "test2");
        assertThat(routine.asyncCall().afterMax(timeout).all()).isEmpty();
        assertThat(routine.asyncCall(Arrays.asList("test1", "test2"))
                          .afterMax(timeout)
                          .all()).containsExactly("test1", "test2");
        assertThat(routine.asyncCall(routine.syncCall("test1", "test2"))
                          .afterMax(timeout)
                          .all()).containsExactly("test1", "test2");
        assertThat(routine.asyncCall("test1").afterMax(timeout).all()).containsExactly("test1");
        assertThat(routine.asyncCall("test1", "test2").afterMax(timeout).all()).containsExactly(
                "test1", "test2");
        assertThat(routine.parallelCall().afterMax(timeout).all()).isEmpty();
        assertThat(routine.parallelCall(Arrays.asList("test1", "test2"))
                          .afterMax(timeout)
                          .all()).containsOnly("test1", "test2");
        assertThat(routine.parallelCall(routine.syncCall("test1", "test2")).afterMax(timeout).all())
                .containsOnly("test1", "test2");
        assertThat(routine.parallelCall("test1").afterMax(timeout).all()).containsOnly("test1");
        assertThat(routine.parallelCall("test1", "test2").afterMax(timeout).all()).containsOnly(
                "test1", "test2");

        assertThat(routine.syncInvoke().pass().result().all()).isEmpty();
        assertThat(routine.syncInvoke()
                          .pass(Arrays.asList("test1", "test2"))
                          .result()
                          .afterMax(timeout)
                          .all()).containsExactly("test1", "test2");
        assertThat(routine.syncInvoke()
                          .pass(routine.syncCall("test1", "test2"))
                          .result()
                          .afterMax(timeout)
                          .all()).containsExactly("test1", "test2");
        assertThat(routine.syncInvoke()
                          .pass("test1")
                          .result()
                          .afterMax(timeout)
                          .all()).containsExactly("test1");
        assertThat(routine.syncInvoke()
                          .pass("test1", "test2")
                          .result()
                          .afterMax(timeout)
                          .all()).containsExactly("test1", "test2");
        assertThat(routine.asyncInvoke().pass().result().afterMax(timeout).all()).isEmpty();
        assertThat(routine.asyncInvoke()
                          .pass(Arrays.asList("test1", "test2"))
                          .result()
                          .afterMax(timeout)
                          .all()).containsExactly("test1", "test2");
        assertThat(routine.asyncInvoke()
                          .pass(routine.syncCall("test1", "test2"))
                          .result()
                          .afterMax(timeout)
                          .all()).containsExactly("test1", "test2");
        assertThat(routine.asyncInvoke()
                          .pass("test1")
                          .result()
                          .afterMax(timeout)
                          .all()).containsExactly("test1");
        assertThat(routine.asyncInvoke()
                          .pass("test1", "test2")
                          .result()
                          .afterMax(timeout)
                          .all()).containsExactly("test1", "test2");
        assertThat(routine.parallelInvoke().pass().result().afterMax(timeout).all()).isEmpty();
        assertThat(routine.parallelInvoke()
                          .pass(Arrays.asList("test1", "test2"))
                          .result()
                          .afterMax(timeout)
                          .all()).containsOnly("test1", "test2");
        assertThat(routine.parallelInvoke()
                          .pass(routine.syncCall("test1", "test2"))
                          .result()
                          .afterMax(timeout)
                          .all()).containsOnly("test1", "test2");
        assertThat(routine.parallelInvoke()
                          .pass("test1")
                          .result()
                          .afterMax(timeout)
                          .all()).containsOnly("test1");
        assertThat(routine.parallelInvoke().pass("test1", "test2").result().afterMax(timeout).all())
                .containsOnly("test1", "test2");
    }

    @Test
    public void testChainedRoutine() {

        final TimeDuration timeout = seconds(1);
        final FunctionInvocation<Integer, Integer> execSum =
                new FunctionInvocation<Integer, Integer>() {

                    @Override
                    protected void onCall(@NotNull final List<? extends Integer> integers,
                            @NotNull final ResultChannel<Integer> result) {

                        int sum = 0;

                        for (final Integer integer : integers) {

                            sum += integer;
                        }

                        result.pass(sum);
                    }
                };

        final Routine<Integer, Integer> sumRoutine =
                JRoutineCore.on(factoryOf(execSum, this)).buildRoutine();
        final Routine<Integer, Integer> squareRoutine =
                JRoutineCore.on(new SquareInvocation()).buildRoutine();

        assertThat(sumRoutine.syncCall(squareRoutine.syncCall(1, 2, 3, 4))
                             .afterMax(timeout)
                             .all()).containsExactly(30);
        assertThat(sumRoutine.asyncCall(squareRoutine.syncCall(1, 2, 3, 4)).afterMax(timeout).all())
                .containsExactly(30);
        assertThat(sumRoutine.asyncCall(squareRoutine.asyncCall(1, 2, 3, 4))
                             .afterMax(timeout)
                             .all()).containsExactly(30);
        assertThat(sumRoutine.asyncCall(squareRoutine.parallelCall(1, 2, 3, 4))
                             .afterMax(timeout)
                             .all()).containsExactly(30);
    }

    @Test
    public void testComposedRoutine() {

        final TimeDuration timeout = seconds(1);
        final FunctionInvocation<Integer, Integer> execSum =
                new FunctionInvocation<Integer, Integer>() {

                    @Override
                    protected void onCall(@NotNull final List<? extends Integer> integers,
                            @NotNull final ResultChannel<Integer> result) {

                        int sum = 0;

                        for (final Integer integer : integers) {

                            sum += integer;
                        }

                        result.pass(sum);
                    }
                };

        final Routine<Integer, Integer> sumRoutine =
                JRoutineCore.on(factoryOf(execSum, this)).buildRoutine();
        final Routine<Integer, Integer> squareRoutine =
                JRoutineCore.on(new SquareInvocation()).buildRoutine();

        final TemplateInvocation<Integer, Integer> invokeSquareSum =
                new TemplateInvocation<Integer, Integer>() {

                    private InvocationChannel<Integer, Integer> mChannel;

                    @Override
                    public void onAbort(@NotNull final RoutineException reason) {

                        mChannel.abort(reason);
                    }

                    @Override
                    public void onInitialize() {

                        mChannel = sumRoutine.asyncInvoke();
                    }

                    @Override
                    public void onInput(final Integer integer,
                            @NotNull final ResultChannel<Integer> result) {

                        squareRoutine.asyncCall(integer).bindTo(mChannel);
                    }

                    @Override
                    public void onResult(@NotNull final ResultChannel<Integer> result) {

                        result.pass(mChannel.result());
                    }
                };

        final Routine<Integer, Integer> squareSumRoutine =
                JRoutineCore.on(factoryOf(invokeSquareSum, this, sumRoutine, squareRoutine))
                            .buildRoutine();

        //        assertThat(squareSumRoutine.syncCall(1, 2, 3, 4).afterMax(timeout).all())
        // .containsExactly(
        //                30);
        assertThat(squareSumRoutine.asyncCall(1, 2, 3, 4).afterMax(timeout).all()).containsExactly(
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

        final Routine<Object, Object> routine2 =
                JRoutineCore.on(new AllInvocation()).buildRoutine();

        try {

            routine2.asyncCall("test").afterMax(seconds(1)).all();

            fail();

        } catch (final DeadlockException ignored) {

        }
    }

    @Test
    public void testDeadlockOnCheckComplete() {

        final Routine<Object, Object> routine1 =
                JRoutineCore.on(new CheckCompleteInvocation()).buildRoutine();

        try {

            routine1.asyncCall("test").afterMax(seconds(1)).all();

            fail();

        } catch (final DeadlockException ignored) {

        }
    }

    @Test
    public void testDeadlockOnHasNext() {

        final Routine<Object, Object> routine3 =
                JRoutineCore.on(new HasNextInvocation()).buildRoutine();

        try {

            routine3.asyncCall("test").afterMax(seconds(1)).all();

            fail();

        } catch (final DeadlockException ignored) {

        }
    }

    @Test
    public void testDeadlockOnNext() {

        final Routine<Object, Object> routine4 =
                JRoutineCore.on(new NextInvocation()).buildRoutine();

        try {

            routine4.asyncCall("test").afterMax(seconds(1)).all();

            fail();

        } catch (final DeadlockException ignored) {

        }
    }

    @Test
    public void testDelay() {

        long startTime = System.currentTimeMillis();

        final InvocationChannel<String, String> channel =
                JRoutineCore.on(factoryOf(DelayedInvocation.class, millis(10))).asyncInvoke();
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
                JRoutineCore.on(factoryOf(ClassToken.tokenOf(DelayedInvocation.class), millis(10)))
                            .withInvocations()
                            .withInputOrder(OrderType.BY_CALL)
                            .withOutputOrder(OrderType.BY_CALL)
                            .getConfigured()
                            .asyncInvoke();
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
                JRoutineCore.on(factoryOf(DelayedListInvocation.class, millis(10), 2))
                            .asyncInvoke();
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

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.BY_CALL)
                                                               .withOutputOrder(OrderType.BY_CALL)
                                                               .getConfigured();
        final InvocationChannel<String, String> channel3 =
                JRoutineCore.on(factoryOf(DelayedListInvocation.class, millis(10), 2))
                            .withInvocations()
                            .with(configuration)
                            .getConfigured()
                            .asyncInvoke();
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
                JRoutineCore.on(factoryOf(DelayedListInvocation.class, TimeDuration.ZERO, 2))
                            .asyncInvoke();
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
                JRoutineCore.on(factoryOf(DelayedListInvocation.class, TimeDuration.ZERO, 2))
                            .withInvocations()
                            .with(configuration)
                            .getConfigured()
                            .asyncInvoke();
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
                JRoutineCore.on(factoryOf(DelayedChannelInvocation.class, millis(10)))
                            .asyncInvoke();
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
                JRoutineCore.on(factoryOf(DelayedChannelInvocation.class, millis(10)))
                            .withInvocations()
                            .with(configuration)
                            .getConfigured()
                            .asyncInvoke();
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
                JRoutineCore.on(factoryOf(DelayedChannelInvocation.class, TimeDuration.ZERO))
                            .asyncInvoke();
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
                JRoutineCore.on(factoryOf(DelayedChannelInvocation.class, TimeDuration.ZERO))
                            .withInvocations()
                            .with(configuration)
                            .getConfigured()
                            .asyncInvoke();
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
                JRoutineCore.on(PassingInvocation.<String>factoryOf()).buildRoutine();

        final InvocationChannel<String, String> channel1 = passingRoutine.asyncInvoke();
        channel1.after(seconds(2)).abort();
        assertThat(channel1.now().pass("test").result().afterMax(timeout).next()).isEqualTo("test");

        final InvocationChannel<String, String> channel2 = passingRoutine.asyncInvoke();
        channel2.after(millis(100)).abort();

        try {

            channel2.after(millis(200)).pass("test").result().afterMax(timeout).next();

            fail();

        } catch (final AbortException ignored) {

        }

        final Routine<String, String> abortRoutine =
                JRoutineCore.on(factoryOf(DelayedAbortInvocation.class, millis(200)))
                            .buildRoutine();

        assertThat(abortRoutine.asyncCall("test").afterMax(timeout).next()).isEqualTo("test");

        try {

            final OutputChannel<String> channel = abortRoutine.asyncCall("test");

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
                JRoutineCore.on(PassingInvocation.factoryOf()).buildRoutine();
        final Routine<Object, Object> routine2 =
                JRoutineCore.on(PassingInvocation.factoryOf()).buildRoutine();

        final long startTime = System.currentTimeMillis();

        assertThat(routine1.asyncInvoke()
                           .after(millis(500))
                           .pass(routine2.asyncCall("test"))
                           .result()
                           .afterMax(timeout)
                           .next()).isEqualTo("test");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(500);
    }

    @Test
    public void testDelegation() {

        final TimeDuration timeout = seconds(1);
        final Routine<Object, Object> routine1 =
                JRoutineCore.on(PassingInvocation.factoryOf()).buildRoutine();
        final Routine<Object, Object> routine2 =
                JRoutineCore.on(DelegatingInvocation.factoryFrom(routine1, DelegationType.SYNC))
                            .buildRoutine();

        assertThat(routine2.asyncCall("test1").afterMax(timeout).all()).containsExactly("test1");

        final InvocationChannel<Object, Object> channel =
                routine2.asyncInvoke().after(timeout).pass("test2");
        channel.now().abort(new IllegalArgumentException());

        try {

            channel.result().next();

        } catch (final AbortException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
        }

        final Routine<String, String> routine3 =
                JRoutineCore.on(factoryOf(TestDestroy.class)).buildRoutine();
        final Routine<String, String> routine4 =
                JRoutineCore.on(DelegatingInvocation.factoryFrom(routine3, DelegationType.ASYNC))
                            .buildRoutine();

        assertThat(routine4.asyncCall("test4").afterMax(timeout).all()).containsExactly("test4");
        routine4.purge();
        assertThat(TestDestroy.getInstanceCount()).isZero();
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testDelegationError() {

        try {

            DelegatingInvocation.factoryFrom(null, DelegationType.SYNC);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            DelegatingInvocation.factoryFrom(
                    JRoutineCore.on(PassingInvocation.factoryOf()).buildRoutine(), null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testDestroyAsync() {

        final TimeDuration timeout = seconds(1);
        final Routine<String, String> routine1 = JRoutineCore.on(factoryOf(TestDestroy.class))
                                                             .withInvocations()
                                                             .withCoreInstances(0)
                                                             .getConfigured()
                                                             .buildRoutine();
        assertThat(routine1.syncCall("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(
                routine1.asyncCall("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(routine1.parallelCall("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .all()).containsOnly("1", "2", "3", "4", "5");
        assertThat(TestDestroy.getInstanceCount()).isZero();

        final Routine<String, String> routine2 =
                JRoutineCore.on(factoryOf(TestDiscardException.class))
                            .withInvocations()
                            .withCoreInstances(0)
                            .getConfigured()
                            .buildRoutine();
        assertThat(routine2.syncCall("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(
                routine2.asyncCall("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(routine2.parallelCall("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .all()).containsOnly("1", "2", "3", "4", "5");
        assertThat(TestDestroy.getInstanceCount()).isZero();

        final Routine<String, String> routine3 =
                JRoutineCore.on(factoryOf(TestDestroyDiscard.class)).buildRoutine();
        assertThat(routine3.syncCall("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .hasCompleted()).isTrue();
        assertThat(routine3.parallelCall("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .hasCompleted()).isTrue();
        assertThat(routine3.asyncCall("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .hasCompleted()).isTrue();
        assertThat(TestDestroy.getInstanceCount()).isZero();

        final Routine<String, String> routine4 =
                JRoutineCore.on(factoryOf(TestDestroyDiscardException.class)).buildRoutine();
        assertThat(routine4.syncCall("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .hasCompleted()).isTrue();
        assertThat(routine4.parallelCall("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .hasCompleted()).isTrue();
        assertThat(routine4.asyncCall("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .hasCompleted()).isTrue();
        assertThat(TestDestroy.getInstanceCount()).isZero();

        final Routine<String, String> routine5 =
                JRoutineCore.on(factoryOf(TestDestroy.class)).buildRoutine();
        assertThat(routine5.syncCall("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(
                routine5.asyncCall("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(routine5.parallelCall("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .all()).containsOnly("1", "2", "3", "4", "5");
        routine5.purge();
        assertThat(TestDestroy.getInstanceCount()).isZero();

        final Routine<String, String> routine6 =
                JRoutineCore.on(factoryOf(TestDestroyException.class)).buildRoutine();

        assertThat(routine6.syncCall("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(
                routine6.asyncCall("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(routine6.parallelCall("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .all()).containsOnly("1", "2", "3", "4", "5");
        routine6.purge();
        assertThat(TestDestroy.getInstanceCount()).isZero();
    }

    @Test
    public void testDestroySync() {

        final TimeDuration timeout = seconds(1);
        final Routine<String, String> routine1 = JRoutineCore.on(factoryOf(TestDestroy.class))
                                                             .withInvocations()
                                                             .withCoreInstances(0)
                                                             .getConfigured()
                                                             .buildRoutine();
        assertThat(
                routine1.asyncCall("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(routine1.parallelCall("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .all()).containsOnly("1", "2", "3", "4", "5");
        assertThat(routine1.syncCall("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(TestDestroy.getInstanceCount()).isZero();

        final Routine<String, String> routine2 =
                JRoutineCore.on(factoryOf(TestDiscardException.class))
                            .withInvocations()
                            .withCoreInstances(0)
                            .getConfigured()
                            .buildRoutine();
        assertThat(
                routine2.asyncCall("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(routine2.parallelCall("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .all()).containsOnly("1", "2", "3", "4", "5");
        assertThat(routine2.syncCall("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(TestDestroy.getInstanceCount()).isZero();

        final Routine<String, String> routine3 =
                JRoutineCore.on(factoryOf(TestDestroyDiscard.class)).buildRoutine();
        assertThat(routine3.parallelCall("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .hasCompleted()).isTrue();
        assertThat(routine3.asyncCall("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .hasCompleted()).isTrue();
        assertThat(routine3.syncCall("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .hasCompleted()).isTrue();
        assertThat(TestDestroy.getInstanceCount()).isZero();

        final Routine<String, String> routine4 =
                JRoutineCore.on(factoryOf(TestDestroyDiscardException.class)).buildRoutine();
        assertThat(routine4.parallelCall("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .hasCompleted()).isTrue();
        assertThat(routine4.asyncCall("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .hasCompleted()).isTrue();
        assertThat(routine4.syncCall("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .hasCompleted()).isTrue();
        assertThat(TestDestroy.getInstanceCount()).isZero();

        final Routine<String, String> routine5 =
                JRoutineCore.on(factoryOf(TestDestroy.class)).buildRoutine();
        assertThat(
                routine5.asyncCall("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(routine5.parallelCall("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .all()).containsOnly("1", "2", "3", "4", "5");
        assertThat(routine5.syncCall("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        routine5.purge();
        assertThat(TestDestroy.getInstanceCount()).isZero();

        final Routine<String, String> routine6 =
                JRoutineCore.on(factoryOf(TestDestroyException.class)).buildRoutine();

        assertThat(
                routine6.asyncCall("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(routine6.parallelCall("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .all()).containsOnly("1", "2", "3", "4", "5");
        assertThat(routine6.syncCall("1", "2", "3", "4", "5").afterMax(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        routine6.purge();
        assertThat(TestDestroy.getInstanceCount()).isZero();
    }

    @Test
    public void testEmpty() {

        final InvocationChannel<Object, Object> channel =
                JRoutineCore.on(new SleepInvocation(millis(500)))
                            .withInvocations()
                            .withInputLimit(1)
                            .withInputMaxDelay(seconds(3))
                            .getConfigured()
                            .asyncInvoke();
        assertThat(channel.isEmpty()).isTrue();
        assertThat(channel.pass("test1").pass("test2").isEmpty()).isFalse();
        final OutputChannel<Object> result = channel.result();
        assertThat(result.isEmpty()).isTrue();
        assertThat(result.afterMax(seconds(10)).hasCompleted()).isTrue();
        assertThat(channel.isEmpty()).isTrue();
        assertThat(result.isEmpty()).isFalse();
    }

    @Test
    public void testEmptyAbort() {

        final Routine<Object, Object> routine = JRoutineCore.on(new SleepInvocation(millis(500)))
                                                            .withInvocations()
                                                            .withInputLimit(1)
                                                            .withInputMaxDelay(seconds(3))
                                                            .getConfigured()
                                                            .buildRoutine();
        InvocationChannel<Object, Object> channel = routine.asyncInvoke();
        assertThat(channel.isEmpty()).isTrue();
        assertThat(channel.pass("test1").abort()).isTrue();
        assertThat(channel.isEmpty()).isTrue();
        channel = routine.asyncInvoke();
        assertThat(channel.isEmpty()).isTrue();
        assertThat(channel.pass("test1").pass("test2").isEmpty()).isFalse();
        final OutputChannel<Object> result = channel.result();
        assertThat(result.isEmpty()).isTrue();
        assertThat(result.abort()).isTrue();
        assertThat(channel.isEmpty()).isTrue();
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testError() {

        try {

            JRoutineCore.on(factoryOf(ConstructorException.class))
                        .withInvocations()
                        .withLogLevel(Level.SILENT)
                        .getConfigured()
                        .syncCall()
                        .all();

            fail();

        } catch (final InvocationException ignored) {

        }

        try {

            new AbstractRoutine<Object, Object>(null) {

                @NotNull
                @Override
                protected Invocation<Object, Object> newInvocation(
                        @NotNull final InvocationType type) {

                    return new ConstructorException();
                }
            };

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultRoutine<Object, Object>(null, new InvocationFactory<Object, Object>() {

                @NotNull
                @Override
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
                                                     new TestAbortHandler(), Runners.syncRunner(),
                                                     logger);

            new InvocationExecution<Object, Object>(null, new TestInputIterator(), channel, logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final DefaultResultChannel<Object> channel =
                    new DefaultResultChannel<Object>(InvocationConfiguration.DEFAULT_CONFIGURATION,
                                                     new TestAbortHandler(), Runners.syncRunner(),
                                                     logger);

            new InvocationExecution<Object, Object>(new TestInvocationManager(), null, channel,
                                                    logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new InvocationExecution<Object, Object>(new TestInvocationManager(),
                                                    new TestInputIterator(), null, logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final DefaultResultChannel<Object> channel =
                    new DefaultResultChannel<Object>(InvocationConfiguration.DEFAULT_CONFIGURATION,
                                                     new TestAbortHandler(), Runners.syncRunner(),
                                                     logger);

            new InvocationExecution<Object, Object>(new TestInvocationManager(),
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
                JRoutineCore.on(factoryOf(exceptionOnInit, this)).buildRoutine();

        testException(exceptionRoutine, "test", "test1");

        final Routine<String, String> passingRoutine =
                JRoutineCore.on(PassingInvocation.<String>factoryOf()).buildRoutine();

        testChained(passingRoutine, exceptionRoutine, "test", "test1");
        testChained(exceptionRoutine, passingRoutine, "test", "test1");
    }

    @Test
    public void testErrorOnInput() {

        final TemplateInvocation<String, String> exceptionOnInput =
                new TemplateInvocation<String, String>() {

                    @Override
                    public void onInput(final String s,
                            @NotNull final ResultChannel<String> result) {

                        throw new NullPointerException(s);
                    }
                };

        final Routine<String, String> exceptionRoutine =
                JRoutineCore.on(factoryOf(exceptionOnInput, this)).buildRoutine();

        testException(exceptionRoutine, "test2", "test2");

        final Routine<String, String> passingRoutine =
                JRoutineCore.on(PassingInvocation.<String>factoryOf()).buildRoutine();

        testChained(passingRoutine, exceptionRoutine, "test2", "test2");
        testChained(exceptionRoutine, passingRoutine, "test2", "test2");
    }

    @Test
    public void testErrorOnResult() {

        final TemplateInvocation<String, String> exceptionOnResult =
                new TemplateInvocation<String, String>() {

                    @Override
                    public void onResult(@NotNull final ResultChannel<String> result) {

                        throw new NullPointerException("test3");
                    }
                };

        final Routine<String, String> exceptionRoutine =
                JRoutineCore.on(factoryOf(exceptionOnResult, this)).buildRoutine();

        testException(exceptionRoutine, "test", "test3");

        final Routine<String, String> passingRoutine =
                JRoutineCore.on(PassingInvocation.<String>factoryOf()).buildRoutine();

        testChained(passingRoutine, exceptionRoutine, "test", "test3");
        testChained(exceptionRoutine, passingRoutine, "test", "test3");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testFactoryError() {

        try {

            JRoutineCore.on((InvocationFactory<?, ?>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testInitInvocationException() {

        final ExceptionRoutine routine =
                new ExceptionRoutine(InvocationConfiguration.DEFAULT_CONFIGURATION);

        try {

            routine.asyncCall().afterMax(seconds(1)).all();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    public void testInitInvocationNull() {

        final NullRoutine routine = new NullRoutine(InvocationConfiguration.DEFAULT_CONFIGURATION);

        try {

            routine.asyncCall().afterMax(seconds(1)).all();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(NullPointerException.class);
        }
    }

    @Test
    public void testInputDeadlock() {

        try {

            JRoutineCore.on(new SleepInvocation(millis(100)))
                        .withInvocations()
                        .withInputMaxSize(1)
                        .getConfigured()
                        .asyncCall("test", "test")
                        .all();

            fail();

        } catch (final InputDeadlockException ignored) {

        }

        try {

            JRoutineCore.on(new SleepInvocation(millis(100)))
                        .withInvocations()
                        .withInputMaxSize(1)
                        .getConfigured()
                        .asyncCall(Arrays.asList("test", "test"))
                        .all();

            fail();

        } catch (final InputDeadlockException ignored) {

        }
    }

    @Test
    public void testInputDelay() {

        assertThat(JRoutineCore.on(PassingInvocation.factoryOf())
                               .withInvocations()
                               .withInputOrder(OrderType.BY_CALL)
                               .withInputLimit(1)
                               .withInputMaxDelay(TimeDuration.ZERO)
                               .getConfigured()
                               .asyncInvoke()
                               .orderByDelay()
                               .orderByCall()
                               .after(millis(100))
                               .pass("test1")
                               .now()
                               .pass("test2")
                               .result()
                               .afterMax(seconds(1))
                               .all()).containsExactly("test1", "test2");

        assertThat(JRoutineCore.on(PassingInvocation.factoryOf())
                               .withInvocations()
                               .withInputOrder(OrderType.BY_CALL)
                               .withInputLimit(1)
                               .withInputMaxDelay(millis(1000))
                               .getConfigured()
                               .asyncInvoke()
                               .orderByCall()
                               .after(millis(100))
                               .pass("test1")
                               .now()
                               .pass("test2")
                               .result()
                               .afterMax(seconds(1))
                               .all()).containsExactly("test1", "test2");

        assertThat(JRoutineCore.on(PassingInvocation.factoryOf())
                               .withInvocations()
                               .withInputOrder(OrderType.BY_CALL)
                               .withInputLimit(1)
                               .withInputMaxDelay(millis(1000))
                               .getConfigured()
                               .asyncInvoke()
                               .orderByCall()
                               .after(millis(100))
                               .pass("test1")
                               .now()
                               .pass(asArgs("test2"))
                               .result()
                               .afterMax(seconds(1))
                               .all()).containsExactly("test1", "test2");

        assertThat(JRoutineCore.on(PassingInvocation.factoryOf())
                               .withInvocations()
                               .withInputOrder(OrderType.BY_CALL)
                               .withInputLimit(1)
                               .withInputMaxDelay(millis(1000))
                               .getConfigured()
                               .asyncInvoke()
                               .orderByCall()
                               .after(millis(100))
                               .pass("test1")
                               .now()
                               .pass(Collections.singletonList("test2"))
                               .result()
                               .afterMax(seconds(1))
                               .all()).containsExactly("test1", "test2");

        final IOChannel<Object> channel = JRoutineCore.io().buildChannel();
        channel.pass("test2").close();
        assertThat(JRoutineCore.on(PassingInvocation.factoryOf())
                               .withInvocations()
                               .withInputOrder(OrderType.BY_CALL)
                               .withInputLimit(1)
                               .withInputMaxDelay(millis(1000))
                               .getConfigured()
                               .asyncInvoke()
                               .orderByCall()
                               .after(millis(100))
                               .pass("test1")
                               .now()
                               .pass(channel)
                               .result()
                               .afterMax(seconds(1))
                               .all()).containsExactly("test1", "test2");
    }

    @Test
    public void testInputRunnerDeadlock() {

        try {

            JRoutineCore.on(new InputRunnerDeadlock()).asyncCall("test").afterMax(seconds(1)).all();

            fail();

        } catch (final DeadlockException ignored) {

        }

        try {

            JRoutineCore.on(new InputListRunnerDeadlock())
                        .asyncCall("test")
                        .afterMax(seconds(1))
                        .all();

            fail();

        } catch (final DeadlockException ignored) {

        }

        try {

            JRoutineCore.on(new InputArrayRunnerDeadlock())
                        .asyncCall("test")
                        .afterMax(seconds(1))
                        .all();

            fail();

        } catch (final DeadlockException ignored) {

        }

        try {

            JRoutineCore.on(new InputConsumerRunnerDeadlock())
                        .asyncCall("test")
                        .afterMax(seconds(1))
                        .all();

            fail();

        } catch (final DeadlockException ignored) {

        }
    }

    @Test
    public void testInputTimeoutIssue() {

        try {

            final IOChannel<Object> channel = JRoutineCore.io().buildChannel();
            channel.pass("test2").close();
            JRoutineCore.on(PassingInvocation.factoryOf())
                        .withInvocations()
                        .withInputOrder(OrderType.BY_CALL)
                        .withInputMaxSize(1)
                        .withInputMaxDelay(millis(1000))
                        .getConfigured()
                        .asyncInvoke()
                        .orderByCall()
                        .after(millis(100))
                        .pass("test1")
                        .now()
                        .pass(channel)
                        .result()
                        .afterMax(seconds(10))
                        .all();

            fail();

        } catch (final InputDeadlockException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testInvocationChannelError() {

        final Logger logger = Logger.newLogger(new NullLog(), Level.DEBUG, this);

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
                JRoutineCore.on(factoryOf(TestLifecycle.class)).asyncCall("test");

        Thread.sleep(500);

        outputChannel.abort();
        outputChannel.afterMax(INFINITY).hasCompleted();
        assertThat(TestLifecycle.sIsError).isFalse();
    }

    @Test
    public void testInvocationNotAvailable() throws InterruptedException {

        final Routine<Void, Void> routine = JRoutineCore.on(new SleepCommand())
                                                        .withInvocations()
                                                        .withMaxInstances(1)
                                                        .getConfigured()
                                                        .buildRoutine();

        routine.asyncCall();
        millis(100).sleepAtLeast();

        try {

            routine.asyncCall().afterMax(seconds(1)).next();

            fail();

        } catch (final InvocationDeadlockException ignored) {

        }
    }

    @Test
    public void testNextList() {

        assertThat(JRoutineCore.on(PassingInvocation.factoryOf())
                               .asyncCall("test1", "test2", "test3", "test4")
                               .afterMax(seconds(1))
                               .next(2)).containsExactly("test1", "test2");

        assertThat(JRoutineCore.on(PassingInvocation.factoryOf())
                               .asyncCall("test1")
                               .eventuallyExit()
                               .afterMax(seconds(1))
                               .next(2)).containsExactly("test1");

        try {

            JRoutineCore.on(PassingInvocation.factoryOf())
                        .asyncInvoke()
                        .pass("test1")
                        .after(millis(300))
                        .pass("test2")
                        .result()
                        .eventuallyAbort()
                        .afterMax(millis(100))
                        .next(2);

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            JRoutineCore.on(PassingInvocation.factoryOf())
                        .asyncInvoke()
                        .pass("test1")
                        .after(millis(300))
                        .pass("test2")
                        .result()
                        .eventuallyAbort(new IllegalStateException())
                        .afterMax(millis(100))
                        .next(2);

            fail();

        } catch (final AbortException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(IllegalStateException.class);
        }

        try {

            JRoutineCore.on(PassingInvocation.factoryOf())
                        .asyncInvoke()
                        .pass("test1")
                        .after(millis(300))
                        .pass("test2")
                        .result()
                        .eventuallyThrow()
                        .afterMax(millis(100))
                        .next(2);

            fail();

        } catch (final TimeoutException ignored) {

        }
    }

    @Test
    public void testNextOr() {

        assertThat(JRoutineCore.on(PassingInvocation.factoryOf())
                               .asyncCall("test1")
                               .afterMax(seconds(1))
                               .nextOr(2)).isEqualTo("test1");

        assertThat(JRoutineCore.on(PassingInvocation.factoryOf())
                               .asyncCall()
                               .eventuallyExit()
                               .afterMax(seconds(1))
                               .nextOr(2)).isEqualTo(2);

        try {

            JRoutineCore.on(factoryOf(DelayedInvocation.class, millis(300)))
                        .asyncCall("test1")
                        .eventuallyAbort()
                        .afterMax(millis(100))
                        .nextOr("test2");

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            JRoutineCore.on(factoryOf(DelayedInvocation.class, millis(300)))
                        .asyncCall("test1")
                        .eventuallyAbort(new IllegalStateException())
                        .afterMax(millis(100))
                        .nextOr("test2");

            fail();

        } catch (final AbortException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(IllegalStateException.class);
        }

        try {

            JRoutineCore.on(factoryOf(DelayedInvocation.class, millis(300)))
                        .asyncCall("test1")
                        .eventuallyThrow()
                        .afterMax(millis(100))
                        .nextOr("test2");

            fail();

        } catch (final TimeoutException ignored) {

        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testNullDelegatedRoutine() {

        try {

            DelegatingInvocation.factoryFrom(null, DelegationType.ASYNC);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            DelegatingInvocation.factoryFrom(JRoutineCore.on(PassingInvocation.factoryOf()), null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testOutputDeadlock() {

        final Routine<String, String> routine1 =
                JRoutineCore.on(factoryOf(new FunctionInvocation<String, String>() {

                    @Override
                    protected void onCall(@NotNull final List<? extends String> strings,
                            @NotNull final ResultChannel<String> result) {

                        result.pass(strings);
                    }
                }, this)).withInvocations().withOutputMaxSize(1).getConfigured().buildRoutine();

        try {

            routine1.asyncCall("test1", "test2").afterMax(seconds(1)).all();

            fail();

        } catch (final OutputDeadlockException ignored) {

        }

        final Routine<String, String> routine2 =
                JRoutineCore.on(factoryOf(new FunctionInvocation<String, String>() {

                    @Override
                    protected void onCall(@NotNull final List<? extends String> strings,
                            @NotNull final ResultChannel<String> result) {

                        result.pass(strings.toArray(new String[strings.size()]));
                    }
                }, this)).withInvocations().withOutputMaxSize(1).getConfigured().buildRoutine();

        try {

            routine2.asyncCall("test1", "test2").afterMax(seconds(1)).all();

            fail();

        } catch (final OutputDeadlockException ignored) {

        }
    }

    @Test
    public void testOutputTimeout() throws InterruptedException {

        final Routine<String, String> routine =
                JRoutineCore.on(PassingInvocation.<String>factoryOf())
                            .withInvocations()
                            .withOutputLimit(1)
                            .withOutputMaxDelay(TimeDuration.ZERO)
                            .getConfigured()
                            .buildRoutine();
        final OutputChannel<String> outputChannel =
                routine.asyncCall("test1", "test2").afterMax(seconds(1));
        outputChannel.hasCompleted();
        assertThat(outputChannel.all()).containsExactly("test1", "test2");

        final IOChannel<String> channel1 = JRoutineCore.io()
                                                       .withChannels()
                                                       .withChannelMaxSize(1)
                                                       .withChannelMaxDelay(millis(1000))
                                                       .getConfigured()
                                                       .buildChannel();
        new Thread() {

            @Override
            public void run() {

                channel1.pass("test1").pass("test2").close();
            }
        }.start();
        millis(100).sleepAtLeast();
        assertThat(channel1.afterMax(seconds(10)).all()).containsOnly("test1", "test2");

        final IOChannel<String> channel2 = JRoutineCore.io()
                                                       .withChannels()
                                                       .withChannelMaxSize(1)
                                                       .withChannelMaxDelay(millis(1000))
                                                       .getConfigured()
                                                       .buildChannel();
        new Thread() {

            @Override
            public void run() {

                channel2.pass("test1").pass(new String[]{"test2"}).close();
            }
        }.start();
        millis(100).sleepAtLeast();
        assertThat(channel2.afterMax(seconds(10)).all()).containsOnly("test1", "test2");

        final IOChannel<String> channel3 = JRoutineCore.io()
                                                       .withChannels()
                                                       .withChannelMaxSize(1)
                                                       .withChannelMaxDelay(millis(1000))
                                                       .getConfigured()
                                                       .buildChannel();
        new Thread() {

            @Override
            public void run() {

                channel3.pass("test1").pass(Collections.singletonList("test2")).close();
            }
        }.start();
        millis(100).sleepAtLeast();
        assertThat(channel3.afterMax(seconds(10)).all()).containsOnly("test1", "test2");

        final IOChannel<String> channel4 = JRoutineCore.io()
                                                       .withChannels()
                                                       .withChannelMaxSize(1)
                                                       .withChannelMaxDelay(millis(1000))
                                                       .getConfigured()
                                                       .buildChannel();
        new Thread() {

            @Override
            public void run() {

                final IOChannel<String> channel = JRoutineCore.io().buildChannel();
                channel.pass("test1", "test2").close();
                channel4.pass(channel).close();
            }
        }.start();
        millis(100).sleepAtLeast();
        assertThat(channel4.afterMax(seconds(10)).all()).containsOnly("test1", "test2");
    }

    @Test
    public void testPartialOut() {

        final TemplateInvocation<String, String> invocation =
                new TemplateInvocation<String, String>() {

                    @Override
                    public void onInput(final String s,
                            @NotNull final ResultChannel<String> result) {

                        result.now().pass(s).after(seconds(2)).abort();
                    }
                };

        assertThat(JRoutineCore.on(factoryOf(invocation, this))
                               .asyncCall("test")
                               .afterMax(millis(500))
                               .all()).containsExactly("test");
    }

    @Test
    public void testPendingInputs() throws InterruptedException {

        final InvocationChannel<Object, Object> channel =
                JRoutineCore.on(PassingInvocation.factoryOf()).asyncInvoke();
        assertThat(channel.isOpen()).isTrue();
        channel.pass("test");
        assertThat(channel.isOpen()).isTrue();
        channel.after(millis(500)).pass("test");
        assertThat(channel.isOpen()).isTrue();
        final IOChannel<Object> ioChannel = JRoutineCore.io().buildChannel();
        channel.pass(ioChannel);
        assertThat(channel.isOpen()).isTrue();
        channel.result();
        assertThat(channel.isOpen()).isFalse();
        ioChannel.close();
        assertThat(channel.isOpen()).isFalse();
    }

    @Test
    public void testPendingInputsAbort() throws InterruptedException {

        final InvocationChannel<Object, Object> channel =
                JRoutineCore.on(PassingInvocation.factoryOf()).asyncInvoke();
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
    @SuppressWarnings("ConstantConditions")
    public void testResultChannelError() {

        final Logger logger = Logger.newLogger(new NullLog(), Level.DEBUG, this);

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
                JRoutineCore.on(factoryOf(DelayedInvocation.class, TimeDuration.ZERO))
                            .withInvocations()
                            .withLogLevel(Level.SILENT)
                            .getConfigured()
                            .syncCall();

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

            channel.bindTo((InputChannel<String>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            channel.bindTo((OutputConsumer<String>) null);

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

            channel.bindTo(consumer).bindTo(consumer);

            fail();

        } catch (final IllegalStateException ignored) {

        }

        try {

            channel.iterator();

            fail();

        } catch (final IllegalStateException ignored) {

        }

        final Routine<String, String> routine1 =
                JRoutineCore.on(factoryOf(DelayedInvocation.class, millis(100)))
                            .withInvocations()
                            .withLogLevel(Level.SILENT)
                            .getConfigured()
                            .buildRoutine();
        final Iterator<String> iterator =
                routine1.asyncCall("test").afterMax(millis(500)).eventuallyExit().iterator();

        assertThat(iterator.next()).isEqualTo("test");

        try {

            iterator.remove();

            fail();

        } catch (final UnsupportedOperationException ignored) {

        }

        try {

            iterator.next();

            fail();

        } catch (final NoSuchElementException ignored) {

        }

        try {

            routine1.syncCall().immediately().eventuallyExit().iterator().next();

            fail();

        } catch (final NoSuchElementException ignored) {

        }

        try {

            routine1.asyncCall("test").immediately().iterator().next();

            fail();

        } catch (final ExecutionTimeoutException ignored) {

        }
    }

    @Test
    public void testResultRunnerDeadlock() {

        try {

            JRoutineCore.on(new ResultRunnerDeadlock())
                        .withInvocations()
                        .withOutputMaxSize(1)
                        .getConfigured()
                        .asyncCall("test")
                        .afterMax(seconds(1))
                        .all();

            fail();

        } catch (final OutputDeadlockException ignored) {

        }

        try {

            JRoutineCore.on(new ResultListRunnerDeadlock())
                        .withInvocations()
                        .withOutputMaxSize(1)
                        .getConfigured()
                        .asyncCall("test")
                        .afterMax(seconds(1))
                        .all();

            fail();

        } catch (final OutputDeadlockException ignored) {

        }

        try {

            JRoutineCore.on(new ResultArrayRunnerDeadlock())
                        .withInvocations()
                        .withOutputMaxSize(1)
                        .getConfigured()
                        .asyncCall("test")
                        .afterMax(seconds(1))
                        .all();

            fail();

        } catch (final OutputDeadlockException ignored) {

        }
    }

    @Test
    public void testRoutine() {

        final TimeDuration timeout = seconds(1);
        final TemplateInvocation<Integer, Integer> execSquare =
                new TemplateInvocation<Integer, Integer>() {

                    @Override
                    public void onInput(final Integer integer,
                            @NotNull final ResultChannel<Integer> result) {

                        final int input = integer;

                        result.pass(input * input);
                    }
                };

        final Routine<Integer, Integer> squareRoutine =
                JRoutineCore.on(factoryOf(execSquare, this)).buildRoutine();

        assertThat(squareRoutine.syncCall(1, 2, 3, 4).afterMax(timeout).all()).containsExactly(1, 4,
                                                                                               9,
                                                                                               16);
        assertThat(squareRoutine.asyncCall(1, 2, 3, 4).afterMax(timeout).all()).containsExactly(1,
                                                                                                4,
                                                                                                9,
                                                                                                16);
        assertThat(squareRoutine.parallelCall(1, 2, 3, 4).afterMax(timeout).all()).containsOnly(1,
                                                                                                4,
                                                                                                9,
                                                                                                16);
    }

    @Test
    public void testRoutineBuilder() {

        assertThat(JRoutineCore.on(factoryOf(new ClassToken<PassingInvocation<String>>() {}))
                               .withInvocations()
                               .withRunner(Runners.poolRunner())
                               .withCoreInstances(0)
                               .withMaxInstances(1)
                               .withInputLimit(2)
                               .withInputMaxDelay(1, TimeUnit.SECONDS)
                               .withInputMaxSize(2)
                               .withOutputLimit(2)
                               .withOutputMaxDelay(1, TimeUnit.SECONDS)
                               .withOutputMaxSize(2)
                               .withOutputOrder(OrderType.BY_CALL)
                               .getConfigured()
                               .syncCall("test1", "test2")
                               .all()).containsExactly("test1", "test2");

        assertThat(JRoutineCore.on(factoryOf(new ClassToken<PassingInvocation<String>>() {}))
                               .withInvocations()
                               .withRunner(Runners.poolRunner())
                               .withCoreInstances(0)
                               .withMaxInstances(1)
                               .withInputLimit(2)
                               .withInputMaxDelay(TimeDuration.ZERO)
                               .withInputMaxSize(2)
                               .withOutputLimit(2)
                               .withOutputMaxDelay(TimeDuration.ZERO)
                               .withOutputMaxSize(2)
                               .withOutputOrder(OrderType.BY_CALL)
                               .getConfigured()
                               .syncCall("test1", "test2")
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
                    protected void onCall(@NotNull final List<? extends Integer> integers,
                            @NotNull final ResultChannel<Integer> result) {

                        int sum = 0;

                        for (final Integer integer : integers) {

                            sum += integer;
                        }

                        result.pass(sum);
                    }
                };

        final Routine<Integer, Integer> sumRoutine =
                JRoutineCore.on(factoryOf(execSum, this)).buildRoutine();

        assertThat(sumRoutine.syncCall(1, 2, 3, 4).afterMax(timeout).all()).containsExactly(10);
        assertThat(sumRoutine.asyncCall(1, 2, 3, 4).afterMax(timeout).all()).containsExactly(10);
    }

    @Test
    public void testSkip() {

        assertThat(JRoutineCore.on(PassingInvocation.factoryOf())
                               .asyncCall("test1", "test2", "test3", "test4")
                               .afterMax(seconds(1))
                               .skip(2)
                               .all()).containsExactly("test3", "test4");

        assertThat(JRoutineCore.on(PassingInvocation.factoryOf())
                               .asyncCall("test1")
                               .eventuallyExit()
                               .afterMax(seconds(1))
                               .skip(2)
                               .all()).isEmpty();

        try {

            JRoutineCore.on(PassingInvocation.factoryOf())
                        .asyncCall("test1")
                        .eventuallyAbort()
                        .afterMax(seconds(1))
                        .skip(2);

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            JRoutineCore.on(PassingInvocation.factoryOf())
                        .asyncCall("test1")
                        .eventuallyAbort(new IllegalStateException())
                        .afterMax(seconds(1))
                        .skip(2);

            fail();

        } catch (final AbortException e) {

            assertThat(e.getCause()).isExactlyInstanceOf(IllegalStateException.class);
        }

        try {

            JRoutineCore.on(PassingInvocation.factoryOf())
                        .asyncCall("test1")
                        .eventuallyThrow()
                        .afterMax(seconds(1))
                        .skip(2);

            fail();

        } catch (final TimeoutException ignored) {

        }
    }

    @Test
    public void testTimeoutActions() {

        final Routine<String, String> routine1 =
                JRoutineCore.on(factoryOf(DelayedInvocation.class, seconds(1)))
                            .withInvocations()
                            .withReadTimeoutAction(TimeoutActionType.ABORT)
                            .getConfigured()
                            .buildRoutine();

        try {

            routine1.asyncCall("test1").next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            routine1.asyncCall("test1").all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            final ArrayList<String> results = new ArrayList<String>();
            routine1.asyncCall("test1").allInto(results);

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            routine1.asyncCall("test1").iterator().hasNext();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            routine1.asyncCall("test1").iterator().next();

            fail();

        } catch (final AbortException ignored) {

        }

        assertThat(routine1.asyncCall("test1").hasCompleted()).isFalse();

        final Routine<String, String> routine2 =
                JRoutineCore.on(factoryOf(DelayedInvocation.class, seconds(1)))
                            .withInvocations()
                            .withReadTimeoutAction(TimeoutActionType.ABORT)
                            .withReadTimeout(millis(10))
                            .getConfigured()
                            .buildRoutine();

        try {

            routine2.asyncCall("test1").next();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            routine2.asyncCall("test1").all();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            final ArrayList<String> results = new ArrayList<String>();
            routine2.asyncCall("test1").allInto(results);

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            routine2.asyncCall("test1").iterator().hasNext();

            fail();

        } catch (final AbortException ignored) {

        }

        try {

            routine2.asyncCall("test1").iterator().next();

            fail();

        } catch (final AbortException ignored) {

        }

        assertThat(routine2.asyncCall("test1").hasCompleted()).isFalse();

        final Routine<String, String> routine3 =
                JRoutineCore.on(factoryOf(DelayedInvocation.class, seconds(1)))
                            .withInvocations()
                            .withReadTimeoutAction(TimeoutActionType.THROW)
                            .getConfigured()
                            .buildRoutine();
        final OutputChannel<String> channel3 = routine3.asyncCall("test1");

        try {

            channel3.next();

            fail();

        } catch (final ExecutionTimeoutException ignored) {

        }

        try {

            channel3.all();

            fail();

        } catch (final ExecutionTimeoutException ignored) {

        }

        try {

            final ArrayList<String> results = new ArrayList<String>();
            channel3.allInto(results);

            fail();

        } catch (final ExecutionTimeoutException ignored) {

        }

        try {

            channel3.iterator().hasNext();

            fail();

        } catch (final ExecutionTimeoutException ignored) {

        }

        try {

            channel3.iterator().next();

            fail();

        } catch (final ExecutionTimeoutException ignored) {

        }

        assertThat(channel3.hasCompleted()).isFalse();

        channel3.afterMax(millis(10));

        try {

            channel3.next();

            fail();

        } catch (final ExecutionTimeoutException ignored) {

        }

        try {

            channel3.all();

            fail();

        } catch (final ExecutionTimeoutException ignored) {

        }

        try {

            final ArrayList<String> results = new ArrayList<String>();
            channel3.allInto(results);

            fail();

        } catch (final ExecutionTimeoutException ignored) {

        }

        try {

            channel3.iterator().hasNext();

            fail();

        } catch (final ExecutionTimeoutException ignored) {

        }

        try {

            channel3.iterator().next();

            fail();

        } catch (final ExecutionTimeoutException ignored) {

        }

        assertThat(channel3.hasCompleted()).isFalse();

        final Routine<String, String> routine4 =
                JRoutineCore.on(factoryOf(DelayedInvocation.class, seconds(1)))
                            .withInvocations()
                            .withReadTimeoutAction(TimeoutActionType.EXIT)
                            .getConfigured()
                            .buildRoutine();
        final OutputChannel<String> channel4 = routine4.asyncCall("test1");

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

        assertThat(channel4.hasCompleted()).isFalse();

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

        assertThat(channel4.hasCompleted()).isFalse();
    }

    private void testChained(final Routine<String, String> before,
            final Routine<String, String> after, final String input, final String expected) {

        final TimeDuration timeout = seconds(1);

        try {

            before.syncCall(after.syncCall(input)).afterMax(timeout).all();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.syncCall(after.syncCall(input)).afterMax(timeout)) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.asyncCall(after.syncCall(input)).afterMax(timeout).all();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.asyncCall(after.syncCall(input)).afterMax(timeout)) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.parallelCall(after.syncCall(input)).afterMax(timeout).all();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.parallelCall(after.syncCall(input)).afterMax(timeout)) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.syncInvoke().pass(after.syncCall(input)).result().afterMax(timeout).all();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.syncInvoke()
                                        .pass(after.syncCall(input))
                                        .result()
                                        .afterMax(timeout)) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.asyncInvoke().pass(after.syncCall(input)).result().afterMax(timeout).all();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.asyncInvoke()
                                        .pass(after.syncCall(input))
                                        .result()
                                        .afterMax(timeout)) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.parallelInvoke().pass(after.syncCall(input)).result().afterMax(timeout).all();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.parallelInvoke()
                                        .pass(after.syncCall(input))
                                        .result()
                                        .afterMax(timeout)) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.asyncCall(after.asyncCall(input)).afterMax(timeout).all();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.asyncCall(after.asyncCall(input)).afterMax(timeout)) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.asyncInvoke().pass(after.asyncCall(input)).result().afterMax(timeout).all();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.asyncInvoke()
                                        .pass(after.asyncCall(input))
                                        .result()
                                        .afterMax(timeout)) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.asyncCall(after.parallelCall(input)).afterMax(timeout).all();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.asyncCall(after.parallelCall(input)).afterMax(timeout)) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.asyncInvoke().pass(after.parallelCall(input)).result().afterMax(timeout).all();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.asyncInvoke()
                                        .pass(after.parallelCall(input))
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
                JRoutineCore.on(factoryOf(DelayedInvocation.class, TimeDuration.ZERO))
                            .buildRoutine();

        assertThat(
                routine.syncCall(input).bindTo(consumer).afterMax(timeout).hasCompleted()).isTrue();
        assertThat(routine.asyncCall(input)
                          .bindTo(consumer)
                          .afterMax(timeout)
                          .hasCompleted()).isTrue();
        assertThat(routine.parallelCall(input)
                          .bindTo(consumer)
                          .afterMax(timeout)
                          .hasCompleted()).isTrue();
        assertThat(routine.syncInvoke()
                          .pass(input)
                          .result()
                          .bindTo(consumer)
                          .afterMax(timeout)
                          .hasCompleted()).isTrue();
        assertThat(routine.asyncInvoke()
                          .pass(input)
                          .result()
                          .bindTo(consumer)
                          .afterMax(timeout)
                          .hasCompleted()).isTrue();
        assertThat(routine.parallelInvoke()
                          .pass(input)
                          .result()
                          .bindTo(consumer)
                          .afterMax(timeout)
                          .hasCompleted()).isTrue();
    }

    private void testException(final Routine<String, String> routine, final String input,
            final String expected) {

        final TimeDuration timeout = seconds(1);

        try {

            routine.syncCall(input).afterMax(timeout).all();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : routine.syncCall(input).afterMax(timeout)) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            routine.asyncCall(input).afterMax(timeout).all();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : routine.asyncCall(input).afterMax(timeout)) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            routine.parallelCall(input).afterMax(timeout).all();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : routine.parallelCall(input).afterMax(timeout)) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            routine.syncInvoke().pass(input).result().afterMax(timeout).all();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : routine.syncInvoke().pass(input).result().afterMax(timeout)) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            routine.asyncInvoke().pass(input).result().afterMax(timeout).all();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : routine.asyncInvoke().pass(input).result().afterMax(timeout)) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            routine.parallelInvoke().pass(input).result().afterMax(timeout).all();

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : routine.parallelInvoke().pass(input).result().afterMax(timeout)) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }
    }

    private static class AbortInvocation extends FilterInvocation<String, String> {

        public void onInput(final String s, @NotNull final ResultChannel<String> result) {

            result.orderByCall().orderByDelay();
            assertThat(result.isOpen()).isTrue();
            assertThat(result.abort(new IllegalArgumentException(s))).isTrue();
            assertThat(result.abort()).isFalse();
            assertThat(result.isOpen()).isFalse();

            try {

                result.orderByCall();

                fail();

            } catch (final InvocationException ignored) {

            }

            try {

                result.orderByDelay();

                fail();

            } catch (final InvocationException ignored) {

            }

            try {

                result.pass(s);

                fail();

            } catch (final InvocationException ignored) {

            }

            try {

                result.pass(new String[]{s});

                fail();

            } catch (final InvocationException ignored) {

            }

            try {

                result.pass(Collections.singletonList(s));

                fail();

            } catch (final InvocationException ignored) {

            }
        }
    }

    private static class AbortInvocation2 extends FilterInvocation<String, String> {

        public void onInput(final String s, @NotNull final ResultChannel<String> result) {

            assertThat(result.abort()).isTrue();
            assertThat(result.abort(new IllegalArgumentException(s))).isFalse();
        }
    }

    private static class AllInvocation extends FilterInvocation<Object, Object> {

        public void onInput(final Object o, @NotNull final ResultChannel<Object> result) {

            JRoutineCore.on(factoryOf(DelayedInvocation.class, millis(100)))
                        .asyncCall("test")
                        .afterMax(seconds(1))
                        .all();
        }
    }

    private static class CheckCompleteInvocation extends FilterInvocation<Object, Object> {

        public void onInput(final Object o, @NotNull final ResultChannel<Object> result) {

            JRoutineCore.on(factoryOf(DelayedInvocation.class, millis(100)))
                        .asyncCall("test")
                        .afterMax(seconds(1))
                        .hasCompleted();
        }
    }

    private static class CloseInvocation extends FilterInvocation<String, String> {

        private final AtomicBoolean mIsFailed;

        private final Semaphore mSemaphore;

        private CloseInvocation(@NotNull final Semaphore semaphore,
                @NotNull final AtomicBoolean isFailed) {

            mSemaphore = semaphore;
            mIsFailed = isFailed;
        }

        public void onInput(final String s, @NotNull final ResultChannel<String> result) {

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
        public void onInput(final String s, @NotNull final ResultChannel<String> result) {

            result.now().pass(s).after(mDelay).abort();
        }
    }

    private static class DelayedChannelInvocation extends TemplateInvocation<String, String> {

        private final TimeDuration mDelay;

        private final Routine<String, String> mRoutine;

        private boolean mFlag;

        public DelayedChannelInvocation(final TimeDuration delay) {

            mDelay = delay;
            mRoutine = JRoutineCore.on(factoryOf(DelayedInvocation.class, TimeDuration.ZERO))
                                   .buildRoutine();
        }

        @Override
        public void onInput(final String s, @NotNull final ResultChannel<String> result) {

            if (mFlag) {

                result.after(mDelay).pass((OutputChannel<String>) null);

            } else {

                result.after(mDelay.time, mDelay.unit).pass((OutputChannel<String>) null);
            }

            result.pass(mRoutine.asyncCall(s));

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
        public void onInput(final String s, @NotNull final ResultChannel<String> result) {

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
        public void onInput(final String s, @NotNull final ResultChannel<String> result) {

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
        public void onResult(@NotNull final ResultChannel<String> result) {

            final ArrayList<String> list = mList;
            result.after(mDelay).pass(list);
            list.clear();
        }
    }

    private static class ExceptionRoutine extends AbstractRoutine<Object, Object> {

        protected ExceptionRoutine(@NotNull final InvocationConfiguration configuration) {

            super(configuration);
        }

        @NotNull
        @Override
        protected Invocation<Object, Object> newInvocation(@NotNull final InvocationType type) {

            throw new IllegalStateException();
        }
    }

    private static class HasNextInvocation extends FilterInvocation<Object, Object> {

        public void onInput(final Object o, @NotNull final ResultChannel<Object> result) {

            JRoutineCore.on(factoryOf(DelayedInvocation.class, millis(100)))
                        .asyncCall("test")
                        .afterMax(seconds(1))
                        .iterator()
                        .hasNext();
        }
    }

    private static class InputArrayRunnerDeadlock extends FilterInvocation<String, String> {

        public void onInput(final String s, @NotNull final ResultChannel<String> result) {

            JRoutineCore.on(PassingInvocation.<String>factoryOf())
                        .withInvocations()
                        .withInputMaxSize(1)
                        .withInputMaxDelay(TimeDuration.INFINITY)
                        .getConfigured()
                        .asyncInvoke()
                        .after(millis(500))
                        .pass(s)
                        .now()
                        .pass(new String[]{s})
                        .result()
                        .all();
        }
    }

    private static class InputConsumerRunnerDeadlock extends FilterInvocation<String, String> {

        public void onInput(final String s, @NotNull final ResultChannel<String> result) {

            final IOChannel<String> channel = JRoutineCore.io().buildChannel();
            result.pass(JRoutineCore.on(PassingInvocation.<String>factoryOf())
                                    .withInvocations()
                                    .withInputMaxSize(1)
                                    .withInputMaxDelay(TimeDuration.INFINITY)
                                    .getConfigured()
                                    .asyncInvoke()
                                    .after(millis(500))
                                    .pass(channel)
                                    .result());
            channel.pass(s, s).close();
        }
    }

    private static class InputListRunnerDeadlock extends FilterInvocation<String, String> {

        public void onInput(final String s, @NotNull final ResultChannel<String> result) {

            JRoutineCore.on(PassingInvocation.<String>factoryOf())
                        .withInvocations()
                        .withInputMaxSize(1)
                        .withInputMaxDelay(TimeDuration.INFINITY)
                        .getConfigured()
                        .asyncInvoke()
                        .after(millis(500))
                        .pass(s)
                        .now()
                        .pass(Collections.singletonList(s))
                        .result()
                        .all();
        }
    }

    private static class InputRunnerDeadlock extends FilterInvocation<String, String> {

        public void onInput(final String s, @NotNull final ResultChannel<String> result) {

            JRoutineCore.on(PassingInvocation.<String>factoryOf())
                        .withInvocations()
                        .withInputMaxSize(1)
                        .withInputMaxDelay(TimeDuration.INFINITY)
                        .getConfigured()
                        .asyncInvoke()
                        .after(millis(500))
                        .pass(s)
                        .now()
                        .pass(s)
                        .result()
                        .all();
        }
    }

    private static class NextInvocation extends FilterInvocation<Object, Object> {

        public void onInput(final Object o, @NotNull final ResultChannel<Object> result) {

            JRoutineCore.on(factoryOf(DelayedInvocation.class, millis(100)))
                        .parallelCall("test")
                        .afterMax(seconds(1))
                        .iterator()
                        .next();
        }
    }

    private static class NullRoutine extends AbstractRoutine<Object, Object> {

        protected NullRoutine(@NotNull final InvocationConfiguration configuration) {

            super(configuration);
        }

        @NotNull
        @Override
        @SuppressWarnings("ConstantConditions")
        protected Invocation<Object, Object> newInvocation(@NotNull final InvocationType type) {

            return null;
        }
    }

    private static class ResultArrayRunnerDeadlock extends FilterInvocation<String, String> {

        public void onInput(final String s, @NotNull final ResultChannel<String> result) {

            result.after(millis(500)).pass(s).after(millis(100)).pass(new String[]{s});
        }
    }

    private static class ResultListRunnerDeadlock extends FilterInvocation<String, String> {

        public void onInput(final String s, @NotNull final ResultChannel<String> result) {

            result.after(millis(500)).pass(s).after(millis(100)).pass(Collections.singletonList(s));
        }
    }

    private static class ResultRunnerDeadlock extends FilterInvocation<String, String> {

        public void onInput(final String s, @NotNull final ResultChannel<String> result) {

            result.after(millis(500)).pass(s).after(millis(100)).pass(s);
        }
    }

    private static class SleepCommand extends CommandInvocation<Void> {

        public void onResult(@NotNull final ResultChannel<Void> result) throws Exception {

            seconds(1).sleepAtLeast();
        }
    }

    private static class SleepInvocation extends FilterInvocation<Object, Object> {

        private final TimeDuration mSleepDuration;

        private SleepInvocation(@NotNull final TimeDuration sleepDuration) {

            mSleepDuration = sleepDuration;
        }

        public void onInput(final Object input, @NotNull final ResultChannel<Object> result) {

            try {

                mSleepDuration.sleepAtLeast();

            } catch (final InterruptedException e) {

                throw new InvocationInterruptedException(e);
            }

            result.pass(input);
        }
    }

    private static class SquareInvocation extends FilterInvocation<Integer, Integer> {

        public void onInput(final Integer integer, @NotNull final ResultChannel<Integer> result) {

            final int input = integer;
            result.pass(input * input);
        }
    }

    private static class TestAbortHandler implements AbortHandler {

        public void onAbort(@NotNull final RoutineException reason, final long delay,
                @NotNull final TimeUnit timeUnit) {

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
        public void onInput(final String input, @NotNull final ResultChannel<String> result) {

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
        public void onTerminate() throws Exception {

            super.onTerminate();
            throw new IllegalArgumentException("test");
        }
    }

    private static class TestDestroyDiscardException extends TestDestroyException {

        @Override
        public void onTerminate() throws Exception {

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
        public void onTerminate() throws Exception {

            super.onTerminate();
            throw new IllegalArgumentException("test");
        }
    }

    private static class TestInputIterator implements InputIterator<Object> {

        @NotNull
        public RoutineException getAbortException() {

            return new RoutineException();
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

        public void create(@NotNull final InvocationObserver<Object, Object> observer) {

            observer.onCreate(new TemplateInvocation<Object, Object>() {});
        }

        public void discard(@NotNull final Invocation<Object, Object> invocation) {

        }

        public void recycle(@NotNull final Invocation<Object, Object> invocation) {

        }
    }

    private static class TestLifecycle extends TemplateInvocation<String, String> {

        private static boolean sActive;

        private static boolean sIsError;

        @Override
        public void onInput(final String input, @NotNull final ResultChannel<String> result) {

            result.after(millis(1000)).pass(input);
        }

        @Override
        public void onAbort(@NotNull final RoutineException reason) {

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

        public void cancel(@NotNull final Execution execution) {

        }

        public boolean isExecutionThread() {

            return true;
        }

        public void run(@NotNull final Execution execution, final long delay,
                @NotNull final TimeUnit timeUnit) {

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
