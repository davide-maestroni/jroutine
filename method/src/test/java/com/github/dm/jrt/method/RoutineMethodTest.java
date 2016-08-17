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

package com.github.dm.jrt.method;

import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.runner.Runners;

import org.junit.Test;

import java.util.Arrays;
import java.util.Locale;

import static com.github.dm.jrt.core.util.UnitDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Routine method unit tests.
 * <p>
 * Created by davide-maestroni on 08/10/2016.
 */
@SuppressWarnings("unused")
public class RoutineMethodTest {

    public static int length(final String str) {
        return str.length();
    }

    private static void testStaticInternal() {
        final InputChannel<String> inputStrings = RoutineMethod.inputChannel();
        final OutputChannel<Object> outputLengths = RoutineMethod.outputChannel();
        new RoutineMethod() {

            void length(final InputChannel<String> input, final OutputChannel<Integer> output) {
                if (input.hasNext()) {
                    output.pass(input.next().length());
                }
            }
        }.callParallel(inputStrings, outputLengths);
        inputStrings.pass("test", "test1", "test22");
        assertThat(outputLengths.after(seconds(1)).next(3)).containsOnly(4, 5, 6);
    }

    private static void testStaticInternal2() {
        final Locale locale = Locale.getDefault();
        final RoutineMethod method = new RoutineMethod(locale) {

            String switchCase(final InputChannel<String> input, final boolean isUpper) {
                final String str = input.next();
                return (isUpper) ? str.toUpperCase(locale) : str.toLowerCase(locale);
            }
        };
        InputChannel<Object> inputChannel = RoutineMethod.inputChannel().pass("test");
        assertThat(method.call(inputChannel, true).after(seconds(1)).next()).isEqualTo("TEST");
        inputChannel = RoutineMethod.inputChannel().pass("TEST");
        assertThat(method.call(inputChannel, false).after(seconds(1)).next()).isEqualTo("test");
    }

    @Test
    public void testAbort() {
        final InputChannel<Integer> inputChannel = RoutineMethod.inputChannel();
        final OutputChannel<Integer> outputChannel = RoutineMethod.outputChannel();
        new RoutineMethod() {

            public int mSum;

            void sum(final InputChannel<Integer> input, final OutputChannel<Integer> output) {
                if (input.hasNext()) {
                    mSum += input.next();

                } else {
                    output.pass(Integer.MIN_VALUE);
                }
            }
        }.call(inputChannel, outputChannel);
        inputChannel.pass(1, 2, 3, 4).abort();
        assertThat(outputChannel.after(seconds(1)).getError()).isExactlyInstanceOf(
                AbortException.class);
    }

    @Test
    public void testAbort2() {
        final InputChannel<Integer> inputChannel1 = RoutineMethod.inputChannel();
        final InputChannel<Integer> inputChannel2 = RoutineMethod.inputChannel();
        final OutputChannel<Integer> outputChannel = RoutineMethod.outputChannel();
        new RoutineMethod() {

            public int mSum;

            void sum(final InputChannel<Integer> input1, final InputChannel<Integer> input2,
                    final OutputChannel<Integer> output) {
                final InputChannel<Integer> input = switchInput();
                if (input.hasNext()) {
                    mSum += input.next();

                } else {
                    output.pass(mSum);
                }
            }
        }.call(inputChannel1, inputChannel2, outputChannel);
        inputChannel1.pass(1, 2, 3, 4);
        inputChannel2.abort();
        assertThat(outputChannel.after(seconds(1)).getError()).isExactlyInstanceOf(
                AbortException.class);
    }

