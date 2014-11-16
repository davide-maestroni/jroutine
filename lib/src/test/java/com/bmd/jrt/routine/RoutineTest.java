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
package com.bmd.jrt.routine;

import com.bmd.jrt.annotation.Async;
import com.bmd.jrt.annotation.AsyncOverride;
import com.bmd.jrt.builder.RoutineConfiguration;
import com.bmd.jrt.builder.RoutineConfigurationBuilder;
import com.bmd.jrt.channel.BasicOutputConsumer;
import com.bmd.jrt.channel.OutputChannel;
import com.bmd.jrt.channel.OutputConsumer;
import com.bmd.jrt.channel.ParameterChannel;
import com.bmd.jrt.channel.ResultChannel;
import com.bmd.jrt.common.ClassToken;
import com.bmd.jrt.common.RoutineException;
import com.bmd.jrt.invocation.BasicInvocation;
import com.bmd.jrt.invocation.Invocation;
import com.bmd.jrt.invocation.SimpleInvocation;
import com.bmd.jrt.log.Log.LogLevel;
import com.bmd.jrt.log.Logger;
import com.bmd.jrt.log.NullLog;
import com.bmd.jrt.routine.DefaultExecution.InputIterator;
import com.bmd.jrt.routine.DefaultParameterChannel.InvocationManager;
import com.bmd.jrt.routine.DefaultResultChannel.AbortHandler;
import com.bmd.jrt.runner.Runners;
import com.bmd.jrt.time.TimeDuration;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.bmd.jrt.common.ClassToken.tokenOf;
import static com.bmd.jrt.routine.JavaRoutine.on;
import static com.bmd.jrt.time.TimeDuration.ZERO;
import static com.bmd.jrt.time.TimeDuration.seconds;
import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Routine unit tests.
 * <p/>
 * Created by davide on 9/9/14.
 */
public class RoutineTest extends TestCase {

