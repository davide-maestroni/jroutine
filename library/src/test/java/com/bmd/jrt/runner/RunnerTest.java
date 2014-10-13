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
package com.bmd.jrt.runner;

import com.bmd.jrt.time.Time;
import com.bmd.jrt.time.TimeDuration;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Semaphore;

import static com.bmd.jrt.time.Time.current;
import static com.bmd.jrt.time.TimeDuration.ZERO;
import static com.bmd.jrt.time.TimeDuration.micros;
import static com.bmd.jrt.time.TimeDuration.millis;
import static com.bmd.jrt.time.TimeDuration.nanos;
import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Runners unit tests.
 * <p/>
 * Created by davide on 10/2/14.
 */
public class RunnerTest extends TestCase {

    @SuppressWarnings("ConstantConditions")
    public void testError() {

        try {

            new ThreadPoolRunner(-1);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            Runners.pool(-1);

            fail();

        } catch (final Exception ignored) {

        }

        try {

            new RunnerDecorator(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testPoolRunner() throws InterruptedException {

        testRunner(new ThreadPoolRunner(4));
        testRunner(Runners.pool(3));
        testRunner(Runners.shared());
        testRunner(new RunnerDecorator(new ThreadPoolRunner(4)));
    }

    public void testQueuedRunner() throws InterruptedException {

        testRunner(new QueuedRunner());
        testRunner(Runners.queued());
        testRunner(new RunnerDecorator(new QueuedRunner()));
    }

    public void testSequentialRunner() throws InterruptedException {

        testRunner(new SequentialRunner());
        testRunner(Runners.sequential());
        testRunner(new RunnerDecorator(new SequentialRunner()));
    }

    private void testRunner(final Runner runner) throws InterruptedException {

        final Random random = new Random(System.currentTimeMillis());
        final ArrayList<TestRunInvocation> invocations = new ArrayList<TestRunInvocation>();

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

            if (random.nextBoolean()) {

                final TestRunInvocation invocation = new TestRunInvocation(delay);
                invocations.add(invocation);

                runner.run(invocation, delay.time, delay.unit);

            } else {

                final TestAbortInvocation invocation = new TestAbortInvocation(delay);
                invocations.add(invocation);

                runner.runAbort(invocation, delay.time, delay.unit);
            }
        }

        for (final TestRunInvocation invocation : invocations) {

            invocation.await();
            assertThat(invocation.isPassed()).isTrue();
        }

        invocations.clear();

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

            if (random.nextBoolean()) {

                final TestRunInvocation invocation = new TestRunInvocation(delay);
                invocations.add(invocation);

            } else {

                final TestAbortInvocation invocation = new TestAbortInvocation(delay);
                invocations.add(invocation);
            }
        }

        final TestRecursiveInvocation recursiveInvocation =
                new TestRecursiveInvocation(runner, invocations, delays, ZERO);

        runner.run(recursiveInvocation, ZERO.time, ZERO.unit);

        for (final TestRunInvocation invocation : invocations) {

            invocation.await();
            assertThat(invocation.isPassed()).isTrue();
        }
    }

    private static class TestAbortInvocation extends TestRunInvocation {

        public TestAbortInvocation(final TimeDuration delay) {

            super(delay);
        }

        @Override
        public void abort() {

            super.run();
        }

        @Override
        public void run() {

            super.abort();
        }


    }

    private static class TestRecursiveInvocation extends TestRunInvocation {

        private final ArrayList<TimeDuration> mDelays;

        private final ArrayList<TestRunInvocation> mInvocations;

        private final Runner mRunner;

        public TestRecursiveInvocation(final Runner runner,
                final ArrayList<TestRunInvocation> invocations,
                final ArrayList<TimeDuration> delays, final TimeDuration delay) {

            super(delay);

            mRunner = runner;
            mInvocations = invocations;
            mDelays = delays;
        }

        @Override
        public void run() {

            final ArrayList<TestRunInvocation> invocations = mInvocations;
            final ArrayList<TimeDuration> delays = mDelays;
            final Runner runner = mRunner;
            final int size = invocations.size();

            for (int i = 0; i < size; i++) {

                final TimeDuration delay = delays.get(i);
                final TestRunInvocation invocation = invocations.get(i);

                if (invocation instanceof TestAbortInvocation) {

                    runner.runAbort(invocation, delay.time, delay.unit);

                } else {

                    runner.run(invocation, delay.time, delay.unit);
                }
            }

            super.run();
        }
    }

    private static class TestRunInvocation implements Invocation {

        private final TimeDuration mDelay;

        private final Semaphore mSemaphore = new Semaphore(0);

        private final Time mStartTime;

        private boolean mIsPassed;

        public TestRunInvocation(final TimeDuration delay) {

            mStartTime = current();
            mDelay = delay;
        }

        @Override
        public void abort() {

            mSemaphore.release();
        }

        @Override
        public void run() {

            // the JVM might not have nanosecond precision...
            mIsPassed = (current().toMillis() - mStartTime.toMillis() >= mDelay.toMillis());

            mSemaphore.release();
        }

        public void await() throws InterruptedException {

            mSemaphore.acquire();
        }

        public boolean isPassed() {

            return mIsPassed;
        }
    }
}