    @Test
    public void testAbort3() {
        final InputChannel<Integer> inputChannel1 = RoutineMethod.inputChannel();
        final InputChannel<Integer> inputChannel2 = RoutineMethod.inputChannel();
        final OutputChannel<Integer> outputChannel = RoutineMethod.outputChannel();
        new RoutineMethod() {

            public int mSum;

            void sum(final InputChannel<Integer> input1, final InputChannel<Integer> input2,
                    final OutputChannel<Integer> output) {
                if (input1.equals(switchInput())) {
                    if (input1.hasNext()) {
                        mSum += input1.next();

                    } else {
                        output.pass(mSum);
                    }
                }
            }
        }.call(inputChannel1, inputChannel2, outputChannel);
        inputChannel1.pass(1, 2, 3, 4);
        inputChannel2.abort();
        assertThat(outputChannel.after(seconds(1)).getError()).isExactlyInstanceOf(
                AbortException.class);
    }

    @Test
    public void testBind() {
        final InputChannel<Integer> inputChannel = RoutineMethod.inputChannel();
        final OutputChannel<Integer> outputChannel = RoutineMethod.outputChannel();
        new RoutineMethod() {

            public void square(final InputChannel<Integer> input,
                    final OutputChannel<Integer> output) {
                if (input.hasNext()) {
                    final int i = input.next();
                    output.pass(i * i);
                }
            }
        }.call(inputChannel, outputChannel);
        final OutputChannel<Integer> resultChannel = RoutineMethod.outputChannel();
        new SumRoutine().call(RoutineMethod.inputFrom(outputChannel), resultChannel);
        inputChannel.pass(1, 2, 3, 4, 5).close();
        assertThat(resultChannel.after(seconds(1)).next()).isEqualTo(55);
    }

    @Test
    public void testCall() {
        final InputChannel<Integer> inputChannel = RoutineMethod.inputChannel();
        final OutputChannel<Integer> outputChannel = RoutineMethod.outputChannel();
        new RoutineMethod() {

            public int mSum;

            public void sum(final InputChannel<Integer> input,
                    final OutputChannel<Integer> output) {
                if (input.hasNext()) {
                    mSum += input.next();

                } else {
                    output.pass(mSum);
                }
            }
        }.call(inputChannel, outputChannel);
        inputChannel.pass(1, 2, 3, 4, 5).close();
        assertThat(outputChannel.after(seconds(1)).next()).isEqualTo(15);
    }

    @Test
    public void testCallError() {
        final RoutineMethod method = new RoutineMethod() {

            int zero() {
                return 0;
            }
        };
        assertThat(method.call().after(seconds(1)).next()).isEqualTo(0);
        try {
            method.call();
            fail();

        } catch (final IllegalStateException ignored) {
        }
    }

    @Test
    public void testCallSync() {
        final InputChannel<Integer> inputChannel = RoutineMethod.inputChannel();
        final OutputChannel<Integer> outputChannel = RoutineMethod.outputChannel();
        new SumRoutine().applyInvocationConfiguration()
                        .withRunner(Runners.syncRunner())
                        .configured()
                        .call(inputChannel, outputChannel);
        inputChannel.pass(1, 2, 3, 4, 5).close();
        assertThat(outputChannel.next()).isEqualTo(15);
    }

    @Test
    public void testFromClass() throws NoSuchMethodException {
        assertThat(RoutineMethod.from(RoutineMethodTest.class.getMethod("length", String.class))
                                .call("test")
                                .after(seconds(1))
                                .next()).isEqualTo(4);
        assertThat(RoutineMethod.from(RoutineMethodTest.class.getMethod("length", String.class))
                                .call(RoutineMethod.inputOf("test"))
                                .after(seconds(1))
                                .next()).isEqualTo(4);
        final InputChannel<String> inputChannel = RoutineMethod.inputChannel();
        final OutputChannel<Object> outputChannel =
                RoutineMethod.from(RoutineMethodTest.class.getMethod("length", String.class))
                             .callParallel(inputChannel);
        inputChannel.pass("test", "test1", "test22").close();
        assertThat(outputChannel.after(seconds(1)).all()).containsOnly(4, 5, 6);
    }

