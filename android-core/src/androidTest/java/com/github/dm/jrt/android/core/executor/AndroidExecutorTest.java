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

package com.github.dm.jrt.android.core.executor;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.test.AndroidTestCase;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.executor.ScheduledExecutorDecorator;
import com.github.dm.jrt.core.invocation.CallInvocation;
import com.github.dm.jrt.core.invocation.CommandInvocation;
import com.github.dm.jrt.core.invocation.InterruptedInvocationException;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.MappingInvocation;
import com.github.dm.jrt.core.invocation.TemplateInvocation;
import com.github.dm.jrt.core.util.DurationMeasure;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import static com.github.dm.jrt.core.invocation.InvocationFactory.factoryOf;
import static com.github.dm.jrt.core.util.DurationMeasure.micros;
import static com.github.dm.jrt.core.util.DurationMeasure.millis;
import static com.github.dm.jrt.core.util.DurationMeasure.nanos;
import static com.github.dm.jrt.core.util.DurationMeasure.noTime;
import static com.github.dm.jrt.core.util.DurationMeasure.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Android executors unit tests.
 * <p>
 * Created by davide-maestroni on 10/10/2014.
 */
public class AndroidExecutorTest extends AndroidTestCase {

  private static void testExecutor(final ScheduledExecutor executor) throws InterruptedException {

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
  }

  public void testConstructor() {

    boolean failed = false;
    try {
      new AndroidExecutors();
      failed = true;

    } catch (final Throwable ignored) {

    }

    assertThat(failed).isFalse();
  }

  @SuppressWarnings("ConstantConditions")
  public void testError() {

    try {

      AndroidExecutors.looperExecutor(null);

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  public void testHandlerExecutor() {

    final CommandInvocation<Handler> invocation = new CommandInvocation<Handler>(null) {

      @Override
      public void onComplete(@NotNull final Channel<Handler, ?> result) {

        result.pass(new Handler());
      }
    };
    final Channel<?, Handler> channel =
        JRoutineCore.routineOn(AndroidExecutors.handlerExecutor(new HandlerThread("test")))
                    .of(factoryOf(invocation, this, null))
                    .invoke()
                    .close();
    assertThat(JRoutineCore.routine()
                           .of(new HandlerInvocationFactory())
                           .invoke()
                           .pass(channel)
                           .close()
                           .in(seconds(30))
                           .next()).isEqualTo(true);
  }

  public void testLooperExecutor() {

    final TemplateInvocation<Object, Object> invocation = new TemplateInvocation<Object, Object>() {

      @Override
      public void onComplete(@NotNull final Channel<Object, ?> result) {

        result.pass(Looper.myLooper()).pass(AndroidExecutors.myExecutor());
      }
    };
    final Channel<?, Object> channel =
        JRoutineCore.routineOn(AndroidExecutors.handlerExecutor(new HandlerThread("test")))
                    .of(factoryOf(invocation, this))
                    .invoke()
                    .close();
    assertThat(JRoutineCore.routine()
                           .of(new LooperInvocationFactory())
                           .invoke()
                           .pass(channel)
                           .close()
                           .in(seconds(30))
                           .next()).isEqualTo(true);
  }

  public void testMainExecutor() throws InterruptedException {

    testExecutor(AndroidExecutors.mainExecutor());
    testExecutor(new MainExecutor());
    testExecutor(AndroidExecutors.looperExecutor(Looper.getMainLooper()));
    testExecutor(
        new ScheduledExecutorDecorator(AndroidExecutors.looperExecutor(Looper.getMainLooper())));
  }

  public void testTaskExecutor() throws InterruptedException {

    testExecutor(new AsyncTaskExecutor(null));
    testExecutor(AndroidExecutors.taskExecutor());
    testExecutor(AndroidExecutors.taskExecutor(Executors.newCachedThreadPool()));
    testExecutor(new ScheduledExecutorDecorator(
        AndroidExecutors.taskExecutor(Executors.newSingleThreadExecutor())));
  }

  public void testThreadExecutor() throws InterruptedException {

    testExecutor(AndroidExecutors.handlerExecutor(new HandlerThread("test")));
  }

  private static class HandlerInvocationFactory extends MappingInvocation<Handler, Object> {

    /**
     * Constructor.
     */
    protected HandlerInvocationFactory() {

      super(null);
    }

    public void onInput(final Handler input, @NotNull final Channel<Object, ?> result) throws
        Exception {

      testExecutor(new HandlerExecutor(input));
      testExecutor(AndroidExecutors.handlerExecutor(input));
      result.pass(true);
    }
  }

  private static class LooperInvocationFactory extends InvocationFactory<Object, Object> {

    /**
     * Constructor.
     */
    protected LooperInvocationFactory() {

      super(null);
    }

    @NotNull
    @Override
    public Invocation<Object, Object> newInvocation() {

      return new CallInvocation<Object, Object>() {

        @Override
        protected void onCall(@NotNull final List<?> objects,
            @NotNull final Channel<Object, ?> result) {

          try {

            testExecutor(AndroidExecutors.looperExecutor((Looper) objects.get(0)));
            testExecutor((ScheduledExecutor) objects.get(1));

            result.pass(true);

          } catch (final InterruptedException e) {

            throw new InterruptedInvocationException(e);
          }
        }
      };
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
        executor.execute(executions.get(i), delay.value, delay.unit);
      }

      super.run();
    }
  }

  private static class TestRunExecution implements Runnable {

    private final DurationMeasure mDelay;

    private final Semaphore mSemaphore = new Semaphore(0);

    private final long mStartTime;

    private boolean mIsPassed;

    public TestRunExecution(final DurationMeasure delay) {

      mStartTime = System.currentTimeMillis();
      mDelay = delay;
    }

    public void await() throws InterruptedException {

      mSemaphore.acquire();
    }

    public boolean isPassed() {

      return mIsPassed;
    }

    public void run() {

      // It looks like that handlers and the kind are not so accurate after all...
      // Let's have a 10 millisecond error tolerance
      mIsPassed = (System.currentTimeMillis() - mStartTime + 10 >= mDelay.toMillis());
      mSemaphore.release();
    }
  }
}
