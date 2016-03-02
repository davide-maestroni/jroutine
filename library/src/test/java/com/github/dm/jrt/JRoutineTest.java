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

import com.github.dm.jrt.JRoutine.ProxyTarget;
import com.github.dm.jrt.builder.InvocationConfiguration.TimeoutActionType;
import com.github.dm.jrt.channel.Channel.OutputChannel;
import com.github.dm.jrt.channel.IOChannel;
import com.github.dm.jrt.channel.InvocationChannel;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.invocation.FunctionInvocation;
import com.github.dm.jrt.invocation.InvocationException;
import com.github.dm.jrt.invocation.PassingInvocation;
import com.github.dm.jrt.log.Log.Level;
import com.github.dm.jrt.log.NullLog;
import com.github.dm.jrt.object.annotation.Alias;
import com.github.dm.jrt.object.annotation.AsyncOut;
import com.github.dm.jrt.object.annotation.ReadTimeout;
import com.github.dm.jrt.proxy.annotation.Proxy;
import com.github.dm.jrt.routine.Routine;
import com.github.dm.jrt.runner.Runners;
import com.github.dm.jrt.util.TimeDuration;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.List;

import static com.github.dm.jrt.function.Functions.functionFilter;
import static com.github.dm.jrt.invocation.InvocationFactories.factoryOf;
import static com.github.dm.jrt.object.core.InvocationTarget.instance;
import static com.github.dm.jrt.util.TimeDuration.millis;
import static com.github.dm.jrt.util.TimeDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * JRoutine unit tests.
 * <p/>
 * Created by davide-maestroni on 02/29/2016.
 */
public class JRoutineTest {

    @Test
    public void testAliasMethod() throws NoSuchMethodException {

        final TimeDuration timeout = seconds(1);
        final TestClass test = new TestClass();
        final Routine<Object, Object> routine = JRoutine.on(instance(test))
                                                        .withInvocations()
                                                        .withRunner(Runners.poolRunner())
                                                        .withMaxInstances(1)
                                                        .withCoreInstances(1)
                                                        .withReadTimeoutAction(
                                                                TimeoutActionType.EXIT)
                                                        .withLogLevel(Level.DEBUG)
                                                        .withLog(new NullLog())
                                                        .getConfigured()
                                                        .aliasMethod(TestClass.GET);

        assertThat(routine.syncCall().afterMax(timeout).all()).containsExactly(-77L);
    }

    @Test
    public void testChainedRoutine() {

        final TimeDuration timeout = seconds(1);
        final FunctionInvocation<Integer, Integer> execSum =
                new FunctionInvocation<Integer, Integer>() {

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

        final TestStatic testStatic = JRoutine.on(ProxyTarget.classOfType(TestClass.class))
                                              .withInvocations()
                                              .withRunner(Runners.poolRunner())
                                              .withLogLevel(Level.DEBUG)
                                              .withLog(new NullLog())
                                              .getConfigured()
                                              .buildProxy(TestStatic.class);

        try {

            assertThat(testStatic.getOne().all()).containsExactly(1);

            fail();

        } catch (final InvocationException ignored) {

        }

        assertThat(testStatic.getTwo().all()).containsExactly(2);
    }

    @Test
    public void testObjectStaticMethod() {

        final TestClass test = new TestClass();
        final TestStatic testStatic = JRoutine.on(ProxyTarget.instance(test))
                                              .withInvocations()
                                              .withRunner(Runners.poolRunner())
                                              .withLogLevel(Level.DEBUG)
                                              .withLog(new NullLog())
                                              .getConfigured()
                                              .buildProxy(TestStatic.class);

        assertThat(testStatic.getOne().all()).containsExactly(1);
        assertThat(testStatic.getTwo().all()).containsExactly(2);
    }

    @Test
    public void testPendingInputs() throws InterruptedException {

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

    @Proxy(TestClass.class)
    public interface TestStatic {

        @ReadTimeout(300)
        @AsyncOut
        OutputChannel<Integer> getOne();

        @ReadTimeout(300)
        @AsyncOut
        OutputChannel<Integer> getTwo();
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
}
