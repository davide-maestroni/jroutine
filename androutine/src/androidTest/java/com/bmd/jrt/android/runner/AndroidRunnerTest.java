/**
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
package com.bmd.jrt.android.runner;

import android.os.HandlerThread;
import android.os.Looper;
import android.test.AndroidTestCase;

import com.bmd.jrt.runner.Execution;
import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.runner.RunnerWrapper;
import com.bmd.jrt.time.TimeDuration;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import static com.bmd.jrt.time.TimeDuration.ZERO;
import static com.bmd.jrt.time.TimeDuration.micros;
import static com.bmd.jrt.time.TimeDuration.millis;
import static com.bmd.jrt.time.TimeDuration.nanos;
import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Android runners unit tests.
 * <p/>
 * Created by davide on 10/10/14.
 */
public class AndroidRunnerTest extends AndroidTestCase {

    @SuppressWarnings("ConstantConditions")
    public void testError() {

        try {

            new LooperRunner(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testLooperRunner() throws InterruptedException {

        testRunner(new LooperRunner(Looper.myLooper()));
        testRunner(AndroidRunners.mainRunner());
        testRunner(AndroidRunners.myRunner());
        testRunner(AndroidRunners.threadRunner(new HandlerThread("test")));
        testRunner(new RunnerWrapper(AndroidRunners.mainRunner()));
    }

    public void testMainRunner() throws InterruptedException {

        testRunner(new MainRunner());
    }

    public void testTaskRunner() throws InterruptedException {

        testRunner(new AsyncTaskRunner(null));
        testRunner(AndroidRunners.taskRunner());
        testRunner(AndroidRunners.taskRunner(Executors.newCachedThreadPool()));
        testRunner(
                new RunnerWrapper(AndroidRunners.taskRunner(Executors.newSingleThreadExecutor())));
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
                runner.run(executions.get(i), delay.time, delay.unit);
            }

            super.run();
        }
    }

    private static class TestRunExecution implements Execution {

        private final TimeDuration mDelay;

        private final Semaphore mSemaphore = new Semaphore(0);

        private final long mStartTime;

        private boolean mIsPassed;

        public TestRunExecution(final TimeDuration delay) {

            mStartTime = System.currentTimeMillis();
            mDelay = delay;
        }

        public void await() throws InterruptedException {

            mSemaphore.acquire();
        }

        public boolean isPassed() {

            return mIsPassed;
        }

        @Override
        public void run() {

            // it looks like that handlers and the kind are not so accurate after all...
            // let's have a 1 millisecond error tolerance
            mIsPassed = (System.currentTimeMillis() - mStartTime + 1 >= mDelay.toMillis());

            mSemaphore.release();
        }
    }
}
