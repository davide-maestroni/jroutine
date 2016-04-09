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

package com.github.dm.jrt;

import com.github.dm.jrt.AutoProxyRoutineBuilder.BuilderType;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.channel.IOChannel;
import com.github.dm.jrt.core.channel.InvocationChannel;
import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.config.InvocationConfiguration.TimeoutActionType;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.invocation.CallInvocation;
import com.github.dm.jrt.core.invocation.CommandInvocation;
import com.github.dm.jrt.core.invocation.FilterInvocation;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.PassingInvocation;
import com.github.dm.jrt.core.log.Log.Level;
import com.github.dm.jrt.core.log.NullLog;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.runner.Runners;
import com.github.dm.jrt.core.util.TimeDuration;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.OutputConsumerBuilder;
import com.github.dm.jrt.function.Predicate;
import com.github.dm.jrt.function.Supplier;
import com.github.dm.jrt.object.annotation.Alias;
import com.github.dm.jrt.object.annotation.AsyncOut;
import com.github.dm.jrt.object.annotation.ReadTimeout;
import com.github.dm.jrt.proxy.annotation.Proxy;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.List;

import static com.github.dm.jrt.core.invocation.InvocationFactory.factoryOf;
import static com.github.dm.jrt.core.util.ClassToken.tokenOf;
import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.core.util.TimeDuration.millis;
import static com.github.dm.jrt.core.util.TimeDuration.seconds;
import static com.github.dm.jrt.function.Functions.functionFilter;
import static com.github.dm.jrt.function.Functions.wrap;
import static com.github.dm.jrt.object.InvocationTarget.classOfType;
import static com.github.dm.jrt.object.InvocationTarget.instance;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * JRoutine unit tests.
 * <p>
 * Created by davide-maestroni on 02/29/2016.
 */
public class JRoutineTest {

    @Test
    public void testAliasMethod() throws NoSuchMethodException {

        final TimeDuration timeout = seconds(1);
        final TestClass test = new TestClass();
        final Routine<Object, Object> routine = JRoutine.on(instance(test))
                                                        .getInvocationConfiguration()
                                                        .withRunner(Runners.poolRunner())
                                                        .withMaxInstances(1)
                                                        .withCoreInstances(1)
                                                        .withReadTimeoutAction(
                                                                TimeoutActionType.EXIT)
                                                        .withLogLevel(Level.DEBUG)
                                                        .withLog(new NullLog())
                                                        .setConfiguration()
                                                        .method(TestClass.GET);
        assertThat(routine.syncCall().afterMax(timeout).all()).containsExactly(-77L);
    }

    @Test
    public void testChainedRoutine() {

        final TimeDuration timeout = seconds(1);
        final CallInvocation<Integer, Integer> execSum = new CallInvocation<Integer, Integer>() {

            @Override
            protected void onCall(@NotNull final List<? extends Integer> integers,
                    @NotNull final ResultChannel<Integer> result) {

                int sum = 0;
                for (final Integer integer : integers) {
                    sum += integer;
                }

                result.pass(sum);
            }
        };

        final Routine<Integer, Integer> sumRoutine =
                JRoutine.on(factoryOf(execSum, this)).buildRoutine();
        final Routine<Integer, Integer> squareRoutine =
                JRoutine.on(functionFilter(new Function<Integer, Integer>() {

                    public Integer apply(final Integer integer) {

                        final int i = integer;
                        return i * i;
                    }
                })).buildRoutine();

        assertThat(sumRoutine.syncCall(squareRoutine.syncCall(1, 2, 3, 4))
                             .afterMax(timeout)
                             .all()).containsExactly(30);
        assertThat(sumRoutine.asyncCall(squareRoutine.syncCall(1, 2, 3, 4)).afterMax(timeout).all())
                .containsExactly(30);
        assertThat(sumRoutine.asyncCall(squareRoutine.asyncCall(1, 2, 3, 4))
                             .afterMax(timeout)
                             .all()).containsExactly(30);
        assertThat(sumRoutine.asyncCall(squareRoutine.parallelCall(1, 2, 3, 4))
                             .afterMax(timeout)
                             .all()).containsExactly(30);
    }

