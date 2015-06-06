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
package com.gh.bmd.jrt.android.runner;

import android.os.HandlerThread;
import android.os.Looper;
import android.test.AndroidTestCase;

import com.gh.bmd.jrt.android.v11.core.JRoutine;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.ResultChannel;
import com.gh.bmd.jrt.invocation.FunctionInvocation;
import com.gh.bmd.jrt.invocation.Invocation;
import com.gh.bmd.jrt.invocation.InvocationFactory;
import com.gh.bmd.jrt.invocation.InvocationInterruptedException;
import com.gh.bmd.jrt.invocation.TemplateInvocation;
import com.gh.bmd.jrt.runner.Execution;
import com.gh.bmd.jrt.runner.Runner;
import com.gh.bmd.jrt.runner.RunnerDecorator;
import com.gh.bmd.jrt.util.ClassToken;
import com.gh.bmd.jrt.util.TimeDuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import javax.annotation.Nonnull;

import static com.gh.bmd.jrt.util.TimeDuration.ZERO;
import static com.gh.bmd.jrt.util.TimeDuration.micros;
import static com.gh.bmd.jrt.util.TimeDuration.millis;
import static com.gh.bmd.jrt.util.TimeDuration.nanos;
import static com.gh.bmd.jrt.util.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Android runners unit tests.
 * <p/>
 * Created by davide-maestroni on 10/10/14.
 */
public class AndroidRunnerTest extends AndroidTestCase {

    private static void testRunner(final Runner runner) throws InterruptedException {

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

    @SuppressWarnings("ConstantConditions")
    public void testError() {

        try {

            new LooperRunner(null, null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testLooperRunner() throws InterruptedException {

        testRunner(new LooperRunner(Looper.myLooper(), Runners.queuedRunner()));
        testRunner(Runners.myRunner());

        final TemplateInvocation<Object, Object> invocation =
                new TemplateInvocation<Object, Object>() {

                    @Override
                    public void onResult(@Nonnull final ResultChannel<Object> result) {

                        result.pass(Looper.myLooper()).pass(Runners.myRunner());
                    }
                };
        final OutputChannel<Object> channel = JRoutine.on(ClassToken.tokenOf(invocation))
                                                      .withInvocation()
                                                      .withFactoryArgs(this)
                                                      .withAsyncRunner(Runners.handlerRunner(
                                                              new HandlerThread("test")))
                                                      .set()
                                                      .callAsync();

        assertThat(JRoutine.on(new LooperInvocationFactory())
                           .callAsync(channel)
                           .afterMax(seconds(30))
                           .next()).isEqualTo(true);
    }

    public void testMainRunner() throws InterruptedException {

        testRunner(Runners.mainRunner());
        testRunner(new MainRunner());
        testRunner(Runners.looperRunner(Looper.getMainLooper()));
        testRunner(new RunnerDecorator(Runners.looperRunner(Looper.getMainLooper())));
        testRunner(Runners.looperRunner(Looper.getMainLooper(), Runners.queuedRunner()));
        testRunner(new RunnerDecorator(
                Runners.looperRunner(Looper.getMainLooper(), Runners.queuedRunner())));
    }

    public void testTaskRunner() throws InterruptedException {

        testRunner(new AsyncTaskRunner(null));
        testRunner(Runners.taskRunner());
        testRunner(Runners.taskRunner(Executors.newCachedThreadPool()));
        testRunner(new RunnerDecorator(Runners.taskRunner(Executors.newSingleThreadExecutor())));
    }

    public void testThreadRunner() throws InterruptedException {

        testRunner(Runners.handlerRunner(new HandlerThread("test")));
    }

    private static class LooperInvocationFactory implements InvocationFactory<Object, Object> {

        @Nonnull
        public Invocation<Object, Object> newInvocation(@Nonnull final Object... args) {

            return new FunctionInvocation<Object, Object>() {

                @Override
                public void onCall(@Nonnull final List<?> objects,
                        @Nonnull final ResultChannel<Object> result) {

                    try {

                        testRunner(new LooperRunner((Looper) objects.get(0), null));
                        testRunner((Runner) objects.get(1));

                        result.pass(true);

                    } catch (final InterruptedException e) {

                        throw new InvocationInterruptedException(e);
                    }
                }
            };
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

        public void run() {

            // it looks like that handlers and the kind are not so accurate after all...
            // let's have a 10 millisecond error tolerance
            mIsPassed = (System.currentTimeMillis() - mStartTime + 10 >= mDelay.toMillis());

            mSemaphore.release();
        }
    }
}
