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

import com.gh.bmd.jrt.runner.PriorityRunner.AgingPriority;
import com.gh.bmd.jrt.runner.PriorityRunner.NotAgingPriority;
import com.gh.bmd.jrt.time.Time;
import com.gh.bmd.jrt.time.TimeDuration;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static com.gh.bmd.jrt.time.Time.current;
import static com.gh.bmd.jrt.time.TimeDuration.ZERO;
import static com.gh.bmd.jrt.time.TimeDuration.micros;
import static com.gh.bmd.jrt.time.TimeDuration.millis;
import static com.gh.bmd.jrt.time.TimeDuration.nanos;
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
    public void testPoolRunner() throws InterruptedException {

        testRunner(Runners.poolRunner(3));
        testRunner(new RunnerDecorator(Runners.poolRunner(4)));
    }

    @Test
    public void testPoolRunnerError() {

        try {

            Runners.poolRunner(-1);

            fail();

        } catch (final Exception ignored) {

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
    public void testRunnerDecoratorError() {

        try {

            new RunnerDecorator(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
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

        } catch (final Exception ignored) {

        }

        try {

            Runners.scheduledRunner(null);

            fail();

        } catch (final Exception ignored) {

        }
    }

    @Test
    public void testSequentialRunner() throws InterruptedException {

        testRunner(new SequentialRunner());
        testRunner(Runners.sequentialRunner());
        testRunner(new RunnerDecorator(new SequentialRunner()));
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

    private static class TestRunExecution implements Execution {

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
}
