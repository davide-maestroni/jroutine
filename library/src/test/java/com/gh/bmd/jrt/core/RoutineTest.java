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
import com.gh.bmd.jrt.channel.ExecutionTimeoutException;
import com.gh.bmd.jrt.channel.InputChannel;
import com.gh.bmd.jrt.channel.InputTimeoutException;
import com.gh.bmd.jrt.channel.InvocationChannel;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.OutputConsumer;
import com.gh.bmd.jrt.channel.OutputTimeoutException;
import com.gh.bmd.jrt.channel.ResultChannel;
import com.gh.bmd.jrt.channel.RoutineException;
import com.gh.bmd.jrt.channel.TemplateOutputConsumer;
import com.gh.bmd.jrt.channel.TransportChannel;
import com.gh.bmd.jrt.core.DefaultExecution.InputIterator;
import com.gh.bmd.jrt.core.DefaultInvocationChannel.InvocationManager;
import com.gh.bmd.jrt.core.DefaultInvocationChannel.InvocationObserver;
import com.gh.bmd.jrt.core.DefaultResultChannel.AbortHandler;
import com.gh.bmd.jrt.invocation.DelegatingInvocation;
import com.gh.bmd.jrt.invocation.DelegatingInvocation.DelegationType;
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
import java.util.Collections;
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
import static com.gh.bmd.jrt.core.InvocationTarget.targetClass;
import static com.gh.bmd.jrt.core.InvocationTarget.targetObject;
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

        assertThat(channel.checkComplete()).isTrue();
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

        assertThat(channel1.checkComplete()).isTrue();
        assertThat(channel1.isOpen()).isFalse();

        try {

            JRoutine.on(new AbortInvocation())
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

            JRoutine.on(new AbortInvocation2())
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
        assertThat(JRoutine.on(new CloseInvocation(semaphore, isFailed))
                           .invocations()
                           .withLogLevel(LogLevel.SILENT)
                           .set()
                           .asyncCall("test")
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
                    public void onAbort(@Nullable final RoutineException reason) {

                        abortReason.set(reason);
                        semaphore.release();
                    }
                };

        final Routine<String, String> routine =
                JRoutine.on(factoryOf(abortInvocation, this, abortReason, semaphore))
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
        final Routine<Object, Object> routine1 = JRoutine.on(PassingInvocation.factoryOf())
                                                         .invocations()
                                                         .withAsyncRunner(runner)
                                                         .withPriority(
                                                                 AgingPriority.NORMAL_PRIORITY)
                                                         .set()
                                                         .buildRoutine();
        final Routine<Object, Object> routine2 = JRoutine.on(PassingInvocation.factoryOf())
                                                         .invocations()
                                                         .withAsyncRunner(runner)
                                                         .withPriority(AgingPriority.HIGH_PRIORITY)
                                                         .set()
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
        final OutputChannel<Object> channel1 = JRoutine.on(PassingInvocation.factoryOf())
                                                       .asyncInvoke()
                                                       .after(seconds(1))
                                                       .pass("test1")
                                                       .result();

        channel1.passTo(consumer);
        assertThat(channel1.isBound()).isTrue();
        assertThat(consumer.isOutput()).isFalse();

        final OutputChannel<Object> channel2 =
                JRoutine.on(PassingInvocation.factoryOf()).syncInvoke().pass("test2").result();

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
                    protected void onCall(@Nonnull final List<? extends Integer> integers,
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
                    protected void onCall(@Nonnull final List<? extends Integer> integers,
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
                    public void onAbort(@Nullable final RoutineException reason) {

                        mChannel.abort(reason);
                    }

                    @Override
                    public void onInitialize() {

                        mChannel = sumRoutine.asyncInvoke();
                    }

                    @Override
                    public void onInput(final Integer integer,
                            @Nonnull final ResultChannel<Integer> result) {

                        squareRoutine.asyncCall(integer).passTo(mChannel);
                    }

                    @Override
                    public void onResult(@Nonnull final ResultChannel<Integer> result) {

                        result.pass(mChannel.result());
                    }
                };

        final Routine<Integer, Integer> squareSumRoutine =
                JRoutine.on(factoryOf(invokeSquareSum, this, sumRoutine, squareRoutine))
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

        final Routine<Object, Object> routine2 = JRoutine.on(new AllInvocation()).buildRoutine();

        try {

            routine2.asyncCall("test").eventually().all();

            fail();

        } catch (final DeadlockException ignored) {

        }
    }

    @Test
    public void testDeadlockOnCheckComplete() {

        final Routine<Object, Object> routine1 =
                JRoutine.on(new CheckCompleteInvocation()).buildRoutine();

        try {

            routine1.asyncCall("test").eventually().all();

            fail();

        } catch (final DeadlockException ignored) {

        }
    }

    @Test
    public void testDeadlockOnHasNext() {

        final Routine<Object, Object> routine3 =
                JRoutine.on(new HasNextInvocation()).buildRoutine();

        try {

            routine3.asyncCall("test").eventually().all();

            fail();

        } catch (final DeadlockException ignored) {

        }
    }

    @Test
    public void testDeadlockOnNext() {

        final Routine<Object, Object> routine4 = JRoutine.on(new NextInvocation()).buildRoutine();

        try {

            routine4.asyncCall("test").eventually().all();

            fail();

        } catch (final DeadlockException ignored) {

        }
    }

    @Test
    public void testDelay() {

        long startTime = System.currentTimeMillis();

        final InvocationChannel<String, String> channel =
                JRoutine.on(factoryOf(DelayedInvocation.class, millis(10))).asyncInvoke();
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
                        .invocations()
                        .withInputOrder(OrderType.BY_CALL)
                        .withOutputOrder(OrderType.BY_CALL)
                        .set()
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
                JRoutine.on(factoryOf(DelayedListInvocation.class, millis(10), 2)).asyncInvoke();
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
                                                               .set();
        final InvocationChannel<String, String> channel3 =
                JRoutine.on(factoryOf(DelayedListInvocation.class, millis(10), 2))
                        .invocations()
                        .with(configuration)
                        .set()
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
                JRoutine.on(factoryOf(DelayedListInvocation.class, TimeDuration.ZERO, 2))
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
                JRoutine.on(factoryOf(DelayedListInvocation.class, TimeDuration.ZERO, 2))
                        .invocations()
                        .with(configuration)
                        .set()
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
                JRoutine.on(factoryOf(DelayedChannelInvocation.class, millis(10))).asyncInvoke();
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
                        .invocations()
                        .with(configuration)
                        .set()
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
                JRoutine.on(factoryOf(DelayedChannelInvocation.class, TimeDuration.ZERO))
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
                JRoutine.on(factoryOf(DelayedChannelInvocation.class, TimeDuration.ZERO))
                        .invocations()
                        .with(configuration)
                        .set()
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
                JRoutine.on(PassingInvocation.<String>factoryOf()).buildRoutine();

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
                JRoutine.on(factoryOf(DelayedAbortInvocation.class, millis(200))).buildRoutine();

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
                JRoutine.on(PassingInvocation.factoryOf()).buildRoutine();
        final Routine<Object, Object> routine2 =
                JRoutine.on(PassingInvocation.factoryOf()).buildRoutine();

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
                JRoutine.on(PassingInvocation.factoryOf()).buildRoutine();
        final Routine<Object, Object> routine2 =
                JRoutine.on(DelegatingInvocation.factoryFrom(routine1, DelegationType.SYNCHRONOUS))
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
                JRoutine.on(factoryOf(TestDestroy.class)).buildRoutine();
        final Routine<String, String> routine4 =
                JRoutine.on(DelegatingInvocation.factoryFrom(routine3, DelegationType.ASYNCHRONOUS))
                        .buildRoutine();

        assertThat(routine4.asyncCall("test4").afterMax(timeout).all()).containsExactly("test4");
        routine4.purge();
        assertThat(TestDestroy.getInstanceCount()).isZero();
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testDelegationError() {

        try {

            DelegatingInvocation.factoryFrom(null, DelegationType.SYNCHRONOUS);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            DelegatingInvocation.factoryFrom(
                    JRoutine.on(PassingInvocation.factoryOf()).buildRoutine(), null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testDestroyAsync() {

        final TimeDuration timeout = seconds(1);
        final Routine<String, String> routine1 = JRoutine.on(factoryOf(TestDestroy.class))
                                                         .invocations()
                                                         .withCoreInstances(0)
                                                         .set()
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

        final Routine<String, String> routine2 = JRoutine.on(factoryOf(TestDiscardException.class))
                                                         .invocations()
                                                         .withCoreInstances(0)
                                                         .set()
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
                JRoutine.on(factoryOf(TestDestroyDiscard.class)).buildRoutine();
        assertThat(routine3.syncCall("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .checkComplete()).isTrue();
        assertThat(routine3.parallelCall("1", "2", "3", "4", "5").afterMax(timeout).checkComplete())
                .isTrue();
        assertThat(routine3.asyncCall("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .checkComplete()).isTrue();
        assertThat(TestDestroy.getInstanceCount()).isZero();

        final Routine<String, String> routine4 =
                JRoutine.on(factoryOf(TestDestroyDiscardException.class)).buildRoutine();
        assertThat(routine4.syncCall("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .checkComplete()).isTrue();
        assertThat(routine4.parallelCall("1", "2", "3", "4", "5").afterMax(timeout).checkComplete())
                .isTrue();
        assertThat(routine4.asyncCall("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .checkComplete()).isTrue();
        assertThat(TestDestroy.getInstanceCount()).isZero();

        final Routine<String, String> routine5 =
                JRoutine.on(factoryOf(TestDestroy.class)).buildRoutine();
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
                JRoutine.on(factoryOf(TestDestroyException.class)).buildRoutine();

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
        final Routine<String, String> routine1 = JRoutine.on(factoryOf(TestDestroy.class))
                                                         .invocations()
                                                         .withCoreInstances(0)
                                                         .set()
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

        final Routine<String, String> routine2 = JRoutine.on(factoryOf(TestDiscardException.class))
                                                         .invocations()
                                                         .withCoreInstances(0)
                                                         .set()
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
                JRoutine.on(factoryOf(TestDestroyDiscard.class)).buildRoutine();
        assertThat(routine3.parallelCall("1", "2", "3", "4", "5").afterMax(timeout).checkComplete())
                .isTrue();
        assertThat(routine3.asyncCall("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .checkComplete()).isTrue();
        assertThat(routine3.syncCall("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .checkComplete()).isTrue();
        assertThat(TestDestroy.getInstanceCount()).isZero();

        final Routine<String, String> routine4 =
                JRoutine.on(factoryOf(TestDestroyDiscardException.class)).buildRoutine();
        assertThat(routine4.parallelCall("1", "2", "3", "4", "5").afterMax(timeout).checkComplete())
                .isTrue();
        assertThat(routine4.asyncCall("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .checkComplete()).isTrue();
        assertThat(routine4.syncCall("1", "2", "3", "4", "5")
                           .afterMax(timeout)
                           .checkComplete()).isTrue();
        assertThat(TestDestroy.getInstanceCount()).isZero();

        final Routine<String, String> routine5 =
                JRoutine.on(factoryOf(TestDestroy.class)).buildRoutine();
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
                JRoutine.on(factoryOf(TestDestroyException.class)).buildRoutine();

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
    @SuppressWarnings("ConstantConditions")
    public void testError() {

        try {

            JRoutine.on(factoryOf(ConstructorException.class))
                    .invocations()
                    .withLogLevel(LogLevel.SILENT)
                    .set()
                    .syncCall()
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

            JRoutine.on(PassingInvocation.<String>factoryOf())
                    .invocations()
                    .withInputMaxSize(1)
                    .withInputTimeout(TimeDuration.ZERO)
                    .set()
                    .asyncCall("test1", "test2")
                    .all();

            fail();

        } catch (final InputTimeoutException ignored) {

        }

        try {

            JRoutine.on(PassingInvocation.<String>factoryOf())
                    .invocations()
                    .withInputMaxSize(1)
                    .withInputTimeout(TimeDuration.ZERO)
                    .set()
                    .asyncCall(Arrays.asList("test1", "test2"))
                    .all();

            fail();

        } catch (final InputTimeoutException ignored) {

        }
    }

    @Test
    public void testInputRunnerDeadlock() {

        try {

            JRoutine.on(new InputRunnerDeadlock()).asyncCall("test").eventually().all();

            fail();

        } catch (final DeadlockException ignored) {

        }

        try {

            JRoutine.on(new InputListRunnerDeadlock()).asyncCall("test").eventually().all();

            fail();

        } catch (final DeadlockException ignored) {

        }

        try {

            JRoutine.on(new InputArrayRunnerDeadlock()).asyncCall("test").eventually().all();

            fail();

        } catch (final DeadlockException ignored) {

        }

        try {

            JRoutine.on(new InputConsumerRunnerDeadlock()).asyncCall("test").eventually().all();

            fail();

        } catch (final DeadlockException ignored) {

        }
    }

    @Test
    public void testInputTimeout() {

        try {

            JRoutine.on(PassingInvocation.factoryOf())
                    .invocations()
                    .withInputOrder(OrderType.BY_CALL)
                    .withInputMaxSize(1)
                    .withInputTimeout(TimeDuration.ZERO)
                    .set()
                    .asyncInvoke()
                    .orderByChance()
                    .orderByDelay()
                    .orderByCall()
                    .after(millis(100))
                    .pass("test1")
                    .now()
                    .pass("test2")
                    .result()
                    .all();

            fail();

        } catch (final InputTimeoutException ignored) {

        }

        assertThat(JRoutine.on(PassingInvocation.factoryOf())
                           .invocations()
                           .withInputOrder(OrderType.BY_CALL)
                           .withInputMaxSize(1)
                           .withInputTimeout(millis(1000))
                           .set()
                           .asyncInvoke()
                           .orderByCall()
                           .after(millis(100))
                           .pass("test1")
                           .now()
                           .pass("test2")
                           .result()
                           .eventually()
                           .all()).containsExactly("test1", "test2");

        assertThat(JRoutine.on(PassingInvocation.factoryOf())
                           .invocations()
                           .withInputOrder(OrderType.BY_CALL)
                           .withInputMaxSize(1)
                           .withInputTimeout(millis(1000))
                           .set()
                           .asyncInvoke()
                           .orderByCall()
                           .after(millis(100))
                           .pass("test1")
                           .now()
                           .pass(new Object[]{"test2"})
                           .result()
                           .eventually()
                           .all()).containsExactly("test1", "test2");

        assertThat(JRoutine.on(PassingInvocation.factoryOf())
                           .invocations()
                           .withInputOrder(OrderType.BY_CALL)
                           .withInputMaxSize(1)
                           .withInputTimeout(millis(1000))
                           .set()
                           .asyncInvoke()
                           .orderByCall()
                           .after(millis(100))
                           .pass("test1")
                           .now()
                           .pass(Collections.singletonList("test2"))
                           .result()
                           .eventually()
                           .all()).containsExactly("test1", "test2");

        final TransportChannel<Object> channel = JRoutine.transport().buildChannel();
        channel.pass("test2").close();
        assertThat(JRoutine.on(PassingInvocation.factoryOf())
                           .invocations()
                           .withInputOrder(OrderType.BY_CALL)
                           .withInputMaxSize(1)
                           .withInputTimeout(millis(1000))
                           .set()
                           .asyncInvoke()
                           .orderByCall()
                           .after(millis(100))
                           .pass("test1")
                           .now()
                           .pass(channel)
                           .result()
                           .eventually()
                           .all()).containsExactly("test1", "test2");
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
                JRoutine.on(factoryOf(TestLifecycle.class)).asyncCall("test");

        Thread.sleep(500);

        outputChannel.abort();
        outputChannel.afterMax(INFINITY).checkComplete();
        assertThat(TestLifecycle.sIsError).isFalse();
    }

    @Test
    public void testInvocationNotAvailable() throws InterruptedException {

        final Routine<Void, Void> routine = JRoutine.on(new SleepInvocation())
                                                    .invocations()
                                                    .withMaxInstances(1)
                                                    .set()
                                                    .buildRoutine();

        routine.asyncCall();
        millis(100).sleepAtLeast();

        try {

            routine.asyncCall().eventually().next();

            fail();

        } catch (final InvocationDeadlockException ignored) {

        }
    }

    @Test
    public void testMethod() throws NoSuchMethodException {

        final TimeDuration timeout = seconds(1);
        final TestClass test = new TestClass();
        assertThat(JRoutine.on(targetObject(test))
                           .method(TestClass.class.getMethod("getOne"))
                           .syncCall()
                           .afterMax(timeout)
                           .all()).containsExactly(1);
        assertThat(
                JRoutine.on(targetObject(test)).method("getOne").syncCall().afterMax(timeout).all())
                .containsExactly(1);
        assertThat(JRoutine.on(targetObject(test))
                           .aliasMethod(TestClass.GET)
                           .syncCall()
                           .afterMax(timeout)
                           .all()).containsExactly(1);
        assertThat(JRoutine.on(targetClass(TestClass.class))
                           .aliasMethod(TestClass.STATIC_GET)
                           .syncCall(3)
                           .afterMax(timeout)
                           .all()).containsExactly(3);
        assertThat(JRoutine.on(targetClass(TestClass.class))
                           .aliasMethod("sget")
                           .asyncCall(-3)
                           .afterMax(timeout)
                           .all()).containsExactly(-3);
        assertThat(JRoutine.on(targetClass(TestClass.class))
                           .method("get", int.class)
                           .parallelCall(17)
                           .afterMax(timeout)
                           .all()).containsExactly(17);

        assertThat(JRoutine.on(targetObject(test))
                           .buildProxy(TestInterface.class)
                           .getInt(2)).isEqualTo(2);

        try {

            JRoutine.on(targetClass(TestClass.class))
                    .aliasMethod("sget")
                    .asyncCall()
                    .afterMax(timeout)
                    .all();

            fail();

        } catch (final InvocationException ignored) {

        }

        try {

            JRoutine.on(targetClass(TestClass.class)).aliasMethod("take");

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        assertThat(JRoutine.on(targetObject(test))
                           .invocations()
                           .withExecutionTimeout(timeout)
                           .set()
                           .buildProxy(TestInterfaceAsync.class)
                           .take(77)).isEqualTo(77);
        assertThat(JRoutine.on(targetObject(test))
                           .buildProxy(TestInterfaceAsync.class)
                           .getOne()
                           .afterMax(timeout)
                           .next()).isEqualTo(1);

        final TestInterfaceAsync testInterfaceAsync = JRoutine.on(targetObject(test))
                                                              .invocations()
                                                              .withExecutionTimeout(1,
                                                                                    TimeUnit.SECONDS)
                                                              .set()
                                                              .buildProxy(TestInterfaceAsync.class);
        assertThat(testInterfaceAsync.getInt(testInterfaceAsync.getOne())).isEqualTo(1);
    }

    @Test
    public void testOutputDeadlock() {

        final Routine<String, String> routine1 =
                JRoutine.on(factoryOf(new FunctionInvocation<String, String>() {

                    @Override
                    protected void onCall(@Nonnull final List<? extends String> strings,
                            @Nonnull final ResultChannel<String> result) {

                        result.pass(strings);
                    }
                }, this))
                        .invocations()
                        .withOutputMaxSize(1)
                        .withOutputTimeout(TimeDuration.ZERO)
                        .set()
                        .buildRoutine();

        try {

            routine1.asyncCall("test1", "test2").eventually().all();

            fail();

        } catch (final OutputTimeoutException ignored) {

        }

        final Routine<String, String> routine2 =
                JRoutine.on(factoryOf(new FunctionInvocation<String, String>() {

                    @Override
                    protected void onCall(@Nonnull final List<? extends String> strings,
                            @Nonnull final ResultChannel<String> result) {

                        result.pass(strings.toArray(new String[strings.size()]));
                    }
                }, this))
                        .invocations()
                        .withOutputMaxSize(1)
                        .withOutputTimeout(TimeDuration.ZERO)
                        .set()
                        .buildRoutine();

        try {

            routine2.asyncCall("test1", "test2").eventually().all();

            fail();

        } catch (final OutputTimeoutException ignored) {

        }
    }

    @Test
    public void testOutputTimeout() throws InterruptedException {

        final Routine<String, String> routine = JRoutine.on(PassingInvocation.<String>factoryOf())
                                                        .invocations()
                                                        .withOutputMaxSize(1)
                                                        .withOutputTimeout(TimeDuration.ZERO)
                                                        .set()
                                                        .buildRoutine();

        try {

            final OutputChannel<String> outputChannel =
                    routine.asyncCall("test1", "test2").eventually();
            outputChannel.checkComplete();
            outputChannel.all();

            fail();

        } catch (final OutputTimeoutException ignored) {

        }

        final TransportChannel<String> channel1 = JRoutine.transport()
                                                          .channels()
                                                          .withChannelMaxSize(1)
                                                          .withChannelTimeout(millis(1000))
                                                          .set()
                                                          .buildChannel();
        new Thread() {

            @Override
            public void run() {

                channel1.pass("test1").pass("test2").close();
            }
        }.start();
        millis(100).sleepAtLeast();
        assertThat(channel1.eventually().all()).containsOnly("test1", "test2");

        final TransportChannel<String> channel2 = JRoutine.transport()
                                                          .channels()
                                                          .withChannelMaxSize(1)
                                                          .withChannelTimeout(millis(1000))
                                                          .set()
                                                          .buildChannel();
        new Thread() {

            @Override
            public void run() {

                channel2.pass("test1").pass(new String[]{"test2"}).close();
            }
        }.start();
        millis(100).sleepAtLeast();
        assertThat(channel2.eventually().all()).containsOnly("test1", "test2");

        final TransportChannel<String> channel3 = JRoutine.transport()
                                                          .channels()
                                                          .withChannelMaxSize(1)
                                                          .withChannelTimeout(millis(1000))
                                                          .set()
                                                          .buildChannel();
        new Thread() {

            @Override
            public void run() {

                channel3.pass("test1").pass(Collections.singletonList("test2")).close();
            }
        }.start();
        millis(100).sleepAtLeast();
        assertThat(channel3.eventually().all()).containsOnly("test1", "test2");

        final TransportChannel<String> channel4 = JRoutine.transport()
                                                          .channels()
                                                          .withChannelMaxSize(1)
                                                          .withChannelTimeout(millis(1000))
                                                          .set()
                                                          .buildChannel();
        new Thread() {

            @Override
            public void run() {

                final TransportChannel<String> channel = JRoutine.transport().buildChannel();
                channel.pass("test1", "test2").close();
                channel4.pass(channel).close();
            }
        }.start();
        millis(100).sleepAtLeast();
        assertThat(channel4.eventually().all()).containsOnly("test1", "test2");
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
                           .asyncCall("test")
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
                        .invocations()
                        .withLogLevel(LogLevel.SILENT)
                        .set()
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
                        .invocations()
                        .withLogLevel(LogLevel.SILENT)
                        .set()
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

            JRoutine.on(new ResultRunnerDeadlock())
                    .invocations()
                    .withOutputMaxSize(1)
                    .withOutputTimeout(millis(500))
                    .set()
                    .asyncCall("test")
                    .eventually()
                    .all();

            fail();

        } catch (final DeadlockException ignored) {

        }

        try {

            JRoutine.on(new ResultListRunnerDeadlock())
                    .invocations()
                    .withOutputMaxSize(1)
                    .withOutputTimeout(millis(500))
                    .set()
                    .asyncCall("test")
                    .eventually()
                    .all();

            fail();

        } catch (final DeadlockException ignored) {

        }

        try {

            JRoutine.on(new ResultArrayRunnerDeadlock())
                    .invocations()
                    .withOutputMaxSize(1)
                    .withOutputTimeout(millis(500))
                    .set()
                    .asyncCall("test")
                    .eventually()
                    .all();

            fail();

        } catch (final DeadlockException ignored) {

        }

        try {

            JRoutine.on(new ResultConsumerRunnerDeadlock())
                    .invocations()
                    .withOutputMaxSize(1)
                    .withOutputTimeout(millis(500))
                    .set()
                    .asyncCall("test")
                    .eventually()
                    .all();

            fail();

        } catch (final DeadlockException ignored) {

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

        assertThat(JRoutine.on(factoryOf(new ClassToken<PassingInvocation<String>>() {}))
                           .invocations()
                           .withSyncRunner(Runners.sequentialRunner())
                           .withAsyncRunner(Runners.poolRunner())
                           .withCoreInstances(0)
                           .withMaxInstances(1)
                           .withInputMaxSize(2)
                           .withInputTimeout(1, TimeUnit.SECONDS)
                           .withOutputMaxSize(2)
                           .withOutputTimeout(1, TimeUnit.SECONDS)
                           .withOutputOrder(OrderType.BY_CALL)
                           .set()
                           .syncCall("test1", "test2")
                           .all()).containsExactly("test1", "test2");

        assertThat(JRoutine.on(factoryOf(new ClassToken<PassingInvocation<String>>() {}))
                           .invocations()
                           .withSyncRunner(Runners.queuedRunner())
                           .withAsyncRunner(Runners.poolRunner())
                           .withCoreInstances(0)
                           .withMaxInstances(1)
                           .withInputMaxSize(2)
                           .withInputTimeout(TimeDuration.ZERO)
                           .withOutputMaxSize(2)
                           .withOutputTimeout(TimeDuration.ZERO)
                           .withOutputOrder(OrderType.BY_CALL)
                           .set()
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
                    protected void onCall(@Nonnull final List<? extends Integer> integers,
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

        assertThat(sumRoutine.syncCall(1, 2, 3, 4).afterMax(timeout).all()).containsExactly(10);
        assertThat(sumRoutine.asyncCall(1, 2, 3, 4).afterMax(timeout).all()).containsExactly(10);
    }

    @Test
    public void testTimeoutActions() {

        final Routine<String, String> routine1 =
                JRoutine.on(factoryOf(DelayedInvocation.class, seconds(1)))
                        .invocations()
                        .withExecutionTimeoutAction(TimeoutActionType.ABORT)
                        .set()
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

        assertThat(routine1.asyncCall("test1").checkComplete()).isFalse();

        final Routine<String, String> routine2 =
                JRoutine.on(factoryOf(DelayedInvocation.class, seconds(1)))
                        .invocations()
                        .withExecutionTimeoutAction(TimeoutActionType.ABORT)
                        .withExecutionTimeout(millis(10))
                        .set()
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

        assertThat(routine2.asyncCall("test1").checkComplete()).isFalse();

        final Routine<String, String> routine3 =
                JRoutine.on(factoryOf(DelayedInvocation.class, seconds(1)))
                        .invocations()
                        .withExecutionTimeoutAction(TimeoutActionType.THROW)
                        .set()
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

        assertThat(channel3.checkComplete()).isFalse();

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

        assertThat(channel3.checkComplete()).isFalse();

        final Routine<String, String> routine4 =
                JRoutine.on(factoryOf(DelayedInvocation.class, seconds(1)))
                        .invocations()
                        .withExecutionTimeoutAction(TimeoutActionType.EXIT)
                        .set()
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
                JRoutine.on(factoryOf(DelayedInvocation.class, TimeDuration.ZERO)).buildRoutine();

        assertThat(routine.syncCall(input)
                          .passTo(consumer)
                          .afterMax(timeout)
                          .checkComplete()).isTrue();
        assertThat(routine.asyncCall(input)
                          .passTo(consumer)
                          .afterMax(timeout)
                          .checkComplete()).isTrue();
        assertThat(routine.parallelCall(input)
                          .passTo(consumer)
                          .afterMax(timeout)
                          .checkComplete()).isTrue();
        assertThat(routine.syncInvoke()
                          .pass(input)
                          .result()
                          .passTo(consumer)
                          .afterMax(timeout)
                          .checkComplete()).isTrue();
        assertThat(routine.asyncInvoke()
                          .pass(input)
                          .result()
                          .passTo(consumer)
                          .afterMax(timeout)
                          .checkComplete()).isTrue();
        assertThat(routine.parallelInvoke()
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

            result.orderByCall().orderByDelay().orderByChance();
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

                result.orderByChance();

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

        public void onInput(final String s, @Nonnull final ResultChannel<String> result) {

            assertThat(result.abort()).isTrue();
            assertThat(result.abort(new IllegalArgumentException(s))).isFalse();
        }
    }

    private static class AllInvocation extends FilterInvocation<Object, Object> {

        public void onInput(final Object o, @Nonnull final ResultChannel<Object> result) {

            JRoutine.on(factoryOf(DelayedInvocation.class, millis(100)))
                    .asyncCall("test")
                    .eventually()
                    .all();
        }
    }

    private static class CheckCompleteInvocation extends FilterInvocation<Object, Object> {

        public void onInput(final Object o, @Nonnull final ResultChannel<Object> result) {

            JRoutine.on(factoryOf(DelayedInvocation.class, millis(100)))
                    .asyncCall("test")
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
                    .asyncCall("test")
                    .eventually()
                    .iterator()
                    .hasNext();
        }
    }

    private static class InputArrayRunnerDeadlock extends FilterInvocation<String, String> {

        public void onInput(final String s, @Nonnull final ResultChannel<String> result) {

            JRoutine.on(PassingInvocation.<String>factoryOf())
                    .invocations()
                    .withInputMaxSize(1)
                    .withInputTimeout(TimeDuration.INFINITY)
                    .set()
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

        public void onInput(final String s, @Nonnull final ResultChannel<String> result) {

            final TransportChannel<String> channel = JRoutine.transport().buildChannel();
            result.pass(JRoutine.on(PassingInvocation.<String>factoryOf())
                                .invocations()
                                .withInputMaxSize(1)
                                .withInputTimeout(TimeDuration.INFINITY)
                                .set()
                                .asyncInvoke()
                                .after(millis(500))
                                .pass(channel)
                                .result());
            channel.pass(s, s).close();
        }
    }

    private static class InputListRunnerDeadlock extends FilterInvocation<String, String> {

        public void onInput(final String s, @Nonnull final ResultChannel<String> result) {

            JRoutine.on(PassingInvocation.<String>factoryOf())
                    .invocations()
                    .withInputMaxSize(1)
                    .withInputTimeout(TimeDuration.INFINITY)
                    .set()
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

        public void onInput(final String s, @Nonnull final ResultChannel<String> result) {

            JRoutine.on(PassingInvocation.<String>factoryOf())
                    .invocations()
                    .withInputMaxSize(1)
                    .withInputTimeout(TimeDuration.INFINITY)
                    .set()
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

        public void onInput(final Object o, @Nonnull final ResultChannel<Object> result) {

            JRoutine.on(factoryOf(DelayedInvocation.class, millis(100)))
                    .asyncCall("test")
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

    private static class ResultArrayRunnerDeadlock extends FilterInvocation<String, String> {

        public void onInput(final String s, @Nonnull final ResultChannel<String> result) {

            result.after(millis(500)).pass(s).after(millis(100)).pass(new String[]{s});
        }
    }

    private static class ResultConsumerRunnerDeadlock extends FilterInvocation<String, String> {

        public void onInput(final String s, @Nonnull final ResultChannel<String> result) {

            final TransportChannel<String> channel = JRoutine.transport().buildChannel();
            result.after(millis(500)).pass(channel);
            channel.pass(s, s).close();
        }
    }

    private static class ResultListRunnerDeadlock extends FilterInvocation<String, String> {

        public void onInput(final String s, @Nonnull final ResultChannel<String> result) {

            result.after(millis(500)).pass(s).after(millis(100)).pass(Collections.singletonList(s));
        }
    }

    private static class ResultRunnerDeadlock extends FilterInvocation<String, String> {

        public void onInput(final String s, @Nonnull final ResultChannel<String> result) {

            result.after(millis(500)).pass(s).after(millis(100)).pass(s);
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

    private static class SquareInvocation extends FilterInvocation<Integer, Integer> {

        public void onInput(final Integer integer, @Nonnull final ResultChannel<Integer> result) {

            final int input = integer;
            result.pass(input * input);
        }
    }

    private static class TestAbortHandler implements AbortHandler {

        public void onAbort(@Nullable final RoutineException reason, final long delay,
                @Nonnull final TimeUnit timeUnit) {

        }
    }

    @SuppressWarnings("unused")
    private static class TestClass implements TestInterface {

        public static final String GET = "get";

        public static final String STATIC_GET = "sget";

        @Alias(STATIC_GET)
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
        public RoutineException getAbortException() {

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

        public void create(@Nonnull final InvocationObserver<Object, Object> observer) {

            observer.onCreate(new TemplateInvocation<Object, Object>() {});
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
        public void onAbort(@Nullable final RoutineException reason) {

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

        public void cancel(@Nonnull final Execution execution) {

        }

        public boolean isExecutionThread() {

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
