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

package com.github.dm.jrt.android.core.runner;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.test.AndroidTestCase;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.invocation.CallInvocation;
import com.github.dm.jrt.core.invocation.CommandInvocation;
import com.github.dm.jrt.core.invocation.ConversionInvocation;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.InvocationInterruptedException;
import com.github.dm.jrt.core.invocation.TemplateInvocation;
import com.github.dm.jrt.core.runner.Execution;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.runner.RunnerDecorator;
import com.github.dm.jrt.core.util.UnitDuration;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.core.invocation.InvocationFactory.factoryOf;
import static com.github.dm.jrt.core.util.UnitDuration.micros;
import static com.github.dm.jrt.core.util.UnitDuration.millis;
import static com.github.dm.jrt.core.util.UnitDuration.nanos;
import static com.github.dm.jrt.core.util.UnitDuration.seconds;
import static com.github.dm.jrt.core.util.UnitDuration.zero;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Android runners unit tests.
 * <p>
 * Created by davide-maestroni on 10/10/2014.
 */
public class RunnerTest extends AndroidTestCase {

    private static void testRunner(final Runner runner) throws InterruptedException {

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

    public void testConstructor() {

        boolean failed = false;
        try {
            new AndroidRunners();
            failed = true;

        } catch (final Throwable ignored) {

        }

        assertThat(failed).isFalse();
    }

    @SuppressWarnings("ConstantConditions")
    public void testError() {

        try {

            AndroidRunners.looperRunner(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testHandlerRunner() throws InterruptedException {

        final CommandInvocation<Handler> invocation = new CommandInvocation<Handler>(null) {

            public void onResult(@NotNull final ResultChannel<Handler> result) {

                result.pass(new Handler());
            }
        };
        final OutputChannel<Handler> channel = JRoutineCore.on(factoryOf(invocation, this, null))
                                                           .invocationConfiguration()
                                                           .withRunner(AndroidRunners.handlerRunner(
                                                                   new HandlerThread("test")))
                                                           .apply()
                                                           .asyncCall();
        assertThat(JRoutineCore.on(new HandlerInvocationFactory())
                               .asyncCall(channel)
                               .afterMax(seconds(30))
                               .next()).isEqualTo(true);
    }

    public void testLooperRunner() {

        final TemplateInvocation<Object, Object> invocation =
                new TemplateInvocation<Object, Object>() {

                    @Override
                    public void onResult(@NotNull final ResultChannel<Object> result) {

                        result.pass(Looper.myLooper()).pass(AndroidRunners.myRunner());
                    }
                };
        final OutputChannel<Object> channel = JRoutineCore.on(factoryOf(invocation, this))
                                                          .invocationConfiguration()
                                                          .withRunner(AndroidRunners.handlerRunner(
                                                                  new HandlerThread("test")))
                                                          .apply()
                                                          .asyncCall();

        assertThat(JRoutineCore.on(new LooperInvocationFactory())
                               .asyncCall(channel)
                               .afterMax(seconds(30))
                               .next()).isEqualTo(true);
    }

    public void testMainRunner() throws InterruptedException {

        testRunner(AndroidRunners.mainRunner());
        testRunner(new MainRunner());
        testRunner(AndroidRunners.looperRunner(Looper.getMainLooper()));
        testRunner(new RunnerDecorator(AndroidRunners.looperRunner(Looper.getMainLooper())));
    }

    public void testTaskRunner() throws InterruptedException {

        testRunner(new AsyncTaskRunner(null));
        testRunner(AndroidRunners.taskRunner());
        testRunner(AndroidRunners.taskRunner(Executors.newCachedThreadPool()));
        testRunner(new RunnerDecorator(
                AndroidRunners.taskRunner(Executors.newSingleThreadExecutor())));
    }

    public void testThreadRunner() throws InterruptedException {

        testRunner(AndroidRunners.handlerRunner(new HandlerThread("test")));
    }

    private static class HandlerInvocationFactory extends ConversionInvocation<Handler, Object> {

        /**
         * Constructor.
         */
        protected HandlerInvocationFactory() {

            super(null);
        }

        public void onInput(final Handler input, @NotNull final ResultChannel<Object> result) throws
                Exception {

            testRunner(new HandlerRunner(input));
            testRunner(AndroidRunners.handlerRunner(input));
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
                        @NotNull final ResultChannel<Object> result) {

                    try {

                        testRunner(AndroidRunners.looperRunner((Looper) objects.get(0)));
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

        private final ArrayList<UnitDuration> mDelays;

        private final ArrayList<TestRunExecution> mExecutions;

        private final Runner mRunner;

        public TestRecursiveExecution(final Runner runner,
                final ArrayList<TestRunExecution> executions, final ArrayList<UnitDuration> delays,
                final UnitDuration delay) {

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
                runner.run(executions.get(i), delay.value, delay.unit);
            }

            super.run();
        }
    }

    private static class TestRunExecution implements Execution {

        private final UnitDuration mDelay;

        private final Semaphore mSemaphore = new Semaphore(0);

        private final long mStartTime;

        private boolean mIsPassed;

        public TestRunExecution(final UnitDuration delay) {

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
