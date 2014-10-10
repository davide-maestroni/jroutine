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

import com.bmd.jrt.channel.OutputChannel;
import com.bmd.jrt.channel.OutputConsumer;
import com.bmd.jrt.channel.OutputConsumerAdapter;
import com.bmd.jrt.channel.ParameterChannel;
import com.bmd.jrt.channel.ResultChannel;
import com.bmd.jrt.common.ClassToken;
import com.bmd.jrt.common.RoutineException;
import com.bmd.jrt.execution.Execution;
import com.bmd.jrt.execution.ExecutionAdapter;
import com.bmd.jrt.execution.ExecutionBody;
import com.bmd.jrt.log.Log.LogLevel;
import com.bmd.jrt.log.Logger;
import com.bmd.jrt.routine.DefaultInvocation.InputIterator;
import com.bmd.jrt.routine.DefaultParameterChannel.ExecutionProvider;
import com.bmd.jrt.routine.DefaultResultChannel.AbortHandler;
import com.bmd.jrt.runner.Runners;
import com.bmd.jrt.time.TimeDuration;

import junit.framework.TestCase;

import java.util.List;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

import static com.bmd.jrt.routine.JRoutine.on;
import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Routine unit tests.
 * <p/>
 * Created by davide on 9/9/14.
 */
public class RoutineTest extends TestCase {