    @Test
    public void testClassStaticMethod() {

        final TestStatic testStatic = JRoutine.on(classOfType(TestClass.class))
                                              .getInvocationConfiguration()
                                              .withRunner(Runners.poolRunner())
                                              .withLogLevel(Level.DEBUG)
                                              .withLog(new NullLog())
                                              .setConfiguration()
                                              .buildProxy(TestStatic.class);
        try {
            assertThat(testStatic.getOne().all()).containsExactly(1);
            fail();

        } catch (final InvocationException ignored) {

        }

        assertThat(testStatic.getTwo().all()).containsExactly(2);
    }

    @Test
    public void testCommandInvocation() {

        final Routine<Void, String> routine = JRoutine.on(new GetString()).buildRoutine();
        assertThat(routine.asyncCall().afterMax(seconds(1)).all()).containsOnly("test");
    }

    @Test
    public void testConsumerCommand() {

        final Routine<Void, String> routine =
                JRoutine.command(new Consumer<ResultChannel<String>>() {

                    public void accept(final ResultChannel<String> result) {

                        result.pass("test", "1");
                    }
                }).buildRoutine();
        assertThat(routine.asyncCall().afterMax(seconds(1)).all()).containsOnly("test", "1");
    }

    @Test
    public void testConsumerFilter() {

        final Routine<Object, String> routine =
                JRoutine.filter(new BiConsumer<Object, ResultChannel<String>>() {

                    public void accept(final Object o, final ResultChannel<String> result) {

                        result.pass(o.toString());
                    }
                }).buildRoutine();
        assertThat(routine.asyncCall("test", 1).afterMax(seconds(1)).all()).containsOnly("test",
                "1");
    }

    @Test
    public void testConsumerFunction() {

        final Routine<String, String> routine =
                JRoutine.call(new BiConsumer<List<String>, ResultChannel<String>>() {

                    public void accept(final List<String> strings,
                            final ResultChannel<String> result) {

                        final StringBuilder builder = new StringBuilder();
                        for (final String string : strings) {
                            builder.append(string);
                        }

                        result.pass(builder.toString());
                    }
                }).buildRoutine();
        assertThat(routine.asyncCall("test", "1").afterMax(seconds(1)).all()).containsOnly("test1");
    }

    @Test
    public void testFilterInvocation() {

        final Routine<String, String> routine = JRoutine.on(new ToCase()).buildRoutine();
        assertThat(routine.asyncCall("TEST").afterMax(seconds(1)).all()).containsOnly("test");
    }

    @Test
    public void testFunctionFilter() {

        final Routine<Object, String> routine = JRoutine.filter(new Function<Object, String>() {

            public String apply(final Object o) {

                return o.toString();
            }
        }).buildRoutine();
        assertThat(routine.asyncCall("test", 1).afterMax(seconds(1)).all()).containsOnly("test",
                "1");
    }

    @Test
    public void testFunctionFunction() {

        final Routine<String, String> routine = JRoutine.call(new Function<List<String>, String>() {

            public String apply(final List<String> strings) {

                final StringBuilder builder = new StringBuilder();
                for (final String string : strings) {
                    builder.append(string);
                }

                return builder.toString();
            }
        }).buildRoutine();
        assertThat(routine.asyncCall("test", "1").afterMax(seconds(1)).all()).containsOnly("test1");
    }

    @Test
    public void testInvocation() {

        final Routine<String, String> routine =
                JRoutine.on((Invocation<String, String>) new ToCase()).buildRoutine();
        assertThat(routine.asyncCall("TEST").afterMax(seconds(1)).all()).containsOnly("test");
    }

    @Test
    public void testInvocationAndArgs() {

        final Routine<String, String> routine = JRoutine.on(new ToCase(), true).buildRoutine();
        assertThat(routine.asyncCall("test").afterMax(seconds(1)).all()).containsOnly("TEST");
    }

    @Test
    public void testInvocationClass() {

        final Routine<String, String> routine = JRoutine.on(ToCase.class).buildRoutine();
        assertThat(routine.asyncCall("TEST").afterMax(seconds(1)).all()).containsOnly("test");
    }

    @Test
    public void testInvocationClassAndArgs() {

        final Routine<String, String> routine = JRoutine.on(ToCase.class, true).buildRoutine();
        assertThat(routine.asyncCall("test").afterMax(seconds(1)).all()).containsOnly("TEST");
    }

    @Test
    public void testInvocationFactory() {

        final Routine<String, String> routine =
                JRoutine.on((InvocationFactory<String, String>) new ToCase()).buildRoutine();
        assertThat(routine.asyncCall("TEST").afterMax(seconds(1)).all()).containsOnly("test");
    }

