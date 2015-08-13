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
package com.gh.bmd.jrt.runner;

import com.gh.bmd.jrt.builder.InvocationConfiguration.AgingPriority;
import com.gh.bmd.jrt.builder.InvocationConfiguration.NotAgingPriority;
import com.gh.bmd.jrt.util.Time;
import com.gh.bmd.jrt.util.TimeDuration;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import static com.gh.bmd.jrt.util.Time.current;
import static com.gh.bmd.jrt.util.TimeDuration.ZERO;
import static com.gh.bmd.jrt.util.TimeDuration.micros;
import static com.gh.bmd.jrt.util.TimeDuration.millis;
import static com.gh.bmd.jrt.util.TimeDuration.nanos;
import static com.gh.bmd.jrt.util.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Runners unit tests.
 * <p/>
 * Created by davide-maestroni on 10/2/14.
 */
public class RunnerTest {

    @Test
    public void testDynamicPoolRunner() throws InterruptedException {

        testRunner(Runners.sharedRunner());
        testRunner(Runners.dynamicPoolRunner(3, 4, 5L, TimeUnit.SECONDS));
        testRunner(new RunnerDecorator(Runners.sharedRunner()));
        testRunner(new RunnerDecorator(Runners.dynamicPoolRunner(1, 4, 0L, TimeUnit.SECONDS)));
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

        testRunner(Runners.priorityRunner(Runners.sharedRunner())
                          .getRunner(AgingPriority.NORMAL_PRIORITY));
        testRunner(Runners.priorityRunner(Runners.queuedRunner())
                          .getRunner(AgingPriority.LOW_PRIORITY));
        testRunner(new RunnerDecorator(Runners.priorityRunner(Runners.poolRunner())
                                              .getRunner(AgingPriority.LOWEST_PRIORITY)));

        final PriorityRunner priorityRunner = Runners.priorityRunner(Runners.sharedRunner());
        testRunner(priorityRunner.getRunner(NotAgingPriority.NORMAL_PRIORITY));
        testRunner(priorityRunner.getRunner(NotAgingPriority.LOW_PRIORITY));
        testRunner(new RunnerDecorator(priorityRunner.getRunner(NotAgingPriority.LOWEST_PRIORITY)));
    }