    public void testAbort() {

        final Routine<String, String> routine =
                on(ClassToken.tokenOf(DelayExecution.class)).withArgs(TimeDuration.millis(100))
                                                            .buildRoutine();

        final ParameterChannel<String, String> inputChannel = routine.invokeAsyn().pass("test1");
        final OutputChannel<String> outputChannel = inputChannel.results();

        assertThat(inputChannel.abort(new IllegalArgumentException("test1"))).isFalse();
        assertThat(outputChannel.readFirst()).isEqualTo("test1");

        final OutputChannel<String> channel = routine.runAsyn("test2");
        assertThat(channel.abort(new IllegalArgumentException("test2"))).isTrue();

        try {

            channel.readFirst();

            fail();

        } catch (final RoutineException ex) {

            assertThat(ex.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
            assertThat(ex.getCause().getMessage()).isEqualTo("test2");
        }

        final Execution<String, String> abortExecution = new ExecutionAdapter<String, String>() {

            @Override
            public void onInput(@Nullable final String s,
                    @NonNull final ResultChannel<String> results) {

                assertThat(results.abort(new IllegalArgumentException(s))).isTrue();
            }
        };

        final Routine<String, String> routine2 =
                on(ClassToken.classOf(abortExecution)).withArgs(this).buildRoutine();

        try {

            routine2.callAsyn("test_abort");

            fail();

        } catch (final RoutineException ex) {

            assertThat(ex.getCause()).isExactlyInstanceOf(IllegalArgumentException.class);
            assertThat(ex.getCause().getMessage()).isEqualTo("test_abort");
        }
    }

    public void testChainedRoutine() {

        final ExecutionBody<Integer, Integer> execSum = new ExecutionBody<Integer, Integer>() {

            @Override
            public void onExec(@NonNull final List<? extends Integer> integers,
                    @NonNull final ResultChannel<Integer> results) {

                int sum = 0;

                for (final Integer integer : integers) {

                    sum += integer;
                }

                results.pass(sum);
            }
        };

        final Routine<Integer, Integer> sumRoutine =
                on(ClassToken.classOf(execSum)).withArgs(this).buildRoutine();

        final ExecutionAdapter<Integer, Integer> invokeSquare =
                new ExecutionAdapter<Integer, Integer>() {

                    @Override
                    public void onInput(final Integer integer,
                            @NonNull final ResultChannel<Integer> results) {

                        final int input = integer;

                        results.pass(input * input);
                    }
                };

        final Routine<Integer, Integer> squareRoutine =
                on(ClassToken.classOf(invokeSquare)).withArgs(this).buildRoutine();

        assertThat(sumRoutine.call(squareRoutine.run(1, 2, 3, 4))).containsExactly(30);
        assertThat(sumRoutine.callAsyn(squareRoutine.run(1, 2, 3, 4))).containsExactly(30);
        assertThat(sumRoutine.run(squareRoutine.run(1, 2, 3, 4)).readAll()).containsExactly(30);
        assertThat(sumRoutine.runAsyn(squareRoutine.run(1, 2, 3, 4)).readAll()).containsExactly(30);

        assertThat(sumRoutine.call(squareRoutine.runAsyn(1, 2, 3, 4))).containsExactly(30);
        assertThat(sumRoutine.callAsyn(squareRoutine.runAsyn(1, 2, 3, 4))).containsExactly(30);
        assertThat(sumRoutine.run(squareRoutine.runAsyn(1, 2, 3, 4)).readAll()).containsExactly(30);
        assertThat(sumRoutine.runAsyn(squareRoutine.runAsyn(1, 2, 3, 4)).readAll()).containsExactly(
                30);

        assertThat(sumRoutine.call(squareRoutine.runParall(1, 2, 3, 4))).containsExactly(30);
        assertThat(sumRoutine.callAsyn(squareRoutine.runParall(1, 2, 3, 4))).containsExactly(30);
        assertThat(sumRoutine.run(squareRoutine.runParall(1, 2, 3, 4)).readAll()).containsExactly(
                30);
        assertThat(
                sumRoutine.runAsyn(squareRoutine.runParall(1, 2, 3, 4)).readAll()).containsExactly(
                30);
    }

    public void testComposedRoutine() {

        final ExecutionBody<Integer, Integer> execSum = new ExecutionBody<Integer, Integer>() {

            @Override
            public void onExec(@NonNull final List<? extends Integer> integers,
                    @NonNull final ResultChannel<Integer> results) {

                int sum = 0;

                for (final Integer integer : integers) {

                    sum += integer;
                }

                results.pass(sum);
            }
        };

        final Routine<Integer, Integer> sumRoutine =
                on(ClassToken.classOf(execSum)).withArgs(this).buildRoutine();

        final ExecutionAdapter<Integer, Integer> invokeSquare =
                new ExecutionAdapter<Integer, Integer>() {

                    @Override
                    public void onInput(final Integer integer,
                            @NonNull final ResultChannel<Integer> results) {

                        final int input = integer;

                        results.pass(input * input);
                    }
                };

        final Routine<Integer, Integer> squareRoutine =
                on(ClassToken.classOf(invokeSquare)).withArgs(this).buildRoutine();

        final ExecutionAdapter<Integer, Integer> invokeSquareSum =
                new ExecutionAdapter<Integer, Integer>() {

                    private ParameterChannel<Integer, Integer> mChannel;

                    @Override
                    public void onAbort(final Throwable throwable) {

                        mChannel.abort(throwable);
                    }

                    @Override
                    public void onInit() {

                        mChannel = sumRoutine.invokeAsyn();
                    }

                    @Override
                    public void onInput(final Integer integer,
                            @NonNull final ResultChannel<Integer> results) {

                        mChannel.pass(squareRoutine.runAsyn(integer));
                    }

                    @Override
                    public void onResult(@NonNull final ResultChannel<Integer> results) {

                        results.pass(mChannel.results());
                    }
                };

        final Routine<Integer, Integer> squareSumRoutine =
                on(ClassToken.classOf(invokeSquareSum)).withArgs(this, sumRoutine, squareRoutine)
                                                       .buildRoutine();

        assertThat(squareSumRoutine.call(1, 2, 3, 4)).containsExactly(30);
        assertThat(squareSumRoutine.callAsyn(1, 2, 3, 4)).containsExactly(30);
        assertThat(squareSumRoutine.run(1, 2, 3, 4).readAll()).containsExactly(30);
        assertThat(squareSumRoutine.runAsyn(1, 2, 3, 4).readAll()).containsExactly(30);
    }

    @SuppressWarnings("ConstantConditions")
    public void testError() {

        try {

            new ParallelExecution<Object, Object>(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            on(ClassToken.tokenOf(ConstructorException.class)).logLevel(LogLevel.SILENT)
                                                              .buildRoutine()
                                                              .call();

            fail();

        } catch (final RoutineException ignored) {

        }

        try {

            new AbstractRoutine<Object, Object>(null, Runners.shared(), 1, 1, TimeDuration.ZERO,
                                                false, false, Logger.getDefaultLog(),
                                                Logger.getDefaultLogLevel()) {

                @Override
                @NonNull
                protected Execution<Object, Object> createExecution(final boolean async) {

                    return new ConstructorException();
                }
            };

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new AbstractRoutine<Object, Object>(Runners.queued(), null, 1, 1, TimeDuration.ZERO,
                                                false, false, Logger.getDefaultLog(),
                                                Logger.getDefaultLogLevel()) {

                @Override
                @NonNull
                protected Execution<Object, Object> createExecution(final boolean async) {

                    return new ConstructorException();
                }
            };

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new AbstractRoutine<Object, Object>(Runners.queued(), Runners.shared(), 1, 1, null,
                                                false, false, Logger.getDefaultLog(),
                                                Logger.getDefaultLogLevel()) {

                @Override
                @NonNull
                protected Execution<Object, Object> createExecution(final boolean async) {

                    return new ConstructorException();
                }
            };

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new AbstractRoutine<Object, Object>(Runners.queued(), Runners.shared(), 1, 0,
                                                TimeDuration.ZERO, false, false, null, null) {

                @Override
                @NonNull
                protected Execution<Object, Object> createExecution(final boolean async) {

                    return new ConstructorException();
                }
            };

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new AbstractRoutine<Object, Object>(Runners.queued(), Runners.shared(), 0, 1,
                                                TimeDuration.ZERO, false, false,
                                                Logger.getDefaultLog(),
                                                Logger.getDefaultLogLevel()) {

                @Override
                @NonNull
                protected Execution<Object, Object> createExecution(final boolean async) {

                    return new ConstructorException();
                }
            };

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        try {

            new AbstractRoutine<Object, Object>(Runners.queued(), Runners.shared(), 1, -1,
                                                TimeDuration.ZERO, false, false,
                                                Logger.getDefaultLog(),
                                                Logger.getDefaultLogLevel()) {

                @Override
                @NonNull
                protected Execution<Object, Object> createExecution(final boolean async) {

                    return new ConstructorException();
                }
            };

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        final Logger logger =
                Logger.create(Logger.getDefaultLog(), Logger.getDefaultLogLevel(), this);

        try {

            new DefaultInvocation<Object, Object>(null, new TestInputIterator(),
                                                  new DefaultResultChannel<Object>(
                                                          new TestAbortHandler(),
                                                          Runners.sequential(), false, logger),
                                                  logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultInvocation<Object, Object>(new TestExecutionProvider(), null,
                                                  new DefaultResultChannel<Object>(
                                                          new TestAbortHandler(),
                                                          Runners.sequential(), false, logger),
                                                  logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultInvocation<Object, Object>(new TestExecutionProvider(),
                                                  new TestInputIterator(), null, logger);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new DefaultInvocation<Object, Object>(new TestExecutionProvider(),
                                                  new TestInputIterator(),
                                                  new DefaultResultChannel<Object>(
                                                          new TestAbortHandler(),
                                                          Runners.sequential(), false, logger),
                                                  null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testErrorConsumerOnResult() {

        final OutputConsumerAdapter<String> exceptionConsumer =
                new OutputConsumerAdapter<String>() {

                    @Override
                    public void onOutput(final String output) {

                        throw new NullPointerException(output);
                    }
                };

        testConsumer(exceptionConsumer);
    }

    public void testErrorConsumerOnReturn() {

        final OutputConsumerAdapter<String> exceptionConsumer =
                new OutputConsumerAdapter<String>() {

                    @Override
                    public void onClose() {

                        throw new NullPointerException("test2");
                    }
                };

        testConsumer(exceptionConsumer);
    }

    public void testErrorOnInit() {

        final Execution<String, String> exceptionOnInit = new ExecutionAdapter<String, String>() {

            @Override
            public void onInit() {

                throw new NullPointerException("test1");
            }
        };

        final Routine<String, String> exceptionRoutine =
                on(ClassToken.classOf(exceptionOnInit)).withArgs(this).buildRoutine();

        testException(exceptionRoutine, "test", "test1");

        final Routine<String, String> passThroughRoutine =
                on(ClassToken.tokenOf(PassThroughExecution.class)).buildRoutine();

        testChained(passThroughRoutine, exceptionRoutine, "test", "test1");
        testChained(exceptionRoutine, passThroughRoutine, "test", "test1");
    }

    public void testErrorOnInput() {

        final Execution<String, String> exceptionOnInput = new ExecutionAdapter<String, String>() {

            @Override
            public void onInput(final String s, @NonNull final ResultChannel<String> results) {

                throw new NullPointerException(s);
            }
        };

        final Routine<String, String> exceptionRoutine =
                on(ClassToken.classOf(exceptionOnInput)).withArgs(this).buildRoutine();

        testException(exceptionRoutine, "test2", "test2");

        final Routine<String, String> passThroughRoutine =
                on(ClassToken.tokenOf(PassThroughExecution.class)).buildRoutine();

        testChained(passThroughRoutine, exceptionRoutine, "test2", "test2");
        testChained(exceptionRoutine, passThroughRoutine, "test2", "test2");
    }

    public void testErrorOnResult() {

        final Execution<String, String> exceptionOnResult = new ExecutionAdapter<String, String>() {

            @Override
            public void onResult(@NonNull final ResultChannel<String> results) {

                throw new NullPointerException("test3");
            }
        };

        final Routine<String, String> exceptionRoutine =
                on(ClassToken.classOf(exceptionOnResult)).withArgs(this).buildRoutine();

        testException(exceptionRoutine, "test", "test3");

        final Routine<String, String> passThroughRoutine =
                on(ClassToken.tokenOf(PassThroughExecution.class)).buildRoutine();

        testChained(passThroughRoutine, exceptionRoutine, "test", "test3");
        testChained(exceptionRoutine, passThroughRoutine, "test", "test3");
    }

    public void testErrorOnReturn() {

        final Execution<String, String> exceptionOnReturn = new ExecutionAdapter<String, String>() {

            @Override
            public void onInput(final String s, @NonNull final ResultChannel<String> results) {

                results.pass(s);
            }

            @Override
            public void onReturn() {

                throw new NullPointerException("test4");
            }
        };

        final Routine<String, String> exceptionRoutine =
                on(ClassToken.classOf(exceptionOnReturn)).withArgs(this).buildRoutine();

        testException(exceptionRoutine, "test", "test4");

        final Routine<String, String> passThroughRoutine =
                on(ClassToken.tokenOf(PassThroughExecution.class)).buildRoutine();

        testChained(passThroughRoutine, exceptionRoutine, "test", "test4");
        testChained(exceptionRoutine, passThroughRoutine, "test", "test4");
    }

    public void testMethod() throws NoSuchMethodException {

        assertThat(on(new TestClass()).classMethod(TestClass.class.getMethod("getOne"))
                                      .call()).containsExactly(1);
        assertThat(on(new TestClass()).classMethod("getOne").call()).containsExactly(1);
        assertThat(on(new TestClass()).method(TestClass.GET_METHOD).call()).containsExactly(1);
        assertThat(on(TestClass.class).method(TestClass.GET_METHOD).call(3)).containsExactly(3);
        assertThat(on(TestClass.class).method("get").callAsyn(-3)).containsExactly(-3);
        assertThat(
                on(TestClass.class).classMethod("get", int.class).callParall(17)).containsExactly(
                17);

        assertThat(on(new TestClass()).asAsyn(TestInterface.class).getInt(2)).isEqualTo(2);

        try {

            on(TestClass.class).method("get").callAsyn();

            fail();

        } catch (final RoutineException ignored) {

        }

        try {

            on(TestClass.class).method("take");

            fail();

        } catch (final IllegalArgumentException ignored) {

        }

        assertThat(on(new TestClass()).asAsyn(TestInterfaceAsyn.class).take(77)).isEqualTo(77);
        assertThat(
                on(new TestClass()).asAsyn(TestInterfaceAsyn.class).getOne().readFirst()).isEqualTo(
                1);

        final TestInterfaceAsyn testInterfaceAsyn =
                on(new TestClass()).asAsyn(TestInterfaceAsyn.class);
        assertThat(testInterfaceAsyn.getInt(testInterfaceAsyn.getOne())).isEqualTo(1);
    }

    public void testRoutine() {

        final ExecutionAdapter<Integer, Integer> execSquare =
                new ExecutionAdapter<Integer, Integer>() {

                    @Override
                    public void onInput(final Integer integer,
                            @NonNull final ResultChannel<Integer> results) {

                        final int input = integer;

                        results.pass(input * input);
                    }
                };

        final Routine<Integer, Integer> squareRoutine =
                on(ClassToken.classOf(execSquare)).withArgs(this).buildRoutine();

        assertThat(squareRoutine.call(1, 2, 3, 4)).containsExactly(1, 4, 9, 16);
        assertThat(squareRoutine.callAsyn(1, 2, 3, 4)).containsExactly(1, 4, 9, 16);
        assertThat(squareRoutine.callParall(1, 2, 3, 4)).containsOnly(1, 4, 9, 16);
        assertThat(squareRoutine.run(1, 2, 3, 4).readAll()).containsExactly(1, 4, 9, 16);
        assertThat(squareRoutine.runAsyn(1, 2, 3, 4).readAll()).containsExactly(1, 4, 9, 16);
        assertThat(squareRoutine.runParall(1, 2, 3, 4).readAll()).containsOnly(1, 4, 9, 16);
    }

    public void testRoutineFunction() {

        final ExecutionBody<Integer, Integer> execSum = new ExecutionBody<Integer, Integer>() {

            @Override
            public void onExec(@NonNull final List<? extends Integer> integers,
                    @NonNull final ResultChannel<Integer> results) {

                int sum = 0;

                for (final Integer integer : integers) {

                    sum += integer;
                }

                results.pass(sum);
            }
        };

        final Routine<Integer, Integer> sumRoutine =
                on(ClassToken.classOf(execSum)).withArgs(this).buildRoutine();

        assertThat(sumRoutine.call(1, 2, 3, 4)).containsExactly(10);
        assertThat(sumRoutine.callAsyn(1, 2, 3, 4)).containsExactly(10);
        assertThat(sumRoutine.run(1, 2, 3, 4).readAll()).containsExactly(10);
        assertThat(sumRoutine.runAsyn(1, 2, 3, 4).readAll()).containsExactly(10);
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

            before.callAsyn(after.run(input));

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.callParall(after.run(input));

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

            before.runAsyn(after.run(input)).readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.runAsyn(after.run(input))) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.runParall(after.run(input)).readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.runParall(after.run(input))) {

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

            before.invokeAsyn().pass(after.run(input)).results().readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.invokeAsyn().pass(after.run(input)).results()) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.invokeParall().pass(after.run(input)).results().readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.invokeParall().pass(after.run(input)).results()) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.call(after.runAsyn(input));

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.callAsyn(after.runAsyn(input));

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.callParall(after.runAsyn(input));

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.run(after.runAsyn(input)).readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.run(after.runAsyn(input))) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.runAsyn(after.runAsyn(input)).readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.runAsyn(after.runAsyn(input))) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.invoke().pass(after.runAsyn(input)).results().readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.invoke().pass(after.runAsyn(input)).results()) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.invokeAsyn().pass(after.runAsyn(input)).results().readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.invokeAsyn().pass(after.runAsyn(input)).results()) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.call(after.runParall(input));

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.callAsyn(after.runParall(input));

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.callParall(after.runParall(input));

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.run(after.runParall(input)).readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.run(after.runParall(input))) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.runAsyn(after.runParall(input)).readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.runAsyn(after.runParall(input))) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.invoke().pass(after.runParall(input)).results().readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.invoke().pass(after.runParall(input)).results()) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.invokeAsyn().pass(after.runParall(input)).results().readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : before.invokeAsyn().pass(after.runParall(input)).results()) {

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
                on(ClassToken.tokenOf(DelayExecution.class)).withArgs(TimeDuration.millis(0))
                                                            .buildRoutine();

        assertThat(routine.run(input).bind(consumer).waitComplete()).isTrue();
        assertThat(routine.runAsyn(input).bind(consumer).waitComplete()).isTrue();
        assertThat(routine.runParall(input).bind(consumer).waitComplete()).isTrue();
        assertThat(routine.invoke().pass(input).results().bind(consumer).waitComplete()).isTrue();
        assertThat(
                routine.invokeAsyn().pass(input).results().bind(consumer).waitComplete()).isTrue();
        assertThat(routine.invokeParall()
                          .pass(input)
                          .results()
                          .bind(consumer)
                          .waitComplete()).isTrue();
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

            routine.callAsyn(input);

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            routine.callParall(input);

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

            routine.runAsyn(input).readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : routine.runAsyn(input)) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            routine.runParall(input).readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : routine.runParall(input)) {

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

            routine.invokeAsyn().pass(input).results().readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : routine.invokeAsyn().pass(input).results()) {

                assertThat(s).isNotEmpty();
            }

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            routine.invokeParall().pass(input).results().readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            for (final String s : routine.invokeParall().pass(input).results()) {

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

    private interface TestInterfaceAsyn {

        @AsyncParameters({int.class})
        public int getInt(OutputChannel<Integer> i);

        @AsyncResult
        public OutputChannel<Integer> getOne();

        @Async(name = "getInt")
        public int take(int i);
    }

    private static class ConstructorException extends ExecutionAdapter<Object, Object> {

        public ConstructorException() {

            throw new IllegalStateException();
        }
    }

    private static class DelayExecution extends ExecutionAdapter<String, String> {

        private final TimeDuration mDelay;

        public DelayExecution(final TimeDuration delay) {

            mDelay = delay;
        }

        @Override
        public void onInput(final String s, @NonNull final ResultChannel<String> results) {

            results.after(mDelay).pass(s);
        }
    }

    private static class PassThroughExecution extends ExecutionAdapter<String, String> {

        @Override
        public void onInput(final String s, @NonNull final ResultChannel<String> results) {

            results.pass(s);
        }
    }

    private static class TestAbortHandler implements AbortHandler {

        @Override
        public void onAbort(@Nullable final Throwable throwable) {

        }
    }

    private static class TestClass implements TestInterface {

        public static final String GET_METHOD = "get";

        @Async(name = GET_METHOD)
        public static int get(final int i) {

            return i;
        }

        @Override
        public int getInt(final int i) {

            return i;
        }

        @Async(name = GET_METHOD)
        public int getOne() {

            return 1;
        }
    }

    @SuppressWarnings("ConstantConditions")
    private static class TestExecutionProvider implements ExecutionProvider<Object, Object> {

        @NonNull
        @Override
        public Execution<Object, Object> create() {

            return null;
        }

        @Override
        public void discard(@NonNull final Execution<Object, Object> execution) {

        }

        @Override
        public void recycle(@NonNull final Execution<Object, Object> execution) {

        }
    }

    private static class TestInputIterator implements InputIterator<Object> {

        @Nullable
        @Override
        public Throwable getAbortException() {

            return null;
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
        public void onAbortComplete() {

        }

        @Override
        public boolean onConsumeInput() {

            return false;
        }

        @Override
        public boolean hasNext() {

            return false;
        }

        @Override
        public Object next() {

            return null;
        }

        @Override
        public void remove() {

        }
    }
}