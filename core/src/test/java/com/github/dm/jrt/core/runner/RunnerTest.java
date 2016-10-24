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

package com.github.dm.jrt.core.runner;

import com.github.dm.jrt.core.config.InvocationConfiguration.AgingPriority;
import com.github.dm.jrt.core.config.InvocationConfiguration.NotAgingPriority;
import com.github.dm.jrt.core.util.UnitDuration;
import com.github.dm.jrt.core.util.UnitTime;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.dm.jrt.core.util.UnitDuration.micros;
import static com.github.dm.jrt.core.util.UnitDuration.millis;
import static com.github.dm.jrt.core.util.UnitDuration.nanos;
import static com.github.dm.jrt.core.util.UnitDuration.seconds;
import static com.github.dm.jrt.core.util.UnitDuration.zero;
import static com.github.dm.jrt.core.util.UnitTime.current;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Runners unit tests.
 * <p>
 * Created by davide-maestroni on 10/02/2014.
 */
public class RunnerTest {

  @Test
  public void testConstructor() {

    boolean failed = false;
    try {
      new Runners();
      failed = true;

    } catch (final Throwable ignored) {

    }

    assertThat(failed).isFalse();
  }

  @Test
  public void testDynamicPoolRunner() throws InterruptedException {

    testRunner(Runners.sharedRunner());
    testRunner(Runners.dynamicPoolRunner(3, 4, 5L, TimeUnit.SECONDS));
    testRunner(new RunnerDecorator(Runners.sharedRunner()));
    testRunner(new RunnerDecorator(Runners.dynamicPoolRunner(1, 4, 0L, TimeUnit.SECONDS)));
  }

