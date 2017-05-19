/*
 * Copyright 2017 Davide Maestroni
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

package com.github.dm.jrt.core.executor;

import com.github.dm.jrt.core.config.InvocationConfiguration.AgingPriority;
import com.github.dm.jrt.core.config.InvocationConfiguration.NotAgingPriority;
import com.github.dm.jrt.core.util.DurationMeasure;
import com.github.dm.jrt.core.util.TimeMeasure;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.dm.jrt.core.util.DurationMeasure.micros;
import static com.github.dm.jrt.core.util.DurationMeasure.millis;
import static com.github.dm.jrt.core.util.DurationMeasure.nanos;
import static com.github.dm.jrt.core.util.DurationMeasure.noTime;
import static com.github.dm.jrt.core.util.DurationMeasure.seconds;
import static com.github.dm.jrt.core.util.TimeMeasure.current;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Executors unit tests.
 * <p>
 * Created by davide-maestroni on 10/02/2014.
 */
public class ScheduledExecutorTest {

  @Test
  public void testConstructor() {

    boolean failed = false;
    try {
      new ScheduledExecutors();
      failed = true;

    } catch (final Throwable ignored) {

    }

    assertThat(failed).isFalse();
  }

  @Test
  public void testDynamicPoolExecutor() throws InterruptedException {

    testExecutor(ScheduledExecutors.defaultExecutor());
    testExecutor(ScheduledExecutors.dynamicPoolExecutor(3, 4, 5L, TimeUnit.SECONDS));
    testExecutor(new ScheduledExecutorDecorator(ScheduledExecutors.defaultExecutor()));
    testExecutor(new ScheduledExecutorDecorator(
        ScheduledExecutors.dynamicPoolExecutor(1, 4, 0L, TimeUnit.SECONDS)));
  }