    @Test
    public void testFromClass2() throws NoSuchMethodException {
        assertThat(RoutineMethod.from(RoutineMethodTest.class, "length", String.class)
                                .call("test")
                                .after(seconds(1))
                                .next()).isEqualTo(4);
        assertThat(RoutineMethod.from(RoutineMethodTest.class, "length", String.class)
                                .call(RoutineMethod.inputOf("test"))
                                .after(seconds(1))
                                .next()).isEqualTo(4);
        final InputChannel<String> inputChannel = RoutineMethod.inputChannel();
        final OutputChannel<Object> outputChannel =
                RoutineMethod.from(RoutineMethodTest.class, "length", String.class)
                             .call(inputChannel);
        inputChannel.pass("test").close();
        assertThat(outputChannel.after(seconds(1)).next()).isEqualTo(4);
    }

    @Test
    public void testFromError() throws NoSuchMethodException {
        try {
            RoutineMethod.from(String.class.getMethod("toString"));
            fail();

        } catch (final IllegalArgumentException ignored) {
        }

        try {
            RoutineMethod.from("test", RoutineMethodTest.class.getMethod("length", String.class));
            fail();

        } catch (final IllegalArgumentException ignored) {
        }
    }

    @Test
    public void testFromInstance() throws NoSuchMethodException {
        final String test = "test";
        assertThat(RoutineMethod.from(test, String.class.getMethod("toString"))
                                .call()
                                .after(seconds(1))
                                .next()).isEqualTo("test");
        assertThat(RoutineMethod.from(test, String.class.getMethod("toString"))
                                .applyInvocationConfiguration()
                                .withRunner(Runners.syncRunner())
                                .configured()
                                .applyObjectConfiguration()
                                .withSharedFields()
                                .configured()
                                .call()
                                .next()).isEqualTo("test");
    }

    @Test
    public void testFromInstance2() throws NoSuchMethodException {
        final String test = "test";
        assertThat(RoutineMethod.from(test, "toString").call().after(seconds(1)).next()).isEqualTo(
                "test");
        assertThat(RoutineMethod.from(test, "toString")
                                .applyInvocationConfiguration()
                                .withRunner(Runners.syncRunner())
                                .configured()
                                .applyObjectConfiguration()
                                .withSharedFields()
                                .configured()
                                .call()
                                .next()).isEqualTo("test");
    }

    @Test
    public void testInputs() {
        OutputChannel<Integer> outputChannel = RoutineMethod.outputChannel();
        new SumRoutine().call(RoutineMethod.inputOf(), outputChannel);
        assertThat(outputChannel.after(seconds(1)).next()).isEqualTo(0);
        outputChannel = RoutineMethod.outputChannel();
        new SumRoutine().call(RoutineMethod.inputOf(1), outputChannel);
        assertThat(outputChannel.after(seconds(1)).next()).isEqualTo(1);
        outputChannel = RoutineMethod.outputChannel();
        new SumRoutine().call(RoutineMethod.inputOf(1, 2, 3, 4, 5), outputChannel);
        assertThat(outputChannel.after(seconds(1)).next()).isEqualTo(15);
        outputChannel = RoutineMethod.outputChannel();
        new SumRoutine().call(RoutineMethod.inputOf(Arrays.asList(1, 2, 3, 4, 5)), outputChannel);
        assertThat(outputChannel.after(seconds(1)).next()).isEqualTo(15);
    }

    @Test
    public void testLocal() {
        class SumRoutine extends RoutineMethod {

            public int mSum;

            private SumRoutine(final int i) {
                super(RoutineMethodTest.this, i);
            }

            public void sum(final InputChannel<Integer> input,
                    final OutputChannel<Integer> output) {
                if (input.hasNext()) {
                    mSum += input.next();

                } else {
                    output.pass(mSum);
                }
            }
        }

        final InputChannel<Integer> inputChannel = RoutineMethod.inputChannel();
        final OutputChannel<Integer> outputChannel = RoutineMethod.outputChannel();
        new SumRoutine(0).applyInvocationConfiguration()
                         .withRunner(Runners.syncRunner())
                         .configured()
                         .callParallel(inputChannel, outputChannel);
        inputChannel.pass(1, 2, 3, 4, 5).close();
        assertThat(outputChannel.all()).containsOnly(1, 2, 3, 4, 5);
    }

