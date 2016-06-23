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

import com.github.dm.jrt.core.InvocationExecution.InputIterator;
import com.github.dm.jrt.core.ResultChannel.AbortHandler;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.InputDeadlockException;
import com.github.dm.jrt.core.channel.OutputConsumer;
import com.github.dm.jrt.core.channel.OutputDeadlockException;
import com.github.dm.jrt.core.channel.OutputTimeoutException;
import com.github.dm.jrt.core.channel.TemplateOutputConsumer;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.config.ChannelConfiguration.TimeoutActionType;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.AgingPriority;
import com.github.dm.jrt.core.error.DeadlockException;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.error.TimeoutException;
import com.github.dm.jrt.core.invocation.CallInvocation;
import com.github.dm.jrt.core.invocation.CommandInvocation;
import com.github.dm.jrt.core.invocation.IdentityInvocation;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationDeadlockException;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.InvocationInterruptedException;
import com.github.dm.jrt.core.invocation.MappingInvocation;
import com.github.dm.jrt.core.invocation.TemplateInvocation;
import com.github.dm.jrt.core.log.Log.Level;
import com.github.dm.jrt.core.log.Logger;
import com.github.dm.jrt.core.log.NullLog;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.runner.Execution;
import com.github.dm.jrt.core.runner.Runners;
import com.github.dm.jrt.core.runner.SyncRunner;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.core.util.UnitDuration;

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

import static com.github.dm.jrt.core.config.InvocationConfiguration.builder;
import static com.github.dm.jrt.core.invocation.InvocationFactory.factoryOf;
import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.core.util.UnitDuration.infinity;
import static com.github.dm.jrt.core.util.UnitDuration.millis;
import static com.github.dm.jrt.core.util.UnitDuration.seconds;
import static com.github.dm.jrt.core.util.UnitDuration.zero;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Routine unit tests.
 * <p>
 * Created by davide-maestroni on 09/09/2014.
 */
public class RoutineTest {