    @Test
    public void testInvocationToken() {

        final Routine<String, String> routine = JRoutine.on(tokenOf(ToCase.class)).buildRoutine();
        assertThat(routine.asyncCall("TEST").afterMax(seconds(1)).all()).containsOnly("test");
    }

    @Test
    public void testInvocationTokenAndArgs() {

        final Routine<String, String> routine =
                JRoutine.on(tokenOf(ToCase.class), true).buildRoutine();
        assertThat(routine.asyncCall("test").afterMax(seconds(1)).all()).containsOnly("TEST");
    }

    @Test
    public void testObjectStaticMethod() {

        final TestClass test = new TestClass();
        final TestStatic testStatic = JRoutine.on(instance(test))
                                              .withType(BuilderType.OBJECT)
                                              .getInvocationConfiguration()
                                              .withRunner(Runners.poolRunner())
                                              .withLogLevel(Level.DEBUG)
                                              .withLog(new NullLog())
                                              .setConfiguration()
                                              .buildProxy(TestStatic.class);
        assertThat(testStatic.getOne().all()).containsExactly(1);
        assertThat(testStatic.getTwo().all()).containsExactly(2);
    }

    @Test
    public void testObjectWrapAlias() {

        final TestClass test = new TestClass();
        final Routine<Object, Object> routine = JRoutine.on(test)
                                                        .getInvocationConfiguration()
                                                        .withRunner(Runners.poolRunner())
                                                        .withLogLevel(Level.DEBUG)
                                                        .withLog(new NullLog())
                                                        .setConfiguration()
                                                        .method(TestClass.GET);
        assertThat(routine.syncCall().all()).containsExactly(-77L);
    }

    @Test
    public void testObjectWrapGeneratedProxy() {

        final TestClass test = new TestClass();
        final TestStatic proxy = JRoutine.on(test)
                                         .withType(BuilderType.PROXY)
                                         .getInvocationConfiguration()
                                         .withRunner(Runners.poolRunner())
                                         .withLogLevel(Level.DEBUG)
                                         .withLog(new NullLog())
                                         .setConfiguration()
                                         .buildProxy(TestStatic.class);
        assertThat(proxy.getOne().all()).containsExactly(1);
    }

    @Test
    public void testObjectWrapGeneratedProxyToken() {

        final TestClass test = new TestClass();
        final TestStatic proxy = JRoutine.on(test)
                                         .getInvocationConfiguration()
                                         .withRunner(Runners.poolRunner())
                                         .withLogLevel(Level.DEBUG)
                                         .withLog(new NullLog())
                                         .setConfiguration()
                                         .buildProxy(tokenOf(TestStatic.class));
        assertThat(proxy.getOne().all()).containsExactly(1);
    }

    @Test
    public void testObjectWrapMethod() throws NoSuchMethodException {

        final TestClass test = new TestClass();
        final Routine<Object, Object> routine = JRoutine.on(test)
                                                        .getProxyConfiguration()
                                                        .withSharedFields()
                                                        .setConfiguration()
                                                        .method(TestClass.class.getMethod(
                                                                "getLong"));
        assertThat(routine.syncCall().all()).containsExactly(-77L);
    }

    @Test
    public void testObjectWrapMethodName() {

        final TestClass test = new TestClass();
        final Routine<Object, Object> routine = JRoutine.on(test)
                                                        .getInvocationConfiguration()
                                                        .withRunner(Runners.poolRunner())
                                                        .withLogLevel(Level.DEBUG)
                                                        .withLog(new NullLog())
                                                        .setConfiguration()
                                                        .method("getLong");
        assertThat(routine.syncCall().all()).containsExactly(-77L);
    }

    @Test
    public void testObjectWrapProxy() {

        final TestClass test = new TestClass();
        final TestItf proxy = JRoutine.on(test)
                                      .getInvocationConfiguration()
                                      .withRunner(Runners.poolRunner())
                                      .withLogLevel(Level.DEBUG)
                                      .withLog(new NullLog())
                                      .setConfiguration()
                                      .buildProxy(TestItf.class);
        assertThat(proxy.getOne().all()).containsExactly(1);
    }