    @Test
    public void testLocal2() {
        final int[] i = new int[1];
        class SumRoutine extends RoutineMethod {

            public int mSum;

            private SumRoutine() {
                super(RoutineMethodTest.this, i);
            }

            public void sum(final InputChannel<Integer> input,
                    final OutputChannel<Integer> output) {
                if (input.hasNext()) {
                    mSum += input.next();

                } else {
                    output.pass(mSum + i[0]);
                }
            }
        }

        i[0] = 1;
        final InputChannel<Integer> inputChannel = RoutineMethod.inputChannel();
        final OutputChannel<Integer> outputChannel = RoutineMethod.outputChannel();
        new SumRoutine().applyInvocationConfiguration()
                        .withRunner(Runners.syncRunner())
                        .configured()
                        .callParallel(inputChannel, outputChannel);
        inputChannel.pass(1, 2, 3, 4, 5).close();
        assertThat(outputChannel.all()).containsOnly(2, 3, 4, 5, 6);
    }

    @Test
    public void testNoInputs() {
        assertThat(new RoutineMethod() {

            String get() {
                return "test";
            }
        }.call().after(seconds(1)).next()).isEqualTo("test");
        final OutputChannel<String> outputChannel = RoutineMethod.outputChannel();
        new RoutineMethod() {

            void get(final OutputChannel<String> outputChannel) {
                outputChannel.pass("test");
            }
        }.call(outputChannel);
        assertThat(outputChannel.after(seconds(1)).next()).isEqualTo("test");
    }

    @Test
    public void testParallel() {
        final InputChannel<Integer> inputChannel = RoutineMethod.inputChannel();
        final OutputChannel<Integer> outputChannel = RoutineMethod.outputChannel();
        new SumRoutine().applyInvocationConfiguration()
                        .withRunner(Runners.syncRunner())
                        .configured()
                        .callParallel(inputChannel, outputChannel);
        inputChannel.pass(1, 2, 3, 4, 5).close();
        assertThat(outputChannel.all()).containsOnly(1, 2, 3, 4, 5);
    }

    @Test
    public void testParallel2() {
        final InputChannel<String> inputStrings = RoutineMethod.inputChannel();
        final OutputChannel<Object> outputChannel = new RoutineMethod(this) {

            int length(final InputChannel<String> input) {
                if (input.hasNext()) {
                    return input.next().length();
                }
                return 0;
            }
        }.callParallel(inputStrings);
        inputStrings.pass("test");
        assertThat(outputChannel.after(seconds(1)).next()).isEqualTo(4);
    }

    @Test
    public void testParallel3() {
        final InputChannel<Integer> inputChannel = RoutineMethod.inputChannel();
        final OutputChannel<Integer> outputChannel = RoutineMethod.outputChannel();
        new SumRoutineInner(0).applyInvocationConfiguration()
                              .withRunner(Runners.syncRunner())
                              .configured()
                              .callParallel(inputChannel, outputChannel);
        inputChannel.pass(1, 2, 3, 4, 5).close();
        assertThat(outputChannel.all()).containsOnly(1, 2, 3, 4, 5);
    }

    @Test
    public void testParallelError() {
        try {
            new RoutineMethod() {

                int zero() {
                    return 0;
                }
            }.callParallel();
            fail();

        } catch (final IllegalStateException ignored) {
        }
    }

    @Test
    public void testParams() {
        final RoutineMethod method = new RoutineMethod(this) {

            String switchCase(final InputChannel<String> input, final boolean isUpper) {
                final String str = input.next();
                return (isUpper) ? str.toUpperCase() : str.toLowerCase();
            }
        };
        InputChannel<Object> inputChannel = RoutineMethod.inputChannel().pass("test");
        assertThat(method.call(inputChannel, true).after(seconds(1)).next()).isEqualTo("TEST");
        inputChannel = RoutineMethod.inputChannel().pass("TEST");
        assertThat(method.call(inputChannel, false).after(seconds(1)).next()).isEqualTo("test");
    }

