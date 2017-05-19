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

import com.github.dm.jrt.core.InvocationExecution.ExecutionObserver;
import com.github.dm.jrt.core.InvocationExecution.InputData;
import com.github.dm.jrt.core.ResultChannel.AbortHandler;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ChannelConsumer;
import com.github.dm.jrt.core.channel.InputDeadlockException;
import com.github.dm.jrt.core.channel.OutputDeadlockException;
import com.github.dm.jrt.core.channel.OutputTimeoutException;
import com.github.dm.jrt.core.channel.TemplateChannelConsumer;
import com.github.dm.jrt.core.common.DeadlockException;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.common.TimeoutException;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.config.ChannelConfiguration.TimeoutActionType;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.AgingPriority;
import com.github.dm.jrt.core.executor.SyncExecutor;
import com.github.dm.jrt.core.invocation.CallInvocation;
import com.github.dm.jrt.core.invocation.CommandInvocation;
import com.github.dm.jrt.core.invocation.InterruptedInvocationException;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationDeadlockException;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.MappingInvocation;
import com.github.dm.jrt.core.invocation.TemplateInvocation;
import com.github.dm.jrt.core.log.Log.Level;
import com.github.dm.jrt.core.log.Logger;
import com.github.dm.jrt.core.log.NullLog;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.core.util.DurationMeasure;

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