    @Test
    public void testObjectWrapProxyToken() {

        final TestClass test = new TestClass();
        final TestItf proxy = JRoutine.on(test)
                                      .getInvocationConfiguration()
                                      .withRunner(Runners.poolRunner())
                                      .withLogLevel(Level.DEBUG)
                                      .withLog(new NullLog())
                                      .setConfiguration()
                                      .buildProxy(tokenOf(TestItf.class));
        assertThat(proxy.getOne().all()).containsExactly(1);
    }

    @Test
    public void testOnComplete() {

        final TestConsumer<Void> consumer1 = new TestConsumer<Void>();
        final TestConsumer<Void> consumer2 = new TestConsumer<Void>();
        final TestConsumer<Void> consumer3 = new TestConsumer<Void>();
        OutputConsumerBuilder<Object> outputConsumer = JRoutine.onComplete(consumer1);
        outputConsumer.onOutput("test");
        assertThat(consumer1.isCalled()).isFalse();
        outputConsumer.onError(new RoutineException());
        assertThat(consumer1.isCalled()).isFalse();
        outputConsumer.onComplete();
        assertThat(consumer1.isCalled()).isTrue();
        consumer1.reset();
        outputConsumer = outputConsumer.thenComplete(consumer2);
        outputConsumer.onOutput("test");
        assertThat(consumer1.isCalled()).isFalse();
        assertThat(consumer2.isCalled()).isFalse();
        outputConsumer.onError(new RoutineException());
        assertThat(consumer1.isCalled()).isFalse();
        assertThat(consumer2.isCalled()).isFalse();
        outputConsumer.onComplete();
        assertThat(consumer1.isCalled()).isTrue();
        assertThat(consumer2.isCalled()).isTrue();
        consumer1.reset();
        consumer2.reset();
        outputConsumer =
                JRoutine.onComplete(consumer1).thenComplete(wrap(consumer2).andThen(consumer3));
        outputConsumer.onOutput("test");
        assertThat(consumer1.isCalled()).isFalse();
        assertThat(consumer2.isCalled()).isFalse();
        assertThat(consumer3.isCalled()).isFalse();
        outputConsumer.onError(new RoutineException());
        assertThat(consumer1.isCalled()).isFalse();
        assertThat(consumer2.isCalled()).isFalse();
        assertThat(consumer3.isCalled()).isFalse();
        outputConsumer.onComplete();
        assertThat(consumer1.isCalled()).isTrue();
        assertThat(consumer2.isCalled()).isTrue();
        assertThat(consumer3.isCalled()).isTrue();
        consumer1.reset();
        final TestConsumer<Object> outConsumer = new TestConsumer<Object>();
        final TestConsumer<RoutineException> errorConsumer = new TestConsumer<RoutineException>();
        outputConsumer =
                JRoutine.onComplete(consumer1).thenOutput(outConsumer).thenError(errorConsumer);
        outputConsumer.onOutput("test");
        assertThat(consumer1.isCalled()).isFalse();
        assertThat(outConsumer.isCalled()).isTrue();
        assertThat(errorConsumer.isCalled()).isFalse();
        outputConsumer.onError(new RoutineException());
        assertThat(consumer1.isCalled()).isFalse();
        assertThat(errorConsumer.isCalled()).isTrue();
        outputConsumer.onComplete();
        assertThat(consumer1.isCalled()).isTrue();
    }