    @Test
    public void testParams2() {
        final Locale locale = Locale.getDefault();
        final RoutineMethod method = new RoutineMethod(this, locale) {

            String switchCase(final InputChannel<String> input, final boolean isUpper) {
                final String str = input.next();
                return (isUpper) ? str.toUpperCase(locale) : str.toLowerCase(locale);
            }
        };
        InputChannel<Object> inputChannel = RoutineMethod.inputChannel().pass("test");
        assertThat(method.call(inputChannel, true).after(seconds(1)).next()).isEqualTo("TEST");
        inputChannel = RoutineMethod.inputChannel().pass("TEST");
        assertThat(method.call(inputChannel, false).after(seconds(1)).next()).isEqualTo("test");
    }

    @Test
    public void testReturnValue() {
        final InputChannel<String> inputStrings = RoutineMethod.inputChannel();
        final OutputChannel<Object> outputChannel = new RoutineMethod() {

            int length(final InputChannel<String> input) {
                if (input.hasNext()) {
                    return input.next().length();
                }
                return 0;
            }
        }.call(inputStrings);
        inputStrings.pass("test");
        assertThat(outputChannel.after(seconds(1)).next()).isEqualTo(4);
    }

    @Test
    public void testStatic() {
        testStaticInternal();
    }

    @Test
    public void testStatic2() {
        testStaticInternal2();
    }

    @Test
    public void testSwitchInput() {
        final InputChannel<Integer> inputInts = RoutineMethod.inputChannel();
        final InputChannel<String> inputStrings = RoutineMethod.inputChannel();
        final OutputChannel<String> outputChannel = RoutineMethod.outputChannel();
        new RoutineMethod() {

            void run(final InputChannel<Integer> inputInts, final InputChannel<String> inputStrings,
                    final OutputChannel<String> output) {
                output.pass(switchInput().next().toString());
            }

        }.call(inputInts, inputStrings, outputChannel);
        inputStrings.pass("test1", "test2");
        inputInts.pass(1, 2, 3);
        assertThat(outputChannel.after(seconds(1)).next(4)).containsExactly("test1", "test2", "1",
                "2");
    }

    @Test
    public void testSwitchInput2() {
        final InputChannel<Integer> inputInts = RoutineMethod.inputChannel();
        final InputChannel<String> inputStrings = RoutineMethod.inputChannel();
        final OutputChannel<String> outputChannel = RoutineMethod.outputChannel();
        new RoutineMethod() {

            void run(final InputChannel<Integer> inputInts, final InputChannel<String> inputStrings,
                    final OutputChannel<String> output) {
                final InputChannel<?> inputChannel = switchInput();
                if (inputChannel == inputStrings) {
                    output.pass(inputStrings.next());
                }
            }

        }.call(inputInts, inputStrings, outputChannel);
        inputStrings.pass("test1", "test2");
        inputInts.pass(1, 2, 3);
        assertThat(outputChannel.after(seconds(1)).next(2)).containsExactly("test1", "test2");
    }

    private static class SumRoutine extends RoutineMethod {

        private int mSum;

        public void sum(final InputChannel<Integer> input, final OutputChannel<Integer> output) {
            if (input.hasNext()) {
                mSum += input.next();

            } else {
                output.pass(mSum);
            }
        }
    }

    private class SumRoutineInner extends RoutineMethod {

        private int mSum;

        private SumRoutineInner(final int i) {
            super(RoutineMethodTest.this, i);
        }

        public void sum(final InputChannel<Integer> input, final OutputChannel<Integer> output) {
            if (input.hasNext()) {
                mSum += input.next();

            } else {
                output.pass(mSum);
            }
        }
    }
}