import static com.github.dm.jrt.core.common.BackoffBuilder.afterCount;
import static com.github.dm.jrt.core.config.InvocationConfiguration.builder;
import static com.github.dm.jrt.core.executor.ScheduledExecutors.defaultExecutor;
import static com.github.dm.jrt.core.executor.ScheduledExecutors.syncExecutor;
import static com.github.dm.jrt.core.invocation.InvocationFactory.factoryOf;
import static com.github.dm.jrt.core.invocation.InvocationFactory.factoryOfParallel;
import static com.github.dm.jrt.core.util.DurationMeasure.indefiniteTime;
import static com.github.dm.jrt.core.util.DurationMeasure.millis;
import static com.github.dm.jrt.core.util.DurationMeasure.noTime;
import static com.github.dm.jrt.core.util.DurationMeasure.seconds;
import static com.github.dm.jrt.core.util.Reflection.asArgs;
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
    final DurationMeasure timeout = seconds(1);
    final Routine<String, String> routine =
        JRoutineCore.routine().of(factoryOf(DelayedInvocation.class, millis(100)));
    final Channel<String, String> channel = routine.invoke().pass("test1");
    assertThat(channel.isOpen()).isTrue();
    assertThat(channel.abort(new IllegalArgumentException("test1"))).isTrue();
    assertThat(channel.isOpen()).isFalse();

    final Channel<String, String> channel1 =
        routine.invoke().after(millis(10)).pass("test1").afterNoDelay().close();
    assertThat(channel1.isOpen()).isFalse();
    assertThat(channel1.in(timeout).getComplete()).isTrue();
    assertThat(channel1.abort()).isFalse();
    assertThat(channel1.isOpen()).isFalse();
    assertThat(channel1.in(timeout).hasNext()).isTrue();
    assertThat(channel1.in(timeout).all()).containsExactly("test1");
    assertThat(channel1.in(timeout).hasNext()).isFalse();

    final Channel<String, String> channel2 = routine.invoke().after(millis(10)).pass("test2");
    assertThat(channel2.isOpen()).isTrue();
    assertThat(channel2.afterNoDelay().abort(new IllegalArgumentException("test2"))).isTrue();
    assertThat(channel2.in(timeout).getComplete()).isTrue();
    assertThat(channel2.abort()).isFalse();
    assertThat(channel2.isOpen()).isFalse();
    try {
      channel2.in(timeout).all();
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
    assertThat(channel2.getComplete()).isTrue();
    assertThat(channel2.isOpen()).isFalse();
    final Channel<String, String> channel3 = routine.invoke().after(millis(1000000)).pass("test2");
    assertThat(channel3.isOpen()).isTrue();
    assertThat(channel3.afterNoDelay().abort()).isTrue();
    assertThat(channel3.abort(new IllegalArgumentException("test2"))).isFalse();
    assertThat(channel3.isOpen()).isFalse();
    try {
      channel3.in(timeout).all();
      fail();

    } catch (final AbortException ex) {
      assertThat(ex.getCause()).isNull();
    }

    assertThat(channel3.getComplete()).isTrue();
    assertThat(channel3.isOpen()).isFalse();
    try {
      JRoutineCore.routine()
                  .of(new AbortInvocation())
                  .invoke()
                  .after(millis(10))
                  .pass("test_abort")
                  .in(timeout)
                  .next();
      fail();

    } catch (final AbortException ex) {
      assertThat(ex.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
      assertThat(ex.getCause().getMessage()).isEqualTo("test_abort");
    }

    try {
      JRoutineCore.routine()
                  .of(new AbortInvocation2())
                  .invoke()
                  .after(millis(10))
                  .pass("test_abort")
                  .in(timeout)
                  .next();
      fail();

    } catch (final AbortException ex) {
      assertThat(ex.getCause()).isNull();
    }

    final Semaphore semaphore = new Semaphore(0);
    final AtomicBoolean isFailed = new AtomicBoolean(false);
    assertThat(JRoutineCore.routine()
                           .withInvocation()
                           .withLogLevel(Level.SILENT)
                           .configuration()
                           .of(new CloseInvocation(semaphore, isFailed))
                           .invoke()
                           .pass("test")
                           .close()
                           .in(timeout)
                           .all()).isEmpty();
    semaphore.tryAcquire(1, 1, TimeUnit.SECONDS);
    assertThat(isFailed.get()).isFalse();
    final Channel<Object, Object> channel4 =
        JRoutineCore.routine().of(IdentityInvocation.factory()).invoke();
    channel4.after(millis(300)).abort(new IllegalArgumentException("test_abort"));
    try {
      channel4.close().in(seconds(1)).throwError();

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
        JRoutineCore.routine().of(factoryOf(abortInvocation, this, abortReason, semaphore));
    final Channel<String, String> channel = routine.invoke();
    final IllegalArgumentException exception = new IllegalArgumentException();
    channel.after(millis(100)).abort(exception);
    semaphore.tryAcquire(1, TimeUnit.SECONDS);
    assertThat(abortReason.get().getCause()).isEqualTo(exception);
    final Channel<String, String> channel1 = routine.invoke();
    final IllegalAccessError exception1 = new IllegalAccessError();
    channel1.afterNoDelay().abort(exception1);
    semaphore.tryAcquire(1, TimeUnit.SECONDS);
    assertThat(abortReason.get().getCause()).isEqualTo(exception1);
  }

  @Test
  public void testAgingPriority() {
    final TestExecutor executor = new TestExecutor();
    final Routine<Object, Object> routine1 = JRoutineCore.routineOn(executor)
                                                         .withInvocation()
                                                         .withPriority(
                                                             AgingPriority.NORMAL_PRIORITY)
                                                         .configuration()
                                                         .of(IdentityInvocation.factory());
    final Routine<Object, Object> routine2 = JRoutineCore.routineOn(executor)
                                                         .withInvocation()
                                                         .withPriority(AgingPriority.HIGH_PRIORITY)
                                                         .configuration()
                                                         .of(IdentityInvocation.factory());
    final Channel<Object, Object> output1 =
        routine1.invoke().pass("test1").close().eventuallyContinue();
    final Channel<Object, Object> input2 = routine2.invoke();
    for (int i = 0; i < AgingPriority.HIGH_PRIORITY - 1; i++) {
      input2.pass("test2");
      executor.run(1);
      assertThat(output1.all()).isEmpty();
    }

    final Channel<Object, Object> output2 = input2.pass("test2").close();
    executor.run(1);
    assertThat(output1.all()).containsExactly("test1");
    executor.run(Integer.MAX_VALUE);
    final List<Object> result2 = output2.all();
    assertThat(result2).hasSize(AgingPriority.HIGH_PRIORITY);
    assertThat(result2).containsOnly("test2");
  }

  @Test
  public void testBind() {
    final TestChannelConsumer consumer = new TestChannelConsumer();
    final Channel<Object, Object> channel1 = JRoutineCore.routine()
                                                         .of(IdentityInvocation.factory())
                                                         .invoke()
                                                         .after(seconds(1))
                                                         .pass("test1");
    channel1.consume(consumer);
    assertThat(channel1.isBound()).isTrue();
    assertThat(consumer.isOutput()).isFalse();
    final Channel<Object, Object> channel2 = JRoutineCore.routineOn(syncExecutor())
                                                         .of(IdentityInvocation.factory())
                                                         .invoke()
                                                         .pass("test2");
    channel2.consume(consumer);
    assertThat(channel1.isBound()).isTrue();
    assertThat(channel2.isBound()).isTrue();
    assertThat(consumer.isOutput()).isTrue();
    assertThat(consumer.getOutput()).isEqualTo("test2");
  }

  @Test
  public void testCalls() {
    final DurationMeasure timeout = seconds(1);
    final Routine<String, String> routine =
        JRoutineCore.routineOn(syncExecutor()).of(IdentityInvocation.<String>factory());
    final Routine<String, String> parallelRoutine =
        JRoutineCore.routineOn(syncExecutor()).of(factoryOfParallel(routine));
    assertThat(routine.invoke().close().in(timeout).all()).isEmpty();
    assertThat(routine.invoke()
                      .pass(Arrays.asList("test1", "test2"))
                      .close()
                      .in(timeout)
                      .all()).containsExactly("test1", "test2");
    assertThat(routine.invoke()
                      .pass(routine.invoke().pass("test1", "test2").close())
                      .close()
                      .in(timeout)
                      .all()).containsExactly("test1", "test2");
    assertThat(routine.invoke().pass("test1").close().in(timeout).all()).containsExactly("test1");
    assertThat(routine.invoke().pass("test1", "test2").close().in(timeout).all()).containsExactly(
        "test1", "test2");
    assertThat(parallelRoutine.invoke().close().in(timeout).all()).isEmpty();
    assertThat(parallelRoutine.invoke()
                              .pass(Arrays.asList("test1", "test2"))
                              .close()
                              .in(timeout)
                              .all()).containsOnly("test1", "test2");
    assertThat(parallelRoutine.invoke()
                              .pass(routine.invoke().pass("test1", "test2").close())
                              .close()
                              .in(timeout)
                              .all()).containsOnly("test1", "test2");
    assertThat(parallelRoutine.invoke().pass("test1").close().in(timeout).all()).containsOnly(
        "test1");
    assertThat(
        parallelRoutine.invoke().pass("test1", "test2").close().in(timeout).all()).containsOnly(
        "test1", "test2");

    assertThat(routine.invoke().pass().close().in(timeout).all()).isEmpty();
    assertThat(routine.invoke()
                      .pass(Arrays.asList("test1", "test2"))
                      .close()
                      .in(timeout)
                      .all()).containsExactly("test1", "test2");
    assertThat(routine.invoke()
                      .pass(routine.invoke().pass("test1", "test2").close())
                      .close()
                      .in(timeout)
                      .all()).containsExactly("test1", "test2");
    assertThat(routine.invoke().pass("test1").close().in(timeout).all()).containsExactly("test1");
    assertThat(routine.invoke().pass("test1", "test2").close().in(timeout).all()).containsExactly(
        "test1", "test2");
    assertThat(parallelRoutine.invoke().pass().close().in(timeout).all()).isEmpty();
    assertThat(parallelRoutine.invoke()
                              .pass(Arrays.asList("test1", "test2"))
                              .close()
                              .in(timeout)
                              .all()).containsOnly("test1", "test2");
    assertThat(parallelRoutine.invoke()
                              .pass(routine.invoke().pass("test1", "test2").close())
                              .close()
                              .in(timeout)
                              .all()).containsOnly("test1", "test2");
    assertThat(parallelRoutine.invoke().pass("test1").close().in(timeout).all()).containsOnly(
        "test1");
    assertThat(
        parallelRoutine.invoke().pass("test1", "test2").close().in(timeout).all()).containsOnly(
        "test1", "test2");
  }

  @Test
  public void testChainedRoutine() {
    final DurationMeasure timeout = seconds(1);
    final MappingInvocation<String, Integer> parse = new MappingInvocation<String, Integer>(null) {

      public void onInput(final String input, @NotNull final Channel<Integer, ?> result) {
        result.pass(Integer.parseInt(input));
      }
    };
    final Routine<String, Integer> parseRoutine =
        JRoutineCore.routine().of(factoryOfParallel(JRoutineCore.routine().of(parse)));
    final CallInvocation<Integer, Integer> sum = new CallInvocation<Integer, Integer>() {

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
        JRoutineCore.routineOn(syncExecutor()).of(factoryOf(sum, this));
    final Routine<Integer, Integer> squareRoutine = JRoutineCore.routine()
                                                                .of(factoryOfParallel(
                                                                    JRoutineCore.routine()
                                                                                .of(new
                                                                                    SquareInvocation())));
    assertThat(sumRoutine.invoke()
                         .pass(squareRoutine.invoke().pass(1, 2, 3, 4).close())
                         .close()
                         .in(timeout)
                         .all()).containsExactly(30);
    assertThat(sumRoutine.invoke()
                         .pass(squareRoutine.invoke().pass(1, 2, 3, 4).close())
                         .close()
                         .in(timeout)
                         .all()).containsExactly(30);
    assertThat(sumRoutine.invoke()
                         .pass(squareRoutine.invoke()
                                            .pass(parseRoutine.invoke()
                                                              .pass("1", "2", "3", "4")
                                                              .close())
                                            .close())
                         .close()
                         .in(timeout)
                         .all()).containsExactly(30);
    assertThat(parseRoutine.invoke()
                           .pipe(squareRoutine.invoke())
                           .pipe(sumRoutine.invoke())
                           .pass("1", "2", "3", "4")
                           .close()
                           .in(timeout)
                           .all()).containsExactly(30);
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testChannelError() {
    final Logger logger = Logger.newLogger(new NullLog(), Level.DEBUG, this);
    try {
      new InvocationChannel<Object, Object>(InvocationConfiguration.defaultConfiguration(), null,
          new ConcurrentExecutor(defaultExecutor()), logger);
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
          new TestInvocationManager(), new ConcurrentExecutor(defaultExecutor()), null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      new InvocationChannel<Object, Object>(null, new TestInvocationManager(),
          new ConcurrentExecutor(defaultExecutor()), logger);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      final InvocationChannel<Object, Object> channel =
          new InvocationChannel<Object, Object>(InvocationConfiguration.defaultConfiguration(),
              new TestInvocationManager(), new ConcurrentExecutor(defaultExecutor()), logger);
      channel.close();
      channel.pass("test");
      fail();

    } catch (final IllegalStateException ignored) {
    }

    try {

      final InvocationChannel<Object, Object> channel =
          new InvocationChannel<Object, Object>(InvocationConfiguration.defaultConfiguration(),
              new TestInvocationManager(), new ConcurrentExecutor(defaultExecutor()), logger);
      channel.after(null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      final InvocationChannel<Object, Object> channel =
          new InvocationChannel<Object, Object>(InvocationConfiguration.defaultConfiguration(),
              new TestInvocationManager(), new ConcurrentExecutor(defaultExecutor()), logger);
      channel.after(1, null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      final InvocationChannel<Object, Object> channel =
          new InvocationChannel<Object, Object>(InvocationConfiguration.defaultConfiguration(),
              new TestInvocationManager(), new ConcurrentExecutor(defaultExecutor()), logger);
      channel.after(-1, TimeUnit.MILLISECONDS);
      fail();

    } catch (final IllegalArgumentException ignored) {
    }

    try {

      final InvocationChannel<Object, Object> channel =
          new InvocationChannel<Object, Object>(InvocationConfiguration.defaultConfiguration(),
              new TestInvocationManager(), new ConcurrentExecutor(defaultExecutor()), logger);
      channel.in(null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      final InvocationChannel<Object, Object> channel =
          new InvocationChannel<Object, Object>(InvocationConfiguration.defaultConfiguration(),
              new TestInvocationManager(), new ConcurrentExecutor(defaultExecutor()), logger);
      channel.in(1, null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      final InvocationChannel<Object, Object> channel =
          new InvocationChannel<Object, Object>(InvocationConfiguration.defaultConfiguration(),
              new TestInvocationManager(), new ConcurrentExecutor(defaultExecutor()), logger);
      channel.in(-1, TimeUnit.MILLISECONDS);
      fail();

    } catch (final IllegalArgumentException ignored) {
    }
  }

  @Test
  public void testComposedRoutine() {
    final DurationMeasure timeout = seconds(1);
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
        JRoutineCore.routine().of(factoryOf(execSum, this));
    final Routine<Integer, Integer> squareRoutine =
        JRoutineCore.routine().of(new SquareInvocation());
    final TemplateInvocation<Integer, Integer> invokeSquareSum =
        new TemplateInvocation<Integer, Integer>() {

          private Channel<Integer, Integer> mChannel;

          @Override
          public void onAbort(@NotNull final RoutineException reason) {
            mChannel.abort(reason);
          }

          @Override
          public void onStart() {
            mChannel = sumRoutine.invoke();
          }

          @Override
          public void onInput(final Integer integer, @NotNull final Channel<Integer, ?> result) {
            squareRoutine.invoke().pass(integer).close().pipe(mChannel);
          }

          @Override
          public void onComplete(@NotNull final Channel<Integer, ?> result) {
            result.pass(mChannel.close());
          }
        };
    final Routine<Integer, Integer> squareSumRoutine =
        JRoutineCore.routine().of(factoryOf(invokeSquareSum, this, sumRoutine, squareRoutine));
    assertThat(
        squareSumRoutine.invoke().pass(1, 2, 3, 4).close().in(timeout).all()).containsExactly(30);
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testConfigurationErrors() {
    try {
      new DefaultRoutineBuilder(defaultExecutor()).withConfiguration(null);
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
    final Routine<Object, Object> routine2 = JRoutineCore.routine().of(new AllInvocation());
    try {
      routine2.invoke().pass("test").close().in(seconds(1)).all();
      fail();

    } catch (final DeadlockException ignored) {
    }
  }

  @Test
  public void testDeadlockOnCheckComplete() {
    final Routine<Object, Object> routine1 =
        JRoutineCore.routine().of(new CheckCompleteInvocation());
    try {
      routine1.invoke().pass("test").close().in(seconds(1)).all();
      fail();

    } catch (final DeadlockException ignored) {
    }
  }

  @Test
  public void testDeadlockOnHasNext() {
    final Routine<Object, Object> routine3 = JRoutineCore.routine().of(new HasNextInvocation());
    try {
      routine3.invoke().pass("test").close().in(seconds(1)).all();
      fail();

    } catch (final DeadlockException ignored) {
    }
  }

  @Test
  public void testDeadlockOnNext() {
    final Routine<Object, Object> routine4 = JRoutineCore.routine().of(new NextInvocation());
    try {
      routine4.invoke().pass("test").close().in(seconds(1)).all();
      fail();

    } catch (final DeadlockException ignored) {
    }
  }

  @Test
  public void testDelay() {
    long startTime = System.currentTimeMillis();
    final Channel<String, String> channel =
        JRoutineCore.routine().of(factoryOf(DelayedInvocation.class, millis(10))).invoke();
    channel.after(100, TimeUnit.MILLISECONDS).pass("test1");
    channel.after(millis(10).nanosTime()).pass("test2");
    channel.after(millis(10).microsTime()).pass("test3", "test4");
    channel.after(millis(10)).pass((String[]) null);
    channel.afterNoDelay().pass((List<String>) null).pass((Channel<String, String>) null);
    assertThat(channel.close().in(3, TimeUnit.SECONDS).all()).containsOnly("test1", "test2",
        "test3", "test4");
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(110);

    startTime = System.currentTimeMillis();

    final Channel<String, String> channel1 = JRoutineCore.routine()
                                                         .withInvocation()
                                                         .withInputOrder(OrderType.SORTED)
                                                         .withOutputOrder(OrderType.SORTED)
                                                         .configuration()
                                                         .of(factoryOf(ClassToken.tokenOf(
                                                             DelayedInvocation.class), millis(10)))
                                                         .invoke();
    channel1.after(100, TimeUnit.MILLISECONDS).pass("test1");
    channel1.after(millis(10).nanosTime()).pass("test2");
    channel1.after(millis(10).microsTime()).pass(Arrays.asList("test3", "test4"));
    channel1.after(millis(10)).pass((String[]) null);
    channel1.afterNoDelay().pass((List<String>) null).pass((Channel<String, String>) null);
    assertThat(channel1.close().in(seconds(7000)).all()).containsExactly("test1", "test2", "test3",
        "test4");
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(110);

    startTime = System.currentTimeMillis();
    final Channel<String, String> channel2 =
        JRoutineCore.routine().of(factoryOf(DelayedListInvocation.class, millis(10), 2)).invoke();
    channel2.after(100, TimeUnit.MILLISECONDS).pass("test1");
    channel2.after(millis(10).nanosTime()).pass("test2");
    channel2.after(millis(10).microsTime()).pass("test3", "test4");
    channel2.after(millis(10)).pass((String[]) null);
    channel2.afterNoDelay().pass((List<String>) null).pass((Channel<String, String>) null);
    assertThat(channel2.close().in(3, TimeUnit.SECONDS).all()).containsOnly("test1", "test2",
        "test3", "test4");
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(110);

    startTime = System.currentTimeMillis();
    final InvocationConfiguration configuration =
        builder().withInputOrder(OrderType.SORTED).withOutputOrder(OrderType.SORTED).configuration();
    final Channel<String, String> channel3 = JRoutineCore.routine()
                                                         .withInvocation()
                                                         .withPatch(configuration)
                                                         .configuration()
                                                         .of(factoryOf(DelayedListInvocation.class,
                                                             millis(10), 2))
                                                         .invoke();
    channel3.after(100, TimeUnit.MILLISECONDS).pass("test1");
    channel3.after(millis(10).nanosTime()).pass("test2");
    channel3.after(millis(10).microsTime()).pass("test3", "test4");
    channel3.after(millis(10)).pass((String[]) null);
    channel3.afterNoDelay().pass((List<String>) null).pass((Channel<String, String>) null);
    assertThat(channel3.close().in(3, TimeUnit.SECONDS).all()).containsExactly("test1", "test2",
        "test3", "test4");
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(110);

    startTime = System.currentTimeMillis();
    final Channel<String, String> channel4 =
        JRoutineCore.routine().of(factoryOf(DelayedListInvocation.class, noTime(), 2)).invoke();
    channel4.after(100, TimeUnit.MILLISECONDS).pass("test1");
    channel4.after(millis(10).nanosTime()).pass("test2");
    channel4.after(millis(10).microsTime()).pass("test3", "test4");
    channel4.after(millis(10)).pass((String[]) null);
    channel4.afterNoDelay().pass((List<String>) null).pass((Channel<String, String>) null);
    assertThat(channel4.close().in(3, TimeUnit.SECONDS).all()).containsOnly("test1", "test2",
        "test3", "test4");
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(100);

    startTime = System.currentTimeMillis();
    final Channel<String, String> channel5 = JRoutineCore.routine()
                                                         .withInvocation()
                                                         .withPatch(configuration)
                                                         .configuration()
                                                         .of(factoryOf(DelayedListInvocation.class,
                                                             noTime(), 2))
                                                         .invoke();
    channel5.after(100, TimeUnit.MILLISECONDS).pass("test1");
    channel5.after(millis(10).nanosTime()).pass("test2");
    channel5.after(millis(10).microsTime()).pass("test3", "test4");
    channel5.after(millis(10)).pass((String[]) null);
    channel5.afterNoDelay().pass((List<String>) null).pass((Channel<String, String>) null);
    assertThat(channel5.close().in(3, TimeUnit.SECONDS).all()).containsExactly("test1", "test2",
        "test3", "test4");
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(100);

    startTime = System.currentTimeMillis();
    final Channel<String, String> channel6 =
        JRoutineCore.routine().of(factoryOf(DelayedChannelInvocation.class, millis(10))).invoke();
    channel6.after(100, TimeUnit.MILLISECONDS).pass("test1");
    channel6.after(millis(10).nanosTime()).pass("test2");
    channel6.after(millis(10).microsTime()).pass("test3", "test4");
    channel6.after(millis(10)).pass((String[]) null);
    channel6.afterNoDelay().pass((List<String>) null).pass((Channel<String, String>) null);
    assertThat(channel6.close().in(3, TimeUnit.SECONDS).all()).containsOnly("test1", "test2",
        "test3", "test4");
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(110);

    startTime = System.currentTimeMillis();
    final Channel<String, String> channel7 = JRoutineCore.routine()
                                                         .withInvocation()
                                                         .withPatch(configuration)
                                                         .configuration()
                                                         .of(factoryOf(
                                                             DelayedChannelInvocation.class,
                                                             millis(10)))
                                                         .invoke();
    channel7.after(100, TimeUnit.MILLISECONDS).pass("test1");
    channel7.after(millis(10).nanosTime()).pass("test2");
    channel7.after(millis(10).microsTime()).pass("test3", "test4");
    channel7.after(millis(10)).pass((String[]) null);
    channel7.afterNoDelay().pass((List<String>) null).pass((Channel<String, String>) null);
    assertThat(channel7.close().in(3, TimeUnit.SECONDS).all()).containsExactly("test1", "test2",
        "test3", "test4");
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(110);

    startTime = System.currentTimeMillis();
    final Channel<String, String> channel8 =
        JRoutineCore.routine().of(factoryOf(DelayedChannelInvocation.class, noTime())).invoke();
    channel8.after(100, TimeUnit.MILLISECONDS).pass("test1");
    channel8.after(millis(10).nanosTime()).pass("test2");
    channel8.after(millis(10).microsTime()).pass("test3", "test4");
    channel8.after(millis(10)).pass((String[]) null);
    channel8.afterNoDelay().pass((List<String>) null).pass((Channel<String, String>) null);
    assertThat(channel8.close().in(3, TimeUnit.SECONDS).all()).containsOnly("test1", "test2",
        "test3", "test4");
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(100);

    startTime = System.currentTimeMillis();
    final Channel<String, String> channel9 = JRoutineCore.routine()
                                                         .withInvocation()
                                                         .withPatch(configuration)
                                                         .configuration()
                                                         .of(factoryOf(
                                                             DelayedChannelInvocation.class,
                                                             noTime()))
                                                         .invoke();
    channel9.after(100, TimeUnit.MILLISECONDS).pass("test1");
    channel9.after(millis(10).nanosTime()).pass("test2");
    channel9.after(millis(10).microsTime()).pass("test3", "test4");
    channel9.after(millis(10)).pass((String[]) null);
    channel9.afterNoDelay().pass((List<String>) null).pass((Channel<String, String>) null);
    assertThat(channel9.close().in(3, TimeUnit.SECONDS).all()).containsExactly("test1", "test2",
        "test3", "test4");
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(100);
  }

  @Test
  public void testDelayedAbort() throws InterruptedException {
    final DurationMeasure timeout = seconds(1);
    final Routine<String, String> passingRoutine =
        JRoutineCore.routine().of(IdentityInvocation.<String>factory());
    final Channel<String, String> channel1 = passingRoutine.invoke();
    channel1.after(seconds(2)).abort();
    assertThat(channel1.afterNoDelay().pass("test").close().in(timeout).next()).isEqualTo("test");
    final Channel<String, String> channel2 = passingRoutine.invoke();
    channel2.after(millis(100)).abort();
    try {
      channel2.after(millis(200)).pass("test").afterNoDelay().close().in(timeout).next();
      fail();

    } catch (final AbortException ignored) {
    }

    final Routine<String, String> abortRoutine =
        JRoutineCore.routine().of(factoryOf(DelayedAbortInvocation.class, millis(200)));
    assertThat(abortRoutine.invoke().pass("test").close().in(timeout).next()).isEqualTo("test");
    try {
      final Channel<String, String> channel = abortRoutine.invoke().pass("test").close();
      millis(500).sleepAtLeast();
      channel.in(timeout).all();
      fail();

    } catch (final AbortException ignored) {
    }
  }

  @Test
  public void testDelayedBind() {
    final DurationMeasure timeout = seconds(1);
    final Routine<Object, Object> routine1 =
        JRoutineCore.routine().of(IdentityInvocation.factory());
    final Routine<Object, Object> routine2 =
        JRoutineCore.routine().of(IdentityInvocation.factory());
    final long startTime = System.currentTimeMillis();
    assertThat(routine1.invoke()
                       .after(millis(500))
                       .pass(routine2.invoke().pass("test").close())
                       .afterNoDelay()
                       .close()
                       .in(timeout)
                       .next()).isEqualTo("test");
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(500);
  }

  @Test
  public void testDelayedClose() {
    final DurationMeasure timeout = seconds(1);
    final Routine<String, String> passingRoutine =
        JRoutineCore.routine().of(IdentityInvocation.<String>factory());
    final Channel<String, String> channel1 = passingRoutine.invoke();
    channel1.after(seconds(2)).close();
    assertThat(channel1.afterNoDelay().pass("test").in(timeout).next()).isEqualTo("test");
    assertThat(channel1.isOpen()).isTrue();
    final Channel<String, String> channel2 = passingRoutine.invoke();
    channel2.after(millis(100)).close();
    assertThat(channel2.after(millis(200)).pass("test").in(timeout).all()).containsExactly("test");
    final Channel<String, String> channel3 = passingRoutine.invoke();
    channel3.after(millis(200)).close();
    assertThat(channel3.afterNoDelay().pass("test").in(timeout).all()).containsExactly("test");
  }

  @Test
  public void testDelayedConsumer() {
    final Routine<String, String> passingRoutine =
        JRoutineCore.routine().of(IdentityInvocation.<String>factory());
    final Channel<String, String> channel1 = JRoutineCore.channel().ofType();
    final Channel<String, String> channel2 = passingRoutine.invoke();
    channel2.after(millis(300)).pass(channel1).afterNoDelay().close();
    channel1.pass("test").close();
    long startTime = System.currentTimeMillis();
    assertThat(channel2.in(seconds(1)).all()).containsExactly("test");
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(300);
    final Channel<String, String> channel3 = JRoutineCore.channel().ofType();
    final Channel<String, String> channel4 = passingRoutine.invoke();
    channel4.after(millis(300)).pass(channel3).afterNoDelay().close();
    startTime = System.currentTimeMillis();
    channel3.abort();
    assertThat(channel4.in(seconds(1)).getComplete()).isTrue();
    assertThat(channel4.getError()).isNotNull();
    assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(300);
  }

  @Test
  public void testDelegation() {
    final DurationMeasure timeout = seconds(1);
    final Routine<Object, Object> routine1 =
        JRoutineCore.routineOn(syncExecutor()).of(IdentityInvocation.factory());
    final Routine<Object, Object> routine2 = JRoutineCore.routine().of(factoryOf(routine1));
    assertThat(routine2.invoke().pass("test1").close().in(timeout).all()).containsExactly("test1");
    final Channel<Object, Object> channel = routine2.invoke().after(timeout).pass("test2");
    channel.afterNoDelay().abort(new IllegalArgumentException());
    try {
      channel.in(timeout).next();

    } catch (final AbortException e) {
      assertThat(e.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
    }

    final Routine<String, String> routine3 =
        JRoutineCore.routine().of(factoryOf(TestDiscard.class));
    final Routine<String, String> routine4 = JRoutineCore.routine().of(factoryOf(routine3));
    assertThat(routine4.invoke().pass("test4").close().in(timeout).all()).containsExactly("test4");
    routine4.clear();
    assertThat(TestDiscard.getInstanceCount()).isZero();
    final Routine<String, String> routine5 = JRoutineCore.routine()
                                                         .of(factoryOfParallel(
                                                             JRoutineCore.routine()
                                                                         .of(factoryOf(
                                                                             TestDiscard.class))));
    final Routine<String, String> routine6 = JRoutineCore.routine().of(factoryOf(routine5));
    assertThat(routine6.invoke().pass("test5").close().in(timeout).all()).containsExactly("test5");
    routine6.clear();
    assertThat(TestDiscard.getInstanceCount()).isZero();
  }

  @Test
  public void testDiscard() {
    final DurationMeasure timeout = seconds(1);
    final Routine<String, String> routine1 = JRoutineCore.routine()
                                                         .withInvocation()
                                                         .withCoreInvocations(0)
                                                         .configuration()
                                                         .of(factoryOf(TestDiscard.class));
    final Routine<String, String> routine2 = JRoutineCore.routine()
                                                         .of(factoryOfParallel(
                                                             JRoutineCore.routine()
                                                                         .withInvocation()
                                                                         .withCoreInvocations(0)
                                                                         .configuration()
                                                                         .of(factoryOf(
                                                                             TestDiscard.class))));
    assertThat(
        routine1.invoke().pass("1", "2", "3", "4", "5").close().in(timeout).all()).containsOnly("1",
        "2", "3", "4", "5");
    assertThat(
        routine2.invoke().pass("1", "2", "3", "4", "5").close().in(timeout).all()).containsOnly("1",
        "2", "3", "4", "5");
    assertThat(TestDiscard.getInstanceCount()).isZero();

    final Routine<String, String> routine3 =
        JRoutineCore.routine().of(factoryOf(TestDiscardOnAbort.class));
    Channel<String, String> channel = routine3.invoke().pass("1");
    assertThat(channel.abort()).isTrue();
    assertThat(channel.in(timeout).getComplete()).isTrue();
    channel = routine3.invoke().pass("1");
    assertThat(channel.abort()).isTrue();
    assertThat(channel.in(timeout).getComplete()).isTrue();
    assertThat(TestDiscard.getInstanceCount()).isZero();

    final Routine<String, String> routine5 =
        JRoutineCore.routine().of(factoryOf(TestDiscard.class));
    final Routine<String, String> routine6 =
        JRoutineCore.routine().of(factoryOf(TestDiscard.class));
    assertThat(
        routine5.invoke().pass("1", "2", "3", "4", "5").close().in(timeout).all()).containsOnly("1",
        "2", "3", "4", "5");
    assertThat(JRoutineCore.routine()
                           .of(factoryOfParallel(routine6))
                           .invoke()
                           .pass("1", "2", "3", "4", "5")
                           .close()
                           .in(timeout)
                           .all()).containsOnly("1", "2", "3", "4", "5");
    routine5.clear();
    routine6.clear();
    assertThat(TestDiscard.getInstanceCount()).isZero();

    final Routine<String, String> routine7 =
        JRoutineCore.routine().of(factoryOf(TestDiscardException.class));
    final Routine<String, String> routine8 =
        JRoutineCore.routine().of(factoryOf(TestDiscardException.class));
    assertThat(
        routine7.invoke().pass("1", "2", "3", "4", "5").close().in(timeout).all()).containsOnly("1",
        "2", "3", "4", "5");
    assertThat(JRoutineCore.routine()
                           .of(factoryOfParallel(routine8))
                           .invoke()
                           .pass("1", "2", "3", "4", "5")
                           .close()
                           .in(timeout)
                           .all()).containsOnly("1", "2", "3", "4", "5");
    routine7.clear();
    routine8.clear();
    assertThat(TestDiscard.getInstanceCount()).isZero();
  }

  @Test
  public void testEmpty() {
    final Channel<Object, Object> channel = JRoutineCore.routine()
                                                        .withInvocation()
                                                        .withInputBackoff(
                                                            afterCount(1).constantDelay(seconds(3)))
                                                        .configuration()
                                                        .of(new SleepInvocation(millis(500)))
                                                        .invoke();
    assertThat(channel.isEmpty()).isTrue();
    assertThat(channel.pass("test1").after(millis(500)).pass("test2").isEmpty()).isFalse();
    final Channel<Object, Object> result = channel.afterNoDelay().close();
    assertThat(result.outputSize()).isZero();
    assertThat(result.in(seconds(10)).getComplete()).isTrue();
    assertThat(channel.isEmpty()).isFalse();
    assertThat(result.isEmpty()).isFalse();
  }

  @Test
  public void testEmptyAbort() {
    final Routine<Object, Object> routine = JRoutineCore.routine()
                                                        .withInvocation()
                                                        .withInputBackoff(
                                                            afterCount(1).constantDelay(seconds(3)))
                                                        .configuration()
                                                        .of(new SleepInvocation(millis(500)));
    Channel<Object, Object> channel = routine.invoke();
    assertThat(channel.isEmpty()).isTrue();
    assertThat(channel.pass("test1").abort()).isTrue();
    assertThat(channel.isEmpty()).isTrue();
    channel = routine.invoke();
    assertThat(channel.isEmpty()).isTrue();
    assertThat(channel.pass("test1").after(millis(500)).pass("test2").isEmpty()).isFalse();
    final Channel<Object, Object> result = channel.afterNoDelay().close();
    assertThat(result.isEmpty()).isFalse();
    assertThat(result.abort()).isTrue();
    assertThat(channel.isEmpty()).isTrue();
    assertThat(result.isEmpty()).isTrue();
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testError() {
    try {
      JRoutineCore.routineOn(syncExecutor())
                  .withInvocation()
                  .withLogLevel(Level.SILENT)
                  .configuration()
                  .of(factoryOf(ConstructorException.class))
                  .invoke()
                  .close()
                  .all();
      fail();

    } catch (final InvocationException ignored) {
    }

    try {
      new AbstractRoutine<Object, Object>(null, null) {

        @NotNull
        @Override
        protected Invocation<Object, Object> newInvocation() {
          return new ConstructorException();
        }
      };
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      new DefaultRoutine<Object, Object>(null, null, new InvocationFactory<Object, Object>(null) {

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
      new DefaultRoutine<Object, Object>(InvocationConfiguration.defaultConfiguration(), null,
          null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    final Logger logger = Logger.newLogger(null, null, this);
    try {
      final ResultChannel<Object> channel =
          new ResultChannel<Object>(InvocationConfiguration.defaultConfiguration(), syncExecutor(),
              new TestAbortHandler(), logger);
      new InvocationExecution<Object, Object>(null, new TestExecutionObserver(), channel, logger);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      final ResultChannel<Object> channel =
          new ResultChannel<Object>(ChannelConfiguration.defaultConfiguration(),
              new ConcurrentExecutor(syncExecutor()), new TestAbortHandler(), logger);
      new InvocationExecution<Object, Object>(new TestInvocationManager(), null, channel, logger);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      new InvocationExecution<Object, Object>(new TestInvocationManager(),
          new TestExecutionObserver(), null, logger);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      final ResultChannel<Object> channel =
          new ResultChannel<Object>(InvocationConfiguration.defaultConfiguration(), syncExecutor(),
              new TestAbortHandler(), logger);
      new InvocationExecution<Object, Object>(new TestInvocationManager(),
          new TestExecutionObserver(), channel, null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  public void testErrorConsumerOnComplete() {
    final TemplateChannelConsumer<String> exceptionConsumer =
        new TemplateChannelConsumer<String>() {

          @Override
          public void onComplete() {
            throw new NullPointerException("test2");
          }
        };
    testConsumer(exceptionConsumer);
  }

  @Test
  public void testErrorConsumerOnOutput() {
    final TemplateChannelConsumer<String> exceptionConsumer =
        new TemplateChannelConsumer<String>() {

          @Override
          public void onOutput(final String output) {
            throw new NullPointerException(output);
          }
        };
    testConsumer(exceptionConsumer);
    final MappingInvocation<String, String> producer = new MappingInvocation<String, String>(null) {

      public void onInput(final String input, @NotNull final Channel<String, ?> result) {
        for (int i = 0; i < 100; i++) {
          result.pass(input + i, input + i);
        }
      }
    };
    final Channel<String, String> channel =
        JRoutineCore.routine().of(producer).invoke().pass("test").consume(exceptionConsumer);
    assertThat(channel.in(seconds(3)).getError()).isNotNull();
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
        JRoutineCore.routine().of(factoryOf(exceptionOnResult, this));
    testException(exceptionRoutine, "test", "test3");
    testException(JRoutineCore.routine().of(factoryOfParallel(exceptionRoutine)), "test", "test3");

    final Routine<String, String> passingRoutine =
        JRoutineCore.routine().of(IdentityInvocation.<String>factory());
    testChained(passingRoutine, JRoutineCore.routine().of(factoryOfParallel(exceptionRoutine)),
        "test", "test3");
    testChained(JRoutineCore.routine().of(factoryOfParallel(exceptionRoutine)), passingRoutine,
        "test", "test3");
    testChained(passingRoutine, exceptionRoutine, "test", "test3");
    testChained(exceptionRoutine, passingRoutine, "test", "test3");
  }

  @Test
  public void testErrorOnInit() {
    final TemplateInvocation<String, String> exceptionOnInit =
        new TemplateInvocation<String, String>() {

          @Override
          public void onStart() {
            throw new NullPointerException("test1");
          }
        };

    final Routine<String, String> exceptionRoutine =
        JRoutineCore.routine().of(factoryOf(exceptionOnInit, this));
    testException(exceptionRoutine, "test", "test1");
    testException(JRoutineCore.routine().of(factoryOfParallel(exceptionRoutine)), "test", "test1");

    final Routine<String, String> passingRoutine =
        JRoutineCore.routine().of(IdentityInvocation.<String>factory());
    testChained(passingRoutine, JRoutineCore.routine().of(factoryOfParallel(exceptionRoutine)),
        "test", "test1");
    testChained(JRoutineCore.routine().of(factoryOfParallel(exceptionRoutine)), passingRoutine,
        "test", "test1");
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
        JRoutineCore.routine().of(factoryOf(exceptionOnInput, this));
    testException(exceptionRoutine, "test2", "test2");
    testException(JRoutineCore.routine().of(factoryOfParallel(exceptionRoutine)), "test2", "test2");

    final Routine<String, String> passingRoutine =
        JRoutineCore.routine().of(IdentityInvocation.<String>factory());
    testChained(passingRoutine, JRoutineCore.routine().of(factoryOfParallel(exceptionRoutine)),
        "test2", "test2");
    testChained(JRoutineCore.routine().of(factoryOfParallel(exceptionRoutine)), passingRoutine,
        "test2", "test2");
    testChained(passingRoutine, exceptionRoutine, "test2", "test2");
    testChained(exceptionRoutine, passingRoutine, "test2", "test2");
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testFactoryError() {
    try {
      JRoutineCore.routine().of((InvocationFactory<?, ?>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  public void testIllegalBind() {
    final Channel<Object, Object> invocationChannel =
        JRoutineCore.routine().of(IdentityInvocation.factory()).invoke();
    final Channel<Object, Object> channel = JRoutineCore.channel().ofType();
    channel.consume(new TemplateChannelConsumer<Object>() {});
    try {
      invocationChannel.pass(channel);
      fail();

    } catch (final IllegalStateException ignored) {
    }
  }

  @Test
  public void testInitInvocationException() {
    final ExceptionRoutine routine =
        new ExceptionRoutine(InvocationConfiguration.defaultConfiguration());
    try {
      routine.invoke().close().in(seconds(1)).all();
      fail();

    } catch (final InvocationException e) {
      assertThat(e.getCause()).isExactlyInstanceOf(IllegalStateException.class);
    }
  }

  @Test
  public void testInitInvocationNull() {
    final NullRoutine routine = new NullRoutine(InvocationConfiguration.defaultConfiguration());
    try {
      routine.invoke().close().in(seconds(1)).all();
      fail();

    } catch (final InvocationException e) {
      assertThat(e.getCause()).isExactlyInstanceOf(NullPointerException.class);
    }
  }

  @Test
  public void testInputDeadlock() {
    try {
      JRoutineCore.routine()
                  .withInvocation()
                  .withInputMaxSize(1)
                  .configuration()
                  .of(new SleepInvocation(millis(100)))
                  .invoke()
                  .pass("test", "test")
                  .close()
                  .all();
      fail();

    } catch (final InputDeadlockException ignored) {
    }

    try {
      JRoutineCore.routine()
                  .withInvocation()
                  .withInputMaxSize(1)
                  .configuration()
                  .of(new SleepInvocation(millis(100)))
                  .invoke()
                  .pass(Arrays.asList("test", "test"))
                  .close()
                  .all();
      fail();

    } catch (final InputDeadlockException ignored) {
    }
  }

  @Test
  public void testInputDelay() {
    assertThat(JRoutineCore.routine()
                           .withInvocation()
                           .withInputOrder(OrderType.SORTED)
                           .withInputBackoff(afterCount(1).constantDelay(noTime()))
                           .configuration()
                           .of(IdentityInvocation.factory())
                           .invoke()
                           .unsorted()
                           .sorted()
                           .after(millis(100))
                           .pass("test1")
                           .afterNoDelay()
                           .pass("test2")
                           .close()
                           .in(seconds(1))
                           .all()).containsExactly("test1", "test2");

    assertThat(JRoutineCore.routine()
                           .withInvocation()
                           .withInputOrder(OrderType.SORTED)
                           .withInputBackoff(afterCount(1).constantDelay(millis(1000)))
                           .configuration()
                           .of(IdentityInvocation.factory())
                           .invoke()
                           .sorted()
                           .after(millis(100))
                           .pass("test1")
                           .afterNoDelay()
                           .pass("test2")
                           .close()
                           .in(seconds(1))
                           .all()).containsExactly("test1", "test2");

    assertThat(JRoutineCore.routine()
                           .withInvocation()
                           .withInputOrder(OrderType.SORTED)
                           .withInputBackoff(afterCount(1).constantDelay(millis(1000)))
                           .configuration()
                           .of(IdentityInvocation.factory())
                           .invoke()
                           .sorted()
                           .after(millis(100))
                           .pass("test1")
                           .afterNoDelay()
                           .pass(asArgs("test2"))
                           .close()
                           .in(seconds(1))
                           .all()).containsExactly("test1", "test2");

    assertThat(JRoutineCore.routine()
                           .withInvocation()
                           .withInputOrder(OrderType.SORTED)
                           .withInputBackoff(afterCount(1).constantDelay(millis(1000)))
                           .configuration()
                           .of(IdentityInvocation.factory())
                           .invoke()
                           .sorted()
                           .after(millis(100))
                           .pass("test1")
                           .afterNoDelay()
                           .pass(Collections.singletonList("test2"))
                           .close()
                           .in(seconds(1))
                           .all()).containsExactly("test1", "test2");

    final Channel<Object, Object> channel = JRoutineCore.channel().ofType();
    channel.pass("test2").close();
    assertThat(JRoutineCore.routine()
                           .withInvocation()
                           .withInputOrder(OrderType.SORTED)
                           .withInputBackoff(afterCount(1).constantDelay(millis(1000)))
                           .configuration()
                           .of(IdentityInvocation.factory())
                           .invoke()
                           .sorted()
                           .after(millis(100))
                           .pass("test1")
                           .afterNoDelay()
                           .pass(channel)
                           .close()
                           .in(seconds(1))
                           .all()).containsExactly("test1", "test2");
  }

  @Test
  public void testInputExecutorDeadlock() {
    try {
      JRoutineCore.routine()
                  .of(new InputExecutorDeadlock())
                  .invoke()
                  .pass("test")
                  .close()
                  .in(seconds(1))
                  .all();
      fail();

    } catch (final DeadlockException ignored) {
    }

    try {
      JRoutineCore.routine()
                  .of(new InputListExecutorDeadlock())
                  .invoke()
                  .pass("test")
                  .close()
                  .in(seconds(1))
                  .all();
      fail();

    } catch (final DeadlockException ignored) {
    }

    try {
      JRoutineCore.routine()
                  .of(new InputArrayExecutorDeadlock())
                  .invoke()
                  .pass("test")
                  .close()
                  .in(seconds(1))
                  .all();
      fail();

    } catch (final DeadlockException ignored) {
    }

    try {
      JRoutineCore.routine()
                  .of(new InputConsumerExecutorDeadlock())
                  .invoke()
                  .pass("test")
                  .close()
                  .in(seconds(1))
                  .all();
      fail();

    } catch (final DeadlockException ignored) {
    }
  }

  @Test
  public void testInputTimeoutIssue() {
    try {
      final Channel<Object, Object> channel = JRoutineCore.channel().ofType();
      channel.pass("test2").close();
      JRoutineCore.routine()
                  .withInvocation()
                  .withInputOrder(OrderType.SORTED)
                  .withInputMaxSize(1)
                  .withInputBackoff(afterCount(1).constantDelay(millis(1000)))
                  .configuration()
                  .of(IdentityInvocation.factory())
                  .invoke()
                  .sorted()
                  .after(millis(100))
                  .pass("test1")
                  .afterNoDelay()
                  .pass(channel)
                  .close()
                  .in(seconds(10))
                  .all();
      fail();

    } catch (final InputDeadlockException ignored) {
    }
  }

  @Test
  public void testInvocationLifecycle() throws InterruptedException {
    final Channel<String, String> outputChannel =
        JRoutineCore.routine().of(factoryOf(TestLifecycle.class)).invoke().pass("test").close();
    Thread.sleep(500);
    outputChannel.abort();
    outputChannel.in(indefiniteTime()).getComplete();
    assertThat(TestLifecycle.sIsError).isFalse();
  }

  @Test
  public void testInvocationNotAvailable() {
    final Routine<Void, Void> routine = JRoutineCore.routineOn(syncExecutor())
                                                    .withInvocation()
                                                    .withMaxInvocations(1)
                                                    .configuration()
                                                    .of(new SleepCommand());
    routine.invoke().pass((Void) null);
    try {
      routine.invoke().close().in(seconds(1)).next();
      fail();

    } catch (final InvocationDeadlockException ignored) {
    }
  }

  @Test
  public void testNextList() {
    assertThat(JRoutineCore.routine()
                           .of(IdentityInvocation.factory())
                           .invoke()
                           .pass("test1", "test2", "test3", "test4")
                           .close()
                           .in(seconds(1))
                           .next(2)).containsExactly("test1", "test2");
    assertThat(JRoutineCore.routine()
                           .of(IdentityInvocation.factory())
                           .invoke()
                           .pass("test1")
                           .close()
                           .eventuallyContinue()
                           .in(seconds(1))
                           .next(2)).containsExactly("test1");
    try {
      JRoutineCore.routine()
                  .of(IdentityInvocation.factory())
                  .invoke()
                  .pass("test1")
                  .after(millis(300))
                  .pass("test2")
                  .afterNoDelay()
                  .close()
                  .eventuallyAbort()
                  .in(millis(100))
                  .next(2);
      fail();

    } catch (final AbortException ignored) {
    }

    try {
      JRoutineCore.routine()
                  .of(IdentityInvocation.factory())
                  .invoke()
                  .pass("test1")
                  .after(millis(300))
                  .pass("test2")
                  .afterNoDelay()
                  .close()
                  .eventuallyAbort(new IllegalStateException())
                  .in(millis(100))
                  .next(2);
      fail();

    } catch (final AbortException e) {
      assertThat(e.getCause()).isExactlyInstanceOf(IllegalStateException.class);
    }

    try {
      JRoutineCore.routine()
                  .of(IdentityInvocation.factory())
                  .invoke()
                  .pass("test1")
                  .after(millis(300))
                  .pass("test2")
                  .afterNoDelay()
                  .close()
                  .eventuallyFail()
                  .in(millis(100))
                  .next(2);
      fail();

    } catch (final TimeoutException ignored) {
    }
  }

  @Test
  public void testNextOr() {
    assertThat(JRoutineCore.routine()
                           .of(IdentityInvocation.factory())
                           .invoke()
                           .pass("test1")
                           .close()
                           .in(seconds(1))
                           .nextOrElse(2)).isEqualTo("test1");
    assertThat(JRoutineCore.routine()
                           .of(IdentityInvocation.factory())
                           .invoke()
                           .eventuallyContinue()
                           .in(seconds(1))
                           .nextOrElse(2)).isEqualTo(2);
    try {
      JRoutineCore.routine()
                  .of(factoryOf(DelayedInvocation.class, millis(300)))
                  .invoke()
                  .pass("test1")
                  .close()
                  .eventuallyAbort()
                  .in(millis(100))
                  .nextOrElse("test2");
      fail();

    } catch (final AbortException ignored) {
    }

    try {
      JRoutineCore.routine()
                  .of(factoryOf(DelayedInvocation.class, millis(300)))
                  .invoke()
                  .pass("test1")
                  .close()
                  .eventuallyAbort(new IllegalStateException())
                  .in(millis(100))
                  .nextOrElse("test2");
      fail();

    } catch (final AbortException e) {
      assertThat(e.getCause()).isExactlyInstanceOf(IllegalStateException.class);
    }

    try {
      JRoutineCore.routine()
                  .of(factoryOf(DelayedInvocation.class, millis(300)))
                  .invoke()
                  .pass("test1")
                  .close()
                  .eventuallyFail()
                  .in(millis(100))
                  .nextOrElse("test2");
      fail();

    } catch (final TimeoutException ignored) {
    }
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testNullDelegatedRoutine() {
    try {
      factoryOf((Routine<?, ?>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  public void testOutputDeadlock() {
    final Routine<String, String> routine1 = JRoutineCore.routine()
                                                         .withInvocation()
                                                         .withOutputMaxSize(1)
                                                         .configuration()
                                                         .of(factoryOf(
                                                             new CallInvocation<String, String>() {

                                                               @Override
                                                               protected void onCall(
                                                                   @NotNull final List<? extends
                                                                       String> strings,
                                                                   @NotNull final Channel<String,
                                                                       ?> result) {
                                                                 result.pass(strings);
                                                               }
                                                             }, this));
    try {
      routine1.invoke().pass("test1", "test2").close().in(seconds(1)).all();
      fail();

    } catch (final OutputDeadlockException ignored) {
    }

    final Routine<String, String> routine2 = JRoutineCore.routine()
                                                         .withInvocation()
                                                         .withOutputMaxSize(1)
                                                         .configuration()
                                                         .of(factoryOf(
                                                             new CallInvocation<String, String>() {

                                                               @Override
                                                               protected void onCall(
                                                                   @NotNull final List<? extends
                                                                       String> strings,
                                                                   @NotNull final Channel<String,
                                                                       ?> result) {
                                                                 result.pass(strings.toArray(
                                                                     new String[strings.size()]));
                                                               }
                                                             }, this));
    try {
      routine2.invoke().pass("test1", "test2").close().in(seconds(1)).all();
      fail();

    } catch (final OutputDeadlockException ignored) {
    }
  }

  @Test
  public void testOutputTimeout() throws InterruptedException {
    final Routine<String, String> routine = JRoutineCore.routine()
                                                        .withInvocation()
                                                        .withOutputBackoff(
                                                            afterCount(1).constantDelay(noTime()))
                                                        .configuration()
                                                        .of(IdentityInvocation.<String>factory());
    final Channel<String, String> outputChannel =
        routine.invoke().pass("test1", "test2").close().in(seconds(1));
    outputChannel.getComplete();
    assertThat(outputChannel.all()).containsExactly("test1", "test2");
    final Channel<String, String> channel1 = JRoutineCore.channel()
                                                         .withChannel()
                                                         .withBackoff(afterCount(1).constantDelay(
                                                             millis(1000)))
                                                         .configuration()
                                                         .ofType();
    new Thread() {

      @Override
      public void run() {
        channel1.pass("test1").pass("test2").close();
      }
    }.start();
    millis(100).sleepAtLeast();
    assertThat(channel1.in(seconds(10)).all()).containsOnly("test1", "test2");

    final Channel<String, String> channel2 = JRoutineCore.channel()
                                                         .withChannel()
                                                         .withBackoff(afterCount(1).constantDelay(
                                                             millis(1000)))
                                                         .configuration()
                                                         .ofType();
    new Thread() {

      @Override
      public void run() {
        channel2.pass("test1").pass(new String[]{"test2"}).close();
      }
    }.start();
    millis(100).sleepAtLeast();
    assertThat(channel2.in(seconds(10)).all()).containsOnly("test1", "test2");

    final Channel<String, String> channel3 = JRoutineCore.channel()
                                                         .withChannel()
                                                         .withBackoff(afterCount(1).constantDelay(
                                                             millis(1000)))
                                                         .configuration()
                                                         .ofType();
    new Thread() {

      @Override
      public void run() {
        channel3.pass("test1").pass(Collections.singletonList("test2")).close();
      }
    }.start();
    millis(100).sleepAtLeast();
    assertThat(channel3.in(seconds(10)).all()).containsOnly("test1", "test2");

    final Channel<String, String> channel4 = JRoutineCore.channel()
                                                         .withChannel()
                                                         .withBackoff(afterCount(1).constantDelay(
                                                             millis(1000)))
                                                         .configuration()
                                                         .ofType();
    new Thread() {

      @Override
      public void run() {
        final Channel<String, String> channel = JRoutineCore.channel().ofType();
        channel.pass("test1", "test2").close();
        channel4.pass(channel).close();
      }
    }.start();
    millis(100).sleepAtLeast();
    assertThat(channel4.in(seconds(10)).all()).containsOnly("test1", "test2");
  }

  @Test
  public void testParallel() {
    assertThat(JRoutineCore.routine()
                           .withInvocation()
                           .withInputBackoff(afterCount(0).constantDelay(millis(100)))
                           .withMaxInvocations(2)
                           .configuration()
                           .of(factoryOfParallel(
                               JRoutineCore.routine().of(new SleepInvocation(millis(100)))))
                           .invoke()
                           .pass(1, 2, 3, 4, 5)
                           .close()
                           .in(seconds(1))
                           .all()).containsOnly(1, 2, 3, 4, 5);
  }

  @Test
  public void testPartialOut() {
    final TemplateInvocation<String, String> invocation = new TemplateInvocation<String, String>() {

      @Override
      public void onInput(final String s, @NotNull final Channel<String, ?> result) {
        result.afterNoDelay().pass(s).after(seconds(2)).abort();
      }
    };
    assertThat(JRoutineCore.routine()
                           .of(factoryOf(invocation, this))
                           .invoke()
                           .pass("test")
                           .close()
                           .in(millis(500))
                           .all()).containsExactly("test");
  }

  @Test
  public void testPendingInputs() {
    final Channel<Object, Object> channel =
        JRoutineCore.routine().of(IdentityInvocation.factory()).invoke();
    assertThat(channel.isOpen()).isTrue();
    channel.pass("test");
    assertThat(channel.isOpen()).isTrue();
    channel.after(millis(500)).pass("test");
    assertThat(channel.isOpen()).isTrue();
    final Channel<Object, Object> outputChannel = JRoutineCore.channel().ofType();
    channel.pass(outputChannel);
    assertThat(channel.isOpen()).isTrue();
    channel.afterNoDelay().close();
    assertThat(channel.isOpen()).isFalse();
    outputChannel.close();
    assertThat(channel.isOpen()).isFalse();
  }

  @Test
  public void testPendingInputsAbort() {
    final Channel<Object, Object> channel =
        JRoutineCore.routine().of(IdentityInvocation.factory()).invoke();
    assertThat(channel.isOpen()).isTrue();
    channel.pass("test");
    assertThat(channel.isOpen()).isTrue();
    channel.after(millis(500)).pass("test");
    assertThat(channel.isOpen()).isTrue();
    final Channel<Object, Object> outputChannel = JRoutineCore.channel().ofType();
    channel.pass(outputChannel);
    assertThat(channel.isOpen()).isTrue();
    channel.afterNoDelay().abort();
    assertThat(channel.isOpen()).isFalse();
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testResultChannelError() {
    final Logger logger = Logger.newLogger(new NullLog(), Level.DEBUG, this);
    try {
      new ResultChannel<Object>(InvocationConfiguration.defaultConfiguration(), defaultExecutor(),
          null, logger);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      new ResultChannel<Object>(InvocationConfiguration.defaultConfiguration(), null,
          new TestAbortHandler(), logger);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      new ResultChannel<Object>(InvocationConfiguration.defaultConfiguration(), defaultExecutor(),
          new TestAbortHandler(), null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      new ResultChannel<Object>(InvocationConfiguration.defaultConfiguration(), defaultExecutor(),
          new TestAbortHandler(), logger).after(null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      new ResultChannel<Object>(InvocationConfiguration.defaultConfiguration(), defaultExecutor(),
          new TestAbortHandler(), logger).after(0, null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      final ResultChannel<Object> channel =
          new ResultChannel<Object>(InvocationConfiguration.defaultConfiguration(),
              defaultExecutor(), new TestAbortHandler(), logger);
      channel.after(-1, TimeUnit.MILLISECONDS);
      fail();

    } catch (final IllegalArgumentException ignored) {
    }

    try {
      new ResultChannel<Object>(InvocationConfiguration.defaultConfiguration(), defaultExecutor(),
          new TestAbortHandler(), logger).in(null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      new ResultChannel<Object>(InvocationConfiguration.defaultConfiguration(), defaultExecutor(),
          new TestAbortHandler(), logger).in(0, null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      final ResultChannel<Object> channel =
          new ResultChannel<Object>(InvocationConfiguration.defaultConfiguration(),
              defaultExecutor(), new TestAbortHandler(), logger);
      channel.in(-1, TimeUnit.MILLISECONDS);
      fail();

    } catch (final IllegalArgumentException ignored) {
    }

    final Channel<String, String> channel = JRoutineCore.routineOn(syncExecutor())
                                                        .withInvocation()
                                                        .withLogLevel(Level.SILENT)
                                                        .configuration()
                                                        .of(factoryOf(DelayedInvocation.class,
                                                            noTime()))
                                                        .invoke();
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
      channel.in(null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      channel.in(0, null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      channel.in(-1, TimeUnit.MILLISECONDS);
      fail();

    } catch (final IllegalArgumentException ignored) {
    }

    try {
      channel.pipe((Channel<String, String>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      channel.consume(null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      channel.allInto(null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    final TemplateChannelConsumer<String> consumer = new TemplateChannelConsumer<String>() {};
    try {
      channel.consume(consumer).consume(consumer);
      fail();

    } catch (final IllegalStateException ignored) {
    }

    try {
      channel.iterator();
      fail();

    } catch (final IllegalStateException ignored) {
    }

    final Routine<String, String> routine1 = JRoutineCore.routineOn(syncExecutor())
                                                         .withInvocation()
                                                         .withLogLevel(Level.SILENT)
                                                         .configuration()
                                                         .of(factoryOf(DelayedInvocation.class,
                                                             millis(100)));
    final Iterator<String> iterator =
        routine1.invoke().pass("test").close().in(millis(500)).eventuallyContinue().iterator();
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
      routine1.invoke().inNoTime().eventuallyContinue().iterator().next();
      fail();

    } catch (final NoSuchElementException ignored) {
    }

    try {
      JRoutineCore.routine()
                  .withInvocation()
                  .withLogLevel(Level.SILENT)
                  .configuration()
                  .of(factoryOf(DelayedInvocation.class, millis(100)))
                  .invoke()
                  .pass("test")
                  .close()
                  .inNoTime()
                  .iterator()
                  .next();
      fail();

    } catch (final OutputTimeoutException ignored) {
    }
  }

  @Test
  public void testResultExecutorDeadlock() {
    try {
      JRoutineCore.routine()
                  .withInvocation()
                  .withOutputMaxSize(1)
                  .configuration()
                  .of(new ResultExecutorDeadlock())
                  .invoke()
                  .pass("test")
                  .close()
                  .in(seconds(1))
                  .all();
      fail();

    } catch (final OutputDeadlockException ignored) {
    }

    try {
      JRoutineCore.routine()
                  .withInvocation()
                  .withOutputMaxSize(1)
                  .configuration()
                  .of(new ResultListExecutorDeadlock())
                  .invoke()
                  .pass("test")
                  .close()
                  .in(seconds(1))
                  .all();
      fail();

    } catch (final OutputDeadlockException ignored) {
    }

    try {
      JRoutineCore.routine()
                  .withInvocation()
                  .withOutputMaxSize(1)
                  .configuration()
                  .of(new ResultArrayExecutorDeadlock())
                  .invoke()
                  .pass("test")
                  .close()
                  .in(seconds(1))
                  .all();
      fail();

    } catch (final OutputDeadlockException ignored) {
    }
  }

  @Test
  public void testRoutine() {
    final DurationMeasure timeout = seconds(1);
    final TemplateInvocation<Integer, Integer> execSquare =
        new TemplateInvocation<Integer, Integer>() {

          @Override
          public void onInput(final Integer integer, @NotNull final Channel<Integer, ?> result) {
            final int input = integer;
            result.pass(input * input);
          }
        };

    final Routine<Integer, Integer> squareRoutine =
        JRoutineCore.routine().of(factoryOf(execSquare, this));
    assertThat(squareRoutine.invoke().pass(1, 2, 3, 4).close().in(timeout).all()).containsExactly(1,
        4, 9, 16);
    assertThat(JRoutineCore.routine()
                           .of(factoryOfParallel(squareRoutine))
                           .invoke()
                           .pass(1, 2, 3, 4)
                           .close()
                           .in(timeout)
                           .all()).containsOnly(1, 4, 9, 16);
  }

  @Test
  public void testRoutineBuilder() {
    assertThat(JRoutineCore.routineOn(syncExecutor())
                           .withInvocation()
                           .withCoreInvocations(0)
                           .withMaxInvocations(1)
                           .withInputBackoff(afterCount(2).constantDelay(1, TimeUnit.SECONDS))
                           .withInputMaxSize(2)
                           .withOutputBackoff(afterCount(1).constantDelay(1, TimeUnit.SECONDS))
                           .withOutputMaxSize(2)
                           .withOutputOrder(OrderType.SORTED)
                           .configuration()
                           .of(factoryOf(new ClassToken<IdentityInvocation<String>>() {}))
                           .invoke()
                           .pass("test1", "test2")
                           .close()
                           .all()).containsExactly("test1", "test2");

    assertThat(JRoutineCore.routineOn(syncExecutor())
                           .withInvocation()
                           .withCoreInvocations(0)
                           .withMaxInvocations(1)
                           .withInputBackoff(afterCount(2).constantDelay(noTime()))
                           .withInputMaxSize(2)
                           .withOutputBackoff(afterCount(2).constantDelay(noTime()))
                           .withOutputMaxSize(2)
                           .withOutputOrder(OrderType.SORTED)
                           .configuration()
                           .of(factoryOf(new ClassToken<IdentityInvocation<String>>() {}))
                           .invoke()
                           .pass("test1", "test2")
                           .close()
                           .all()).containsExactly("test1", "test2");
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testRoutineBuilderError() {
    try {
      new DefaultRoutineBuilder(null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  public void testRoutineFunction() {
    final DurationMeasure timeout = seconds(1);
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
        JRoutineCore.routine().of(factoryOf(execSum, this));
    assertThat(sumRoutine.invoke().pass(1, 2, 3, 4).close().in(timeout).all()).containsExactly(10);
  }

  @Test
  public void testSingleton() {
    final Routine<Integer, Integer> sumRoutine =
        JRoutineCore.routine().ofSingleton(new CallInvocation<Integer, Integer>() {

          @Override
          protected void onCall(@NotNull final List<? extends Integer> integers,
              @NotNull final Channel<Integer, ?> result) {
            int sum = 0;
            for (final Integer integer : integers) {
              sum += integer;
            }

            result.pass(sum);
          }
        });
    final DurationMeasure timeout = seconds(1);
    assertThat(sumRoutine.invoke().pass(1, 2, 3, 4).close().in(timeout).all()).containsExactly(10);
    assertThat(sumRoutine.invoke().pass(4, 3, 2, 1).close().in(timeout).all()).containsExactly(10);
    sumRoutine.clear();
    assertThat(sumRoutine.invoke().close().in(timeout).getError()).isNotNull();
  }

  @Test
  public void testSize() {
    final Channel<Object, Object> channel =
        JRoutineCore.routine().of(IdentityInvocation.factory()).invoke();
    assertThat(channel.inputSize()).isEqualTo(0);
    channel.after(millis(500)).pass("test");
    assertThat(channel.inputSize()).isEqualTo(1);
    final Channel<Object, Object> result = channel.afterNoDelay().close();
    assertThat(result.in(seconds(1)).getComplete()).isTrue();
    assertThat(result.outputSize()).isEqualTo(1);
    assertThat(result.size()).isEqualTo(1);
    assertThat(result.skipNext(1).outputSize()).isEqualTo(0);
  }

  @Test
  public void testSkip() {
    assertThat(JRoutineCore.routine()
                           .of(IdentityInvocation.factory())
                           .invoke()
                           .pass("test1", "test2", "test3", "test4")
                           .close()
                           .in(seconds(1))
                           .skipNext(2)
                           .all()).containsExactly("test3", "test4");
    assertThat(JRoutineCore.routine()
                           .of(IdentityInvocation.factory())
                           .invoke()
                           .pass("test1")
                           .close()
                           .eventuallyContinue()
                           .in(seconds(1))
                           .skipNext(2)
                           .all()).isEmpty();
    try {
      JRoutineCore.routine()
                  .of(IdentityInvocation.factory())
                  .invoke()
                  .pass("test1")
                  .close()
                  .eventuallyAbort()
                  .in(seconds(1))
                  .skipNext(2);
      fail();

    } catch (final AbortException ignored) {
    }

    try {
      JRoutineCore.routine()
                  .of(IdentityInvocation.factory())
                  .invoke()
                  .pass("test1")
                  .close()
                  .eventuallyAbort(new IllegalStateException())
                  .in(seconds(1))
                  .skipNext(2);
      fail();

    } catch (final AbortException e) {
      assertThat(e.getCause()).isExactlyInstanceOf(IllegalStateException.class);
    }

    try {
      JRoutineCore.routine()
                  .of(IdentityInvocation.factory())
                  .invoke()
                  .pass("test1")
                  .close()
                  .eventuallyFail()
                  .in(seconds(1))
                  .skipNext(2);
      fail();

    } catch (final TimeoutException ignored) {
    }
  }

  @Test
  public void testTimeoutActions() {
    final Routine<String, String> routine1 = JRoutineCore.routine()
                                                         .withInvocation()
                                                         .withOutputTimeoutAction(
                                                             TimeoutActionType.ABORT)
                                                         .configuration()
                                                         .of(factoryOf(DelayedInvocation.class,
                                                             seconds(1)));
    try {
      routine1.invoke().pass("test1").close().next();
      fail();

    } catch (final AbortException ignored) {
    }

    try {
      routine1.invoke().pass("test1").close().all();
      fail();

    } catch (final AbortException ignored) {
    }

    try {
      final ArrayList<String> results = new ArrayList<String>();
      routine1.invoke().pass("test1").close().allInto(results);
      fail();

    } catch (final AbortException ignored) {
    }

    try {
      routine1.invoke().pass("test1").close().iterator().hasNext();
      fail();

    } catch (final AbortException ignored) {
    }

    try {
      routine1.invoke().pass("test1").close().iterator().next();
      fail();

    } catch (final AbortException ignored) {
    }

    assertThat(routine1.invoke().pass("test1").close().getComplete()).isFalse();
    final Routine<String, String> routine2 = JRoutineCore.routine()
                                                         .withInvocation()
                                                         .withOutputTimeoutAction(
                                                             TimeoutActionType.ABORT)
                                                         .withOutputTimeout(millis(10))
                                                         .configuration()
                                                         .of(factoryOf(DelayedInvocation.class,
                                                             seconds(1)));
    try {
      routine2.invoke().pass("test1").close().next();
      fail();

    } catch (final AbortException ignored) {
    }

    try {
      routine2.invoke().pass("test1").close().all();
      fail();

    } catch (final AbortException ignored) {
    }

    try {
      final ArrayList<String> results = new ArrayList<String>();
      routine2.invoke().pass("test1").close().allInto(results);
      fail();

    } catch (final AbortException ignored) {
    }

    try {
      routine2.invoke().pass("test1").close().iterator().hasNext();
      fail();

    } catch (final AbortException ignored) {
    }

    try {
      routine2.invoke().pass("test1").close().iterator().next();
      fail();

    } catch (final AbortException ignored) {
    }

    assertThat(routine2.invoke().pass("test1").close().getComplete()).isFalse();
    final Routine<String, String> routine3 = JRoutineCore.routine()
                                                         .withInvocation()
                                                         .withOutputTimeoutAction(
                                                             TimeoutActionType.FAIL)
                                                         .configuration()
                                                         .of(factoryOf(DelayedInvocation.class,
                                                             seconds(1)));
    final Channel<String, String> channel3 = routine3.invoke().pass("test1").close();
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

    assertThat(channel3.getComplete()).isFalse();
    channel3.in(millis(10));
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

    assertThat(channel3.getComplete()).isFalse();
    final Routine<String, String> routine4 = JRoutineCore.routine()
                                                         .withInvocation()
                                                         .withOutputTimeoutAction(
                                                             TimeoutActionType.CONTINUE)
                                                         .configuration()
                                                         .of(factoryOf(DelayedInvocation.class,
                                                             seconds(1)));
    final Channel<String, String> channel4 = routine4.invoke().pass("test1").close();
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

    assertThat(channel4.getComplete()).isFalse();
    channel4.in(millis(10));
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

    assertThat(channel4.getComplete()).isFalse();
  }

  private void testChained(final Routine<String, String> before,
      final Routine<String, String> after, final String input, final String expected) {
    final DurationMeasure timeout = seconds(1);
    try {
      for (final String s : before.invoke()
                                  .pass(after.invoke().pass(input).close())
                                  .close()
                                  .in(timeout)) {
        assertThat(s).isNotEmpty();
      }

      fail();

    } catch (final InvocationException e) {
      assertThat(e.getCause().getMessage()).isEqualTo(expected);
    }

    try {
      before.invoke().pass(after.invoke().pass(input).close()).close().in(timeout).all();
      fail();

    } catch (final InvocationException e) {
      assertThat(e.getCause().getMessage()).isEqualTo(expected);
    }

    try {
      for (final String s : before.invoke()
                                  .pass(after.invoke().pass(input).close())
                                  .close()
                                  .in(timeout)) {
        assertThat(s).isNotEmpty();
      }

      fail();

    } catch (final InvocationException e) {
      assertThat(e.getCause().getMessage()).isEqualTo(expected);
    }
  }

  private void testConsumer(final ChannelConsumer<String> consumer) {
    final DurationMeasure timeout = seconds(1);
    final String input = "test";
    final Routine<String, String> routine =
        JRoutineCore.routine().of(factoryOf(DelayedInvocation.class, noTime()));
    assertThat(
        routine.invoke().pass(input).close().consume(consumer).in(timeout).getComplete()).isTrue();
    assertThat(
        routine.invoke().pass(input).close().consume(consumer).in(timeout).getComplete()).isTrue();
    assertThat(JRoutineCore.routine()
                           .of(factoryOfParallel(routine))
                           .invoke()
                           .pass(input)
                           .consume(consumer)
                           .close()
                           .in(timeout)
                           .getComplete()).isTrue();
    assertThat(JRoutineCore.routine()
                           .of(factoryOfParallel(routine))
                           .invoke()
                           .pass(input)
                           .close()
                           .consume(consumer)
                           .in(timeout)
                           .getComplete()).isTrue();
  }

  private void testException(final Routine<String, String> routine, final String input,
      final String expected) {
    final DurationMeasure timeout = seconds(1);
    try {
      routine.invoke().pass(input).close().in(timeout).all();
      fail();

    } catch (final InvocationException e) {
      assertThat(e.getCause().getMessage()).isEqualTo(expected);
    }

    try {
      for (final String s : routine.invoke().pass(input).close().in(timeout)) {
        assertThat(s).isNotEmpty();
      }

      fail();

    } catch (final InvocationException e) {
      assertThat(e.getCause().getMessage()).isEqualTo(expected);
    }

    try {
      routine.invoke().pass(input).close().in(timeout).all();
      fail();

    } catch (final InvocationException e) {
      assertThat(e.getCause().getMessage()).isEqualTo(expected);
    }

    try {
      for (final String s : routine.invoke().pass(input).close().in(timeout)) {
        assertThat(s).isNotEmpty();
      }

      fail();

    } catch (final InvocationException e) {
      assertThat(e.getCause().getMessage()).isEqualTo(expected);
    }
  }

  private static class AbortInvocation extends MappingInvocation<String, String> {

    protected AbortInvocation() {
      super(null);
    }

    public void onInput(final String s, @NotNull final Channel<String, ?> result) {
      result.sorted().unsorted();
      assertThat(result.isOpen()).isTrue();
      assertThat(result.abort(new IllegalArgumentException(s))).isTrue();
      assertThat(result.abort()).isFalse();
      assertThat(result.isOpen()).isFalse();
      try {
        result.sorted();
        fail();

      } catch (final InvocationException ignored) {
      }

      try {
        result.unsorted();
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

    protected AbortInvocation2() {
      super(null);
    }

    public void onInput(final String s, @NotNull final Channel<String, ?> result) {
      assertThat(result.abort()).isTrue();
      assertThat(result.abort(new IllegalArgumentException(s))).isFalse();
    }
  }

  private static class AllInvocation extends MappingInvocation<Object, Object> {

    protected AllInvocation() {
      super(null);
    }

    public void onInput(final Object o, @NotNull final Channel<Object, ?> result) {
      JRoutineCore.routine()
                  .of(factoryOf(DelayedInvocation.class, millis(100)))
                  .invoke()
                  .pass("test")
                  .close()
                  .in(seconds(1))
                  .all();
    }
  }

  private static class CheckCompleteInvocation extends MappingInvocation<Object, Object> {

    protected CheckCompleteInvocation() {
      super(null);
    }

    public void onInput(final Object o, @NotNull final Channel<Object, ?> result) {
      JRoutineCore.routine()
                  .of(factoryOf(DelayedInvocation.class, millis(100)))
                  .invoke()
                  .pass("test")
                  .close()
                  .in(seconds(1))
                  .getComplete();
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

    private final DurationMeasure mDelay;

    public DelayedAbortInvocation(final DurationMeasure delay) {
      mDelay = delay;
    }

    @Override
    public void onInput(final String s, @NotNull final Channel<String, ?> result) {
      result.afterNoDelay().pass(s).after(mDelay).abort();
    }

    @Override
    public boolean onRecycle() {
      return true;
    }
  }

  private static class DelayedChannelInvocation extends TemplateInvocation<String, String> {

    private final DurationMeasure mDelay;

    private final Routine<String, String> mRoutine;

    private boolean mFlag;

    public DelayedChannelInvocation(final DurationMeasure delay) {
      mDelay = delay;
      mRoutine = JRoutineCore.routine().of(factoryOf(DelayedInvocation.class, noTime()));
    }

    @Override
    public void onInput(final String s, @NotNull final Channel<String, ?> result) {
      if (mFlag) {
        result.after(mDelay).pass((Channel<String, String>) null);

      } else {
        result.after(mDelay.value, mDelay.unit).pass((Channel<String, String>) null);
      }

      result.pass(mRoutine.invoke().pass(s).close());
      mFlag = !mFlag;
    }

    @Override
    public boolean onRecycle() {
      return true;
    }
  }

  private static class DelayedInvocation extends TemplateInvocation<String, String> {

    private final DurationMeasure mDelay;

    private boolean mFlag;

    public DelayedInvocation(final DurationMeasure delay) {
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

    @Override
    public boolean onRecycle() {
      return true;
    }
  }

  private static class DelayedListInvocation extends TemplateInvocation<String, String> {

    private final int mCount;

    private final DurationMeasure mDelay;

    private final ArrayList<String> mList;

    private boolean mFlag;

    public DelayedListInvocation(final DurationMeasure delay, final int listCount) {
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

        result.afterNoDelay();
        list.clear();
        mFlag = !mFlag;
      }
    }

    @Override
    public void onComplete(@NotNull final Channel<String, ?> result) {
      final ArrayList<String> list = mList;
      result.after(mDelay).pass(list).afterNoDelay();
      list.clear();
    }

    @Override
    public boolean onRecycle() {
      return true;
    }
  }

  private static class ExceptionRoutine extends AbstractRoutine<Object, Object> {

    protected ExceptionRoutine(@NotNull final InvocationConfiguration configuration) {
      super(configuration, defaultExecutor());
    }

    @NotNull
    @Override
    protected Invocation<Object, Object> newInvocation() {
      throw new IllegalStateException();
    }
  }

  private static class HasNextInvocation extends MappingInvocation<Object, Object> {

    protected HasNextInvocation() {
      super(null);
    }

    public void onInput(final Object o, @NotNull final Channel<Object, ?> result) {
      JRoutineCore.routine()
                  .of(factoryOf(DelayedInvocation.class, millis(100)))
                  .invoke()
                  .pass("test")
                  .close()
                  .in(seconds(1))
                  .iterator()
                  .hasNext();
    }
  }

  private static class InputArrayExecutorDeadlock extends MappingInvocation<String, String> {

    protected InputArrayExecutorDeadlock() {
      super(null);
    }

    public void onInput(final String s, @NotNull final Channel<String, ?> result) {
      JRoutineCore.routine()
                  .withInvocation()
                  .withInputMaxSize(1)
                  .withInputBackoff(afterCount(1).constantDelay(indefiniteTime()))
                  .configuration()
                  .of(IdentityInvocation.<String>factory())
                  .invoke()
                  .after(millis(500))
                  .pass(s)
                  .afterNoDelay()
                  .pass(new String[]{s})
                  .close()
                  .all();
    }
  }

  private static class InputConsumerExecutorDeadlock extends MappingInvocation<String, String> {

    protected InputConsumerExecutorDeadlock() {
      super(null);
    }

    public void onInput(final String s, @NotNull final Channel<String, ?> result) {
      final Channel<String, String> channel = JRoutineCore.channel().ofType();
      result.pass(JRoutineCore.routine()
                              .withInvocation()
                              .withInputMaxSize(1)
                              .withInputBackoff(afterCount(1).constantDelay(indefiniteTime()))
                              .configuration()
                              .of(IdentityInvocation.<String>factory())
                              .invoke()
                              .after(millis(500))
                              .pass(channel)
                              .afterNoDelay()
                              .close());
      channel.pass(s, s).close();
    }
  }

  private static class InputExecutorDeadlock extends MappingInvocation<String, String> {

    protected InputExecutorDeadlock() {
      super(null);
    }

    public void onInput(final String s, @NotNull final Channel<String, ?> result) {
      JRoutineCore.routine()
                  .withInvocation()
                  .withInputMaxSize(1)
                  .withInputBackoff(afterCount(1).constantDelay(indefiniteTime()))
                  .configuration()
                  .of(IdentityInvocation.<String>factory())
                  .invoke()
                  .after(millis(500))
                  .pass(s)
                  .afterNoDelay()
                  .pass(s)
                  .close()
                  .all();
    }
  }

  private static class InputListExecutorDeadlock extends MappingInvocation<String, String> {

    protected InputListExecutorDeadlock() {
      super(null);
    }

    public void onInput(final String s, @NotNull final Channel<String, ?> result) {
      JRoutineCore.routine()
                  .withInvocation()
                  .withInputMaxSize(1)
                  .withInputBackoff(afterCount(1).constantDelay(indefiniteTime()))
                  .configuration()
                  .of(IdentityInvocation.<String>factory())
                  .invoke()
                  .after(millis(500))
                  .pass(s)
                  .afterNoDelay()
                  .pass(Collections.singletonList(s))
                  .afterNoDelay()
                  .close()
                  .all();
    }
  }

  private static class NextInvocation extends MappingInvocation<Object, Object> {

    protected NextInvocation() {
      super(null);
    }

    public void onInput(final Object o, @NotNull final Channel<Object, ?> result) {
      JRoutineCore.routine()
                  .of(factoryOfParallel(
                      JRoutineCore.routine().of(factoryOf(DelayedInvocation.class, millis(100)))))
                  .invoke()
                  .pass("test")
                  .close()
                  .in(seconds(1))
                  .iterator()
                  .next();
    }
  }

  private static class NullRoutine extends AbstractRoutine<Object, Object> {

    protected NullRoutine(@NotNull final InvocationConfiguration configuration) {
      super(configuration, defaultExecutor());
    }

    @NotNull
    @Override
    @SuppressWarnings("ConstantConditions")
    protected Invocation<Object, Object> newInvocation() {
      return null;
    }
  }

  private static class ResultArrayExecutorDeadlock extends MappingInvocation<String, String> {

    protected ResultArrayExecutorDeadlock() {
      super(null);
    }

    public void onInput(final String s, @NotNull final Channel<String, ?> result) {
      result.after(millis(500)).pass(s).after(millis(100)).pass(new String[]{s});
    }
  }

  private static class ResultExecutorDeadlock extends MappingInvocation<String, String> {

    protected ResultExecutorDeadlock() {
      super(null);
    }

    public void onInput(final String s, @NotNull final Channel<String, ?> result) {
      result.after(millis(500)).pass(s).after(millis(100)).pass(s);
    }
  }

  private static class ResultListExecutorDeadlock extends MappingInvocation<String, String> {

    protected ResultListExecutorDeadlock() {
      super(null);
    }

    public void onInput(final String s, @NotNull final Channel<String, ?> result) {
      result.after(millis(500)).pass(s).after(millis(100)).pass(Collections.singletonList(s));
    }
  }

  private static class SleepCommand extends CommandInvocation<Void> {

    protected SleepCommand() {
      super(null);
    }

    public void onComplete(@NotNull final Channel<Void, ?> result) throws Exception {
      seconds(1).sleepAtLeast();
    }
  }

  private static class SleepInvocation extends MappingInvocation<Object, Object> {

    private final DurationMeasure mSleepDuration;

    private SleepInvocation(@NotNull final DurationMeasure sleepDuration) {
      super(asArgs(sleepDuration));
      mSleepDuration = sleepDuration;
    }

    public void onInput(final Object input, @NotNull final Channel<Object, ?> result) {
      try {
        mSleepDuration.sleepAtLeast();

      } catch (final InterruptedException e) {
        throw new InterruptedInvocationException(e);
      }

      result.pass(input);
    }
  }

  private static class SquareInvocation extends MappingInvocation<Integer, Integer> {

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

  private static class TestChannelConsumer extends TemplateChannelConsumer<Object> {

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
    public void onDestroy() {
      sInstanceCount.decrementAndGet();
    }

    @Override
    public boolean onRecycle() {
      return true;
    }
  }

  private static class TestDiscardException extends TestDiscard {

    @Override
    public boolean onRecycle() {
      super.onRecycle();
      throw new IllegalArgumentException("test");
    }
  }

  private static class TestDiscardOnAbort extends TestDiscard {

    @Override
    public void onAbort(@NotNull final RoutineException reason) throws Exception {
      throw new IllegalArgumentException("test");
    }
  }

  private static class TestExecutionObserver implements ExecutionObserver<Object> {

    @NotNull
    public RoutineException getAbortException() {
      return new RoutineException();
    }

    public void onAbortComplete() {
    }

    public boolean onConsumeComplete() {
      return false;
    }

    public boolean onFirstInput(@NotNull final InputData<Object> inputData) {
      return false;
    }

    public void onInvocationComplete() {
    }

    public boolean onNextInput(@NotNull final InputData<Object> inputData) {
      return false;
    }
  }

  private static class TestExecutor extends SyncExecutor {

    private final ArrayList<Runnable> mExecutions = new ArrayList<Runnable>();

    @Override
    public void execute(@NotNull final Runnable command, final long delay,
        @NotNull final TimeUnit timeUnit) {
      mExecutions.add(command);
    }

    private void run(int count) {
      final ArrayList<Runnable> executions = mExecutions;
      while (!executions.isEmpty() && (count-- > 0)) {
        final Runnable execution = executions.remove(0);
        execution.run();
      }
    }
  }

  private static class TestInvocationManager implements InvocationManager<Object, Object> {

    public boolean create(@NotNull final InvocationObserver<Object, Object> observer) {
      observer.onCreate(new TemplateInvocation<Object, Object>() {});
      return true;
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
    public boolean onRecycle() {
      return true;
    }

    @Override
    public void onStart() {
      sActive = true;
    }

    @Override
    public void onComplete(@NotNull final Channel<String, ?> result) {
      sActive = false;
    }
  }
}