    @Test
    public void testOnError() {

        final TestConsumer<RoutineException> consumer1 = new TestConsumer<RoutineException>();
        final TestConsumer<RoutineException> consumer2 = new TestConsumer<RoutineException>();
        final TestConsumer<RoutineException> consumer3 = new TestConsumer<RoutineException>();
        OutputConsumerBuilder<Object> outputConsumer = JRoutine.onError(consumer1);
        outputConsumer.onOutput("test");
        assertThat(consumer1.isCalled()).isFalse();
        outputConsumer.onComplete();
        assertThat(consumer1.isCalled()).isFalse();
        outputConsumer.onError(new RoutineException());
        assertThat(consumer1.isCalled()).isTrue();
        consumer1.reset();
        outputConsumer = outputConsumer.thenError(consumer2);
        outputConsumer.onOutput("test");
        assertThat(consumer1.isCalled()).isFalse();
        assertThat(consumer2.isCalled()).isFalse();
        outputConsumer.onComplete();
        assertThat(consumer1.isCalled()).isFalse();
        assertThat(consumer2.isCalled()).isFalse();
        outputConsumer.onError(new RoutineException());
        assertThat(consumer1.isCalled()).isTrue();
        assertThat(consumer2.isCalled()).isTrue();
        consumer1.reset();
        consumer2.reset();
        outputConsumer = JRoutine.onError(consumer1).thenError(wrap(consumer2).andThen(consumer3));
        outputConsumer.onOutput("test");
        assertThat(consumer1.isCalled()).isFalse();
        assertThat(consumer2.isCalled()).isFalse();
        assertThat(consumer3.isCalled()).isFalse();
        outputConsumer.onComplete();
        assertThat(consumer1.isCalled()).isFalse();
        assertThat(consumer2.isCalled()).isFalse();
        assertThat(consumer3.isCalled()).isFalse();
        outputConsumer.onError(new RoutineException());
        assertThat(consumer1.isCalled()).isTrue();
        assertThat(consumer2.isCalled()).isTrue();
        assertThat(consumer3.isCalled()).isTrue();
        consumer1.reset();
        final TestConsumer<Object> outConsumer = new TestConsumer<Object>();
        final TestConsumer<Void> completeConsumer = new TestConsumer<Void>();
        outputConsumer =
                JRoutine.onError(consumer1).thenOutput(outConsumer).thenComplete(completeConsumer);
        outputConsumer.onOutput("test");
        assertThat(consumer1.isCalled()).isFalse();
        assertThat(outConsumer.isCalled()).isTrue();
        assertThat(completeConsumer.isCalled()).isFalse();
        outputConsumer.onComplete();
        assertThat(consumer1.isCalled()).isFalse();
        assertThat(completeConsumer.isCalled()).isTrue();
        outputConsumer.onError(new RoutineException());
        assertThat(consumer1.isCalled()).isTrue();
    }

    @Test
    public void testOnOutput() {

        final TestConsumer<Object> consumer1 = new TestConsumer<Object>();
        final TestConsumer<Object> consumer2 = new TestConsumer<Object>();
        final TestConsumer<Object> consumer3 = new TestConsumer<Object>();
        OutputConsumerBuilder<Object> outputConsumer = JRoutine.onOutput(consumer1);
        outputConsumer.onError(new RoutineException());
        assertThat(consumer1.isCalled()).isFalse();
        outputConsumer.onComplete();
        assertThat(consumer1.isCalled()).isFalse();
        outputConsumer.onOutput("test");
        assertThat(consumer1.isCalled()).isTrue();
        consumer1.reset();
        outputConsumer = outputConsumer.thenOutput(consumer2);
        outputConsumer.onError(new RoutineException());
        assertThat(consumer1.isCalled()).isFalse();
        assertThat(consumer2.isCalled()).isFalse();
        outputConsumer.onComplete();
        assertThat(consumer1.isCalled()).isFalse();
        assertThat(consumer2.isCalled()).isFalse();
        outputConsumer.onOutput("test");
        assertThat(consumer1.isCalled()).isTrue();
        assertThat(consumer2.isCalled()).isTrue();
        consumer1.reset();
        consumer2.reset();
        outputConsumer =
                JRoutine.onOutput(consumer1).thenOutput(wrap(consumer2).andThen(consumer3));
        outputConsumer.onError(new RoutineException());
        assertThat(consumer1.isCalled()).isFalse();
        assertThat(consumer2.isCalled()).isFalse();
        assertThat(consumer3.isCalled()).isFalse();
        outputConsumer.onComplete();
        assertThat(consumer1.isCalled()).isFalse();
        assertThat(consumer2.isCalled()).isFalse();
        assertThat(consumer3.isCalled()).isFalse();
        outputConsumer.onOutput("test");
        assertThat(consumer1.isCalled()).isTrue();
        assertThat(consumer2.isCalled()).isTrue();
        assertThat(consumer3.isCalled()).isTrue();
        consumer1.reset();
        final TestConsumer<RoutineException> errorConsumer = new TestConsumer<RoutineException>();
        final TestConsumer<Void> completeConsumer = new TestConsumer<Void>();
        outputConsumer = JRoutine.onOutput(consumer1)
                                 .thenError(errorConsumer)
                                 .thenComplete(completeConsumer);
        outputConsumer.onError(new RoutineException());
        assertThat(consumer1.isCalled()).isFalse();
        assertThat(errorConsumer.isCalled()).isTrue();
        assertThat(completeConsumer.isCalled()).isFalse();
        outputConsumer.onComplete();
        assertThat(consumer1.isCalled()).isFalse();
        assertThat(completeConsumer.isCalled()).isTrue();
        outputConsumer.onOutput("test");
        assertThat(consumer1.isCalled()).isTrue();
    }