    @Test
    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored", "ConstantConditions"})
    public void testAbort() throws InterruptedException {
        final UnitDuration timeout = seconds(1);
        final Routine<String, String> routine =
                JRoutineCore.with(factoryOf(DelayedInvocation.class, millis(100))).buildRoutine();
        final Channel<String, String> channel = routine.async().pass("test1");
        assertThat(channel.isOpen()).isTrue();
        assertThat(channel.abort(new IllegalArgumentException("test1"))).isTrue();
        assertThat(channel.isOpen()).isFalse();

        final Channel<String, String> channel1 =
                routine.async().after(millis(10)).pass("test1").close();
        assertThat(channel1.isOpen()).isFalse();
        assertThat(channel1.after(timeout).hasCompleted()).isTrue();
        assertThat(channel1.abort()).isFalse();
        assertThat(channel1.isOpen()).isFalse();
        assertThat(channel1.after(timeout).hasNext()).isTrue();
        assertThat(channel1.after(timeout).all()).containsExactly("test1");
        assertThat(channel1.after(timeout).hasNext()).isFalse();

        final Channel<String, String> channel2 = routine.async().after(millis(10)).pass("test2");
        assertThat(channel2.isOpen()).isTrue();
        assertThat(channel2.abort(new IllegalArgumentException("test2"))).isTrue();
        assertThat(channel2.after(timeout).hasCompleted()).isTrue();
        assertThat(channel2.abort()).isFalse();
        assertThat(channel2.isOpen()).isFalse();
        try {
            channel2.after(timeout).all();
            fail();

        } catch (final AbortException ex) {
            assertThat(ex.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
            assertThat(ex.getCause().getMessage()).isEqualTo("test2");
        }

        try {
            channel2.throwError();

        } catch (final AbortException ex) {
            assertThat(ex.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
            assertThat(ex.getCause().getMessage()).isEqualTo("test2");
        }

        final RoutineException error = channel2.getError();
        assertThat(error.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(error.getCause().getMessage()).isEqualTo("test2");
        assertThat(channel2.hasCompleted()).isTrue();
        assertThat(channel2.isOpen()).isFalse();
        final Channel<String, String> channel3 =
                routine.async().after(millis(1000000)).pass("test2");
        assertThat(channel3.isOpen()).isTrue();
        assertThat(channel3.immediately().abort()).isTrue();
        assertThat(channel3.abort(new IllegalArgumentException("test2"))).isFalse();
        assertThat(channel3.isOpen()).isFalse();
        try {
            channel3.after(timeout).all();
            fail();

        } catch (final AbortException ex) {
            assertThat(ex.getCause()).isNull();
        }

        assertThat(channel3.hasCompleted()).isTrue();
        assertThat(channel3.isOpen()).isFalse();
        try {
            JRoutineCore.with(new AbortInvocation())
                        .async()
                        .after(millis(10))
                        .pass("test_abort")
                        .after(timeout)
                        .next();
            fail();

        } catch (final AbortException ex) {
            assertThat(ex.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
            assertThat(ex.getCause().getMessage()).isEqualTo("test_abort");
        }

        try {
            JRoutineCore.with(new AbortInvocation2())
                        .async()
                        .after(millis(10))
                        .pass("test_abort")
                        .after(timeout)
                        .next();
            fail();

        } catch (final AbortException ex) {
            assertThat(ex.getCause()).isNull();
        }

        final Semaphore semaphore = new Semaphore(0);
        final AtomicBoolean isFailed = new AtomicBoolean(false);
        assertThat(JRoutineCore.with(new CloseInvocation(semaphore, isFailed))
                               .invocationConfiguration()
                               .withLogLevel(Level.SILENT)
                               .applied()
                               .async("test")
                               .after(timeout)
                               .all()).isEmpty();
        semaphore.tryAcquire(1, 1, TimeUnit.SECONDS);
        assertThat(isFailed.get()).isFalse();
        final Channel<Object, Object> channel4 =
                JRoutineCore.with(IdentityInvocation.factoryOf()).async();
        channel4.after(millis(300)).abort(new IllegalArgumentException("test_abort"));
        try {
            channel4.close().after(seconds(1)).throwError();

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
                JRoutineCore.with(factoryOf(abortInvocation, this, abortReason, semaphore))
                            .buildRoutine();
        final Channel<String, String> channel = routine.async();
        final IllegalArgumentException exception = new IllegalArgumentException();
        channel.after(millis(100)).abort(exception);
        semaphore.tryAcquire(1, TimeUnit.SECONDS);
        assertThat(abortReason.get().getCause()).isEqualTo(exception);
        final Channel<String, String> channel1 = routine.async();
        final IllegalAccessError exception1 = new IllegalAccessError();
        channel1.immediately().abort(exception1);
        semaphore.tryAcquire(1, TimeUnit.SECONDS);
        assertThat(abortReason.get().getCause()).isEqualTo(exception1);
    }

    @Test
    public void testAgingPriority() {
        final TestRunner runner = new TestRunner();
        final Routine<Object, Object> routine1 = JRoutineCore.with(IdentityInvocation.factoryOf())
                                                             .invocationConfiguration()
                                                             .withRunner(runner)
                                                             .withPriority(
                                                                     AgingPriority.NORMAL_PRIORITY)
                                                             .applied()
                                                             .buildRoutine();
        final Routine<Object, Object> routine2 = JRoutineCore.with(IdentityInvocation.factoryOf())
                                                             .invocationConfiguration()
                                                             .withRunner(runner)
                                                             .withPriority(
                                                                     AgingPriority.HIGH_PRIORITY)
                                                             .applied()
                                                             .buildRoutine();
        final Channel<Object, Object> output1 = routine1.async("test1").eventuallyBreak();
        final Channel<Object, Object> input2 = routine2.async();
        for (int i = 0; i < AgingPriority.HIGH_PRIORITY - 1; i++) {
            input2.pass("test2");
            runner.run(1);
            assertThat(output1.all()).isEmpty();
        }

        final Channel<Object, Object> output2 = input2.pass("test2").close();
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
        final Channel<Object, Object> channel1 = JRoutineCore.with(IdentityInvocation.factoryOf())
                                                             .async()
                                                             .after(seconds(1))
                                                             .pass("test1");
        channel1.bind(consumer);
        assertThat(channel1.isBound()).isTrue();
        assertThat(consumer.isOutput()).isFalse();
        final Channel<Object, Object> channel2 =
                JRoutineCore.with(IdentityInvocation.factoryOf()).sync().pass("test2");
        channel2.bind(consumer);
        assertThat(channel1.isBound()).isTrue();
        assertThat(channel2.isBound()).isTrue();
        assertThat(consumer.isOutput()).isTrue();
        assertThat(consumer.getOutput()).isEqualTo("test2");
    }

    @Test
    public void testCalls() {
        final UnitDuration timeout = seconds(1);
        final Routine<String, String> routine =
                JRoutineCore.with(IdentityInvocation.<String>factoryOf()).buildRoutine();
        assertThat(routine.sync().close().after(timeout).all()).isEmpty();
        assertThat(
                routine.sync(Arrays.asList("test1", "test2")).after(timeout).all()).containsExactly(
                "test1", "test2");
        assertThat(
                routine.sync(routine.sync("test1", "test2")).after(timeout).all()).containsExactly(
                "test1", "test2");
        assertThat(routine.sync("test1").after(timeout).all()).containsExactly("test1");
        assertThat(routine.sync("test1", "test2").after(timeout).all()).containsExactly("test1",
                "test2");
        assertThat(routine.async().close().after(timeout).all()).isEmpty();
        assertThat(routine.async(Arrays.asList("test1", "test2"))
                          .after(timeout)
                          .all()).containsExactly("test1", "test2");
        assertThat(
                routine.async(routine.sync("test1", "test2")).after(timeout).all()).containsExactly(
                "test1", "test2");
        assertThat(routine.async("test1").after(timeout).all()).containsExactly("test1");
        assertThat(routine.async("test1", "test2").after(timeout).all()).containsExactly("test1",
                "test2");
        assertThat(routine.parallel().close().after(timeout).all()).isEmpty();
        assertThat(routine.parallel(Arrays.asList("test1", "test2"))
                          .after(timeout)
                          .all()).containsOnly("test1", "test2");
        assertThat(
                routine.parallel(routine.sync("test1", "test2")).after(timeout).all()).containsOnly(
                "test1", "test2");
        assertThat(routine.parallel("test1").after(timeout).all()).containsOnly("test1");
        assertThat(routine.parallel("test1", "test2").after(timeout).all()).containsOnly("test1",
                "test2");
        assertThat(routine.sequential().close().after(timeout).all()).isEmpty();
        assertThat(routine.sequential(Arrays.asList("test1", "test2"))
                          .after(timeout)
                          .all()).containsOnly("test1", "test2");
        assertThat(routine.sequential(routine.sync("test1", "test2"))
                          .after(timeout)
                          .all()).containsOnly("test1", "test2");
        assertThat(routine.sequential("test1").after(timeout).all()).containsOnly("test1");
        assertThat(routine.sequential("test1", "test2").after(timeout).all()).containsOnly("test1",
                "test2");

        assertThat(routine.sync().pass().close().all()).isEmpty();
        assertThat(routine.sync()
                          .pass(Arrays.asList("test1", "test2"))
                          .close()
                          .after(timeout)
                          .all()).containsExactly("test1", "test2");
        assertThat(routine.sync().pass(routine.sync("test1", "test2")).close().after(timeout).all())
                .containsExactly("test1", "test2");
        assertThat(routine.sync().pass("test1").close().after(timeout).all()).containsExactly(
                "test1");
        assertThat(
                routine.sync().pass("test1", "test2").close().after(timeout).all()).containsExactly(
                "test1", "test2");
        assertThat(routine.async().pass().close().after(timeout).all()).isEmpty();
        assertThat(routine.async()
                          .pass(Arrays.asList("test1", "test2"))
                          .close()
                          .after(timeout)
                          .all()).containsExactly("test1", "test2");
        assertThat(routine.async()
                          .pass(routine.sync("test1", "test2"))
                          .close()
                          .after(timeout)
                          .all()).containsExactly("test1", "test2");
        assertThat(routine.async().pass("test1").close().after(timeout).all()).containsExactly(
                "test1");
        assertThat(routine.async()
                          .pass("test1", "test2")
                          .close()
                          .after(timeout)
                          .all()).containsExactly("test1", "test2");
        assertThat(routine.parallel().pass().close().after(timeout).all()).isEmpty();
        assertThat(routine.parallel()
                          .pass(Arrays.asList("test1", "test2"))
                          .close()
                          .after(timeout)
                          .all()).containsOnly("test1", "test2");
        assertThat(routine.parallel()
                          .pass(routine.sync("test1", "test2"))
                          .close()
                          .after(timeout)
                          .all()).containsOnly("test1", "test2");
        assertThat(routine.parallel().pass("test1").close().after(timeout).all()).containsOnly(
                "test1");
        assertThat(routine.parallel()
                          .pass("test1", "test2")
                          .close()
                          .after(timeout)
                          .all()).containsOnly("test1", "test2");
        assertThat(routine.sequential().pass().close().after(timeout).all()).isEmpty();
        assertThat(routine.sequential()
                          .pass(Arrays.asList("test1", "test2"))
                          .close()
                          .after(timeout)
                          .all()).containsOnly("test1", "test2");
        assertThat(routine.sequential()
                          .pass(routine.sync("test1", "test2"))
                          .close()
                          .after(timeout)
                          .all()).containsOnly("test1", "test2");
        assertThat(routine.sequential().pass("test1").close().after(timeout).all()).containsOnly(
                "test1");
        assertThat(routine.sequential()
                          .pass("test1", "test2")
                          .close()
                          .after(timeout)
                          .all()).containsOnly("test1", "test2");
    }

    @Test
    public void testChainedRoutine() {
        final UnitDuration timeout = seconds(1);
        final CallInvocation<Integer, Integer> execSum = new CallInvocation<Integer, Integer>() {

            @Override
            protected void onCall(@NotNull final List<? extends Integer> integers,
                    @NotNull final Channel<Integer, ?> result) {
                int sum = 0;
                for (final Integer integer : integers) {
                    sum += integer;
                }

                result.pass(sum);
            }
        };
        final Routine<Integer, Integer> sumRoutine =
                JRoutineCore.with(factoryOf(execSum, this)).buildRoutine();
        final Routine<Integer, Integer> squareRoutine =
                JRoutineCore.with(new SquareInvocation()).buildRoutine();
        assertThat(sumRoutine.sync(squareRoutine.sync(1, 2, 3, 4))
                             .after(timeout)
                             .all()).containsExactly(30);
        assertThat(sumRoutine.sync(squareRoutine.sequential(1, 2, 3, 4))
                             .after(timeout)
                             .all()).containsExactly(30);
        assertThat(sumRoutine.async(squareRoutine.sync(1, 2, 3, 4))
                             .after(timeout)
                             .all()).containsExactly(30);
        assertThat(sumRoutine.async(squareRoutine.async(1, 2, 3, 4))
                             .after(timeout)
                             .all()).containsExactly(30);
        assertThat(sumRoutine.async(squareRoutine.parallel(1, 2, 3, 4))
                             .after(timeout)
                             .all()).containsExactly(30);
        assertThat(sumRoutine.async(squareRoutine.sequential(1, 2, 3, 4))
                             .after(timeout)
                             .all()).containsExactly(30);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testChannelError() {
        final Logger logger = Logger.newLogger(new NullLog(), Level.DEBUG, this);
        try {
            new InvocationChannel<Object, Object>(InvocationConfiguration.defaultConfiguration(),
                    null, Runners.sharedRunner(), logger);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            new InvocationChannel<Object, Object>(InvocationConfiguration.defaultConfiguration(),
                    new TestInvocationManager(), null, logger);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            new InvocationChannel<Object, Object>(InvocationConfiguration.defaultConfiguration(),
                    new TestInvocationManager(), Runners.sharedRunner(), null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            new InvocationChannel<Object, Object>(null, new TestInvocationManager(),
                    Runners.sharedRunner(), logger);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            final InvocationChannel<Object, Object> channel = new InvocationChannel<Object, Object>(
                    InvocationConfiguration.defaultConfiguration(), new TestInvocationManager(),
                    Runners.sharedRunner(), logger);
            channel.close();
            channel.pass("test");
            fail();

        } catch (final IllegalStateException ignored) {
        }

        try {

            final InvocationChannel<Object, Object> channel = new InvocationChannel<Object, Object>(
                    InvocationConfiguration.defaultConfiguration(), new TestInvocationManager(),
                    Runners.sharedRunner(), logger);
            channel.after(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            final InvocationChannel<Object, Object> channel = new InvocationChannel<Object, Object>(
                    InvocationConfiguration.defaultConfiguration(), new TestInvocationManager(),
                    Runners.sharedRunner(), logger);
            channel.after(1, null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            final InvocationChannel<Object, Object> channel = new InvocationChannel<Object, Object>(
                    InvocationConfiguration.defaultConfiguration(), new TestInvocationManager(),
                    Runners.sharedRunner(), logger);
            channel.after(-1, TimeUnit.MILLISECONDS);
            fail();

        } catch (final IllegalArgumentException ignored) {
        }
    }

    @Test
    public void testComposedRoutine() {
        final UnitDuration timeout = seconds(1);
        final CallInvocation<Integer, Integer> execSum = new CallInvocation<Integer, Integer>() {

            @Override
            protected void onCall(@NotNull final List<? extends Integer> integers,
                    @NotNull final Channel<Integer, ?> result) {
                int sum = 0;
                for (final Integer integer : integers) {
                    sum += integer;
                }

                result.pass(sum);
            }
        };

        final Routine<Integer, Integer> sumRoutine =
                JRoutineCore.with(factoryOf(execSum, this)).buildRoutine();
        final Routine<Integer, Integer> squareRoutine =
                JRoutineCore.with(new SquareInvocation()).buildRoutine();
        final TemplateInvocation<Integer, Integer> invokeSquareSum =
                new TemplateInvocation<Integer, Integer>() {

                    private Channel<Integer, Integer> mChannel;

                    @Override
                    public void onAbort(@NotNull final RoutineException reason) {
                        mChannel.abort(reason);
                    }

                    @Override
                    public void onRestart() {
                        mChannel = sumRoutine.async();
                    }

                    @Override
                    public void onInput(final Integer integer,
                            @NotNull final Channel<Integer, ?> result) {
                        squareRoutine.async(integer).bind(mChannel);
                    }

                    @Override
                    public void onComplete(@NotNull final Channel<Integer, ?> result) {
                        result.pass(mChannel.close());
                    }
                };
        final Routine<Integer, Integer> squareSumRoutine =
                JRoutineCore.with(factoryOf(invokeSquareSum, this, sumRoutine, squareRoutine))
                            .buildRoutine();
        assertThat(squareSumRoutine.async(1, 2, 3, 4).after(timeout).all()).containsExactly(30);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testConfigurationErrors() {
        try {
            new DefaultRoutineBuilder<Object, Object>(IdentityInvocation.factoryOf()).apply(null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @Test
    public void testConstructor() {
        boolean failed = false;
        try {
            new JRoutineCore();
            failed = true;

        } catch (final Throwable ignored) {
        }

        assertThat(failed).isFalse();
    }

    @Test
    public void testDeadlockOnAll() {
        final Routine<Object, Object> routine2 =
                JRoutineCore.with(new AllInvocation()).buildRoutine();
        try {
            routine2.async("test").after(seconds(1)).all();
            fail();

        } catch (final DeadlockException ignored) {
        }
    }

    @Test
    public void testDeadlockOnCheckComplete() {
        final Routine<Object, Object> routine1 =
                JRoutineCore.with(new CheckCompleteInvocation()).buildRoutine();
        try {
            routine1.async("test").after(seconds(1)).all();
            fail();

        } catch (final DeadlockException ignored) {
        }
    }

    @Test
    public void testDeadlockOnHasNext() {
        final Routine<Object, Object> routine3 =
                JRoutineCore.with(new HasNextInvocation()).buildRoutine();
        try {
            routine3.async("test").after(seconds(1)).all();
            fail();

        } catch (final DeadlockException ignored) {
        }
    }

    @Test
    public void testDeadlockOnNext() {
        final Routine<Object, Object> routine4 =
                JRoutineCore.with(new NextInvocation()).buildRoutine();
        try {
            routine4.async("test").after(seconds(1)).all();
            fail();

        } catch (final DeadlockException ignored) {
        }
    }

    @Test
    public void testDelay() {
        long startTime = System.currentTimeMillis();
        final Channel<String, String> channel =
                JRoutineCore.with(factoryOf(DelayedInvocation.class, millis(10))).async();
        channel.after(100, TimeUnit.MILLISECONDS).pass("test1");
        channel.after(millis(10).nanosTime()).pass("test2");
        channel.after(millis(10).microsTime()).pass("test3", "test4");
        channel.after(millis(10)).pass((String[]) null);
        channel.immediately().pass((List<String>) null).pass((Channel<String, String>) null);
        assertThat(channel.close().after(3, TimeUnit.SECONDS).all()).containsOnly("test1", "test2",
                "test3", "test4");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(110);

        startTime = System.currentTimeMillis();

        final Channel<String, String> channel1 = JRoutineCore.with(
                factoryOf(ClassToken.tokenOf(DelayedInvocation.class), millis(10)))
                                                             .invocationConfiguration()
                                                             .withInputOrder(OrderType.BY_CALL)
                                                             .withOutputOrder(OrderType.BY_CALL)
                                                             .applied()
                                                             .async();
        channel1.after(100, TimeUnit.MILLISECONDS).pass("test1");
        channel1.after(millis(10).nanosTime()).pass("test2");
        channel1.after(millis(10).microsTime()).pass(Arrays.asList("test3", "test4"));
        channel1.after(millis(10)).pass((String[]) null);
        channel1.immediately().pass((List<String>) null).pass((Channel<String, String>) null);
        assertThat(channel1.close().after(seconds(7000)).all()).containsExactly("test1", "test2",
                "test3", "test4");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(110);

        startTime = System.currentTimeMillis();
        final Channel<String, String> channel2 =
                JRoutineCore.with(factoryOf(DelayedListInvocation.class, millis(10), 2)).async();
        channel2.after(100, TimeUnit.MILLISECONDS).pass("test1");
        channel2.after(millis(10).nanosTime()).pass("test2");
        channel2.after(millis(10).microsTime()).pass("test3", "test4");
        channel2.after(millis(10)).pass((String[]) null);
        channel2.immediately().pass((List<String>) null).pass((Channel<String, String>) null);
        assertThat(channel2.close().after(3, TimeUnit.SECONDS).all()).containsOnly("test1", "test2",
                "test3", "test4");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(110);

        startTime = System.currentTimeMillis();
        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.BY_CALL)
                                                               .withOutputOrder(OrderType.BY_CALL)
                                                               .applied();
        final Channel<String, String> channel3 =
                JRoutineCore.with(factoryOf(DelayedListInvocation.class, millis(10), 2))
                            .invocationConfiguration()
                            .with(configuration)
                            .applied()
                            .async();
        channel3.after(100, TimeUnit.MILLISECONDS).pass("test1");
        channel3.after(millis(10).nanosTime()).pass("test2");
        channel3.after(millis(10).microsTime()).pass("test3", "test4");
        channel3.after(millis(10)).pass((String[]) null);
        channel3.immediately().pass((List<String>) null).pass((Channel<String, String>) null);
        assertThat(channel3.close().after(3, TimeUnit.SECONDS).all()).containsExactly("test1",
                "test2", "test3", "test4");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(110);

        startTime = System.currentTimeMillis();
        final Channel<String, String> channel4 =
                JRoutineCore.with(factoryOf(DelayedListInvocation.class, zero(), 2)).async();
        channel4.after(100, TimeUnit.MILLISECONDS).pass("test1");
        channel4.after(millis(10).nanosTime()).pass("test2");
        channel4.after(millis(10).microsTime()).pass("test3", "test4");
        channel4.after(millis(10)).pass((String[]) null);
        channel4.immediately().pass((List<String>) null).pass((Channel<String, String>) null);
        assertThat(channel4.close().after(3, TimeUnit.SECONDS).all()).containsOnly("test1", "test2",
                "test3", "test4");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(100);

        startTime = System.currentTimeMillis();
        final Channel<String, String> channel5 =
                JRoutineCore.with(factoryOf(DelayedListInvocation.class, zero(), 2))
                            .invocationConfiguration()
                            .with(configuration)
                            .applied()
                            .async();
        channel5.after(100, TimeUnit.MILLISECONDS).pass("test1");
        channel5.after(millis(10).nanosTime()).pass("test2");
        channel5.after(millis(10).microsTime()).pass("test3", "test4");
        channel5.after(millis(10)).pass((String[]) null);
        channel5.immediately().pass((List<String>) null).pass((Channel<String, String>) null);
        assertThat(channel5.close().after(3, TimeUnit.SECONDS).all()).containsExactly("test1",
                "test2", "test3", "test4");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(100);

        startTime = System.currentTimeMillis();
        final Channel<String, String> channel6 =
                JRoutineCore.with(factoryOf(DelayedChannelInvocation.class, millis(10))).async();
        channel6.after(100, TimeUnit.MILLISECONDS).pass("test1");
        channel6.after(millis(10).nanosTime()).pass("test2");
        channel6.after(millis(10).microsTime()).pass("test3", "test4");
        channel6.after(millis(10)).pass((String[]) null);
        channel6.immediately().pass((List<String>) null).pass((Channel<String, String>) null);
        assertThat(channel6.close().after(3, TimeUnit.SECONDS).all()).containsOnly("test1", "test2",
                "test3", "test4");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(110);

        startTime = System.currentTimeMillis();
        final Channel<String, String> channel7 =
                JRoutineCore.with(factoryOf(DelayedChannelInvocation.class, millis(10)))
                            .invocationConfiguration()
                            .with(configuration)
                            .applied()
                            .async();
        channel7.after(100, TimeUnit.MILLISECONDS).pass("test1");
        channel7.after(millis(10).nanosTime()).pass("test2");
        channel7.after(millis(10).microsTime()).pass("test3", "test4");
        channel7.after(millis(10)).pass((String[]) null);
        channel7.immediately().pass((List<String>) null).pass((Channel<String, String>) null);
        assertThat(channel7.close().after(3, TimeUnit.SECONDS).all()).containsExactly("test1",
                "test2", "test3", "test4");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(110);

        startTime = System.currentTimeMillis();
        final Channel<String, String> channel8 =
                JRoutineCore.with(factoryOf(DelayedChannelInvocation.class, zero())).async();
        channel8.after(100, TimeUnit.MILLISECONDS).pass("test1");
        channel8.after(millis(10).nanosTime()).pass("test2");
        channel8.after(millis(10).microsTime()).pass("test3", "test4");
        channel8.after(millis(10)).pass((String[]) null);
        channel8.immediately().pass((List<String>) null).pass((Channel<String, String>) null);
        assertThat(channel8.close().after(3, TimeUnit.SECONDS).all()).containsOnly("test1", "test2",
                "test3", "test4");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(100);

        startTime = System.currentTimeMillis();
        final Channel<String, String> channel9 =
                JRoutineCore.with(factoryOf(DelayedChannelInvocation.class, zero()))
                            .invocationConfiguration()
                            .with(configuration)
                            .applied()
                            .async();
        channel9.after(100, TimeUnit.MILLISECONDS).pass("test1");
        channel9.after(millis(10).nanosTime()).pass("test2");
        channel9.after(millis(10).microsTime()).pass("test3", "test4");
        channel9.after(millis(10)).pass((String[]) null);
        channel9.immediately().pass((List<String>) null).pass((Channel<String, String>) null);
        assertThat(channel9.close().after(3, TimeUnit.SECONDS).all()).containsExactly("test1",
                "test2", "test3", "test4");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(100);
    }

    @Test
    public void testDelayedAbort() throws InterruptedException {
        final UnitDuration timeout = seconds(1);
        final Routine<String, String> passingRoutine =
                JRoutineCore.with(IdentityInvocation.<String>factoryOf()).buildRoutine();
        final Channel<String, String> channel1 = passingRoutine.async();
        channel1.after(seconds(2)).abort();
        assertThat(channel1.immediately().pass("test").close().after(timeout).next()).isEqualTo(
                "test");
        final Channel<String, String> channel2 = passingRoutine.async();
        channel2.after(millis(100)).abort();
        try {
            channel2.after(millis(200)).pass("test").close().after(timeout).next();
            fail();

        } catch (final AbortException ignored) {
        }

        final Routine<String, String> abortRoutine =
                JRoutineCore.with(factoryOf(DelayedAbortInvocation.class, millis(200)))
                            .buildRoutine();
        assertThat(abortRoutine.async("test").after(timeout).next()).isEqualTo("test");
        try {
            final Channel<String, String> channel = abortRoutine.async("test");
            millis(500).sleepAtLeast();
            channel.after(timeout).next();
            fail();

        } catch (final AbortException ignored) {
        }
    }

    @Test
    public void testDelayedBind() {
        final UnitDuration timeout = seconds(1);
        final Routine<Object, Object> routine1 =
                JRoutineCore.with(IdentityInvocation.factoryOf()).buildRoutine();
        final Routine<Object, Object> routine2 =
                JRoutineCore.with(IdentityInvocation.factoryOf()).buildRoutine();
        final long startTime = System.currentTimeMillis();
        assertThat(routine1.async()
                           .after(millis(500))
                           .pass(routine2.async("test"))
                           .close()
                           .after(timeout)
                           .next()).isEqualTo("test");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(500);
    }

    @Test
    public void testDelegation() {
        final UnitDuration timeout = seconds(1);
        final Routine<Object, Object> routine1 =
                JRoutineCore.with(IdentityInvocation.factoryOf()).buildRoutine();
        final Routine<Object, Object> routine2 =
                JRoutineCore.with(RoutineInvocation.factoryFrom(routine1, InvocationMode.SYNC))
                            .buildRoutine();
        assertThat(routine2.async("test1").after(timeout).all()).containsExactly("test1");
        final Channel<Object, Object> channel = routine2.async().after(timeout).pass("test2");
        channel.immediately().abort(new IllegalArgumentException());
        try {
            channel.after(timeout).next();

        } catch (final AbortException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
        }

        final Routine<String, String> routine3 =
                JRoutineCore.with(factoryOf(TestDiscard.class)).buildRoutine();
        final Routine<String, String> routine4 =
                JRoutineCore.with(RoutineInvocation.factoryFrom(routine3, InvocationMode.ASYNC))
                            .buildRoutine();
        assertThat(routine4.async("test4").after(timeout).all()).containsExactly("test4");
        routine4.clear();
        assertThat(TestDiscard.getInstanceCount()).isZero();
        final Routine<String, String> routine5 =
                JRoutineCore.with(RoutineInvocation.factoryFrom(routine3, InvocationMode.PARALLEL))
                            .buildRoutine();
        assertThat(routine5.async("test5").after(timeout).all()).containsExactly("test5");
        routine5.clear();
        assertThat(TestDiscard.getInstanceCount()).isZero();
        final Routine<String, String> routine6 = JRoutineCore.with(
                RoutineInvocation.factoryFrom(routine3, InvocationMode.SEQUENTIAL)).buildRoutine();
        assertThat(routine6.async("test5").after(timeout).all()).containsExactly("test5");
        routine6.clear();
        assertThat(TestDiscard.getInstanceCount()).isZero();
    }

    @Test
    public void testDiscard() {
        final UnitDuration timeout = seconds(1);
        final Routine<String, String> routine1 = JRoutineCore.with(factoryOf(TestDiscard.class))
                                                             .invocationConfiguration()
                                                             .withCoreInstances(0)
                                                             .applied()
                                                             .buildRoutine();
        assertThat(routine1.sync("1", "2", "3", "4", "5").after(timeout).all()).containsOnly("1",
                "2", "3", "4", "5");
        assertThat(routine1.async("1", "2", "3", "4", "5").after(timeout).all()).containsOnly("1",
                "2", "3", "4", "5");
        assertThat(routine1.parallel("1", "2", "3", "4", "5").after(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(routine1.sequential("1", "2", "3", "4", "5").after(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(TestDiscard.getInstanceCount()).isZero();

        final Routine<String, String> routine3 =
                JRoutineCore.with(factoryOf(TestDiscardOnAbort.class)).buildRoutine();
        Channel<String, String> channel = routine3.sync().pass("1");
        channel.abort();
        assertThat(channel.after(timeout).hasCompleted()).isTrue();
        channel = routine3.async().pass("1");
        channel.abort();
        assertThat(channel.after(timeout).hasCompleted()).isTrue();
        assertThat(TestDiscard.getInstanceCount()).isZero();
        channel = routine3.parallel().pass("1");
        channel.abort();
        assertThat(channel.after(timeout).hasCompleted()).isTrue();
        routine3.clear();
        assertThat(TestDiscard.getInstanceCount()).isZero();

        final Routine<String, String> routine5 =
                JRoutineCore.with(factoryOf(TestDiscard.class)).buildRoutine();
        assertThat(routine5.sync("1", "2", "3", "4", "5").after(timeout).all()).containsOnly("1",
                "2", "3", "4", "5");
        assertThat(routine5.async("1", "2", "3", "4", "5").after(timeout).all()).containsOnly("1",
                "2", "3", "4", "5");
        assertThat(routine5.parallel("1", "2", "3", "4", "5").after(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(routine5.sequential("1", "2", "3", "4", "5").after(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        routine5.clear();
        assertThat(TestDiscard.getInstanceCount()).isZero();

        final Routine<String, String> routine6 =
                JRoutineCore.with(factoryOf(TestDiscardException.class)).buildRoutine();

        assertThat(routine6.sync("1", "2", "3", "4", "5").after(timeout).all()).containsOnly("1",
                "2", "3", "4", "5");
        assertThat(routine6.async("1", "2", "3", "4", "5").after(timeout).all()).containsOnly("1",
                "2", "3", "4", "5");
        assertThat(routine6.parallel("1", "2", "3", "4", "5").after(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        assertThat(routine6.sequential("1", "2", "3", "4", "5").after(timeout).all()).containsOnly(
                "1", "2", "3", "4", "5");
        routine6.clear();
        assertThat(TestDiscard.getInstanceCount()).isZero();
    }

    @Test
    public void testEmpty() {
        final Channel<Object, Object> channel = JRoutineCore.with(new SleepInvocation(millis(500)))
                                                            .invocationConfiguration()
                                                            .withInputLimit(1)
                                                            .withInputBackoff(seconds(3))
                                                            .applied()
                                                            .async();
        assertThat(channel.isEmpty()).isTrue();
        assertThat(channel.pass("test1").pass("test2").isEmpty()).isFalse();
        final Channel<Object, Object> result = channel.close();
        assertThat(result.outputCount()).isZero();
        assertThat(result.after(seconds(10)).hasCompleted()).isTrue();
        assertThat(channel.isEmpty()).isFalse();
        assertThat(result.isEmpty()).isFalse();
    }

    @Test
    public void testEmptyAbort() {
        final Routine<Object, Object> routine = JRoutineCore.with(new SleepInvocation(millis(500)))
                                                            .invocationConfiguration()
                                                            .withInputLimit(1)
                                                            .withInputBackoff(seconds(3))
                                                            .applied()
                                                            .buildRoutine();
        Channel<Object, Object> channel = routine.async();
        assertThat(channel.isEmpty()).isTrue();
        assertThat(channel.pass("test1").abort()).isTrue();
        assertThat(channel.isEmpty()).isTrue();
        channel = routine.async();
        assertThat(channel.isEmpty()).isTrue();
        assertThat(channel.pass("test1").pass("test2").isEmpty()).isFalse();
        final Channel<Object, Object> result = channel.close();
        assertThat(result.isEmpty()).isFalse();
        assertThat(result.abort()).isTrue();
        assertThat(channel.isEmpty()).isTrue();
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testError() {
        try {
            JRoutineCore.with(factoryOf(ConstructorException.class))
                        .invocationConfiguration()
                        .withLogLevel(Level.SILENT)
                        .applied()
                        .sync()
                        .close()
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
            new DefaultRoutine<Object, Object>(null, new InvocationFactory<Object, Object>(null) {

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
            new DefaultRoutine<Object, Object>(InvocationConfiguration.defaultConfiguration(),
                    null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        final Logger logger = Logger.newLogger(null, null, this);
        try {
            final ResultChannel<Object> channel =
                    new ResultChannel<Object>(ChannelConfiguration.defaultConfiguration(),
                            new TestAbortHandler(), Runners.syncRunner(), logger);
            new InvocationExecution<Object, Object>(null, new TestInputIterator(), channel, logger);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            final ResultChannel<Object> channel =
                    new ResultChannel<Object>(ChannelConfiguration.defaultConfiguration(),
                            new TestAbortHandler(), Runners.syncRunner(), logger);
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
            final ResultChannel<Object> channel =
                    new ResultChannel<Object>(ChannelConfiguration.defaultConfiguration(),
                            new TestAbortHandler(), Runners.syncRunner(), logger);
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
    public void testErrorOnComplete() {
        final TemplateInvocation<String, String> exceptionOnResult =
                new TemplateInvocation<String, String>() {

                    @Override
                    public void onComplete(@NotNull final Channel<String, ?> result) {
                        throw new NullPointerException("test3");
                    }
                };
        final Routine<String, String> exceptionRoutine =
                JRoutineCore.with(factoryOf(exceptionOnResult, this)).buildRoutine();
        testException(exceptionRoutine, "test", "test3");

        final Routine<String, String> passingRoutine =
                JRoutineCore.with(IdentityInvocation.<String>factoryOf()).buildRoutine();
        testChained(passingRoutine, exceptionRoutine, "test", "test3");
        testChained(exceptionRoutine, passingRoutine, "test", "test3");
    }

    @Test
    public void testErrorOnInit() {
        final TemplateInvocation<String, String> exceptionOnInit =
                new TemplateInvocation<String, String>() {

                    @Override
                    public void onRestart() {
                        throw new NullPointerException("test1");
                    }
                };

        final Routine<String, String> exceptionRoutine =
                JRoutineCore.with(factoryOf(exceptionOnInit, this)).buildRoutine();
        testException(exceptionRoutine, "test", "test1");

        final Routine<String, String> passingRoutine =
                JRoutineCore.with(IdentityInvocation.<String>factoryOf()).buildRoutine();
        testChained(passingRoutine, exceptionRoutine, "test", "test1");
        testChained(exceptionRoutine, passingRoutine, "test", "test1");
    }

    @Test
    public void testErrorOnInput() {
        final TemplateInvocation<String, String> exceptionOnInput =
                new TemplateInvocation<String, String>() {

                    @Override
                    public void onInput(final String s, @NotNull final Channel<String, ?> result) {
                        throw new NullPointerException(s);
                    }
                };

        final Routine<String, String> exceptionRoutine =
                JRoutineCore.with(factoryOf(exceptionOnInput, this)).buildRoutine();
        testException(exceptionRoutine, "test2", "test2");

        final Routine<String, String> passingRoutine =
                JRoutineCore.with(IdentityInvocation.<String>factoryOf()).buildRoutine();
        testChained(passingRoutine, exceptionRoutine, "test2", "test2");
        testChained(exceptionRoutine, passingRoutine, "test2", "test2");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testFactoryError() {
        try {
            JRoutineCore.with((InvocationFactory<?, ?>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @Test
    public void testInitInvocationException() {
        final ExceptionRoutine routine =
                new ExceptionRoutine(InvocationConfiguration.defaultConfiguration());
        try {
            routine.async().close().after(seconds(1)).all();
            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    public void testInitInvocationNull() {
        final NullRoutine routine = new NullRoutine(InvocationConfiguration.defaultConfiguration());
        try {
            routine.async().close().after(seconds(1)).all();
            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(NullPointerException.class);
        }
    }

    @Test
    public void testInputDeadlock() {
        try {
            JRoutineCore.with(new SleepInvocation(millis(100)))
                        .invocationConfiguration()
                        .withInputMaxSize(1)
                        .applied()
                        .async("test", "test")
                        .all();
            fail();

        } catch (final InputDeadlockException ignored) {
        }

        try {
            JRoutineCore.with(new SleepInvocation(millis(100)))
                        .invocationConfiguration()
                        .withInputMaxSize(1)
                        .applied()
                        .async(Arrays.asList("test", "test"))
                        .all();
            fail();

        } catch (final InputDeadlockException ignored) {
        }
    }

    @Test
    public void testInputDelay() {
        assertThat(JRoutineCore.with(IdentityInvocation.factoryOf())
                               .invocationConfiguration()
                               .withInputOrder(OrderType.BY_CALL)
                               .withInputLimit(1)
                               .withInputBackoff(zero())
                               .applied()
                               .async()
                               .sortedByDelay()
                               .sortedByCall()
                               .after(millis(100))
                               .pass("test1")
                               .immediately()
                               .pass("test2")
                               .close()
                               .after(seconds(1))
                               .all()).containsExactly("test1", "test2");

        assertThat(JRoutineCore.with(IdentityInvocation.factoryOf())
                               .invocationConfiguration()
                               .withInputOrder(OrderType.BY_CALL)
                               .withInputLimit(1)
                               .withInputBackoff(millis(1000))
                               .applied()
                               .async()
                               .sortedByCall()
                               .after(millis(100))
                               .pass("test1")
                               .immediately()
                               .pass("test2")
                               .close()
                               .after(seconds(1))
                               .all()).containsExactly("test1", "test2");

        assertThat(JRoutineCore.with(IdentityInvocation.factoryOf())
                               .invocationConfiguration()
                               .withInputOrder(OrderType.BY_CALL)
                               .withInputLimit(1)
                               .withInputBackoff(millis(1000))
                               .applied()
                               .async()
                               .sortedByCall()
                               .after(millis(100))
                               .pass("test1")
                               .immediately()
                               .pass(asArgs("test2"))
                               .close()
                               .after(seconds(1))
                               .all()).containsExactly("test1", "test2");

        assertThat(JRoutineCore.with(IdentityInvocation.factoryOf())
                               .invocationConfiguration()
                               .withInputOrder(OrderType.BY_CALL)
                               .withInputLimit(1)
                               .withInputBackoff(millis(1000))
                               .applied()
                               .async()
                               .sortedByCall()
                               .after(millis(100))
                               .pass("test1")
                               .immediately()
                               .pass(Collections.singletonList("test2"))
                               .close()
                               .after(seconds(1))
                               .all()).containsExactly("test1", "test2");

        final Channel<Object, Object> channel = JRoutineCore.io().buildChannel();
        channel.pass("test2").close();
        assertThat(JRoutineCore.with(IdentityInvocation.factoryOf())
                               .invocationConfiguration()
                               .withInputOrder(OrderType.BY_CALL)
                               .withInputLimit(1)
                               .withInputBackoff(millis(1000))
                               .applied()
                               .async()
                               .sortedByCall()
                               .after(millis(100))
                               .pass("test1")
                               .immediately()
                               .pass(channel)
                               .close()
                               .after(seconds(1))
                               .all()).containsExactly("test1", "test2");
    }

    @Test
    public void testInputRunnerDeadlock() {
        try {
            JRoutineCore.with(new InputRunnerDeadlock()).async("test").after(seconds(1)).all();
            fail();

        } catch (final DeadlockException ignored) {
        }

        try {
            JRoutineCore.with(new InputListRunnerDeadlock()).async("test").after(seconds(1)).all();
            fail();

        } catch (final DeadlockException ignored) {
        }

        try {
            JRoutineCore.with(new InputArrayRunnerDeadlock()).async("test").after(seconds(1)).all();
            fail();

        } catch (final DeadlockException ignored) {
        }

        try {
            JRoutineCore.with(new InputConsumerRunnerDeadlock())
                        .async("test")
                        .after(seconds(1))
                        .all();
            fail();

        } catch (final DeadlockException ignored) {
        }
    }

    @Test
    public void testInputTimeoutIssue() {
        try {
            final Channel<Object, Object> channel = JRoutineCore.io().buildChannel();
            channel.pass("test2").close();
            JRoutineCore.with(IdentityInvocation.factoryOf())
                        .invocationConfiguration()
                        .withInputOrder(OrderType.BY_CALL)
                        .withInputMaxSize(1)
                        .withInputBackoff(millis(1000))
                        .applied()
                        .async()
                        .sortedByCall()
                        .after(millis(100))
                        .pass("test1")
                        .immediately()
                        .pass(channel)
                        .close()
                        .after(seconds(10))
                        .all();
            fail();

        } catch (final InputDeadlockException ignored) {
        }
    }

    @Test
    public void testInvocationLifecycle() throws InterruptedException {
        final Channel<String, String> outputChannel =
                JRoutineCore.with(factoryOf(TestLifecycle.class)).async("test");
        Thread.sleep(500);
        outputChannel.abort();
        outputChannel.after(infinity()).hasCompleted();
        assertThat(TestLifecycle.sIsError).isFalse();
    }

    @Test
    public void testInvocationNotAvailable() throws InterruptedException {
        final Routine<Void, Void> routine = JRoutineCore.with(new SleepCommand())
                                                        .invocationConfiguration()
                                                        .withMaxInstances(1)
                                                        .applied()
                                                        .buildRoutine();
        routine.sync().pass((Void) null);
        try {
            routine.async().close().after(seconds(1)).next();
            fail();

        } catch (final InvocationDeadlockException ignored) {
        }
    }

    @Test
    public void testNextList() {
        assertThat(JRoutineCore.with(IdentityInvocation.factoryOf())
                               .async("test1", "test2", "test3", "test4")
                               .after(seconds(1))
                               .next(2)).containsExactly("test1", "test2");
        assertThat(JRoutineCore.with(IdentityInvocation.factoryOf())
                               .async("test1")
                               .eventuallyBreak()
                               .after(seconds(1))
                               .next(2)).containsExactly("test1");
        try {
            JRoutineCore.with(IdentityInvocation.factoryOf())
                        .async()
                        .pass("test1")
                        .after(millis(300))
                        .pass("test2")
                        .close()
                        .eventuallyAbort()
                        .after(millis(100))
                        .next(2);
            fail();

        } catch (final AbortException ignored) {
        }

        try {
            JRoutineCore.with(IdentityInvocation.factoryOf())
                        .async()
                        .pass("test1")
                        .after(millis(300))
                        .pass("test2")
                        .close()
                        .eventuallyAbort(new IllegalStateException())
                        .after(millis(100))
                        .next(2);
            fail();

        } catch (final AbortException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(IllegalStateException.class);
        }

        try {
            JRoutineCore.with(IdentityInvocation.factoryOf())
                        .async()
                        .pass("test1")
                        .after(millis(300))
                        .pass("test2")
                        .close()
                        .eventuallyFail()
                        .after(millis(100))
                        .next(2);
            fail();

        } catch (final TimeoutException ignored) {
        }
    }

    @Test
    public void testNextOr() {
        assertThat(JRoutineCore.with(IdentityInvocation.factoryOf())
                               .async("test1")
                               .after(seconds(1))
                               .nextOrElse(2)).isEqualTo("test1");
        assertThat(JRoutineCore.with(IdentityInvocation.factoryOf())
                               .async()
                               .eventuallyBreak()
                               .after(seconds(1))
                               .nextOrElse(2)).isEqualTo(2);
        try {
            JRoutineCore.with(factoryOf(DelayedInvocation.class, millis(300)))
                        .async("test1")
                        .eventuallyAbort()
                        .after(millis(100))
                        .nextOrElse("test2");
            fail();

        } catch (final AbortException ignored) {
        }

        try {
            JRoutineCore.with(factoryOf(DelayedInvocation.class, millis(300)))
                        .async("test1")
                        .eventuallyAbort(new IllegalStateException())
                        .after(millis(100))
                        .nextOrElse("test2");
            fail();

        } catch (final AbortException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(IllegalStateException.class);
        }

        try {
            JRoutineCore.with(factoryOf(DelayedInvocation.class, millis(300)))
                        .async("test1")
                        .eventuallyFail()
                        .after(millis(100))
                        .nextOrElse("test2");
            fail();

        } catch (final TimeoutException ignored) {
        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testNullDelegatedRoutine() {
        try {
            RoutineInvocation.factoryFrom(null, InvocationMode.ASYNC);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            RoutineInvocation.factoryFrom(JRoutineCore.with(IdentityInvocation.factoryOf()), null);
            fail();

        } catch (final NullPointerException ignored) {
        }
    }

    @Test
    public void testOutputDeadlock() {
        final Routine<String, String> routine1 =
                JRoutineCore.with(factoryOf(new CallInvocation<String, String>() {

                    @Override
                    protected void onCall(@NotNull final List<? extends String> strings,
                            @NotNull final Channel<String, ?> result) {
                        result.pass(strings);
                    }
                }, this)).invocationConfiguration().withOutputMaxSize(1).applied().buildRoutine();
        try {
            routine1.async("test1", "test2").after(seconds(1)).all();
            fail();

        } catch (final OutputDeadlockException ignored) {
        }

        final Routine<String, String> routine2 =
                JRoutineCore.with(factoryOf(new CallInvocation<String, String>() {

                    @Override
                    protected void onCall(@NotNull final List<? extends String> strings,
                            @NotNull final Channel<String, ?> result) {
                        result.pass(strings.toArray(new String[strings.size()]));
                    }
                }, this)).invocationConfiguration().withOutputMaxSize(1).applied().buildRoutine();
        try {
            routine2.async("test1", "test2").after(seconds(1)).all();
            fail();

        } catch (final OutputDeadlockException ignored) {
        }
    }

    @Test
    public void testOutputTimeout() throws InterruptedException {
        final Routine<String, String> routine =
                JRoutineCore.with(IdentityInvocation.<String>factoryOf())
                            .invocationConfiguration()
                            .withOutputLimit(1)
                            .withOutputBackoff(zero())
                            .applied()
                            .buildRoutine();
        final Channel<String, String> outputChannel =
                routine.async("test1", "test2").after(seconds(1));
        outputChannel.hasCompleted();
        assertThat(outputChannel.all()).containsExactly("test1", "test2");
        final Channel<String, String> channel1 = JRoutineCore.io()
                                                             .channelConfiguration()
                                                             .withLimit(1)
                                                             .withBackoff(millis(1000))
                                                             .applied()
                                                             .buildChannel();
        new Thread() {

            @Override
            public void run() {
                channel1.pass("test1").pass("test2").close();
            }
        }.start();
        millis(100).sleepAtLeast();
        assertThat(channel1.after(seconds(10)).all()).containsOnly("test1", "test2");

        final Channel<String, String> channel2 = JRoutineCore.io()
                                                             .channelConfiguration()
                                                             .withLimit(1)
                                                             .withBackoff(millis(1000))
                                                             .applied()
                                                             .buildChannel();
        new Thread() {

            @Override
            public void run() {
                channel2.pass("test1").pass(new String[]{"test2"}).close();
            }
        }.start();
        millis(100).sleepAtLeast();
        assertThat(channel2.after(seconds(10)).all()).containsOnly("test1", "test2");

        final Channel<String, String> channel3 = JRoutineCore.io()
                                                             .channelConfiguration()
                                                             .withLimit(1)
                                                             .withBackoff(millis(1000))
                                                             .applied()
                                                             .buildChannel();
        new Thread() {

            @Override
            public void run() {
                channel3.pass("test1").pass(Collections.singletonList("test2")).close();
            }
        }.start();
        millis(100).sleepAtLeast();
        assertThat(channel3.after(seconds(10)).all()).containsOnly("test1", "test2");

        final Channel<String, String> channel4 = JRoutineCore.io()
                                                             .channelConfiguration()
                                                             .withLimit(1)
                                                             .withBackoff(millis(1000))
                                                             .applied()
                                                             .buildChannel();
        new Thread() {

            @Override
            public void run() {
                final Channel<String, String> channel = JRoutineCore.io().buildChannel();
                channel.pass("test1", "test2").close();
                channel4.pass(channel).close();
            }
        }.start();
        millis(100).sleepAtLeast();
        assertThat(channel4.after(seconds(10)).all()).containsOnly("test1", "test2");
    }

    @Test
    public void testPartialOut() {
        final TemplateInvocation<String, String> invocation =
                new TemplateInvocation<String, String>() {

                    @Override
                    public void onInput(final String s, @NotNull final Channel<String, ?> result) {
                        result.immediately().pass(s).after(seconds(2)).abort();
                    }
                };
        assertThat(JRoutineCore.with(factoryOf(invocation, this))
                               .async("test")
                               .after(millis(500))
                               .all()).containsExactly("test");
    }

    @Test
    public void testPendingInputs() throws InterruptedException {
        final Channel<Object, Object> channel =
                JRoutineCore.with(IdentityInvocation.factoryOf()).async();
        assertThat(channel.isOpen()).isTrue();
        channel.pass("test");
        assertThat(channel.isOpen()).isTrue();
        channel.after(millis(500)).pass("test");
        assertThat(channel.isOpen()).isTrue();
        final Channel<Object, Object> outputChannel = JRoutineCore.io().buildChannel();
        channel.pass(outputChannel);
        assertThat(channel.isOpen()).isTrue();
        channel.close();
        assertThat(channel.isOpen()).isFalse();
        outputChannel.close();
        assertThat(channel.isOpen()).isFalse();
    }

    @Test
    public void testPendingInputsAbort() throws InterruptedException {
        final Channel<Object, Object> channel =
                JRoutineCore.with(IdentityInvocation.factoryOf()).async();
        assertThat(channel.isOpen()).isTrue();
        channel.pass("test");
        assertThat(channel.isOpen()).isTrue();
        channel.after(millis(500)).pass("test");
        assertThat(channel.isOpen()).isTrue();
        final Channel<Object, Object> outputChannel = JRoutineCore.io().buildChannel();
        channel.pass(outputChannel);
        assertThat(channel.isOpen()).isTrue();
        channel.immediately().abort();
        assertThat(channel.isOpen()).isFalse();
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testResultChannelError() {
        final Logger logger = Logger.newLogger(new NullLog(), Level.DEBUG, this);
        try {
            new ResultChannel<Object>(ChannelConfiguration.defaultConfiguration(), null,
                    Runners.sharedRunner(), logger);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            new ResultChannel<Object>(ChannelConfiguration.defaultConfiguration(),
                    new TestAbortHandler(), null, logger);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            new ResultChannel<Object>(ChannelConfiguration.defaultConfiguration(),
                    new TestAbortHandler(), Runners.sharedRunner(), null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            new ResultChannel<Object>(ChannelConfiguration.defaultConfiguration(),
                    new TestAbortHandler(), Runners.sharedRunner(), logger).after(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            new ResultChannel<Object>(ChannelConfiguration.defaultConfiguration(),
                    new TestAbortHandler(), Runners.sharedRunner(), logger).after(0, null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            final ResultChannel<Object> channel =
                    new ResultChannel<Object>(ChannelConfiguration.defaultConfiguration(),
                            new TestAbortHandler(), Runners.sharedRunner(), logger);
            channel.after(-1, TimeUnit.MILLISECONDS);
            fail();

        } catch (final IllegalArgumentException ignored) {
        }

        final Channel<String, String> channel =
                JRoutineCore.with(factoryOf(DelayedInvocation.class, zero()))
                            .invocationConfiguration()
                            .withLogLevel(Level.SILENT)
                            .applied()
                            .sync();
        try {
            channel.after(null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            channel.after(0, null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            channel.after(-1, TimeUnit.MILLISECONDS);
            fail();

        } catch (final IllegalArgumentException ignored) {
        }

        try {
            channel.bind((Channel<String, String>) null);
            fail();

        } catch (final NullPointerException ignored) {
        }

        try {
            channel.bind((OutputConsumer<String>) null);
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
            channel.bind(consumer).bind(consumer);
            fail();

        } catch (final IllegalStateException ignored) {
        }

        try {
            channel.iterator();
            fail();

        } catch (final IllegalStateException ignored) {
        }

        final Routine<String, String> routine1 =
                JRoutineCore.with(factoryOf(DelayedInvocation.class, millis(100)))
                            .invocationConfiguration()
                            .withLogLevel(Level.SILENT)
                            .applied()
                            .buildRoutine();
        final Iterator<String> iterator =
                routine1.async("test").after(millis(500)).eventuallyBreak().iterator();
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
            routine1.sync().immediately().eventuallyBreak().iterator().next();
            fail();

        } catch (final NoSuchElementException ignored) {
        }

        try {
            routine1.async("test").immediately().iterator().next();
            fail();

        } catch (final OutputTimeoutException ignored) {
        }
    }

    @Test
    public void testResultRunnerDeadlock() {
        try {
            JRoutineCore.with(new ResultRunnerDeadlock())
                        .invocationConfiguration()
                        .withOutputMaxSize(1)
                        .applied()
                        .async("test")
                        .after(seconds(1))
                        .all();
            fail();

        } catch (final OutputDeadlockException ignored) {
        }

        try {
            JRoutineCore.with(new ResultListRunnerDeadlock())
                        .invocationConfiguration()
                        .withOutputMaxSize(1)
                        .applied()
                        .async("test")
                        .after(seconds(1))
                        .all();
            fail();

        } catch (final OutputDeadlockException ignored) {
        }

        try {
            JRoutineCore.with(new ResultArrayRunnerDeadlock())
                        .invocationConfiguration()
                        .withOutputMaxSize(1)
                        .applied()
                        .async("test")
                        .after(seconds(1))
                        .all();
            fail();

        } catch (final OutputDeadlockException ignored) {
        }
    }

    @Test
    public void testRoutine() {
        final UnitDuration timeout = seconds(1);
        final TemplateInvocation<Integer, Integer> execSquare =
                new TemplateInvocation<Integer, Integer>() {

                    @Override
                    public void onInput(final Integer integer,
                            @NotNull final Channel<Integer, ?> result) {
                        final int input = integer;
                        result.pass(input * input);
                    }
                };

        final Routine<Integer, Integer> squareRoutine =
                JRoutineCore.with(factoryOf(execSquare, this)).buildRoutine();
        assertThat(squareRoutine.sync(1, 2, 3, 4).after(timeout).all()).containsExactly(1, 4, 9,
                16);
        assertThat(squareRoutine.async(1, 2, 3, 4).after(timeout).all()).containsExactly(1, 4, 9,
                16);
        assertThat(squareRoutine.parallel(1, 2, 3, 4).after(timeout).all()).containsOnly(1, 4, 9,
                16);
    }

    @Test
    public void testRoutineBuilder() {
        assertThat(JRoutineCore.with(factoryOf(new ClassToken<IdentityInvocation<String>>() {}))
                               .invocationConfiguration()
                               .withRunner(Runners.poolRunner())
                               .withCoreInstances(0)
                               .withMaxInstances(1)
                               .withInputLimit(2)
                               .withInputBackoff(1, TimeUnit.SECONDS)
                               .withInputMaxSize(2)
                               .withOutputLimit(2)
                               .withOutputBackoff(1, TimeUnit.SECONDS)
                               .withOutputMaxSize(2)
                               .withOutputOrder(OrderType.BY_CALL)
                               .applied()
                               .sync("test1", "test2")
                               .all()).containsExactly("test1", "test2");

        assertThat(JRoutineCore.with(factoryOf(new ClassToken<IdentityInvocation<String>>() {}))
                               .invocationConfiguration()
                               .withRunner(Runners.poolRunner())
                               .withCoreInstances(0)
                               .withMaxInstances(1)
                               .withInputLimit(2)
                               .withInputBackoff(zero())
                               .withInputMaxSize(2)
                               .withOutputLimit(2)
                               .withOutputBackoff(zero())
                               .withOutputMaxSize(2)
                               .withOutputOrder(OrderType.BY_CALL)
                               .applied()
                               .sync("test1", "test2")
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
        final UnitDuration timeout = seconds(1);
        final CallInvocation<Integer, Integer> execSum = new CallInvocation<Integer, Integer>() {

            @Override
            protected void onCall(@NotNull final List<? extends Integer> integers,
                    @NotNull final Channel<Integer, ?> result) {
                int sum = 0;
                for (final Integer integer : integers) {
                    sum += integer;
                }

                result.pass(sum);
            }
        };

        final Routine<Integer, Integer> sumRoutine =
                JRoutineCore.with(factoryOf(execSum, this)).buildRoutine();
        assertThat(sumRoutine.sync(1, 2, 3, 4).after(timeout).all()).containsExactly(10);
        assertThat(sumRoutine.async(1, 2, 3, 4).after(timeout).all()).containsExactly(10);
    }

    @Test
    public void testSize() {
        final Channel<Object, Object> channel =
                JRoutineCore.with(IdentityInvocation.factoryOf()).async();
        assertThat(channel.inputCount()).isEqualTo(0);
        channel.after(millis(500)).pass("test");
        assertThat(channel.inputCount()).isEqualTo(1);
        final Channel<Object, Object> result = channel.close();
        assertThat(result.after(seconds(1)).hasCompleted()).isTrue();
        assertThat(result.outputCount()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.skipNext(1).outputCount()).isEqualTo(0);
    }

    @Test
    public void testSkip() {
        assertThat(JRoutineCore.with(IdentityInvocation.factoryOf())
                               .async("test1", "test2", "test3", "test4")
                               .after(seconds(1))
                               .skipNext(2)
                               .all()).containsExactly("test3", "test4");
        assertThat(JRoutineCore.with(IdentityInvocation.factoryOf())
                               .async("test1")
                               .eventuallyBreak()
                               .after(seconds(1))
                               .skipNext(2)
                               .all()).isEmpty();
        try {
            JRoutineCore.with(IdentityInvocation.factoryOf())
                        .async("test1")
                        .eventuallyAbort()
                        .after(seconds(1))
                        .skipNext(2);
            fail();

        } catch (final AbortException ignored) {
        }

        try {
            JRoutineCore.with(IdentityInvocation.factoryOf())
                        .async("test1")
                        .eventuallyAbort(new IllegalStateException())
                        .after(seconds(1))
                        .skipNext(2);
            fail();

        } catch (final AbortException e) {
            assertThat(e.getCause()).isExactlyInstanceOf(IllegalStateException.class);
        }

        try {
            JRoutineCore.with(IdentityInvocation.factoryOf())
                        .async("test1")
                        .eventuallyFail()
                        .after(seconds(1))
                        .skipNext(2);
            fail();

        } catch (final TimeoutException ignored) {
        }
    }

    @Test
    public void testTimeoutActions() {
        final Routine<String, String> routine1 =
                JRoutineCore.with(factoryOf(DelayedInvocation.class, seconds(1)))
                            .invocationConfiguration()
                            .withOutputTimeoutAction(TimeoutActionType.ABORT)
                            .applied()
                            .buildRoutine();
        try {
            routine1.async("test1").next();
            fail();

        } catch (final AbortException ignored) {
        }

        try {
            routine1.async("test1").all();
            fail();

        } catch (final AbortException ignored) {
        }

        try {
            final ArrayList<String> results = new ArrayList<String>();
            routine1.async("test1").allInto(results);
            fail();

        } catch (final AbortException ignored) {
        }

        try {
            routine1.async("test1").iterator().hasNext();
            fail();

        } catch (final AbortException ignored) {
        }

        try {
            routine1.async("test1").iterator().next();
            fail();

        } catch (final AbortException ignored) {
        }

        assertThat(routine1.async("test1").hasCompleted()).isFalse();
        final Routine<String, String> routine2 =
                JRoutineCore.with(factoryOf(DelayedInvocation.class, seconds(1)))
                            .invocationConfiguration()
                            .withOutputTimeoutAction(TimeoutActionType.ABORT)
                            .withOutputTimeout(millis(10))
                            .applied()
                            .buildRoutine();
        try {
            routine2.async("test1").next();
            fail();

        } catch (final AbortException ignored) {
        }

        try {
            routine2.async("test1").all();
            fail();

        } catch (final AbortException ignored) {
        }

        try {
            final ArrayList<String> results = new ArrayList<String>();
            routine2.async("test1").allInto(results);
            fail();

        } catch (final AbortException ignored) {
        }

        try {
            routine2.async("test1").iterator().hasNext();
            fail();

        } catch (final AbortException ignored) {
        }

        try {
            routine2.async("test1").iterator().next();
            fail();

        } catch (final AbortException ignored) {
        }

        assertThat(routine2.async("test1").hasCompleted()).isFalse();
        final Routine<String, String> routine3 =
                JRoutineCore.with(factoryOf(DelayedInvocation.class, seconds(1)))
                            .invocationConfiguration()
                            .withOutputTimeoutAction(TimeoutActionType.FAIL)
                            .applied()
                            .buildRoutine();
        final Channel<String, String> channel3 = routine3.async("test1");
        try {
            channel3.next();
            fail();

        } catch (final OutputTimeoutException ignored) {
        }

        try {
            channel3.all();
            fail();

        } catch (final OutputTimeoutException ignored) {
        }

        try {
            final ArrayList<String> results = new ArrayList<String>();
            channel3.allInto(results);
            fail();

        } catch (final OutputTimeoutException ignored) {
        }

        try {
            channel3.iterator().hasNext();
            fail();

        } catch (final OutputTimeoutException ignored) {
        }

        try {
            channel3.iterator().next();
            fail();

        } catch (final OutputTimeoutException ignored) {
        }

        assertThat(channel3.hasCompleted()).isFalse();
        channel3.after(millis(10));
        try {
            channel3.next();
            fail();

        } catch (final OutputTimeoutException ignored) {
        }

        try {
            channel3.all();
            fail();

        } catch (final OutputTimeoutException ignored) {
        }

        try {
            final ArrayList<String> results = new ArrayList<String>();
            channel3.allInto(results);
            fail();

        } catch (final OutputTimeoutException ignored) {
        }

        try {
            channel3.iterator().hasNext();
            fail();

        } catch (final OutputTimeoutException ignored) {
        }

        try {
            channel3.iterator().next();
            fail();

        } catch (final OutputTimeoutException ignored) {
        }

        assertThat(channel3.hasCompleted()).isFalse();
        final Routine<String, String> routine4 =
                JRoutineCore.with(factoryOf(DelayedInvocation.class, seconds(1)))
                            .invocationConfiguration()
                            .withOutputTimeoutAction(TimeoutActionType.BREAK)
                            .applied()
                            .buildRoutine();
        final Channel<String, String> channel4 = routine4.async("test1");
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
        channel4.after(millis(10));
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
        final UnitDuration timeout = seconds(1);
        try {
            before.sync(after.sync(input)).after(timeout).all();
            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {
            for (final String s : before.sync(after.sync(input)).after(timeout)) {
                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {
            before.async(after.sync(input)).after(timeout).all();
            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {
            for (final String s : before.async(after.sync(input)).after(timeout)) {
                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {
            before.parallel(after.sync(input)).after(timeout).all();
            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {
            for (final String s : before.parallel(after.sync(input)).after(timeout)) {
                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {
            before.sync().pass(after.sync(input)).close().after(timeout).all();
            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {
            for (final String s : before.sync().pass(after.sync(input)).close().after(timeout)) {
                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {
            before.async().pass(after.sync(input)).close().after(timeout).all();
            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {
            for (final String s : before.async().pass(after.sync(input)).close().after(timeout)) {
                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {
            before.parallel().pass(after.sync(input)).close().after(timeout).all();
            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {
            for (final String s : before.parallel()
                                        .pass(after.sync(input))
                                        .close()
                                        .after(timeout)) {
                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {
            before.async(after.async(input)).after(timeout).all();
            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {
            for (final String s : before.async(after.async(input)).after(timeout)) {
                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {
            before.async().pass(after.async(input)).close().after(timeout).all();
            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {
            for (final String s : before.async().pass(after.async(input)).close().after(timeout)) {
                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {
            before.async(after.parallel(input)).after(timeout).all();
            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {
            for (final String s : before.async(after.parallel(input)).after(timeout)) {
                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {
            before.async().pass(after.parallel(input)).close().after(timeout).all();
            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {
            for (final String s : before.async()
                                        .pass(after.parallel(input))
                                        .close()
                                        .after(timeout)) {
                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }
    }

    private void testConsumer(final OutputConsumer<String> consumer) {
        final UnitDuration timeout = seconds(1);
        final String input = "test";
        final Routine<String, String> routine =
                JRoutineCore.with(factoryOf(DelayedInvocation.class, zero())).buildRoutine();
        assertThat(routine.sync(input).bind(consumer).after(timeout).hasCompleted()).isTrue();
        assertThat(routine.async(input).bind(consumer).after(timeout).hasCompleted()).isTrue();
        assertThat(routine.parallel(input).bind(consumer).after(timeout).hasCompleted()).isTrue();
        assertThat(routine.sync()
                          .pass(input)
                          .close()
                          .bind(consumer)
                          .after(timeout)
                          .hasCompleted()).isTrue();
        assertThat(routine.async().pass(input).close().bind(consumer).after(timeout).hasCompleted())
                .isTrue();
        assertThat(
                routine.parallel().pass(input).close().bind(consumer).after(timeout).hasCompleted())
                .isTrue();
    }

    private void testException(final Routine<String, String> routine, final String input,
            final String expected) {
        final UnitDuration timeout = seconds(1);
        try {
            routine.sync(input).after(timeout).all();
            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {
            for (final String s : routine.sync(input).after(timeout)) {
                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {
            routine.async(input).after(timeout).all();
            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {
            for (final String s : routine.async(input).after(timeout)) {
                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {
            routine.parallel(input).after(timeout).all();
            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {
            for (final String s : routine.parallel(input).after(timeout)) {
                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {
            routine.sequential(input).after(timeout).all();
            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {
            for (final String s : routine.sequential(input).after(timeout)) {
                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {
            routine.sync().pass(input).close().after(timeout).all();
            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {
            for (final String s : routine.sync().pass(input).close().after(timeout)) {
                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {
            routine.async().pass(input).close().after(timeout).all();
            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {
            for (final String s : routine.async().pass(input).close().after(timeout)) {
                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {
            routine.parallel().pass(input).close().after(timeout).all();
            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {
            for (final String s : routine.parallel().pass(input).close().after(timeout)) {
                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {
            routine.sequential().pass(input).close().after(timeout).all();
            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {
            for (final String s : routine.sequential().pass(input).close().after(timeout)) {
                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final InvocationException e) {
            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }
    }

    private static class AbortInvocation extends MappingInvocation<String, String> {

        /**
         * Constructor.
         */
        protected AbortInvocation() {
            super(null);
        }

        public void onInput(final String s, @NotNull final Channel<String, ?> result) {
            result.sortedByCall().sortedByDelay();
            assertThat(result.isOpen()).isTrue();
            assertThat(result.abort(new IllegalArgumentException(s))).isTrue();
            assertThat(result.abort()).isFalse();
            assertThat(result.isOpen()).isFalse();
            try {
                result.sortedByCall();
                fail();

            } catch (final InvocationException ignored) {
            }

            try {
                result.sortedByDelay();
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

    private static class AbortInvocation2 extends MappingInvocation<String, String> {

        /**
         * Constructor.
         */
        protected AbortInvocation2() {
            super(null);
        }

        public void onInput(final String s, @NotNull final Channel<String, ?> result) {
            assertThat(result.abort()).isTrue();
            assertThat(result.abort(new IllegalArgumentException(s))).isFalse();
        }
    }

    private static class AllInvocation extends MappingInvocation<Object, Object> {

        /**
         * Constructor.
         */
        protected AllInvocation() {
            super(null);
        }

        public void onInput(final Object o, @NotNull final Channel<Object, ?> result) {
            JRoutineCore.with(factoryOf(DelayedInvocation.class, millis(100)))
                        .async("test")
                        .after(seconds(1))
                        .all();
        }
    }

    private static class CheckCompleteInvocation extends MappingInvocation<Object, Object> {

        /**
         * Constructor.
         */
        protected CheckCompleteInvocation() {
            super(null);
        }

        public void onInput(final Object o, @NotNull final Channel<Object, ?> result) {
            JRoutineCore.with(factoryOf(DelayedInvocation.class, millis(100)))
                        .async("test")
                        .after(seconds(1))
                        .hasCompleted();
        }
    }

    private static class CloseInvocation extends MappingInvocation<String, String> {

        private final AtomicBoolean mIsFailed;

        private final Semaphore mSemaphore;

        private CloseInvocation(@NotNull final Semaphore semaphore,
                @NotNull final AtomicBoolean isFailed) {
            super(null);
            mSemaphore = semaphore;
            mIsFailed = isFailed;
        }

        public void onInput(final String s, @NotNull final Channel<String, ?> result) {
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

        private final UnitDuration mDelay;

        public DelayedAbortInvocation(final UnitDuration delay) {
            mDelay = delay;
        }

        @Override
        public void onInput(final String s, @NotNull final Channel<String, ?> result) {
            result.immediately().pass(s).after(mDelay).abort();
        }
    }

    private static class DelayedChannelInvocation extends TemplateInvocation<String, String> {

        private final UnitDuration mDelay;

        private final Routine<String, String> mRoutine;

        private boolean mFlag;

        public DelayedChannelInvocation(final UnitDuration delay) {
            mDelay = delay;
            mRoutine = JRoutineCore.with(factoryOf(DelayedInvocation.class, zero())).buildRoutine();
        }

        @Override
        public void onInput(final String s, @NotNull final Channel<String, ?> result) {
            if (mFlag) {
                result.after(mDelay).pass((Channel<String, String>) null);

            } else {
                result.after(mDelay.value, mDelay.unit).pass((Channel<String, String>) null);
            }

            result.pass(mRoutine.async(s));
            mFlag = !mFlag;
        }
    }

    private static class DelayedInvocation extends TemplateInvocation<String, String> {

        private final UnitDuration mDelay;

        private boolean mFlag;

        public DelayedInvocation(final UnitDuration delay) {
            mDelay = delay;
        }

        @Override
        public void onInput(final String s, @NotNull final Channel<String, ?> result) {
            if (mFlag) {
                result.after(mDelay);

            } else {
                result.after(mDelay.value, mDelay.unit);
            }

            result.pass(s);
            mFlag = !mFlag;
        }
    }

    private static class DelayedListInvocation extends TemplateInvocation<String, String> {

        private final int mCount;

        private final UnitDuration mDelay;

        private final ArrayList<String> mList;

        private boolean mFlag;

        public DelayedListInvocation(final UnitDuration delay, final int listCount) {
            mDelay = delay;
            mCount = listCount;
            mList = new ArrayList<String>(listCount);
        }

        @Override
        public void onInput(final String s, @NotNull final Channel<String, ?> result) {
            final ArrayList<String> list = mList;
            list.add(s);
            if (list.size() >= mCount) {
                if (mFlag) {
                    result.after(mDelay).pass((String[]) null).pass(list);

                } else {
                    result.after(mDelay.value, mDelay.unit)
                          .pass((List<String>) null)
                          .pass(list.toArray(new String[list.size()]));
                }

                result.immediately();
                list.clear();
                mFlag = !mFlag;
            }
        }

        @Override
        public void onComplete(@NotNull final Channel<String, ?> result) {
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

    private static class HasNextInvocation extends MappingInvocation<Object, Object> {

        /**
         * Constructor.
         */
        protected HasNextInvocation() {
            super(null);
        }

        public void onInput(final Object o, @NotNull final Channel<Object, ?> result) {
            JRoutineCore.with(factoryOf(DelayedInvocation.class, millis(100)))
                        .async("test")
                        .after(seconds(1))
                        .iterator()
                        .hasNext();
        }
    }

    private static class InputArrayRunnerDeadlock extends MappingInvocation<String, String> {

        /**
         * Constructor.
         */
        protected InputArrayRunnerDeadlock() {
            super(null);
        }

        public void onInput(final String s, @NotNull final Channel<String, ?> result) {
            JRoutineCore.with(IdentityInvocation.<String>factoryOf())
                        .invocationConfiguration()
                        .withInputMaxSize(1)
                        .withInputBackoff(infinity())
                        .applied()
                        .async()
                        .after(millis(500))
                        .pass(s)
                        .immediately()
                        .pass(new String[]{s})
                        .close()
                        .all();
        }
    }

    private static class InputConsumerRunnerDeadlock extends MappingInvocation<String, String> {

        /**
         * Constructor.
         */
        protected InputConsumerRunnerDeadlock() {
            super(null);
        }

        public void onInput(final String s, @NotNull final Channel<String, ?> result) {
            final Channel<String, String> channel = JRoutineCore.io().buildChannel();
            result.pass(JRoutineCore.with(IdentityInvocation.<String>factoryOf())
                                    .invocationConfiguration()
                                    .withInputMaxSize(1)
                                    .withInputBackoff(infinity())
                                    .applied()
                                    .async()
                                    .after(millis(500))
                                    .pass(channel)
                                    .close());
            channel.pass(s, s).close();
        }
    }

    private static class InputListRunnerDeadlock extends MappingInvocation<String, String> {

        /**
         * Constructor.
         */
        protected InputListRunnerDeadlock() {
            super(null);
        }

        public void onInput(final String s, @NotNull final Channel<String, ?> result) {
            JRoutineCore.with(IdentityInvocation.<String>factoryOf())
                        .invocationConfiguration()
                        .withInputMaxSize(1)
                        .withInputBackoff(infinity())
                        .applied()
                        .async()
                        .after(millis(500))
                        .pass(s)
                        .immediately()
                        .pass(Collections.singletonList(s))
                        .close()
                        .all();
        }
    }

    private static class InputRunnerDeadlock extends MappingInvocation<String, String> {

        /**
         * Constructor.
         */
        protected InputRunnerDeadlock() {
            super(null);
        }

        public void onInput(final String s, @NotNull final Channel<String, ?> result) {
            JRoutineCore.with(IdentityInvocation.<String>factoryOf())
                        .invocationConfiguration()
                        .withInputMaxSize(1)
                        .withInputBackoff(infinity())
                        .applied()
                        .async()
                        .after(millis(500))
                        .pass(s)
                        .immediately()
                        .pass(s)
                        .close()
                        .all();
        }
    }

    private static class NextInvocation extends MappingInvocation<Object, Object> {

        /**
         * Constructor.
         */
        protected NextInvocation() {
            super(null);
        }

        public void onInput(final Object o, @NotNull final Channel<Object, ?> result) {
            JRoutineCore.with(factoryOf(DelayedInvocation.class, millis(100)))
                        .parallel("test")
                        .after(seconds(1))
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

    private static class ResultArrayRunnerDeadlock extends MappingInvocation<String, String> {

        /**
         * Constructor.
         */
        protected ResultArrayRunnerDeadlock() {
            super(null);
        }

        public void onInput(final String s, @NotNull final Channel<String, ?> result) {
            result.after(millis(500)).pass(s).after(millis(100)).pass(new String[]{s});
        }
    }

    private static class ResultListRunnerDeadlock extends MappingInvocation<String, String> {

        /**
         * Constructor.
         */
        protected ResultListRunnerDeadlock() {
            super(null);
        }

        public void onInput(final String s, @NotNull final Channel<String, ?> result) {
            result.after(millis(500)).pass(s).after(millis(100)).pass(Collections.singletonList(s));
        }
    }

    private static class ResultRunnerDeadlock extends MappingInvocation<String, String> {

        /**
         * Constructor.
         */
        protected ResultRunnerDeadlock() {
            super(null);
        }

        public void onInput(final String s, @NotNull final Channel<String, ?> result) {

            result.after(millis(500)).pass(s).after(millis(100)).pass(s);
        }
    }

    private static class SleepCommand extends CommandInvocation<Void> {

        /**
         * Constructor.
         */
        protected SleepCommand() {
            super(null);
        }

        public void onComplete(@NotNull final Channel<Void, ?> result) throws Exception {
            seconds(1).sleepAtLeast();
        }
    }

    private static class SleepInvocation extends MappingInvocation<Object, Object> {

        private final UnitDuration mSleepDuration;

        private SleepInvocation(@NotNull final UnitDuration sleepDuration) {
            super(asArgs(sleepDuration));
            mSleepDuration = sleepDuration;
        }

        public void onInput(final Object input, @NotNull final Channel<Object, ?> result) {
            try {
                mSleepDuration.sleepAtLeast();

            } catch (final InterruptedException e) {
                throw new InvocationInterruptedException(e);
            }

            result.pass(input);
        }
    }

    private static class SquareInvocation extends MappingInvocation<Integer, Integer> {

        /**
         * Constructor.
         */
        protected SquareInvocation() {
            super(null);
        }

        public void onInput(final Integer integer, @NotNull final Channel<Integer, ?> result) {
            final int input = integer;
            result.pass(input * input);
        }
    }

    private static class TestAbortHandler implements AbortHandler {

        public void onAbort(@NotNull final RoutineException reason, final long delay,
                @NotNull final TimeUnit timeUnit) {

        }
    }

    private static class TestDiscard extends TemplateInvocation<String, String> {

        private static final AtomicInteger sInstanceCount = new AtomicInteger();

        public TestDiscard() {
            sInstanceCount.incrementAndGet();
        }

        public static int getInstanceCount() {
            return sInstanceCount.get();
        }

        @Override
        public void onInput(final String input, @NotNull final Channel<String, ?> result) {
            result.after(millis(100)).pass(input);
        }

        @Override
        public void onDiscard() {
            sInstanceCount.decrementAndGet();
        }
    }

    private static class TestDiscardException extends TestDiscard {

        @Override
        public void onDiscard() {
            super.onDiscard();
            throw new IllegalArgumentException("test");
        }
    }

    private static class TestDiscardOnAbort extends TestDiscard {

        @Override
        public void onAbort(@NotNull final RoutineException reason) throws Exception {
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
        public void onInput(final String input, @NotNull final Channel<String, ?> result) {
            result.after(millis(1000)).pass(input);
        }

        @Override
        public void onAbort(@NotNull final RoutineException reason) {
            if (!sActive) {
                sIsError = true;
            }
        }

        @Override
        public void onRestart() {
            sActive = true;
        }

        @Override
        public void onComplete(@NotNull final Channel<String, ?> result) {
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

    private static class TestRunner extends SyncRunner {

        private final ArrayList<Execution> mExecutions = new ArrayList<Execution>();

        @Override
        public boolean isExecutionThread() {
            return false;
        }

        @Override
        public void run(@NotNull final Execution execution, final long delay,
                @NotNull final TimeUnit timeUnit) {
            mExecutions.add(execution);
        }

        private void run(int count) {
            final ArrayList<Execution> executions = mExecutions;
            while (!executions.isEmpty() && (count-- > 0)) {
                final Execution execution = executions.remove(0);
                execution.run();
            }
        }
    }
}