  @Test
  public void testImmediateRunner() throws InterruptedException {

    final TestExecution testExecution = new TestExecution();
    final Runner runner = Runners.immediateRunner();
    runner.run(testExecution, 0, TimeUnit.MILLISECONDS);
    assertThat(testExecution.isRun()).isTrue();
    testExecution.reset();
    final long start = System.currentTimeMillis();
    runner.run(testExecution, 1, TimeUnit.SECONDS);
    assertThat(testExecution.isRun()).isTrue();
    assertThat(System.currentTimeMillis()).isGreaterThanOrEqualTo(start + 1000);
    testRunner(new ImmediateRunner());
    testRunner(Runners.immediateRunner());
    testRunner(new RunnerDecorator(new ImmediateRunner()));
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testNullPriorityRunner() {

    try {
      Runners.priorityRunner(null);
      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testPoolRunner() throws InterruptedException {

    testRunner(Runners.poolRunner(3));
    testRunner(new RunnerDecorator(Runners.poolRunner(4)));
  }

  @Test
  public void testPoolRunnerError() {

    try {
      Runners.poolRunner(-1);
      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  @Test
  public void testPriorityRunner() throws InterruptedException {

    testRunner(
        Runners.priorityRunner(Runners.sharedRunner()).getRunner(AgingPriority.NORMAL_PRIORITY));
    testRunner(Runners.priorityRunner(Runners.syncRunner()).getRunner(AgingPriority.LOW_PRIORITY));
    testRunner(new RunnerDecorator(
        Runners.priorityRunner(Runners.poolRunner()).getRunner(AgingPriority.LOWEST_PRIORITY)));

    final PriorityRunner priorityRunner = Runners.priorityRunner(Runners.sharedRunner());
    testRunner(priorityRunner.getRunner(NotAgingPriority.NORMAL_PRIORITY));
    testRunner(priorityRunner.getRunner(NotAgingPriority.LOW_PRIORITY));
    testRunner(new RunnerDecorator(priorityRunner.getRunner(NotAgingPriority.LOWEST_PRIORITY)));
  }

  @Test
  public void testQueuedRunner() throws InterruptedException {

    testRunner(new QueuedRunner());
    testRunner(Runners.syncRunner());
    testRunner(new RunnerDecorator(new QueuedRunner()));
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testQueuingRunner() {

    final TestExecution execution = new TestExecution();
    final TestRunner testRunner = new TestRunner();
    final Runner runner = Runners.priorityRunner(testRunner).getRunner(0);
    assertThat(runner.isExecutionThread()).isFalse();
    testRunner.setExecutionThread(new Thread());
    assertThat(runner.isExecutionThread()).isFalse();
    runner.run(execution, 0, TimeUnit.MILLISECONDS);
    assertThat(execution.isRun()).isFalse();
    testRunner.getLastExecution().run();
    assertThat(execution.isRun()).isTrue();
    runner.cancel(execution);
    assertThat(testRunner.getLastCancelExecution()).isNull();
    execution.reset();
    runner.run(execution, 0, TimeUnit.MILLISECONDS);
    runner.cancel(execution);
    assertThat(execution.isRun()).isFalse();
    assertThat(testRunner.getLastCancelExecution()).isNotNull();
    testRunner.cancel(null);
    execution.reset();
    runner.run(execution, 0, TimeUnit.MILLISECONDS);
    testRunner.getLastExecution().run();
    runner.cancel(execution);
    assertThat(execution.isRun()).isTrue();
    assertThat(testRunner.getLastCancelExecution()).isNull();
    execution.reset();
    runner.run(execution, 1, TimeUnit.MILLISECONDS);
    testRunner.getLastExecution().run();
    runner.cancel(execution);
    assertThat(execution.isRun()).isTrue();
    execution.reset();
    runner.run(execution, 1, TimeUnit.MILLISECONDS);
    runner.cancel(execution);
    assertThat(execution.isRun()).isFalse();
    assertThat(testRunner.getLastCancelExecution()).isNotNull();
    testRunner.cancel(null);
    runner.run(execution, 1, TimeUnit.MILLISECONDS);
    runner.cancel(execution);
    assertThat(execution.isRun()).isFalse();
    assertThat(testRunner.getLastCancelExecution()).isNotNull();
  }

  @Test
  public void testRunnerDecorator() {

    final TestExecution execution = new TestExecution();
    final TestRunner testRunner = new TestRunner();
    final RunnerDecorator runner = new RunnerDecorator(testRunner);
    assertThat(runner.isExecutionThread()).isFalse();
    testRunner.setExecutionThread(Thread.currentThread());
    assertThat(runner.isExecutionThread()).isTrue();
    runner.run(execution, 0, TimeUnit.MILLISECONDS);
    assertThat(testRunner.getLastExecution()).isSameAs(execution);
    runner.run(new TestExecution(), 1, TimeUnit.MILLISECONDS);
    assertThat(testRunner.getLastExecution()).isNotSameAs(execution);
    runner.cancel(execution);
    assertThat(testRunner.getLastCancelExecution()).isSameAs(execution);
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testRunnerDecoratorError() {

    try {
      new RunnerDecorator(null);
      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testSamePriorityRunner() {

    final PriorityRunner priorityRunner = Runners.priorityRunner(Runners.syncRunner());
    assertThat(Runners.priorityRunner(priorityRunner.getRunner(3))).isSameAs(priorityRunner);
  }

  @Test
  public void testScheduledRunner() throws InterruptedException {

    final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    final ScheduledRunner instance = ScheduledRunner.getInstance(executorService);
    assertThat(instance).isSameAs(ScheduledRunner.getInstance(executorService));
    testRunner(instance);
    testRunner(Runners.scheduledRunner(Executors.newCachedThreadPool()));
    testRunner(Runners.scheduledRunner(executorService));
    testRunner(new RunnerDecorator(instance));
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testScheduledRunnerError() {

    try {
      ScheduledRunner.getInstance(null);
      fail();

    } catch (final NullPointerException ignored) {

    }

    try {
      Runners.scheduledRunner(null);
      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testThrottlingRunner() throws InterruptedException {

    testRunner(new ThrottlingRunner(Runners.sharedRunner(), 5));
    testRunner(Runners.throttlingRunner(Runners.poolRunner(), 5));
    testRunner(new RunnerDecorator(new ThrottlingRunner(Runners.sharedRunner(), 5)));
    testRunner(Runners.throttlingRunner(Runners.syncRunner(), 1));
  }

  @Test
  public void testThrottlingRunnerCancel() throws InterruptedException {

    final TestExecution execution = new TestExecution();
    Runner runner = Runners.throttlingRunner(Runners.syncRunner(), 1);
    assertThat(runner.isExecutionThread()).isTrue();
    runner.run(execution, 0, TimeUnit.MILLISECONDS);
    assertThat(execution.isRun()).isTrue();
    execution.reset();
    runner.run(execution, 1, TimeUnit.MILLISECONDS);
    assertThat(execution.isRun()).isTrue();
    execution.reset();
    runner = Runners.throttlingRunner(Runners.sharedRunner(), 1);
    runner.run(new TestExecution() {

      @Override
      public void run() {

        try {

          seconds(1).sleepAtLeast();

        } catch (final InterruptedException ignored) {

        }

        super.run();
      }
    }, 0, TimeUnit.MILLISECONDS);

    millis(200).sleepAtLeast();
    runner.run(execution, 0, TimeUnit.MILLISECONDS);
    millis(200).sleepAtLeast();
    assertThat(execution.isRun()).isFalse();
    runner.cancel(execution);
    seconds(1).sleepAtLeast();
    assertThat(execution.isRun()).isFalse();
    execution.reset();
    runner.run(new TestExecution() {

      @Override
      public void run() {

        try {

          millis(500).sleepAtLeast();

        } catch (final InterruptedException ignored) {

        }

        super.run();
      }
    }, 0, TimeUnit.MILLISECONDS);

    millis(300).sleepAtLeast();
    runner.run(execution, 0, TimeUnit.MILLISECONDS);
    assertThat(execution.isRun()).isFalse();
    millis(500).sleepAtLeast();
    assertThat(execution.isRun()).isTrue();
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testThrottlingRunnerError() {

    try {
      new ThrottlingRunner(null, 5);
      fail();

    } catch (final NullPointerException ignored) {

    }

    try {
      Runners.throttlingRunner(null, 5);
      fail();

    } catch (final NullPointerException ignored) {

    }

    try {
      new ThrottlingRunner(Runners.sharedRunner(), 0);
      fail();

    } catch (final IllegalArgumentException ignored) {

    }

    try {
      Runners.throttlingRunner(Runners.sharedRunner(), -1);
      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  @Test
  public void testZeroDelayRunner() throws InterruptedException {

    final ZeroDelayRunner instance = ZeroDelayRunner.getInstance(Runners.sharedRunner());
    assertThat(instance).isSameAs(ZeroDelayRunner.getInstance(Runners.sharedRunner()));
    assertThat(instance).isSameAs(ZeroDelayRunner.getInstance(instance));
    testRunner(instance);
    testRunner(Runners.zeroDelayRunner(Runners.poolRunner()));
    testRunner(new RunnerDecorator(instance));
  }

  @Test
  public void testZeroDelayRunnerCancel() throws InterruptedException {

    final AtomicReference<Runner> runner = new AtomicReference<Runner>();
    runner.set(Runners.zeroDelayRunner(new RunnerDecorator(Runners.syncRunner()) {

      @NotNull
      @Override
      protected ThreadManager getThreadManager() {
        return new ThreadManager() {

          public boolean isManagedThread() {
            return true;
          }
        };
      }
    }));
    final TestExecution testExecution1 = new TestExecution() {

      private boolean mIsFirst = true;

      public void run() {

        if (mIsFirst) {
          mIsFirst = false;
          runner.get().run(this, 0, TimeUnit.MILLISECONDS);
          runner.get().cancel(this);

        } else {
          throw new IllegalStateException();
        }
      }
    };
    runner.get().run(testExecution1, 0, TimeUnit.MILLISECONDS);
    assertThat(testExecution1.isRun()).isFalse();

    final TestExecution testExecution2 = new TestExecution() {

      private boolean mIsFirst = true;

      public void run() {

        if (mIsFirst) {
          mIsFirst = false;
          runner.get().run(this, 0, TimeUnit.MILLISECONDS);
          Runners.syncRunner().cancel(this);

        } else {
          super.run();
        }
      }
    };
    runner.get().run(testExecution2, 0, TimeUnit.MILLISECONDS);
    assertThat(testExecution2.isRun()).isTrue();
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testZeroDelayRunnerError() {

    try {
      ZeroDelayRunner.getInstance(null);
      fail();

    } catch (final NullPointerException ignored) {

    }

    try {
      Runners.zeroDelayRunner(null);
      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  private void testRunner(final Runner runner) throws InterruptedException {

    final Random random = new Random(System.currentTimeMillis());
    final ArrayList<TestRunExecution> executions = new ArrayList<TestRunExecution>();

    for (int i = 0; i < 13; i++) {

      final UnitDuration delay;
      final int unit = random.nextInt(4);

      switch (unit) {

        case 0:
          delay = millis((long) Math.floor(random.nextFloat() * 500));
          break;

        case 1:
          delay = micros((long) Math.floor(random.nextFloat() * millis(500).toMicros()));
          break;

        case 2:
          delay = nanos((long) Math.floor(random.nextFloat() * millis(500).toNanos()));
          break;

        default:
          delay = zero();
          break;
      }

      final TestRunExecution execution = new TestRunExecution(delay);
      executions.add(execution);

      runner.run(execution, delay.value, delay.unit);
    }

    for (final TestRunExecution execution : executions) {

      execution.await();
      assertThat(execution.isPassed()).isTrue();
    }

    executions.clear();

    final ArrayList<UnitDuration> delays = new ArrayList<UnitDuration>();

    for (int i = 0; i < 13; i++) {

      final UnitDuration delay;
      final int unit = random.nextInt(4);

      switch (unit) {

        case 0:
          delay = millis((long) Math.floor(random.nextFloat() * 500));
          break;

        case 1:
          delay = micros((long) Math.floor(random.nextFloat() * millis(500).toMicros()));
          break;

        case 2:
          delay = nanos((long) Math.floor(random.nextFloat() * millis(500).toNanos()));
          break;

        default:
          delay = zero();
          break;
      }

      delays.add(delay);

      final TestRunExecution execution = new TestRunExecution(delay);
      executions.add(execution);
    }

    final TestRecursiveExecution recursiveExecution =
        new TestRecursiveExecution(runner, executions, delays, zero());

    runner.run(recursiveExecution, 0, TimeUnit.MILLISECONDS);

    for (final TestRunExecution execution : executions) {

      execution.await();
      assertThat(execution.isPassed()).isTrue();
    }
  }

  private static class TestExecution implements Execution {

    private boolean mIsRun;

    public void run() {

      mIsRun = true;
    }

    private boolean isRun() {

      return mIsRun;
    }

    private void reset() {

      mIsRun = false;
    }
  }

  private static class TestRecursiveExecution extends TestRunExecution {

    private final ArrayList<UnitDuration> mDelays;

    private final ArrayList<TestRunExecution> mExecutions;

    private final Runner mRunner;

    public TestRecursiveExecution(final Runner runner, final ArrayList<TestRunExecution> executions,
        final ArrayList<UnitDuration> delays, final UnitDuration delay) {

      super(delay);

      mRunner = runner;
      mExecutions = executions;
      mDelays = delays;
    }

    @Override
    public void run() {

      final ArrayList<TestRunExecution> executions = mExecutions;
      final ArrayList<UnitDuration> delays = mDelays;
      final Runner runner = mRunner;
      final int size = executions.size();

      for (int i = 0; i < size; i++) {

        final UnitDuration delay = delays.get(i);
        final TestRunExecution execution = executions.get(i);

        runner.run(execution, delay.value, delay.unit);
      }

      super.run();
    }
  }

  private static class TestRunExecution implements Execution {

    private final UnitDuration mDelay;

    private final Semaphore mSemaphore = new Semaphore(0);

    private final UnitTime mStartTime;

    private boolean mIsPassed;

    public TestRunExecution(final UnitDuration delay) {

      mStartTime = current();
      mDelay = delay;
    }

    public void await() throws InterruptedException {

      mSemaphore.acquire();
    }

    public boolean isPassed() {

      return mIsPassed;
    }

    public void run() {

      // The JVM might not have nanosecond precision...
      mIsPassed = (current().toMillis() - mStartTime.toMillis() >= mDelay.toMillis());
      mSemaphore.release();
    }
  }

  private static class TestRunner extends AsyncRunner {

    private Execution mLastCancelExecution;

    private Execution mLastExecution;

    private TestRunner() {
      super(new TestManager());
    }

    @Override
    public void cancel(@NotNull final Execution execution) {

      mLastCancelExecution = execution;
    }

    @Override
    public void run(@NotNull final Execution execution, final long delay,
        @NotNull final TimeUnit timeUnit) {

      mLastExecution = execution;
    }

    private Execution getLastCancelExecution() {

      return mLastCancelExecution;
    }

    private Execution getLastExecution() {

      return mLastExecution;
    }

    private void setExecutionThread(final Thread thread) {

      ((TestManager) getThreadManager()).mThreadId = thread.getId();
    }

    private static class TestManager implements ThreadManager {

      private volatile long mThreadId;

      public boolean isManagedThread() {
        return (Thread.currentThread().getId() == mThreadId);
      }
    }
  }
}