    @Test
    public void testPendingInputs() {

        final InvocationChannel<Object, Object> channel =
                JRoutine.on(PassingInvocation.factoryOf()).asyncInvoke();
        assertThat(channel.isOpen()).isTrue();
        channel.pass("test");
        assertThat(channel.isOpen()).isTrue();
        channel.after(millis(500)).pass("test");
        assertThat(channel.isOpen()).isTrue();
        final IOChannel<Object> ioChannel = JRoutine.io().buildChannel();
        channel.pass(ioChannel);
        assertThat(channel.isOpen()).isTrue();
        channel.result();
        assertThat(channel.isOpen()).isFalse();
        ioChannel.close();
        assertThat(channel.isOpen()).isFalse();
    }

    @Test
    public void testPredicateFilter() {

        final Routine<String, String> routine = JRoutine.filter(new Predicate<String>() {

            public boolean test(final String s) {

                return s.length() > 1;
            }
        }).buildRoutine();
        assertThat(routine.asyncCall("test", "1").afterMax(seconds(1)).all()).containsOnly("test");
    }

    @Test
    public void testProxyConfiguration() {

        final TestClass test = new TestClass();
        final TestItf proxy = JRoutine.on(test)
                                      .getInvocationConfiguration()
                                      .withRunner(Runners.poolRunner())
                                      .withLogLevel(Level.DEBUG)
                                      .withLog(new NullLog())
                                      .setConfiguration()
                                      .getProxyConfiguration()
                                      .withSharedFields()
                                      .setConfiguration()
                                      .buildProxy(TestItf.class);
        assertThat(proxy.getOne().all()).containsExactly(1);
    }

    @Test
    public void testProxyError() {

        try {
            JRoutine.on(TestItf.class);
            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testSupplierCommand() {

        final Routine<Void, String> routine = JRoutine.command(new Supplier<String>() {

            public String get() {

                return "test";
            }
        }).buildRoutine();
        assertThat(routine.asyncCall().afterMax(seconds(1)).all()).containsOnly("test");
    }

    @Test
    public void testSupplierFactory() {

        final Routine<String, String> routine = JRoutine.factory(new Supplier<ToCase>() {

            public ToCase get() {

                return new ToCase();
            }
        }).buildRoutine();
        assertThat(routine.asyncCall("TEST").afterMax(seconds(1)).all()).containsOnly("test");
    }

    public interface TestItf {

        @ReadTimeout(300)
        @AsyncOut
        OutputChannel<Integer> getOne();
    }

    @Proxy(TestClass.class)
    public interface TestStatic {

        @ReadTimeout(300)
        @AsyncOut
        OutputChannel<Integer> getOne();

        @ReadTimeout(300)
        @AsyncOut
        OutputChannel<Integer> getTwo();
    }

    public static class GetString extends CommandInvocation<String> {

        /**
         * Constructor.
         */
        protected GetString() {

            super(null);
        }

        public void onResult(@NotNull final ResultChannel<String> result) {

            result.pass("test");
        }
    }

    @SuppressWarnings("unused")
    public static class TestClass {

        public static final String GET = "get";

        public static final String THROW = "throw";

        public static int getTwo() {

            return 2;
        }

        @Alias(GET)
        public long getLong() {

            return -77;
        }

        public int getOne() {

            return 1;
        }

        @Alias(THROW)
        public void throwException(final RuntimeException ex) {

            throw ex;
        }
    }

    public static class ToCase extends FilterInvocation<String, String> {

        private final boolean mIsUpper;

        public ToCase() {

            this(false);
        }

        public ToCase(final boolean isUpper) {

            super(asArgs(isUpper));
            mIsUpper = isUpper;
        }

        public void onInput(final String input, @NotNull final ResultChannel<String> result) {

            result.pass(mIsUpper ? input.toUpperCase() : input.toLowerCase());
        }
    }

    private static class TestConsumer<OUT> implements Consumer<OUT> {

        private boolean mIsCalled;

        public boolean isCalled() {

            return mIsCalled;
        }

        public void reset() {

            mIsCalled = false;
        }

        public void accept(final OUT out) {

            mIsCalled = true;
        }
    }
}