    public void testAbort() {

        final Routine<String, String> routine =
                on(tokenOf(DelayedInvocation.class)).withArgs(TimeDuration.millis(100))
                                                    .buildRoutine();

        final ParameterChannel<String, String> inputChannel = routine.invokeAsync().pass("test1");
        final OutputChannel<String> outputChannel = inputChannel.results();

        assertThat(inputChannel.isOpen()).isFalse();
        assertThat(inputChannel.abort(new IllegalArgumentException("test1"))).isFalse();
        assertThat(inputChannel.isOpen()).isFalse();
        assertThat(outputChannel.readFirst()).isEqualTo("test1");

        final ParameterChannel<String, String> inputChannel1 = routine.invokeAsync().pass("test1");
        final OutputChannel<String> outputChannel1 = inputChannel1.results();

        assertThat(inputChannel1.isOpen()).isFalse();
        assertThat(inputChannel1.abort()).isFalse();
        assertThat(inputChannel1.isOpen()).isFalse();
        assertThat(outputChannel1.isOpen()).isTrue();
        assertThat(outputChannel1.readFirst()).isEqualTo("test1");
        assertThat(outputChannel1.isOpen()).isFalse();

        final OutputChannel<String> channel = routine.runAsync("test2");
        assertThat(channel.isOpen()).isTrue();
        assertThat(channel.abort(new IllegalArgumentException("test2"))).isTrue();
        assertThat(channel.abort()).isFalse();
        assertThat(channel.isOpen()).isTrue();

        try {

            channel.readAll();

            fail();

        } catch (final RoutineException ex) {

            assertThat(ex.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
            assertThat(ex.getCause().getMessage()).isEqualTo("test2");
        }

        assertThat(channel.isOpen()).isFalse();


        final OutputChannel<String> channel1 = routine.runAsync("test2");
        assertThat(channel1.isOpen()).isTrue();
        assertThat(channel1.abort()).isTrue();
        assertThat(channel1.abort(new IllegalArgumentException("test2"))).isFalse();
        assertThat(channel1.isOpen()).isTrue();

        try {

            channel1.readAll();

            fail();

        } catch (final RoutineException ex) {

            assertThat(ex.getCause()).isNull();
        }

        assertThat(channel1.isOpen()).isFalse();


        final Invocation<String, String> abortInvocation = new BasicInvocation<String, String>() {

            @Override
            public void onInput(final String s, @Nonnull final ResultChannel<String> results) {

                assertThat(results.isOpen()).isTrue();
                assertThat(results.abort(new IllegalArgumentException(s))).isTrue();
                assertThat(results.abort()).isFalse();
                assertThat(results.isOpen()).isFalse();
            }
        };

        final Routine<String, String> routine1 =
                on(ClassToken.tokenOf(abortInvocation)).withArgs(this).buildRoutine();

        try {

            routine1.invokeAsync()
                    .after(TimeDuration.millis(10))
                    .pass("test_abort")
                    .results()
                    .readFirst();

            fail();

        } catch (final RoutineException ex) {

            assertThat(ex.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
            assertThat(ex.getCause().getMessage()).isEqualTo("test_abort");
        }

        final Invocation<String, String> abortInvocation2 = new BasicInvocation<String, String>() {

            @Override
            public void onInput(final String s, @Nonnull final ResultChannel<String> results) {

                assertThat(results.abort()).isTrue();
                assertThat(results.abort(new IllegalArgumentException(s))).isFalse();
            }
        };

        final Routine<String, String> routine2 =
                on(ClassToken.tokenOf(abortInvocation2)).withArgs(this).buildRoutine();

        try {

            routine2.invokeAsync()
                    .after(TimeDuration.millis(10))
                    .pass("test_abort")
                    .results()
                    .readFirst();

            fail();

        } catch (final RoutineException ex) {

            assertThat(ex.getCause()).isNull();
        }
    }

    public void testAbortInput() throws InterruptedException {

        final Semaphore semaphore = new Semaphore(0);
        final AtomicReference<Throwable> abortReason = new AtomicReference<Throwable>();

        final BasicInvocation<String, String> abortInvocation =
                new BasicInvocation<String, String>() {

                    @Override
                    public void onAbort(@Nullable final Throwable reason) {

                        abortReason.set(reason);
                        semaphore.release();
                    }
                };

        final Routine<String, String> routine = JavaRoutine.on(tokenOf(abortInvocation))
                                                           .withArgs(this, abortReason, semaphore)
                                                           .buildRoutine();

        final ParameterChannel<String, String> channel = routine.invokeAsync();
        final IllegalArgumentException exception = new IllegalArgumentException();
        channel.after(TimeDuration.millis(100)).abort(exception);

        semaphore.tryAcquire(1, TimeUnit.SECONDS);

        assertThat(abortReason.get()).isEqualTo(exception);

        final ParameterChannel<String, String> channel1 = routine.invokeAsync();
        final IllegalAccessError exception1 = new IllegalAccessError();
        channel1.now().abort(exception1);

        semaphore.tryAcquire(1, TimeUnit.SECONDS);

        assertThat(abortReason.get()).isEqualTo(exception1);
    }

    public void testCalls() {

        final Routine<String, String> routine = JavaRoutine.<String>on().buildRoutine();

        assertThat(routine.call()).isEmpty();
        assertThat(routine.call(Arrays.asList("test1", "test2"))).containsExactly("test1", "test2");
        assertThat(routine.call(routine.run("test1", "test2"))).containsExactly("test1", "test2");
        assertThat(routine.call("test1")).containsExactly("test1");
        assertThat(routine.call("test1", "test2")).containsExactly("test1", "test2");
        assertThat(routine.callAsync()).isEmpty();
        assertThat(routine.callAsync(Arrays.asList("test1", "test2"))).containsExactly("test1",
                                                                                       "test2");
        assertThat(routine.callAsync(routine.run("test1", "test2"))).containsExactly("test1",
                                                                                     "test2");
        assertThat(routine.callAsync("test1")).containsExactly("test1");
        assertThat(routine.callAsync("test1", "test2")).containsExactly("test1", "test2");
        assertThat(routine.callParallel()).isEmpty();
        assertThat(routine.callParallel(Arrays.asList("test1", "test2"))).containsOnly("test1",
                                                                                       "test2");
        assertThat(routine.callParallel(routine.run("test1", "test2"))).containsOnly("test1",
                                                                                     "test2");
        assertThat(routine.callParallel("test1")).containsOnly("test1");
        assertThat(routine.callParallel("test1", "test2")).containsOnly("test1", "test2");

        assertThat(routine.run().readAll()).isEmpty();
        assertThat(routine.run(Arrays.asList("test1", "test2")).readAll()).containsExactly("test1",
                                                                                           "test2");
        assertThat(routine.run(routine.run("test1", "test2")).readAll()).containsExactly("test1",
                                                                                         "test2");
        assertThat(routine.run("test1").readAll()).containsExactly("test1");
        assertThat(routine.run("test1", "test2").readAll()).containsExactly("test1", "test2");
        assertThat(routine.runAsync().readAll()).isEmpty();
        assertThat(routine.runAsync(Arrays.asList("test1", "test2")).readAll()).containsExactly(
                "test1", "test2");
        assertThat(routine.runAsync(routine.run("test1", "test2")).readAll()).containsExactly(
                "test1", "test2");
        assertThat(routine.runAsync("test1").readAll()).containsExactly("test1");
        assertThat(routine.runAsync("test1", "test2").readAll()).containsExactly("test1", "test2");
        assertThat(routine.runParallel().readAll()).isEmpty();
        assertThat(routine.runParallel(Arrays.asList("test1", "test2")).readAll()).containsOnly(
                "test1", "test2");
        assertThat(routine.runParallel(routine.run("test1", "test2")).readAll()).containsOnly(
                "test1", "test2");
        assertThat(routine.runParallel("test1").readAll()).containsOnly("test1");
        assertThat(routine.runParallel("test1", "test2").readAll()).containsOnly("test1", "test2");

        assertThat(routine.invoke().pass().results().readAll()).isEmpty();
        assertThat(routine.invoke()
                          .pass(Arrays.asList("test1", "test2"))
                          .results()
                          .readAll()).containsExactly("test1", "test2");
        assertThat(routine.invoke()
                          .pass(routine.run("test1", "test2"))
                          .results()
                          .readAll()).containsExactly("test1", "test2");
        assertThat(routine.invoke().pass("test1").results().readAll()).containsExactly("test1");
        assertThat(routine.invoke().pass("test1", "test2").results().readAll()).containsExactly(
                "test1", "test2");
        assertThat(routine.invokeAsync().pass().results().readAll()).isEmpty();
        assertThat(routine.invokeAsync()
                          .pass(Arrays.asList("test1", "test2"))
                          .results()
                          .readAll()).containsExactly("test1", "test2");
        assertThat(routine.invokeAsync()
                          .pass(routine.run("test1", "test2"))
                          .results()
                          .readAll()).containsExactly("test1", "test2");
        assertThat(routine.invokeAsync().pass("test1").results().readAll()).containsExactly(
                "test1");
        assertThat(
                routine.invokeAsync().pass("test1", "test2").results().readAll()).containsExactly(
                "test1", "test2");
        assertThat(routine.invokeParallel().pass().results().readAll()).isEmpty();
        assertThat(routine.invokeParallel()
                          .pass(Arrays.asList("test1", "test2"))
                          .results()
                          .readAll()).containsOnly("test1", "test2");
        assertThat(routine.invokeParallel().pass(routine.run("test1", "test2")).results().readAll())
                .containsOnly("test1", "test2");
        assertThat(routine.invokeParallel().pass("test1").results().readAll()).containsOnly(
                "test1");
        assertThat(
                routine.invokeParallel().pass("test1", "test2").results().readAll()).containsOnly(
                "test1", "test2");
    }

    public void testChainedRoutine() {

        final SimpleInvocation<Integer, Integer> execSum =
                new SimpleInvocation<Integer, Integer>() {

                    @Override
                    public void onExec(@Nonnull final List<? extends Integer> integers,
                            @Nonnull final ResultChannel<Integer> results) {

                        int sum = 0;

                        for (final Integer integer : integers) {

                            sum += integer;
                        }

                        results.pass(sum);
                    }
                };

        final Routine<Integer, Integer> sumRoutine =
                on(ClassToken.tokenOf(execSum)).withArgs(this).buildRoutine();

        final BasicInvocation<Integer, Integer> invokeSquare =
                new BasicInvocation<Integer, Integer>() {

                    @Override
                    public void onInput(final Integer integer,
                            @Nonnull final ResultChannel<Integer> results) {

                        final int input = integer;

                        results.pass(input * input);
                    }
                };

        final Routine<Integer, Integer> squareRoutine =
                on(ClassToken.tokenOf(invokeSquare)).withArgs(this).buildRoutine();

        assertThat(sumRoutine.call(squareRoutine.run(1, 2, 3, 4))).containsExactly(30);
        assertThat(sumRoutine.callAsync(squareRoutine.run(1, 2, 3, 4))).containsExactly(30);
        assertThat(sumRoutine.run(squareRoutine.run(1, 2, 3, 4)).readAll()).containsExactly(30);
        assertThat(sumRoutine.runAsync(squareRoutine.run(1, 2, 3, 4)).readAll()).containsExactly(
                30);

        assertThat(sumRoutine.call(squareRoutine.runAsync(1, 2, 3, 4))).containsExactly(30);
        assertThat(sumRoutine.callAsync(squareRoutine.runAsync(1, 2, 3, 4))).containsExactly(30);
        assertThat(sumRoutine.run(squareRoutine.runAsync(1, 2, 3, 4)).readAll()).containsExactly(
                30);
        assertThat(
                sumRoutine.runAsync(squareRoutine.runAsync(1, 2, 3, 4)).readAll()).containsExactly(
                30);

        assertThat(sumRoutine.call(squareRoutine.runParallel(1, 2, 3, 4))).containsExactly(30);
        assertThat(sumRoutine.callAsync(squareRoutine.runParallel(1, 2, 3, 4))).containsExactly(30);
        assertThat(sumRoutine.run(squareRoutine.runParallel(1, 2, 3, 4)).readAll()).containsExactly(
                30);
        assertThat(sumRoutine.runAsync(squareRoutine.runParallel(1, 2, 3, 4))
                             .readAll()).containsExactly(30);
    }

    public void testComposedRoutine() {

        final SimpleInvocation<Integer, Integer> execSum =
                new SimpleInvocation<Integer, Integer>() {

                    @Override
                    public void onExec(@Nonnull final List<? extends Integer> integers,
                            @Nonnull final ResultChannel<Integer> results) {

                        int sum = 0;

                        for (final Integer integer : integers) {

                            sum += integer;
                        }

                        results.pass(sum);
                    }
                };

        final Routine<Integer, Integer> sumRoutine =
                on(ClassToken.tokenOf(execSum)).withArgs(this).buildRoutine();

        final BasicInvocation<Integer, Integer> invokeSquare =
                new BasicInvocation<Integer, Integer>() {

                    @Override
                    public void onInput(final Integer integer,
                            @Nonnull final ResultChannel<Integer> results) {

                        final int input = integer;

                        results.pass(input * input);
                    }
                };

        final Routine<Integer, Integer> squareRoutine =
                on(ClassToken.tokenOf(invokeSquare)).withArgs(this).buildRoutine();

        final BasicInvocation<Integer, Integer> invokeSquareSum =
                new BasicInvocation<Integer, Integer>() {

                    private ParameterChannel<Integer, Integer> mChannel;

                    @Override
                    public void onAbort(final Throwable reason) {

                        mChannel.abort(reason);
                    }

                    @Override
                    public void onInit() {

                        mChannel = sumRoutine.invokeAsync();
                    }

                    @Override
                    public void onInput(final Integer integer,
                            @Nonnull final ResultChannel<Integer> results) {

                        mChannel.pass(squareRoutine.runAsync(integer));
                    }

                    @Override
                    public void onResult(@Nonnull final ResultChannel<Integer> results) {

                        results.pass(mChannel.results());
                    }
                };

        final Routine<Integer, Integer> squareSumRoutine =
                on(ClassToken.tokenOf(invokeSquareSum)).withArgs(this, sumRoutine, squareRoutine)
                                                       .buildRoutine();

        assertThat(squareSumRoutine.call(1, 2, 3, 4)).containsExactly(30);
        assertThat(squareSumRoutine.callAsync(1, 2, 3, 4)).containsExactly(30);
        assertThat(squareSumRoutine.run(1, 2, 3, 4).readAll()).containsExactly(30);
        assertThat(squareSumRoutine.runAsync(1, 2, 3, 4).readAll()).containsExactly(30);
    }

    public void testDelay() {

        final Routine<String, String> routine = JavaRoutine.on(tokenOf(DelayedInvocation.class))
                                                           .withArgs(TimeDuration.millis(10))
                                                           .buildRoutine();

        long startTime = System.currentTimeMillis();

        final ParameterChannel<String, String> channel = routine.invokeAsync();
        channel.after(100, TimeUnit.MILLISECONDS).pass("test1");
        channel.after(TimeDuration.millis(10).nanosTime()).pass("test2");
        channel.after(TimeDuration.millis(10).microsTime()).pass("test3", "test4");
        channel.after(TimeDuration.millis(10)).pass((String[]) null);
        channel.now().pass((List<String>) null).pass((OutputChannel<String>) null);
        assertThat(channel.results().afterMax(3, TimeUnit.SECONDS).readAll()).containsOnly("test1",
                                                                                           "test2",
                                                                                           "test3",
                                                                                           "test4");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(110);

        final Routine<String, String> routine1 = JavaRoutine.on(tokenOf(DelayedInvocation.class))
                                                            .orderedInput()
                                                            .orderedOutput()
                                                            .withArgs(TimeDuration.millis(10))
                                                            .buildRoutine();

        startTime = System.currentTimeMillis();

        final ParameterChannel<String, String> channel1 = routine1.invokeAsync();
        channel1.after(100, TimeUnit.MILLISECONDS).pass("test1");
        channel1.after(TimeDuration.millis(10).nanosTime()).pass("test2");
        channel1.after(TimeDuration.millis(10).microsTime()).pass(Arrays.asList("test3", "test4"));
        channel1.after(TimeDuration.millis(10)).pass((String[]) null);
        channel1.now().pass((List<String>) null).pass((OutputChannel<String>) null);
        assertThat(
                channel1.results().afterMax(TimeDuration.seconds(7000)).readAll()).containsExactly(
                "test1", "test2", "test3", "test4");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(110);

        final Routine<String, String> routine2 =
                JavaRoutine.on(tokenOf(DelayedListInvocation.class))
                           .withArgs(TimeDuration.millis(10), 2)
                           .buildRoutine();

        startTime = System.currentTimeMillis();

        final ParameterChannel<String, String> channel2 = routine2.invokeAsync();
        channel2.after(100, TimeUnit.MILLISECONDS).pass("test1");
        channel2.after(TimeDuration.millis(10).nanosTime()).pass("test2");
        channel2.after(TimeDuration.millis(10).microsTime()).pass("test3", "test4");
        channel2.after(TimeDuration.millis(10)).pass((String[]) null);
        channel2.now().pass((List<String>) null).pass((OutputChannel<String>) null);
        assertThat(channel2.results().afterMax(3, TimeUnit.SECONDS).readAll()).containsOnly("test1",
                                                                                            "test2",
                                                                                            "test3",
                                                                                            "test4");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(110);

        final Routine<String, String> routine3 =
                JavaRoutine.on(tokenOf(DelayedListInvocation.class))
                           .orderedInput()
                           .orderedOutput()
                           .withArgs(TimeDuration.millis(10), 2)
                           .buildRoutine();

        startTime = System.currentTimeMillis();

        final ParameterChannel<String, String> channel3 = routine3.invokeAsync();
        channel3.after(100, TimeUnit.MILLISECONDS).pass("test1");
        channel3.after(TimeDuration.millis(10).nanosTime()).pass("test2");
        channel3.after(TimeDuration.millis(10).microsTime()).pass("test3", "test4");
        channel3.after(TimeDuration.millis(10)).pass((String[]) null);
        channel3.now().pass((List<String>) null).pass((OutputChannel<String>) null);
        assertThat(channel3.results().afterMax(3, TimeUnit.SECONDS).readAll()).containsExactly(
                "test1", "test2", "test3", "test4");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(110);

        final Routine<String, String> routine4 =
                JavaRoutine.on(tokenOf(DelayedListInvocation.class))
                           .withArgs(TimeDuration.ZERO, 2)
                           .buildRoutine();

        startTime = System.currentTimeMillis();

        final ParameterChannel<String, String> channel4 = routine4.invokeAsync();
        channel4.after(100, TimeUnit.MILLISECONDS).pass("test1");
        channel4.after(TimeDuration.millis(10).nanosTime()).pass("test2");
        channel4.after(TimeDuration.millis(10).microsTime()).pass("test3", "test4");
        channel4.after(TimeDuration.millis(10)).pass((String[]) null);
        channel4.now().pass((List<String>) null).pass((OutputChannel<String>) null);
        assertThat(channel4.results().afterMax(3, TimeUnit.SECONDS).readAll()).containsOnly("test1",
                                                                                            "test2",
                                                                                            "test3",
                                                                                            "test4");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(100);

        final Routine<String, String> routine5 =
                JavaRoutine.on(tokenOf(DelayedListInvocation.class))
                           .orderedInput()
                           .orderedOutput()
                           .withArgs(TimeDuration.ZERO, 2)
                           .buildRoutine();

        startTime = System.currentTimeMillis();

        final ParameterChannel<String, String> channel5 = routine5.invokeAsync();
        channel5.after(100, TimeUnit.MILLISECONDS).pass("test1");
        channel5.after(TimeDuration.millis(10).nanosTime()).pass("test2");
        channel5.after(TimeDuration.millis(10).microsTime()).pass("test3", "test4");
        channel5.after(TimeDuration.millis(10)).pass((String[]) null);
        channel5.now().pass((List<String>) null).pass((OutputChannel<String>) null);
        assertThat(channel5.results().afterMax(3, TimeUnit.SECONDS).readAll()).containsExactly(
                "test1", "test2", "test3", "test4");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(100);

        final Routine<String, String> routine6 =
                JavaRoutine.on(tokenOf(DelayedChannelInvocation.class))
                           .withArgs(TimeDuration.millis(10))
                           .buildRoutine();

        startTime = System.currentTimeMillis();

        final ParameterChannel<String, String> channel6 = routine6.invokeAsync();
        channel6.after(100, TimeUnit.MILLISECONDS).pass("test1");
        channel6.after(TimeDuration.millis(10).nanosTime()).pass("test2");
        channel6.after(TimeDuration.millis(10).microsTime()).pass("test3", "test4");
        channel6.after(TimeDuration.millis(10)).pass((String[]) null);
        channel6.now().pass((List<String>) null).pass((OutputChannel<String>) null);
        assertThat(channel6.results().afterMax(3, TimeUnit.SECONDS).readAll()).containsOnly("test1",
                                                                                            "test2",
                                                                                            "test3",
                                                                                            "test4");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(110);

        final Routine<String, String> routine7 =
                JavaRoutine.on(tokenOf(DelayedChannelInvocation.class))
                           .orderedInput()
                           .orderedOutput()
                           .withArgs(TimeDuration.millis(10))
                           .buildRoutine();

        startTime = System.currentTimeMillis();

        final ParameterChannel<String, String> channel7 = routine7.invokeAsync();
        channel7.after(100, TimeUnit.MILLISECONDS).pass("test1");
        channel7.after(TimeDuration.millis(10).nanosTime()).pass("test2");
        channel7.after(TimeDuration.millis(10).microsTime()).pass("test3", "test4");
        channel7.after(TimeDuration.millis(10)).pass((String[]) null);
        channel7.now().pass((List<String>) null).pass((OutputChannel<String>) null);
        assertThat(channel7.results().afterMax(3, TimeUnit.SECONDS).readAll()).containsExactly(
                "test1", "test2", "test3", "test4");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(110);

        final Routine<String, String> routine8 =
                JavaRoutine.on(tokenOf(DelayedChannelInvocation.class))
                           .withArgs(TimeDuration.ZERO)
                           .buildRoutine();

        startTime = System.currentTimeMillis();

        final ParameterChannel<String, String> channel8 = routine8.invokeAsync();
        channel8.after(100, TimeUnit.MILLISECONDS).pass("test1");
        channel8.after(TimeDuration.millis(10).nanosTime()).pass("test2");
        channel8.after(TimeDuration.millis(10).microsTime()).pass("test3", "test4");
        channel8.after(TimeDuration.millis(10)).pass((String[]) null);
        channel8.now().pass((List<String>) null).pass((OutputChannel<String>) null);
        assertThat(channel8.results().afterMax(3, TimeUnit.SECONDS).readAll()).containsOnly("test1",
                                                                                            "test2",
                                                                                            "test3",
                                                                                            "test4");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(100);

        final Routine<String, String> routine9 =
                JavaRoutine.on(tokenOf(DelayedChannelInvocation.class))
                           .orderedInput()
                           .orderedOutput()
                           .withArgs(TimeDuration.ZERO)
                           .buildRoutine();

        startTime = System.currentTimeMillis();

        final ParameterChannel<String, String> channel9 = routine9.invokeAsync();
        channel9.after(100, TimeUnit.MILLISECONDS).pass("test1");
        channel9.after(TimeDuration.millis(10).nanosTime()).pass("test2");
        channel9.after(TimeDuration.millis(10).microsTime()).pass("test3", "test4");
        channel9.after(TimeDuration.millis(10)).pass((String[]) null);
        channel9.now().pass((List<String>) null).pass((OutputChannel<String>) null);
        assertThat(channel9.results().afterMax(3, TimeUnit.SECONDS).readAll()).containsExactly(
                "test1", "test2", "test3", "test4");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(100);
    }

    public void testDelayedAbort() throws InterruptedException {

        final Routine<String, String> passThroughRoutine = JavaRoutine.<String>on().buildRoutine();

        final ParameterChannel<String, String> channel1 = passThroughRoutine.invokeAsync();
        channel1.after(TimeDuration.seconds(2)).abort();
        assertThat(channel1.now().pass("test").results().readFirst()).isEqualTo("test");

        final ParameterChannel<String, String> channel2 = passThroughRoutine.invokeAsync();
        channel2.after(TimeDuration.millis(100)).abort();

        try {

            channel2.after(TimeDuration.millis(200)).pass("test").results().readFirst();

            fail();

        } catch (final RoutineException ignored) {

        }

        final Routine<String, String> abortRoutine =
                JavaRoutine.on(tokenOf(DelayedAbortInvocation.class))
                           .withArgs(TimeDuration.millis(200))
                           .buildRoutine();

        assertThat(abortRoutine.runAsync("test").readFirst()).isEqualTo("test");

        try {

            final OutputChannel<String> channel = abortRoutine.runAsync("test");

            TimeDuration.millis(500).sleepAtLeast();

            channel.readFirst();

            fail();

        } catch (final RoutineException ignored) {

        }
    }

    public void testDelayedBind() {

        final Routine<Object, Object> routine1 = JavaRoutine.on().buildRoutine();
        final Routine<Object, Object> routine2 = JavaRoutine.on().buildRoutine();

        final long startTime = System.currentTimeMillis();

        assertThat(routine1.invokeAsync()
                           .after(TimeDuration.millis(500))
                           .pass(routine2.runAsync("test"))
                           .results()
                           .readFirst()).isEqualTo("test");
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(500);
    }

    @SuppressWarnings("ConstantConditions")
    public void testError() {

        try {

            new ParallelInvocation<Object, Object>(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            on(tokenOf(ConstructorException.class)).logLevel(LogLevel.SILENT).buildRoutine().call();

            fail();

        } catch (final RoutineException ignored) {

        }

        try {

            new AbstractRoutine<Object, Object>(null, Runners.queuedRunner()) {

                @Override
                @Nonnull
                protected Invocation<Object, Object> createInvocation(final boolean async) {

                    return new ConstructorException();
                }
            };

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new AbstractRoutine<Object, Object>(
                    new DefaultConfigurationBuilder().buildConfiguration(), null) {

                @Override
                @Nonnull
                protected Invocation<Object, Object> createInvocation(final boolean async) {

                    return new ConstructorException();
                }
            };

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final RoutineConfigurationBuilder builder = new RoutineConfigurationBuilder();
            final RoutineConfiguration configuration = builder.availableTimeout(seconds(5))
                                                              .maxRunning(Integer.MAX_VALUE)
                                                              .maxRetained(10)
                                                              .maxInputSize(Integer.MAX_VALUE)
                                                              .inputTimeout(ZERO)
                                                              .delayedInput()
                                                              .maxOutputSize(Integer.MAX_VALUE)
                                                              .outputTimeout(ZERO)
                                                              .delayedOutput()
                                                              .loggedWith(Logger.getDefaultLog())
                                                              .logLevel(Logger.getDefaultLogLevel())
                                                              .buildConfiguration();

            new AbstractRoutine<Object, Object>(configuration, Runners.queuedRunner()) {

                @Override
                @Nonnull
                protected Invocation<Object, Object> createInvocation(final boolean async) {

                    return new ConstructorException();
                }
            };

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final RoutineConfigurationBuilder builder = new RoutineConfigurationBuilder();
            final RoutineConfiguration configuration = builder.runBy(Runners.sharedRunner())
                                                              .maxRunning(Integer.MAX_VALUE)
                                                              .maxRetained(10)
                                                              .maxInputSize(Integer.MAX_VALUE)
                                                              .inputTimeout(ZERO)
                                                              .delayedInput()
                                                              .maxOutputSize(Integer.MAX_VALUE)
                                                              .outputTimeout(ZERO)
                                                              .delayedOutput()
                                                              .loggedWith(Logger.getDefaultLog())
                                                              .logLevel(Logger.getDefaultLogLevel())
                                                              .buildConfiguration();

            new AbstractRoutine<Object, Object>(configuration, Runners.queuedRunner()) {

                @Override
                @Nonnull
                protected Invocation<Object, Object> createInvocation(final boolean async) {

                    return new ConstructorException();
                }
            };

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final RoutineConfigurationBuilder builder = new RoutineConfigurationBuilder();
            final RoutineConfiguration configuration = builder.runBy(Runners.sharedRunner())
                                                              .availableTimeout(ZERO)
                                                              .maxRunning(Integer.MAX_VALUE)
                                                              .maxRetained(10)
                                                              .maxInputSize(Integer.MAX_VALUE)
                                                              .delayedInput()
                                                              .maxOutputSize(Integer.MAX_VALUE)
                                                              .outputTimeout(ZERO)
                                                              .delayedOutput()
                                                              .loggedWith(Logger.getDefaultLog())
                                                              .logLevel(Logger.getDefaultLogLevel())
                                                              .buildConfiguration();

            new AbstractRoutine<Object, Object>(configuration, Runners.queuedRunner()) {

                @Override
                @Nonnull
                protected Invocation<Object, Object> createInvocation(final boolean async) {

                    return new ConstructorException();
                }
            };

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final RoutineConfigurationBuilder builder = new RoutineConfigurationBuilder();
            final RoutineConfiguration configuration = builder.runBy(Runners.sharedRunner())
                                                              .availableTimeout(ZERO)
                                                              .maxRunning(Integer.MAX_VALUE)
                                                              .maxRetained(10)
                                                              .maxInputSize(Integer.MAX_VALUE)
                                                              .inputTimeout(ZERO)
                                                              .delayedInput()
                                                              .maxOutputSize(Integer.MAX_VALUE)
                                                              .delayedOutput()
                                                              .loggedWith(Logger.getDefaultLog())
                                                              .logLevel(Logger.getDefaultLogLevel())
                                                              .buildConfiguration();

            new AbstractRoutine<Object, Object>(configuration, Runners.queuedRunner()) {

                @Override
                @Nonnull
                protected Invocation<Object, Object> createInvocation(final boolean async) {

                    return new ConstructorException();
                }
            };

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final RoutineConfigurationBuilder builder = new RoutineConfigurationBuilder();
            final RoutineConfiguration configuration = builder.runBy(Runners.sharedRunner())
                                                              .availableTimeout(ZERO)
                                                              .maxRunning(Integer.MAX_VALUE)
                                                              .maxRetained(10)
                                                              .maxInputSize(Integer.MAX_VALUE)
                                                              .inputTimeout(ZERO)
                                                              .delayedInput()
                                                              .maxOutputSize(Integer.MAX_VALUE)
                                                              .outputTimeout(ZERO)
                                                              .delayedOutput()
                                                              .logLevel(Logger.getDefaultLogLevel())
                                                              .buildConfiguration();

            new AbstractRoutine<Object, Object>(configuration, Runners.queuedRunner()) {

                @Override
                @Nonnull
                protected Invocation<Object, Object> createInvocation(final boolean async) {

                    return new ConstructorException();
                }
            };

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final RoutineConfigurationBuilder builder = new RoutineConfigurationBuilder();
            final RoutineConfiguration configuration = builder.runBy(Runners.sharedRunner())
                                                              .availableTimeout(ZERO)
                                                              .maxRunning(Integer.MAX_VALUE)
                                                              .maxRetained(10)
                                                              .maxInputSize(Integer.MAX_VALUE)
                                                              .inputTimeout(ZERO)
                                                              .delayedInput()
                                                              .maxOutputSize(Integer.MAX_VALUE)
                                                              .outputTimeout(ZERO)
                                                              .delayedOutput()
                                                              .loggedWith(Logger.getDefaultLog())
                                                              .buildConfiguration();

            new AbstractRoutine<Object, Object>(configuration, Runners.queuedRunner()) {

                @Override
                @Nonnull
                protected Invocation<Object, Object> createInvocation(final boolean async) {

                    return new ConstructorException();
                }
            };

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final RoutineConfigurationBuilder builder = new RoutineConfigurationBuilder();
            final RoutineConfiguration configuration = builder.runBy(Runners.sharedRunner())
                                                              .availableTimeout(ZERO)
                                                              .maxRetained(10)
                                                              .maxInputSize(Integer.MAX_VALUE)
                                                              .inputTimeout(ZERO)
                                                              .delayedInput()
                                                              .maxOutputSize(Integer.MAX_VALUE)
                                                              .outputTimeout(ZERO)
                                                              .delayedOutput()
                                                              .loggedWith(Logger.getDefaultLog())
                                                              .logLevel(Logger.getDefaultLogLevel())
                                                              .buildConfiguration();

            new AbstractRoutine<Object, Object>(configuration, Runners.queuedRunner()) {

                @Override
                @Nonnull
                protected Invocation<Object, Object> createInvocation(final boolean async) {

                    return new ConstructorException();
                }
            };

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            final RoutineConfigurationBuilder builder = new RoutineConfigurationBuilder();
            final RoutineConfiguration configuration = builder.runBy(Runners.sharedRunner())
                                                              .availableTimeout(ZERO)
                                                              .maxRunning(Integer.MAX_VALUE)
                                                              .maxInputSize(Integer.MAX_VALUE)
                                                              .inputTimeout(ZERO)
                                                              .delayedInput()
                                                              .maxOutputSize(Integer.MAX_VALUE)
                                                              .outputTimeout(ZERO)
                                                              .delayedOutput()
                                                              .loggedWith(Logger.getDefaultLog())
                                                              .logLevel(Logger.getDefaultLogLevel())
                                                              .buildConfiguration();

            new AbstractRoutine<Object, Object>(configuration, Runners.queuedRunner()) {

                @Override
                @Nonnull
                protected Invocation<Object, Object> createInvocation(final boolean async) {

                    return new ConstructorException();
                }
            };

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            final RoutineConfigurationBuilder builder = new RoutineConfigurationBuilder();
            final RoutineConfiguration configuration = builder.runBy(Runners.sharedRunner())
                                                              .availableTimeout(ZERO)
                                                              .maxRunning(Integer.MAX_VALUE)
                                                              .maxRetained(10)
                                                              .inputTimeout(ZERO)
                                                              .delayedInput()
                                                              .maxOutputSize(Integer.MAX_VALUE)
                                                              .outputTimeout(ZERO)
                                                              .delayedOutput()
                                                              .loggedWith(Logger.getDefaultLog())
                                                              .logLevel(Logger.getDefaultLogLevel())
                                                              .buildConfiguration();

            new AbstractRoutine<Object, Object>(configuration, Runners.queuedRunner()) {

                @Override
                @Nonnull
                protected Invocation<Object, Object> createInvocation(final boolean async) {

                    return new ConstructorException();
                }
            };

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            final RoutineConfigurationBuilder builder = new RoutineConfigurationBuilder();
            final RoutineConfiguration configuration = builder.runBy(Runners.sharedRunner())
                                                              .availableTimeout(ZERO)
                                                              .maxRunning(Integer.MAX_VALUE)
                                                              .maxRetained(10)
                                                              .maxInputSize(Integer.MAX_VALUE)
                                                              .inputTimeout(ZERO)
                                                              .delayedInput()
                                                              .outputTimeout(ZERO)
                                                              .delayedOutput()
                                                              .loggedWith(Logger.getDefaultLog())
                                                              .logLevel(Logger.getDefaultLogLevel())
                                                              .buildConfiguration();

            new AbstractRoutine<Object, Object>(configuration, Runners.queuedRunner()) {

                @Override
                @Nonnull
                protected Invocation<Object, Object> createInvocation(final boolean async) {

                    return new ConstructorException();
                }
            };

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        final Logger logger =
                Logger.create(Logger.getDefaultLog(), Logger.getDefaultLogLevel(), this);

        try {

            final DefaultResultChannel<Object> channel = new DefaultResultChannel<Object>(
                    new DefaultConfigurationBuilder().buildConfiguration(), new TestAbortHandler(),
                    Runners.sequentialRunner(), logger);

            new DefaultExecution<Object, Object>(null, new TestInputIterator(), channel, logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final DefaultResultChannel<Object> channel = new DefaultResultChannel<Object>(
                    new DefaultConfigurationBuilder().buildConfiguration(), new TestAbortHandler(),
                    Runners.sequentialRunner(), logger);

            new DefaultExecution<Object, Object>(new TestInvocationManager(), null, channel,
                                                 logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultExecution<Object, Object>(new TestInvocationManager(),
                                                 new TestInputIterator(), null, logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final DefaultResultChannel<Object> channel = new DefaultResultChannel<Object>(
                    new DefaultConfigurationBuilder().buildConfiguration(), new TestAbortHandler(),
                    Runners.sequentialRunner(), logger);

            new DefaultExecution<Object, Object>(new TestInvocationManager(),
                                                 new TestInputIterator(), channel, null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testErrorConsumerOnResult() {

        final BasicOutputConsumer<String> exceptionConsumer = new BasicOutputConsumer<String>() {

            @Override
            public void onOutput(final String output) {

                throw new NullPointerException(output);
            }
        };

        testConsumer(exceptionConsumer);
    }

    public void testErrorConsumerOnReturn() {

        final BasicOutputConsumer<String> exceptionConsumer = new BasicOutputConsumer<String>() {

            @Override
            public void onComplete() {

                throw new NullPointerException("test2");
            }
        };

        testConsumer(exceptionConsumer);
    }

    public void testErrorOnInit() {

        final BasicInvocation<String, String> exceptionOnInit =
                new BasicInvocation<String, String>() {

                    @Override
                    public void onInit() {

                        throw new NullPointerException("test1");
                    }
                };

        final Routine<String, String> exceptionRoutine =
                on(ClassToken.tokenOf(exceptionOnInit)).withArgs(this).buildRoutine();

        testException(exceptionRoutine, "test", "test1");

        final Routine<String, String> passThroughRoutine = JavaRoutine.<String>on().buildRoutine();

        testChained(passThroughRoutine, exceptionRoutine, "test", "test1");
        testChained(exceptionRoutine, passThroughRoutine, "test", "test1");
    }

    public void testErrorOnInput() {

        final BasicInvocation<String, String> exceptionOnInput =
                new BasicInvocation<String, String>() {

                    @Override
                    public void onInput(final String s,
                            @Nonnull final ResultChannel<String> results) {

                        throw new NullPointerException(s);
                    }
                };

        final Routine<String, String> exceptionRoutine =
                on(ClassToken.tokenOf(exceptionOnInput)).withArgs(this).buildRoutine();

        testException(exceptionRoutine, "test2", "test2");

        final Routine<String, String> passThroughRoutine = JavaRoutine.<String>on().buildRoutine();

        testChained(passThroughRoutine, exceptionRoutine, "test2", "test2");
        testChained(exceptionRoutine, passThroughRoutine, "test2", "test2");
    }

    public void testErrorOnResult() {

        final BasicInvocation<String, String> exceptionOnResult =
                new BasicInvocation<String, String>() {

                    @Override
                    public void onResult(@Nonnull final ResultChannel<String> results) {

                        throw new NullPointerException("test3");
                    }
                };

        final Routine<String, String> exceptionRoutine =
                on(ClassToken.tokenOf(exceptionOnResult)).withArgs(this).buildRoutine();

        testException(exceptionRoutine, "test", "test3");

        final Routine<String, String> passThroughRoutine = JavaRoutine.<String>on().buildRoutine();

        testChained(passThroughRoutine, exceptionRoutine, "test", "test3");
        testChained(exceptionRoutine, passThroughRoutine, "test", "test3");
    }

    public void testErrorOnReturn() {

        final Invocation<String, String> exceptionOnReturn = new BasicInvocation<String, String>() {

            @Override
            public void onInput(final String s, @Nonnull final ResultChannel<String> results) {

                results.pass(s);
            }

            @Override
            public void onReturn() {

                throw new NullPointerException("test4");
            }
        };

        final Routine<String, String> exceptionRoutine =
                on(ClassToken.tokenOf(exceptionOnReturn)).withArgs(this).buildRoutine();

        testException(exceptionRoutine, "test", "test4");

        final Routine<String, String> passThroughRoutine = JavaRoutine.<String>on().buildRoutine();

        testChained(passThroughRoutine, exceptionRoutine, "test", "test4");
        testChained(exceptionRoutine, passThroughRoutine, "test", "test4");
    }

    public void testMethod() throws NoSuchMethodException {

        assertThat(on(new TestClass()).method(TestClass.class.getMethod("getOne"))
                                      .call()).containsExactly(1);
        assertThat(on(new TestClass()).method("getOne").call()).containsExactly(1);
        assertThat(on(new TestClass()).asyncMethod(TestClass.GET).call()).containsExactly(1);
        assertThat(on(TestClass.class).asyncMethod(TestClass.GET).call(3)).containsExactly(3);
        assertThat(on(TestClass.class).asyncMethod("get").callAsync(-3)).containsExactly(-3);
        assertThat(on(TestClass.class).method("get", int.class).callParallel(17)).containsExactly(
                17);

        assertThat(on(new TestClass()).as(TestInterface.class).getInt(2)).isEqualTo(2);

        try {

            on(TestClass.class).asyncMethod("get").callAsync();

            fail();

        } catch (final RoutineException ignored) {

        }

        try {

            on(TestClass.class).asyncMethod("take");

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        assertThat(on(new TestClass()).as(TestInterfaceAsync.class).take(77)).isEqualTo(77);
        assertThat(on(new TestClass()).as(TestInterfaceAsync.class).getOne().readFirst()).isEqualTo(
                1);

        final TestInterfaceAsync testInterfaceAsync =
                on(new TestClass()).as(TestInterfaceAsync.class);
        assertThat(testInterfaceAsync.getInt(testInterfaceAsync.getOne())).isEqualTo(1);
    }

    @SuppressWarnings("ConstantConditions")
    public void testParameterChannelError() {

        final Logger logger = Logger.create(new NullLog(), LogLevel.DEBUG);

        try {

            final RoutineConfiguration configuration =
                    new DefaultConfigurationBuilder().buildConfiguration();

            new DefaultParameterChannel<Object, Object>(configuration, null, Runners.sharedRunner(),
                                                        logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final RoutineConfiguration configuration =
                    new DefaultConfigurationBuilder().buildConfiguration();

            new DefaultParameterChannel<Object, Object>(configuration, new TestInvocationManager(),
                                                        null, logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final RoutineConfiguration configuration =
                    new DefaultConfigurationBuilder().buildConfiguration();

            new DefaultParameterChannel<Object, Object>(configuration, new TestInvocationManager(),
                                                        Runners.sharedRunner(), null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultParameterChannel<Object, Object>(null, new TestInvocationManager(),
                                                        Runners.sharedRunner(), logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final RoutineConfigurationBuilder builder = new RoutineConfigurationBuilder();
            final RoutineConfiguration configuration = builder.maxInputSize(Integer.MAX_VALUE)
                                                              .delayedInput()
                                                              .maxOutputSize(Integer.MAX_VALUE)
                                                              .outputTimeout(ZERO)
                                                              .delayedOutput()
                                                              .buildConfiguration();

            new DefaultParameterChannel<Object, Object>(configuration, new TestInvocationManager(),
                                                        Runners.sharedRunner(), logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final RoutineConfigurationBuilder builder = new RoutineConfigurationBuilder();
            final RoutineConfiguration configuration = builder.maxInputSize(Integer.MAX_VALUE)
                                                              .inputTimeout(ZERO)
                                                              .delayedInput()
                                                              .maxOutputSize(Integer.MAX_VALUE)
                                                              .delayedOutput()
                                                              .buildConfiguration();

            new DefaultParameterChannel<Object, Object>(configuration, new TestInvocationManager(),
                                                        Runners.sharedRunner(), logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final RoutineConfigurationBuilder builder = new RoutineConfigurationBuilder();
            final RoutineConfiguration configuration = builder.inputTimeout(ZERO)
                                                              .delayedInput()
                                                              .maxOutputSize(Integer.MAX_VALUE)
                                                              .outputTimeout(ZERO)
                                                              .delayedOutput()
                                                              .buildConfiguration();

            new DefaultParameterChannel<Object, Object>(configuration, new TestInvocationManager(),
                                                        Runners.sharedRunner(), logger);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            final RoutineConfigurationBuilder builder = new RoutineConfigurationBuilder();
            final RoutineConfiguration configuration = builder.maxInputSize(Integer.MAX_VALUE)
                                                              .inputTimeout(ZERO)
                                                              .delayedInput()
                                                              .outputTimeout(ZERO)
                                                              .delayedOutput()
                                                              .buildConfiguration();

            new DefaultParameterChannel<Object, Object>(configuration, new TestInvocationManager(),
                                                        Runners.sharedRunner(), logger);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            final RoutineConfiguration configuration =
                    new DefaultConfigurationBuilder().buildConfiguration();

            final DefaultParameterChannel<Object, Object> channel =
                    new DefaultParameterChannel<Object, Object>(configuration,
                                                                new TestInvocationManager(),
                                                                Runners.sharedRunner(), logger);

            channel.results();
            channel.pass("test");

            fail();

        } catch (final IllegalStateException ignored) {

        }

        try {

            final RoutineConfiguration configuration =
                    new DefaultConfigurationBuilder().buildConfiguration();

            final DefaultParameterChannel<Object, Object> channel =
                    new DefaultParameterChannel<Object, Object>(configuration,
                                                                new TestInvocationManager(),
                                                                Runners.sharedRunner(), logger);

            channel.after(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final RoutineConfiguration configuration =
                    new DefaultConfigurationBuilder().buildConfiguration();

            final DefaultParameterChannel<Object, Object> channel =
                    new DefaultParameterChannel<Object, Object>(configuration,
                                                                new TestInvocationManager(),
                                                                Runners.sharedRunner(), logger);

            channel.after(1, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final RoutineConfiguration configuration =
                    new DefaultConfigurationBuilder().buildConfiguration();

            final DefaultParameterChannel<Object, Object> channel =
                    new DefaultParameterChannel<Object, Object>(configuration,
                                                                new TestInvocationManager(),
                                                                Runners.sharedRunner(), logger);

            channel.after(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    public void testPartialOut() {

        final BasicInvocation<String, String> invocation = new BasicInvocation<String, String>() {

            @Override
            public void onInput(final String s, @Nonnull final ResultChannel<String> results) {

                results.now().pass(s).after(TimeDuration.seconds(2)).abort();
            }
        };

        final Routine<String, String> routine =
                JavaRoutine.on(tokenOf(invocation)).withArgs(this).buildRoutine();
        assertThat(routine.runAsync("test")
                          .afterMax(TimeDuration.millis(500))
                          .readAll()).containsExactly("test");
    }

    @SuppressWarnings("ConstantConditions")
    public void testResultChannelError() {

        final Logger logger = Logger.create(new NullLog(), LogLevel.DEBUG);

        try {

            final RoutineConfiguration configuration =
                    new DefaultConfigurationBuilder().buildConfiguration();

            new DefaultResultChannel<Object>(configuration, null, Runners.sharedRunner(), logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final RoutineConfiguration configuration =
                    new DefaultConfigurationBuilder().buildConfiguration();

            new DefaultResultChannel<Object>(configuration, new TestAbortHandler(), null, logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final RoutineConfiguration configuration =
                    new DefaultConfigurationBuilder().buildConfiguration();

            new DefaultResultChannel<Object>(configuration, new TestAbortHandler(),
                                             Runners.sharedRunner(), null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final RoutineConfiguration configuration =
                    new DefaultConfigurationBuilder().buildConfiguration();

            new DefaultResultChannel<Object>(configuration, new TestAbortHandler(),
                                             Runners.sharedRunner(), logger).after(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final RoutineConfiguration configuration =
                    new DefaultConfigurationBuilder().buildConfiguration();

            new DefaultResultChannel<Object>(configuration, new TestAbortHandler(),
                                             Runners.sharedRunner(), logger).after(0, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final RoutineConfiguration configuration =
                    new DefaultConfigurationBuilder().buildConfiguration();

            final DefaultResultChannel<Object> channel =
                    new DefaultResultChannel<Object>(configuration, new TestAbortHandler(),
                                                     Runners.sharedRunner(), logger);

            channel.after(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            final RoutineConfigurationBuilder builder = new RoutineConfigurationBuilder();
            final RoutineConfiguration configuration =
                    builder.maxOutputSize(Integer.MAX_VALUE).delayedOutput().buildConfiguration();

            new DefaultResultChannel<Object>(configuration, new TestAbortHandler(),
                                             Runners.sharedRunner(), logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            final RoutineConfigurationBuilder builder = new RoutineConfigurationBuilder();
            final RoutineConfiguration configuration =
                    builder.outputTimeout(ZERO).delayedOutput().buildConfiguration();

            new DefaultResultChannel<Object>(configuration, new TestAbortHandler(),
                                             Runners.sharedRunner(), logger);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        final Routine<String, String> routine = JavaRoutine.on(tokenOf(DelayedInvocation.class))
                                                           .logLevel(LogLevel.SILENT)
                                                           .withArgs(TimeDuration.ZERO)
                                                           .buildRoutine();
        final OutputChannel<String> channel = routine.run();

        try {

            channel.afterMax(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            channel.afterMax(0, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            channel.afterMax(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            channel.bind(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            channel.readAllInto(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        final BasicOutputConsumer<String> consumer = new BasicOutputConsumer<String>() {};

        try {

            channel.bind(consumer).bind(consumer);

            fail();

        } catch (final IllegalStateException ignored) {

        }

        try {

            channel.iterator();

            fail();

        } catch (final IllegalStateException ignored) {

        }

        final Routine<String, String> routine1 = JavaRoutine.on(tokenOf(DelayedInvocation.class))
                                                            .logLevel(LogLevel.SILENT)
                                                            .withArgs(TimeDuration.ZERO)
                                                            .buildRoutine();
        final Iterator<String> iterator =
                routine1.run("test").afterMax(TimeDuration.millis(10)).iterator();

        assertThat(iterator.next()).isEqualTo("test");
        iterator.remove();

        try {

            iterator.remove();

            fail();

        } catch (final IllegalStateException ignored) {

        }

        try {

            iterator.next();

            fail();

        } catch (final NoSuchElementException ignored) {

        }

        try {

            routine1.run().immediately().iterator().next();

            fail();

        } catch (final NoSuchElementException ignored) {

        }
    }

    public void testRoutine() {

        final BasicInvocation<Integer, Integer> execSquare =
                new BasicInvocation<Integer, Integer>() {

                    @Override
                    public void onInput(final Integer integer,
                            @Nonnull final ResultChannel<Integer> results) {

                        final int input = integer;

                        results.pass(input * input);
                    }
                };

        final Routine<Integer, Integer> squareRoutine =
                on(ClassToken.tokenOf(execSquare)).withArgs(this).buildRoutine();

        assertThat(squareRoutine.call(1, 2, 3, 4)).containsExactly(1, 4, 9, 16);
        assertThat(squareRoutine.callAsync(1, 2, 3, 4)).containsExactly(1, 4, 9, 16);
        assertThat(squareRoutine.callParallel(1, 2, 3, 4)).containsOnly(1, 4, 9, 16);
        assertThat(squareRoutine.run(1, 2, 3, 4).readAll()).containsExactly(1, 4, 9, 16);
        assertThat(squareRoutine.runAsync(1, 2, 3, 4).readAll()).containsExactly(1, 4, 9, 16);
        assertThat(squareRoutine.runParallel(1, 2, 3, 4).readAll()).containsOnly(1, 4, 9, 16);
    }

    public void testRoutineFunction() {

        final SimpleInvocation<Integer, Integer> execSum =
                new SimpleInvocation<Integer, Integer>() {

                    @Override
                    public void onExec(@Nonnull final List<? extends Integer> integers,
                            @Nonnull final ResultChannel<Integer> results) {

                        int sum = 0;

                        for (final Integer integer : integers) {

                            sum += integer;
                        }

                        results.pass(sum);
                    }
                };

        final Routine<Integer, Integer> sumRoutine =
                on(ClassToken.tokenOf(execSum)).withArgs(this).buildRoutine();

        assertThat(sumRoutine.call(1, 2, 3, 4)).containsExactly(10);
        assertThat(sumRoutine.callAsync(1, 2, 3, 4)).containsExactly(10);
        assertThat(sumRoutine.run(1, 2, 3, 4).readAll()).containsExactly(10);
        assertThat(sumRoutine.runAsync(1, 2, 3, 4).readAll()).containsExactly(10);
    }

    public void testTimeout() {

        final Routine<String, String> routine =
                on(tokenOf(DelayedInvocation.class)).withArgs(TimeDuration.seconds(3))
                                                    .buildRoutine();

        final OutputChannel<String> channel = routine.runAsync("test");
        assertThat(channel.immediately().readAll()).isEmpty();

        try {

            channel.afterMax(TimeDuration.millis(10))
                   .eventuallyThrow(new IllegalStateException())
                   .readFirst();

            fail();

        } catch (final IllegalStateException ignored) {

        }

        try {

            channel.readAll();

            fail();

        } catch (final IllegalStateException ignored) {

        }

        try {

            channel.iterator().hasNext();

            fail();

        } catch (final IllegalStateException ignored) {

        }

        try {

            channel.iterator().next();

            fail();

        } catch (final IllegalStateException ignored) {

        }

        try {

            channel.isComplete();

            fail();

        } catch (final IllegalStateException ignored) {

        }
    }

    private void testChained(final Routine<String, String> before,
            final Routine<String, String> after, final String input, final String expected) {

        try {

            before.call(after.run(input));

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.callAsync(after.run(input));

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.callParallel(after.run(input));

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.run(after.run(input)).readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.run(after.run(input))) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.runAsync(after.run(input)).readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.runAsync(after.run(input))) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.runParallel(after.run(input)).readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.runParallel(after.run(input))) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.invoke().pass(after.run(input)).results().readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.invoke().pass(after.run(input)).results()) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.invokeAsync().pass(after.run(input)).results().readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.invokeAsync().pass(after.run(input)).results()) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.invokeParallel().pass(after.run(input)).results().readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.invokeParallel().pass(after.run(input)).results()) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.call(after.runAsync(input));

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.callAsync(after.runAsync(input));

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.callParallel(after.runAsync(input));

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.run(after.runAsync(input)).readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.run(after.runAsync(input))) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.runAsync(after.runAsync(input)).readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.runAsync(after.runAsync(input))) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.invoke().pass(after.runAsync(input)).results().readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.invoke().pass(after.runAsync(input)).results()) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.invokeAsync().pass(after.runAsync(input)).results().readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.invokeAsync().pass(after.runAsync(input)).results()) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.call(after.runParallel(input));

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.callAsync(after.runParallel(input));

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.callParallel(after.runParallel(input));

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.run(after.runParallel(input)).readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.run(after.runParallel(input))) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.runAsync(after.runParallel(input)).readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.runAsync(after.runParallel(input))) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.invoke().pass(after.runParallel(input)).results().readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.invoke().pass(after.runParallel(input)).results()) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.invokeAsync().pass(after.runParallel(input)).results().readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.invokeAsync().pass(after.runParallel(input)).results()) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }
    }

    private void testConsumer(final OutputConsumer<String> consumer) {

        final String input = "test";
        final Routine<String, String> routine =
                on(tokenOf(DelayedInvocation.class)).withArgs(TimeDuration.ZERO).buildRoutine();

        assertThat(routine.run(input).bind(consumer).isComplete()).isTrue();
        assertThat(routine.runAsync(input).bind(consumer).isComplete()).isTrue();
        assertThat(routine.runParallel(input).bind(consumer).isComplete()).isTrue();
        assertThat(routine.invoke().pass(input).results().bind(consumer).isComplete()).isTrue();
        assertThat(
                routine.invokeAsync().pass(input).results().bind(consumer).isComplete()).isTrue();
        assertThat(routine.invokeParallel()
                          .pass(input)
                          .results()
                          .bind(consumer)
                          .isComplete()).isTrue();
    }

    private void testException(final Routine<String, String> routine, final String input,
            final String expected) {

        try {

            routine.call(input);

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            routine.callAsync(input);

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            routine.callParallel(input);

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            routine.run(input).readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : routine.run(input)) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            routine.runAsync(input).readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : routine.runAsync(input)) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            routine.runParallel(input).readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : routine.runParallel(input)) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            routine.invoke().pass(input).results().readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : routine.invoke().pass(input).results()) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            routine.invokeAsync().pass(input).results().readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : routine.invokeAsync().pass(input).results()) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            routine.invokeParallel().pass(input).results().readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : routine.invokeParallel().pass(input).results()) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }
    }

    private interface TestInterface {

        public int getInt(int i);
    }

    private interface TestInterfaceAsync {

        @AsyncOverride({int.class})
        public int getInt(OutputChannel<Integer> i);

        @AsyncOverride(result = true)
        public OutputChannel<Integer> getOne();

        @Async(value = "getInt")
        public int take(int i);
    }

    private static class ConstructorException extends BasicInvocation<Object, Object> {

        public ConstructorException() {

            throw new IllegalStateException();
        }
    }

    private static class DelayedAbortInvocation extends BasicInvocation<String, String> {

        private final TimeDuration mDelay;

        public DelayedAbortInvocation(final TimeDuration delay) {

            mDelay = delay;
        }

        @Override
        public void onInput(final String s, @Nonnull final ResultChannel<String> results) {

            results.now().pass(s).after(mDelay).abort();
        }
    }

    private static class DelayedChannelInvocation extends BasicInvocation<String, String> {

        private final TimeDuration mDelay;

        private final Routine<String, String> mRoutine;

        private boolean mFlag;

        public DelayedChannelInvocation(final TimeDuration delay) {

            mDelay = delay;
            mRoutine =
                    on(tokenOf(DelayedInvocation.class)).withArgs(TimeDuration.ZERO).buildRoutine();
        }

        @Override
        public void onInput(final String s, @Nonnull final ResultChannel<String> results) {

            if (mFlag) {

                results.after(mDelay).pass((OutputChannel<String>) null);

            } else {

                results.after(mDelay.time, mDelay.unit).pass((OutputChannel<String>) null);
            }

            results.pass(mRoutine.runAsync(s));

            mFlag = !mFlag;
        }
    }

    private static class DelayedInvocation extends BasicInvocation<String, String> {

        private final TimeDuration mDelay;

        private boolean mFlag;

        public DelayedInvocation(final TimeDuration delay) {

            mDelay = delay;
        }

        @Override
        public void onInput(final String s, @Nonnull final ResultChannel<String> results) {

            if (mFlag) {

                results.after(mDelay);

            } else {

                results.after(mDelay.time, mDelay.unit);
            }

            results.pass(s);

            mFlag = !mFlag;
        }
    }

    private static class DelayedListInvocation extends BasicInvocation<String, String> {

        private final int mCount;

        private final TimeDuration mDelay;

        private final ArrayList<String> mList;

        private boolean mFlag;

        public DelayedListInvocation(final TimeDuration delay, final int listCount) {

            mDelay = delay;
            mCount = listCount;
            mList = new ArrayList<String>(listCount);
        }

        @Override
        public void onInput(final String s, @Nonnull final ResultChannel<String> results) {

            final ArrayList<String> list = mList;
            list.add(s);

            if (list.size() >= mCount) {

                if (mFlag) {

                    results.after(mDelay).pass((String[]) null).pass(list);

                } else {

                    results.after(mDelay.time, mDelay.unit)
                           .pass((List<String>) null)
                           .pass(list.toArray(new String[list.size()]));
                }

                results.now();
                list.clear();

                mFlag = !mFlag;
            }
        }

        @Override
        public void onResult(@Nonnull final ResultChannel<String> results) {

            final ArrayList<String> list = mList;
            results.after(mDelay).pass(list);
            list.clear();
        }
    }

    private static class TestAbortHandler implements AbortHandler {

        @Override
        public void onAbort(@Nullable final Throwable reason, final long delay,
                @Nonnull final TimeUnit timeUnit) {

        }
    }

    private static class TestClass implements TestInterface {

        public static final String GET = "get";

        @Async(value = GET)
        public static int get(final int i) {

            return i;
        }

        @Override
        public int getInt(final int i) {

            return i;
        }

        @Async(value = GET)
        public int getOne() {

            return 1;
        }
    }

    private static class TestInputIterator implements InputIterator<Object> {

        @Override
        @Nullable
        public Throwable getAbortException() {

            return null;
        }

        @Override
        public boolean hasInput() {

            return false;
        }

        @Override
        public boolean isAborting() {

            return false;
        }

        @Override
        public boolean isComplete() {

            return false;
        }

        @Override
        public Object nextInput() {

            return null;
        }

        @Override
        public void onAbortComplete() {

        }

        @Override
        public void onConsumeInput() {

        }
    }

    private static class TestInvocationManager implements InvocationManager<Object, Object> {

        @Override
        @Nonnull
        public Invocation<Object, Object> create() {

            return new BasicInvocation<Object, Object>() {};
        }

        @Override
        public void discard(@Nonnull final Invocation<Object, Object> invocation) {

        }

        @Override
        public void recycle(@Nonnull final Invocation<Object, Object> invocation) {

        }
    }
}