  @Test
  public void testExecutorDecorator() {

    final TestExecution execution = new TestExecution();
    final TestExecutor testExecutor = new TestExecutor();
    final ScheduledExecutorDecorator executor = new ScheduledExecutorDecorator(testExecutor);
    try {
      assertThat(executor.isExecutionThread()).isFalse();
      testExecutor.setExecutionThread(Thread.currentThread());
      assertThat(executor.isExecutionThread()).isTrue();
      executor.execute(execution);
      assertThat(testExecutor.getLastExecution()).isSameAs(execution);
      executor.execute(new TestExecution(), 1, TimeUnit.MILLISECONDS);
      assertThat(testExecutor.getLastExecution()).isNotSameAs(execution);
      executor.cancel(execution);
      assertThat(testExecutor.getLastCancelExecution()).isSameAs(execution);

    } finally {
      testExecutor.resetExecutionThread();
    }
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testExecutorDecoratorError() {

    try {
      new ScheduledExecutorDecorator(null);
      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testImmediateExecutor() throws InterruptedException {

    final TestExecution testExecution = new TestExecution();
    final ScheduledExecutor executor = ScheduledExecutors.immediateExecutor();
    executor.execute(testExecution);
    assertThat(testExecution.isRun()).isTrue();
    testExecution.reset();
    final long start = System.currentTimeMillis();
    executor.execute(testExecution, 1, TimeUnit.SECONDS);
    assertThat(testExecution.isRun()).isTrue();
    assertThat(System.currentTimeMillis()).isGreaterThanOrEqualTo(start + 1000);
    testExecutor(new ImmediateExecutor());
    testExecutor(ScheduledExecutors.immediateExecutor());
    testExecutor(new ScheduledExecutorDecorator(new ImmediateExecutor()));
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testNullPriorityExecutor() {

    try {
      ScheduledExecutors.priorityExecutor(null);
      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testPoolExecutor() throws InterruptedException {

    testExecutor(ScheduledExecutors.poolExecutor(3));
    testExecutor(new ScheduledExecutorDecorator(ScheduledExecutors.poolExecutor(4)));
  }

  @Test
  public void testPoolExecutorError() {

    try {
      ScheduledExecutors.poolExecutor(-1);
      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  @Test
  public void testPriorityExecutor() throws InterruptedException {
    testExecutor(ScheduledExecutors.priorityExecutor(ScheduledExecutors.defaultExecutor())
                                   .ofPriority(AgingPriority.NORMAL_PRIORITY));
    testExecutor(ScheduledExecutors.priorityExecutor(ScheduledExecutors.syncExecutor())
                                   .ofPriority(AgingPriority.LOW_PRIORITY));
    testExecutor(new ScheduledExecutorDecorator(
        ScheduledExecutors.priorityExecutor(ScheduledExecutors.poolExecutor())
                          .ofPriority(AgingPriority.LOWEST_PRIORITY)));

    final PriorityExecutor priorityExecutor =
        ScheduledExecutors.priorityExecutor(ScheduledExecutors.defaultExecutor());
    testExecutor(priorityExecutor.ofPriority(NotAgingPriority.NORMAL_PRIORITY));
    testExecutor(priorityExecutor.ofPriority(NotAgingPriority.LOW_PRIORITY));
    testExecutor(new ScheduledExecutorDecorator(
        priorityExecutor.ofPriority(NotAgingPriority.LOWEST_PRIORITY)));
  }

  @Test
  public void testQueuedExecutor() throws InterruptedException {

    testExecutor(new QueuedExecutor());
    testExecutor(ScheduledExecutors.syncExecutor());
    testExecutor(new ScheduledExecutorDecorator(new QueuedExecutor()));
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testQueuingExecutor() {

    final TestExecution execution = new TestExecution();
    final TestExecutor testExecutor = new TestExecutor();
    final ScheduledExecutor executor =
        ScheduledExecutors.priorityExecutor(testExecutor).ofPriority(0);
    try {
      assertThat(executor.isExecutionThread()).isFalse();
      testExecutor.setExecutionThread(new Thread());
      assertThat(executor.isExecutionThread()).isFalse();
      executor.execute(execution);
      assertThat(execution.isRun()).isFalse();
      testExecutor.getLastExecution().run();
      assertThat(execution.isRun()).isTrue();
      executor.cancel(execution);
      assertThat(testExecutor.getLastCancelExecution()).isNull();
      execution.reset();
      executor.execute(execution);
      executor.cancel(execution);
      assertThat(execution.isRun()).isFalse();
      assertThat(testExecutor.getLastCancelExecution()).isNotNull();
      testExecutor.cancel(null);
      execution.reset();
      executor.execute(execution);
      testExecutor.getLastExecution().run();
      executor.cancel(execution);
      assertThat(execution.isRun()).isTrue();
      assertThat(testExecutor.getLastCancelExecution()).isNull();
      execution.reset();
      executor.execute(execution, 1, TimeUnit.MILLISECONDS);
      testExecutor.getLastExecution().run();
      executor.cancel(execution);
      assertThat(execution.isRun()).isTrue();
      execution.reset();
      executor.execute(execution, 1, TimeUnit.MILLISECONDS);
      executor.cancel(execution);
      assertThat(execution.isRun()).isFalse();
      assertThat(testExecutor.getLastCancelExecution()).isNotNull();
      testExecutor.cancel(null);
      executor.execute(execution, 1, TimeUnit.MILLISECONDS);
      executor.cancel(execution);
      assertThat(execution.isRun()).isFalse();
      assertThat(testExecutor.getLastCancelExecution()).isNotNull();

    } finally {
      testExecutor.resetExecutionThread();
    }
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testSamePriorityExecutor() {

    final PriorityExecutor priorityExecutor =
        ScheduledExecutors.priorityExecutor(ScheduledExecutors.syncExecutor());
    assertThat(ScheduledExecutors.priorityExecutor(priorityExecutor.ofPriority(3))).isSameAs(
        priorityExecutor);
  }

  @Test
  public void testServiceExecutor() throws InterruptedException {

    final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    final ServiceExecutor instance = ServiceExecutor.executorOf(executorService);
    assertThat(instance).isSameAs(ServiceExecutor.executorOf(executorService));
    final ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
    try {
      testExecutor(instance);
      testExecutor(ScheduledExecutors.serviceExecutor(cachedThreadPool));
      testExecutor(ScheduledExecutors.serviceExecutor(executorService));
      testExecutor(new ScheduledExecutorDecorator(instance));

    } finally {
      cachedThreadPool.shutdown();
      executorService.shutdown();
    }
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testServiceExecutorError() {

    try {
      ServiceExecutor.executorOf(null);
      fail();

    } catch (final NullPointerException ignored) {

    }

    try {
      ScheduledExecutors.serviceExecutor(null);
      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testThrottlingExecutor() throws InterruptedException {

    testExecutor(ScheduledExecutors.throttlingExecutor(ScheduledExecutors.poolExecutor(), 5));
    testExecutor(new ScheduledExecutorDecorator(
        ScheduledExecutors.throttlingExecutor(ScheduledExecutors.defaultExecutor(), 5)));
    testExecutor(ScheduledExecutors.throttlingExecutor(ScheduledExecutors.syncExecutor(), 1));
  }

  @Test
  public void testThrottlingExecutorCancel() throws InterruptedException {

    final TestExecution execution = new TestExecution();
    ScheduledExecutor executor =
        ScheduledExecutors.throttlingExecutor(ScheduledExecutors.syncExecutor(), 1);
    assertThat(executor.isExecutionThread()).isTrue();
    executor.execute(execution);
    assertThat(execution.isRun()).isTrue();
    execution.reset();
    executor.execute(execution, 1, TimeUnit.MILLISECONDS);
    assertThat(execution.isRun()).isTrue();
    execution.reset();
    executor = ScheduledExecutors.throttlingExecutor(ScheduledExecutors.defaultExecutor(), 1);
    executor.execute(new TestExecution() {

      @Override
      public void run() {

        try {

          seconds(1).sleepAtLeast();

        } catch (final InterruptedException ignored) {

        }

        super.run();
      }
    });

    millis(200).sleepAtLeast();
    executor.execute(execution);
    millis(200).sleepAtLeast();
    assertThat(execution.isRun()).isFalse();
    executor.cancel(execution);
    seconds(1).sleepAtLeast();
    assertThat(execution.isRun()).isFalse();
    execution.reset();
    executor.execute(new TestExecution() {

      @Override
      public void run() {

        try {

          millis(500).sleepAtLeast();

        } catch (final InterruptedException ignored) {

        }

        super.run();
      }
    });

    millis(300).sleepAtLeast();
    executor.execute(execution);
    assertThat(execution.isRun()).isFalse();
    millis(500).sleepAtLeast();
    assertThat(execution.isRun()).isTrue();
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testThrottlingExecutorError() {

    try {
      ScheduledExecutors.throttlingExecutor(null, 5);
      fail();

    } catch (final NullPointerException ignored) {

    }

    try {
      ScheduledExecutors.throttlingExecutor(ScheduledExecutors.defaultExecutor(), -1);
      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  @Test
  public void testZeroDelayExecutor() throws InterruptedException {

    final ZeroDelayExecutor instance =
        ZeroDelayExecutor.executorOf(ScheduledExecutors.defaultExecutor());
    assertThat(instance).isSameAs(
        ZeroDelayExecutor.executorOf(ScheduledExecutors.defaultExecutor()));
    assertThat(instance).isSameAs(ZeroDelayExecutor.executorOf(instance));
    testExecutor(instance);
    testExecutor(ScheduledExecutors.zeroDelayExecutor(ScheduledExecutors.poolExecutor()));
    testExecutor(new ScheduledExecutorDecorator(instance));
  }

  @Test
  public void testZeroDelayExecutorCancel() throws InterruptedException {
    final AtomicBoolean isManagedThread = new AtomicBoolean(true);
    final AtomicReference<ScheduledExecutor> executor = new AtomicReference<ScheduledExecutor>();
    executor.set(ScheduledExecutors.zeroDelayExecutor(
        new ScheduledExecutorDecorator(ScheduledExecutors.syncExecutor()) {

          @NotNull
          @Override
          protected ThreadManager getThreadManager() {
            return new ThreadManager() {

              public boolean isManagedThread() {
                return isManagedThread.get();
              }
            };
          }
        }));
    final TestExecution testExecution1 = new TestExecution() {

      private boolean mIsFirst = true;

      public void run() {

        if (mIsFirst) {
          mIsFirst = false;
          executor.get().execute(this);
          executor.get().cancel(this);

        } else {
          throw new IllegalStateException();
        }
      }
    };
    executor.get().execute(testExecution1);
    assertThat(testExecution1.isRun()).isFalse();

    final TestExecution testExecution2 = new TestExecution() {

      private boolean mIsFirst = true;

      public void run() {

        if (mIsFirst) {
          mIsFirst = false;
          executor.get().execute(this);
          ScheduledExecutors.syncExecutor().cancel(this);

        } else {
          super.run();
        }
      }
    };
    executor.get().execute(testExecution2);
    assertThat(testExecution2.isRun()).isTrue();
    isManagedThread.set(false);
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testZeroDelayExecutorError() {

    try {
      ZeroDelayExecutor.executorOf(null);
      fail();

    } catch (final NullPointerException ignored) {

    }

    try {
      ScheduledExecutors.zeroDelayExecutor(null);
      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  private void testExecutor(final ScheduledExecutor executor) throws InterruptedException {
    try {
      final Random random = new Random(System.currentTimeMillis());
      final ArrayList<TestRunExecution> executions = new ArrayList<TestRunExecution>();

      for (int i = 0; i < 13; i++) {

        final DurationMeasure delay;
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
            delay = noTime();
            break;
        }

        final TestRunExecution execution = new TestRunExecution(delay);
        executions.add(execution);

        executor.execute(execution, delay.value, delay.unit);
      }

      for (final TestRunExecution execution : executions) {

        execution.await();
        assertThat(execution.isPassed()).isTrue();
      }

      executions.clear();

      final ArrayList<DurationMeasure> delays = new ArrayList<DurationMeasure>();

      for (int i = 0; i < 13; i++) {

        final DurationMeasure delay;
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
            delay = noTime();
            break;
        }

        delays.add(delay);

        final TestRunExecution execution = new TestRunExecution(delay);
        executions.add(execution);
      }

      final TestRecursiveExecution recursiveExecution =
          new TestRecursiveExecution(executor, executions, delays, noTime());

      executor.execute(recursiveExecution);

      for (final TestRunExecution execution : executions) {

        execution.await();
        assertThat(execution.isPassed()).isTrue();
      }

    } finally {
      executor.stop();
    }
  }

  private static class TestExecution implements Runnable {

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

  private static class TestExecutor extends AsyncExecutor {

    private Runnable mLastCancelExecution;

    private Runnable mLastExecution;

    private TestExecutor() {
      super(new TestManager());
    }

    @Override
    public void cancel(@NotNull final Runnable command) {
      mLastCancelExecution = command;
    }

    @Override
    public void execute(@NotNull final Runnable command, final long delay,
        @NotNull final TimeUnit timeUnit) {
      mLastExecution = command;
    }

    private Runnable getLastCancelExecution() {
      return mLastCancelExecution;
    }

    private Runnable getLastExecution() {
      return mLastExecution;
    }

    private void resetExecutionThread() {
      ((TestManager) getThreadManager()).mThreadId = new Thread().getId();
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

  private static class TestRecursiveExecution extends TestRunExecution {

    private final ArrayList<DurationMeasure> mDelays;

    private final ArrayList<TestRunExecution> mExecutions;

    private final ScheduledExecutor mExecutor;

    public TestRecursiveExecution(final ScheduledExecutor executor,
        final ArrayList<TestRunExecution> executions, final ArrayList<DurationMeasure> delays,
        final DurationMeasure delay) {

      super(delay);

      mExecutor = executor;
      mExecutions = executions;
      mDelays = delays;
    }

    @Override
    public void run() {

      final ArrayList<TestRunExecution> executions = mExecutions;
      final ArrayList<DurationMeasure> delays = mDelays;
      final ScheduledExecutor executor = mExecutor;
      final int size = executions.size();

      for (int i = 0; i < size; i++) {

        final DurationMeasure delay = delays.get(i);
        final TestRunExecution execution = executions.get(i);

        executor.execute(execution, delay.value, delay.unit);
      }

      super.run();
    }
  }

  private static class TestRunExecution implements Runnable {

    private final DurationMeasure mDelay;

    private final Semaphore mSemaphore = new Semaphore(0);

    private final TimeMeasure mStartTime;

    private boolean mIsPassed;

    public TestRunExecution(final DurationMeasure delay) {

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
}