    @Test
    public void testQueuedRunner() throws InterruptedException {

        testRunner(new QueuedRunner());
        testRunner(Runners.queuedRunner());
        testRunner(new RunnerDecorator(new QueuedRunner()));
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testQueuingRunner() {

        final TestExecution execution = new TestExecution();
        final TestRunner testRunner = new TestRunner();
        final Runner runner = Runners.priorityRunner(testRunner).getRunner(0);
        assertThat(runner.isExecutionThread()).isFalse();
        testRunner.setExecutionThread(Thread.currentThread());
        assertThat(runner.isExecutionThread()).isTrue();
        execution.setCancelable(true);
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
        assertThat(testRunner.getLastCancelExecution()).isNull();
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
        execution.setCancelable(false);
        testRunner.cancel(null);
        runner.run(execution, 1, TimeUnit.MILLISECONDS);
        runner.cancel(execution);
        assertThat(execution.isRun()).isFalse();
        assertThat(testRunner.getLastCancelExecution()).isNull();
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

        final PriorityRunner priorityRunner = Runners.priorityRunner(Runners.queuedRunner());
        assertThat(Runners.priorityRunner(priorityRunner.getRunner(3))).isSameAs(priorityRunner);
    }

    @Test
    public void testScheduledRunner() throws InterruptedException {

        testRunner(new ScheduledRunner(Executors.newSingleThreadScheduledExecutor()));
        testRunner(Runners.scheduledRunner(Executors.newSingleThreadScheduledExecutor()));
        testRunner(new RunnerDecorator(
                new ScheduledRunner(Executors.newSingleThreadScheduledExecutor())));
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testScheduledRunnerError() {

        try {

            new ScheduledRunner(null);

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
    public void testSequentialRunner() throws InterruptedException {

        testRunner(new SequentialRunner());
        testRunner(Runners.sequentialRunner());
        testRunner(new RunnerDecorator(new SequentialRunner()));
    }

    @Test
    public void testThrottlingRunner() throws InterruptedException {

        testRunner(new ThrottlingRunner(Runners.sharedRunner(), 5));
        testRunner(Runners.throttlingRunner(Runners.poolRunner(), 5));
        testRunner(new RunnerDecorator(new ThrottlingRunner(Runners.sharedRunner(), 5)));
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testThrottlingRunnerCancel() throws InterruptedException {

        final TestExecution execution = new TestExecution();
        Runner runner = Runners.throttlingRunner(Runners.queuedRunner(), 1);
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

        millis(300).sleepAtLeast();
        execution.setCancelable(true);
        runner.run(execution, 0, TimeUnit.MILLISECONDS);
        millis(300).sleepAtLeast();
        assertThat(execution.isRun()).isFalse();
        runner.cancel(execution);
        millis(500).sleepAtLeast();
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

    private void testRunner(final Runner runner) throws InterruptedException {

        final Random random = new Random(System.currentTimeMillis());
        final ArrayList<TestRunExecution> executions = new ArrayList<TestRunExecution>();

        for (int i = 0; i < 13; i++) {

            final TimeDuration delay;
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
                    delay = ZERO;
                    break;
            }

            final TestRunExecution execution = new TestRunExecution(delay);
            executions.add(execution);

            runner.run(execution, delay.time, delay.unit);
        }

        for (final TestRunExecution execution : executions) {

            execution.await();
            assertThat(execution.isPassed()).isTrue();
        }

        executions.clear();

        final ArrayList<TimeDuration> delays = new ArrayList<TimeDuration>();

        for (int i = 0; i < 13; i++) {

            final TimeDuration delay;
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
                    delay = ZERO;
                    break;
            }

            delays.add(delay);

            final TestRunExecution execution = new TestRunExecution(delay);
            executions.add(execution);
        }

        final TestRecursiveExecution recursiveExecution =
                new TestRecursiveExecution(runner, executions, delays, ZERO);

        runner.run(recursiveExecution, ZERO.time, ZERO.unit);

        for (final TestRunExecution execution : executions) {

            execution.await();
            assertThat(execution.isPassed()).isTrue();
        }
    }

    private static class TestExecution implements Execution {

        private boolean mIsCancelable;

        private boolean mIsRun;

        public boolean mayBeCanceled() {

            return mIsCancelable;
        }

        public void run() {

            mIsRun = true;
        }

        private boolean isRun() {

            return mIsRun;
        }

        private void reset() {

            mIsRun = false;
        }

        private void setCancelable(final boolean isCancelable) {

            mIsCancelable = isCancelable;
        }
    }

    private static class TestRecursiveExecution extends TestRunExecution {

        private final ArrayList<TimeDuration> mDelays;

        private final ArrayList<TestRunExecution> mExecutions;

        private final Runner mRunner;

        public TestRecursiveExecution(final Runner runner,
                final ArrayList<TestRunExecution> executions, final ArrayList<TimeDuration> delays,
                final TimeDuration delay) {

            super(delay);

            mRunner = runner;
            mExecutions = executions;
            mDelays = delays;
        }

        @Override
        public void run() {

            final ArrayList<TestRunExecution> executions = mExecutions;
            final ArrayList<TimeDuration> delays = mDelays;
            final Runner runner = mRunner;
            final int size = executions.size();

            for (int i = 0; i < size; i++) {

                final TimeDuration delay = delays.get(i);
                final TestRunExecution execution = executions.get(i);

                runner.run(execution, delay.time, delay.unit);
            }

            super.run();
        }
    }

    private static class TestRunExecution extends TemplateExecution {

        private final TimeDuration mDelay;

        private final Semaphore mSemaphore = new Semaphore(0);

        private final Time mStartTime;

        private boolean mIsPassed;

        public TestRunExecution(final TimeDuration delay) {

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

            // the JVM might not have nanosecond precision...
            mIsPassed = (current().toMillis() - mStartTime.toMillis() >= mDelay.toMillis());
            mSemaphore.release();
        }
    }

    private static class TestRunner implements Runner {

        private Execution mLastCancelExecution;

        private Execution mLastExecution;

        private Thread mThread;

        public void cancel(@Nonnull final Execution execution) {

            mLastCancelExecution = execution;
        }

        public boolean isExecutionThread() {

            return (Thread.currentThread() == mThread);
        }

        private void setExecutionThread(final Thread thread) {

            mThread = thread;
        }

        public void run(@Nonnull final Execution execution, final long delay,
                @Nonnull final TimeUnit timeUnit) {

            mLastExecution = execution;
        }

        private Execution getLastCancelExecution() {

            return mLastCancelExecution;
        }

        private Execution getLastExecution() {

            return mLastExecution;
        }
    }
}
