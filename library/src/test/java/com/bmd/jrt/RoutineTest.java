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
package com.bmd.jrt;

import com.bmd.jrt.channel.ResultChannel;
import com.bmd.jrt.channel.ResultConsumer;
import com.bmd.jrt.channel.ResultConsumerAdapter;
import com.bmd.jrt.channel.RoutineChannel;
import com.bmd.jrt.routine.Routine;
import com.bmd.jrt.subroutine.SubRoutine;
import com.bmd.jrt.subroutine.SubRoutineAdapter;
import com.bmd.jrt.subroutine.SubRoutineFunction;
import com.bmd.jrt.time.TimeDuration;
import com.bmd.jrt.util.Classification;
import com.bmd.jrt.util.RoutineException;

import junit.framework.TestCase;

import java.util.List;

import static com.bmd.jrt.routine.JRoutine.jrt;
import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Unit test for...
 * <p/>
 * Created by davide on 9/9/14.
 */
public class RoutineTest extends TestCase {

    public void testError() {

        final SubRoutine<String, String> testException1 = new SubRoutineAdapter<String, String>() {

            @Override
            public void onInit() {

                throw new NullPointerException("test1");
            }
        };

        final Routine<String, String> exceptionRoutine1 =
                jrt().routineOf(Classification.of(testException1), this);

        testException(exceptionRoutine1, "test", "test1");

        final SubRoutine<String, String> testException2 = new SubRoutineAdapter<String, String>() {

            @Override
            public void onInput(final String s, final ResultChannel<String> results) {

                throw new NullPointerException(s);
            }
        };

        final Routine<String, String> exceptionRoutine2 =
                jrt().routineOf(Classification.of(testException2), this);

        testException(exceptionRoutine2, "test2", "test2");

        final SubRoutine<String, String> testException3 = new SubRoutineAdapter<String, String>() {

            @Override
            public void onResult(final ResultChannel<String> results) {

                throw new NullPointerException("test3");
            }
        };

        final Routine<String, String> exceptionRoutine3 =
                jrt().routineOf(Classification.of(testException3), this);

        testException(exceptionRoutine3, "test", "test3");

        final SubRoutine<String, String> testException4 = new SubRoutineAdapter<String, String>() {

            @Override
            public void onInput(final String s, final ResultChannel<String> results) {

                results.push(s);
            }

            @Override
            public void onReturn() {

                throw new NullPointerException("test4");
            }
        };

        final Routine<String, String> exceptionRoutine4 =
                jrt().routineOf(Classification.of(testException4), this);

        assertThat(exceptionRoutine4.call("test")).containsExactly("test");
        assertThat(exceptionRoutine4.callAsyn("test")).containsExactly("test");
        assertThat(exceptionRoutine4.run("test").takeAll()).containsExactly("test");
        assertThat(exceptionRoutine4.runAsyn("test").takeAll()).containsExactly("test");
        assertThat(exceptionRoutine4.runPar("test").takeAll()).containsExactly("test");
        assertThat(exceptionRoutine4.run("test").iterator().next()).isEqualTo("test");
        assertThat(exceptionRoutine4.runAsyn("test").iterator().next()).isEqualTo("test");
        assertThat(exceptionRoutine4.runPar("test").iterator().next()).isEqualTo("test");
        assertThat(exceptionRoutine4.launch().push("test").close().takeAll()).containsExactly(
                "test");
        assertThat(exceptionRoutine4.launchAsyn().push("test").close().takeAll()).containsExactly(
                "test");
        assertThat(exceptionRoutine4.launchPar().push("test").close().takeAll()).containsExactly(
                "test");
        assertThat(exceptionRoutine4.launch().push("test").close().iterator().next()).isEqualTo(
                "test");
        assertThat(exceptionRoutine4.launchAsyn().push("test").close().iterator().next()).isEqualTo(
                "test");
        assertThat(exceptionRoutine4.launchPar().push("test").close().iterator().next()).isEqualTo(
                "test");

        final Routine<String, String> passThroughRoutine =
                jrt().routineOf(Classification.ofType(PassThroughSubRoutine.class));

        testChained(passThroughRoutine, exceptionRoutine1, "test", "test1");
        testChained(exceptionRoutine1, passThroughRoutine, "test", "test1");
        testChained(passThroughRoutine, exceptionRoutine2, "test2", "test2");
        testChained(exceptionRoutine2, passThroughRoutine, "test2", "test2");
        testChained(passThroughRoutine, exceptionRoutine3, "test", "test3");
        testChained(exceptionRoutine3, passThroughRoutine, "test", "test3");

        assertThat(passThroughRoutine.call(exceptionRoutine4.run("test"))).containsExactly("test");
        assertThat(passThroughRoutine.callAsyn(exceptionRoutine4.run("test"))).containsExactly(
                "test");
        assertThat(passThroughRoutine.run(exceptionRoutine4.run("test")).takeAll()).containsExactly(
                "test");
        assertThat(passThroughRoutine.runAsyn(exceptionRoutine4.run("test"))
                                     .takeAll()).containsExactly("test");
        assertThat(
                passThroughRoutine.runPar(exceptionRoutine4.run("test")).takeAll()).containsExactly(
                "test");
        assertThat(
                passThroughRoutine.run(exceptionRoutine4.run("test")).iterator().next()).isEqualTo(
                "test");
        assertThat(passThroughRoutine.runAsyn(exceptionRoutine4.run("test"))
                                     .iterator()
                                     .next()).isEqualTo("test");
        assertThat(passThroughRoutine.runPar(exceptionRoutine4.run("test"))
                                     .iterator()
                                     .next()).isEqualTo("test");
        assertThat(passThroughRoutine.launch()
                                     .push(exceptionRoutine4.run("test"))
                                     .close().takeAll()).containsExactly("test");
        assertThat(passThroughRoutine.launchAsyn()
                                     .push(exceptionRoutine4.run("test"))
                                     .close().takeAll()).containsExactly("test");
        assertThat(passThroughRoutine.launchPar()
                                     .push(exceptionRoutine4.run("test"))
                                     .close()
                                     .takeAll()).containsExactly("test");
        assertThat(passThroughRoutine.launch()
                                     .push(exceptionRoutine4.run("test"))
                                     .close()
                                     .iterator()
                                     .next()).isEqualTo("test");
        assertThat(passThroughRoutine.launchAsyn()
                                     .push(exceptionRoutine4.run("test"))
                                     .close()
                                     .iterator()
                                     .next()).isEqualTo("test");
        assertThat(passThroughRoutine.launchPar()
                                     .push(exceptionRoutine4.run("test"))
                                     .close()
                                     .iterator()
                                     .next()).isEqualTo("test");

        assertThat(passThroughRoutine.call(exceptionRoutine4.runAsyn("test"))).containsExactly(
                "test");
        assertThat(passThroughRoutine.callAsyn(exceptionRoutine4.runAsyn("test"))).containsExactly(
                "test");
        assertThat(passThroughRoutine.run(exceptionRoutine4.runAsyn("test"))
                                     .takeAll()).containsExactly("test");
        assertThat(passThroughRoutine.runAsyn(exceptionRoutine4.runAsyn("test"))
                                     .takeAll()).containsExactly("test");
        assertThat(passThroughRoutine.runPar(exceptionRoutine4.runAsyn("test"))
                                     .takeAll()).containsExactly("test");
        assertThat(passThroughRoutine.run(exceptionRoutine4.runAsyn("test"))
                                     .iterator()
                                     .next()).isEqualTo("test");
        assertThat(passThroughRoutine.runAsyn(exceptionRoutine4.runAsyn("test"))
                                     .iterator()
                                     .next()).isEqualTo("test");
        assertThat(passThroughRoutine.runPar(exceptionRoutine4.runAsyn("test"))
                                     .iterator()
                                     .next()).isEqualTo("test");
        assertThat(passThroughRoutine.launch()
                                     .push(exceptionRoutine4.runAsyn("test"))
                                     .close().takeAll()).containsExactly("test");
        assertThat(passThroughRoutine.launchAsyn()
                                     .push(exceptionRoutine4.runAsyn("test"))
                                     .close().takeAll()).containsExactly("test");
        assertThat(passThroughRoutine.launchPar()
                                     .push(exceptionRoutine4.runAsyn("test"))
                                     .close().takeAll()).containsExactly("test");
        assertThat(passThroughRoutine.launch()
                                     .push(exceptionRoutine4.runAsyn("test"))
                                     .close()
                                     .iterator()
                                     .next()).isEqualTo("test");
        assertThat(passThroughRoutine.launchAsyn()
                                     .push(exceptionRoutine4.runAsyn("test"))
                                     .close()
                                     .iterator()
                                     .next()).isEqualTo("test");
        assertThat(passThroughRoutine.launchPar()
                                     .push(exceptionRoutine4.runAsyn("test"))
                                     .close()
                                     .iterator()
                                     .next()).isEqualTo("test");

        assertThat(passThroughRoutine.call(exceptionRoutine4.runPar("test"))).containsExactly(
                "test");
        assertThat(passThroughRoutine.callAsyn(exceptionRoutine4.runPar("test"))).containsExactly(
                "test");
        assertThat(
                passThroughRoutine.run(exceptionRoutine4.runPar("test")).takeAll()).containsExactly(
                "test");
        assertThat(passThroughRoutine.runAsyn(exceptionRoutine4.runPar("test"))
                                     .takeAll()).containsExactly("test");
        assertThat(passThroughRoutine.runPar(exceptionRoutine4.runPar("test"))
                                     .takeAll()).containsExactly("test");
        assertThat(passThroughRoutine.run(exceptionRoutine4.runPar("test"))
                                     .iterator()
                                     .next()).isEqualTo("test");
        assertThat(passThroughRoutine.runAsyn(exceptionRoutine4.runPar("test"))
                                     .iterator()
                                     .next()).isEqualTo("test");
        assertThat(passThroughRoutine.runPar(exceptionRoutine4.runPar("test"))
                                     .iterator()
                                     .next()).isEqualTo("test");
        assertThat(passThroughRoutine.launch()
                                     .push(exceptionRoutine4.runPar("test"))
                                     .close()
                                     .takeAll()).containsExactly("test");
        assertThat(passThroughRoutine.launchAsyn()
                                     .push(exceptionRoutine4.runPar("test"))
                                     .close().takeAll()).containsExactly("test");
        assertThat(passThroughRoutine.launchPar()
                                     .push(exceptionRoutine4.runPar("test"))
                                     .close()
                                     .takeAll()).containsExactly("test");
        assertThat(passThroughRoutine.launchAsyn()
                                     .push(exceptionRoutine4.runPar("test"))
                                     .close()
                                     .iterator()
                                     .next()).isEqualTo("test");
        assertThat(passThroughRoutine.launchPar()
                                     .push(exceptionRoutine4.runPar("test"))
                                     .close()
                                     .iterator()
                                     .next()).isEqualTo("test");

        assertThat(exceptionRoutine4.call(passThroughRoutine.run("test"))).containsExactly("test");
        assertThat(exceptionRoutine4.callAsyn(passThroughRoutine.run("test"))).containsExactly(
                "test");
        assertThat(exceptionRoutine4.run(passThroughRoutine.run("test")).takeAll()).containsExactly(
                "test");
        assertThat(exceptionRoutine4.runAsyn(passThroughRoutine.run("test"))
                                    .takeAll()).containsExactly("test");
        assertThat(
                exceptionRoutine4.runPar(passThroughRoutine.run("test")).takeAll()).containsExactly(
                "test");
        assertThat(
                exceptionRoutine4.run(passThroughRoutine.run("test")).iterator().next()).isEqualTo(
                "test");
        assertThat(exceptionRoutine4.runAsyn(passThroughRoutine.run("test"))
                                    .iterator()
                                    .next()).isEqualTo("test");
        assertThat(exceptionRoutine4.runPar(passThroughRoutine.run("test"))
                                    .iterator()
                                    .next()).isEqualTo("test");
        assertThat(exceptionRoutine4.launch()
                                    .push(passThroughRoutine.run("test"))
                                    .close().takeAll()).containsExactly("test");
        assertThat(exceptionRoutine4.launchAsyn()
                                    .push(passThroughRoutine.run("test"))
                                    .close().takeAll()).containsExactly("test");
        assertThat(exceptionRoutine4.launchPar()
                                    .push(passThroughRoutine.run("test"))
                                    .close()
                                    .takeAll()).containsExactly("test");
        assertThat(exceptionRoutine4.launch()
                                    .push(passThroughRoutine.run("test"))
                                    .close()
                                    .iterator()
                                    .next()).isEqualTo("test");
        assertThat(exceptionRoutine4.launchAsyn()
                                    .push(passThroughRoutine.run("test"))
                                    .close()
                                    .iterator()
                                    .next()).isEqualTo("test");
        assertThat(exceptionRoutine4.launchPar()
                                    .push(passThroughRoutine.run("test"))
                                    .close()
                                    .iterator()
                                    .next()).isEqualTo("test");

        assertThat(exceptionRoutine4.call(passThroughRoutine.runAsyn("test"))).containsExactly(
                "test");
        assertThat(exceptionRoutine4.callAsyn(passThroughRoutine.runAsyn("test"))).containsExactly(
                "test");
        assertThat(exceptionRoutine4.run(passThroughRoutine.runAsyn("test"))
                                    .takeAll()).containsExactly("test");
        assertThat(exceptionRoutine4.runAsyn(passThroughRoutine.runAsyn("test"))
                                    .takeAll()).containsExactly("test");
        assertThat(exceptionRoutine4.runPar(passThroughRoutine.runAsyn("test"))
                                    .takeAll()).containsExactly("test");
        assertThat(exceptionRoutine4.run(passThroughRoutine.runAsyn("test"))
                                    .iterator()
                                    .next()).isEqualTo("test");
        assertThat(exceptionRoutine4.runAsyn(passThroughRoutine.runAsyn("test"))
                                    .iterator()
                                    .next()).isEqualTo("test");
        assertThat(exceptionRoutine4.runPar(passThroughRoutine.runAsyn("test"))
                                    .iterator()
                                    .next()).isEqualTo("test");
        assertThat(exceptionRoutine4.launch()
                                    .push(passThroughRoutine.runAsyn("test"))
                                    .close().takeAll()).containsExactly("test");
        assertThat(exceptionRoutine4.launchAsyn()
                                    .push(passThroughRoutine.runAsyn("test"))
                                    .close().takeAll()).containsExactly("test");
        assertThat(exceptionRoutine4.launchPar()
                                    .push(passThroughRoutine.runAsyn("test"))
                                    .close().takeAll()).containsExactly("test");
        assertThat(exceptionRoutine4.launch()
                                    .push(passThroughRoutine.runAsyn("test"))
                                    .close()
                                    .iterator()
                                    .next()).isEqualTo("test");
        assertThat(exceptionRoutine4.launchAsyn()
                                    .push(passThroughRoutine.runAsyn("test"))
                                    .close()
                                    .iterator()
                                    .next()).isEqualTo("test");
        assertThat(exceptionRoutine4.launchPar()
                                    .push(passThroughRoutine.runAsyn("test"))
                                    .close()
                                    .iterator()
                                    .next()).isEqualTo("test");

        assertThat(exceptionRoutine4.call(passThroughRoutine.runPar("test"))).containsExactly(
                "test");
        assertThat(exceptionRoutine4.callAsyn(passThroughRoutine.runPar("test"))).containsExactly(
                "test");
        assertThat(
                exceptionRoutine4.run(passThroughRoutine.runPar("test")).takeAll()).containsExactly(
                "test");
        assertThat(exceptionRoutine4.runAsyn(passThroughRoutine.runPar("test"))
                                    .takeAll()).containsExactly("test");
        assertThat(exceptionRoutine4.runPar(passThroughRoutine.runPar("test"))
                                    .takeAll()).containsExactly("test");
        assertThat(exceptionRoutine4.run(passThroughRoutine.runPar("test"))
                                    .iterator()
                                    .next()).isEqualTo("test");
        assertThat(exceptionRoutine4.runAsyn(passThroughRoutine.runPar("test"))
                                    .iterator()
                                    .next()).isEqualTo("test");
        assertThat(exceptionRoutine4.runPar(passThroughRoutine.runPar("test"))
                                    .iterator()
                                    .next()).isEqualTo("test");
        assertThat(exceptionRoutine4.launch()
                                    .push(passThroughRoutine.runPar("test"))
                                    .close()
                                    .takeAll()).containsExactly("test");
        assertThat(exceptionRoutine4.launchAsyn()
                                    .push(passThroughRoutine.runPar("test"))
                                    .close().takeAll()).containsExactly("test");
        assertThat(exceptionRoutine4.launchPar()
                                    .push(passThroughRoutine.runPar("test"))
                                    .close()
                                    .takeAll()).containsExactly("test");
        assertThat(exceptionRoutine4.launch()
                                    .push(passThroughRoutine.runPar("test"))
                                    .close()
                                    .iterator()
                                    .next()).isEqualTo("test");
        assertThat(exceptionRoutine4.launchAsyn()
                                    .push(passThroughRoutine.runPar("test"))
                                    .close()
                                    .iterator()
                                    .next()).isEqualTo("test");
        assertThat(exceptionRoutine4.launchPar()
                                    .push(passThroughRoutine.runPar("test"))
                                    .close()
                                    .iterator()
                                    .next()).isEqualTo("test");

        final ResultConsumerAdapter<String> exceptionConsumer1 =
                new ResultConsumerAdapter<String>() {

                    @Override
                    public void onResult(final String result) {

                        throw new NullPointerException(result);
                    }
                };

        testConsumer(exceptionConsumer1);

        final ResultConsumerAdapter<String> exceptionConsumer2 =
                new ResultConsumerAdapter<String>() {

                    @Override
                    public void onReturn() {

                        throw new NullPointerException("test2");
                    }
                };

        testConsumer(exceptionConsumer2);
    }

    public void testRoutine() {

        final SubRoutineFunction<Integer, Integer> sumSubRoutine =
                new SubRoutineFunction<Integer, Integer>() {

                    @Override
                    public void onRun(final List<? extends Integer> integers,
                            final ResultChannel<Integer> results) {

                        int sum = 0;

                        for (final Integer integer : integers) {

                            sum += integer;
                        }

                        results.push(sum);
                    }
                };

        final Routine<Integer, Integer> sumRoutine =
                jrt().routineOf(Classification.of(sumSubRoutine), this);

        assertThat(sumRoutine.call(1, 2, 3, 4)).containsExactly(10);
        assertThat(sumRoutine.callAsyn(1, 2, 3, 4)).containsExactly(10);
        assertThat(sumRoutine.run(1, 2, 3, 4).takeAll()).containsExactly(10);
        assertThat(sumRoutine.runAsyn(1, 2, 3, 4).takeAll()).containsExactly(10);

        final SubRoutineAdapter<Integer, Integer> squareSubRoutine =
                new SubRoutineAdapter<Integer, Integer>() {

                    @Override
                    public void onInput(final Integer integer,
                            final ResultChannel<Integer> results) {

                        final int input = integer;

                        results.push(input * input);
                    }
                };

        final Routine<Integer, Integer> squareRoutine =
                jrt().routineOf(Classification.of(squareSubRoutine), this);

        assertThat(sumRoutine.call(squareRoutine.run(1, 2, 3, 4))).containsExactly(30);
        assertThat(sumRoutine.callAsyn(squareRoutine.run(1, 2, 3, 4))).containsExactly(30);
        assertThat(sumRoutine.run(squareRoutine.run(1, 2, 3, 4)).takeAll()).containsExactly(30);
        assertThat(sumRoutine.runAsyn(squareRoutine.run(1, 2, 3, 4)).takeAll()).containsExactly(30);

        assertThat(sumRoutine.call(squareRoutine.runAsyn(1, 2, 3, 4))).containsExactly(30);
        assertThat(sumRoutine.callAsyn(squareRoutine.runAsyn(1, 2, 3, 4))).containsExactly(30);
        assertThat(sumRoutine.run(squareRoutine.runAsyn(1, 2, 3, 4)).takeAll()).containsExactly(30);
        assertThat(sumRoutine.runAsyn(squareRoutine.runAsyn(1, 2, 3, 4)).takeAll()).containsExactly(
                30);

        assertThat(sumRoutine.call(squareRoutine.runPar(1, 2, 3, 4))).containsExactly(30);
        assertThat(sumRoutine.callAsyn(squareRoutine.runPar(1, 2, 3, 4))).containsExactly(30);
        assertThat(sumRoutine.run(squareRoutine.runPar(1, 2, 3, 4)).takeAll()).containsExactly(30);
        assertThat(sumRoutine.runAsyn(squareRoutine.runPar(1, 2, 3, 4)).takeAll()).containsExactly(
                30);

        final SubRoutineAdapter<Integer, Integer> squareSumSubRoutine =
                new SubRoutineAdapter<Integer, Integer>() {

                    private RoutineChannel<Integer, Integer> mChannel;

                    @Override
                    public void onInit() {

                        mChannel = sumRoutine.launchAsyn();
                    }

                    @Override
                    public void onInput(final Integer integer,
                            final ResultChannel<Integer> results) {

                        mChannel.push(squareRoutine.runAsyn(integer));
                    }

                    @Override
                    public void onReset(final Throwable throwable) {

                        mChannel.reset(throwable);
                    }

                    @Override
                    public void onResult(final ResultChannel<Integer> results) {

                        results.push(mChannel.close());
                    }
                };

        final Routine<Integer, Integer> squareSumRoutine =
                jrt().routineOf(Classification.of(squareSumSubRoutine), this, sumRoutine,
                                squareRoutine);

        assertThat(squareSumRoutine.call(1, 2, 3, 4)).containsExactly(30);
        assertThat(squareSumRoutine.callAsyn(1, 2, 3, 4)).containsExactly(30);
        assertThat(squareSumRoutine.run(1, 2, 3, 4).takeAll()).containsExactly(30);
        assertThat(squareSumRoutine.runAsyn(1, 2, 3, 4).takeAll()).containsExactly(30);
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

            before.run(after.run(input)).takeAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.run(after.run(input)).iterator().next();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.runAsyn(after.run(input)).takeAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.runAsyn(after.run(input)).iterator().next();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.runPar(after.run(input)).takeAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.runPar(after.run(input)).iterator().next();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.launch().push(after.run(input)).close().takeAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.launch().push(after.run(input)).close().iterator().next();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.launchAsyn().push(after.run(input)).close().takeAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.launchAsyn().push(after.run(input)).close().iterator().next();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.launchPar().push(after.run(input)).close().takeAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.launchPar().push(after.run(input)).close().iterator().next();

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

            before.run(after.runAsyn(input)).takeAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.run(after.runAsyn(input)).iterator().next();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.runAsyn(after.runAsyn(input)).takeAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.runAsyn(after.runAsyn(input)).iterator().next();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.launch().push(after.runAsyn(input)).close().takeAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.launch().push(after.runAsyn(input)).close().iterator().next();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.launchAsyn().push(after.runAsyn(input)).close().takeAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.launchAsyn().push(after.runAsyn(input)).close().iterator().next();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.call(after.runPar(input));

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.callAsyn(after.runPar(input));

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.run(after.runPar(input)).takeAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.run(after.runPar(input)).iterator().next();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.runAsyn(after.runPar(input)).takeAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.runAsyn(after.runPar(input)).iterator().next();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.launch().push(after.runPar(input)).close().takeAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.launch().push(after.runPar(input)).close().iterator().next();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.launchAsyn().push(after.runPar(input)).close().takeAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.launchAsyn().push(after.runPar(input)).close().iterator().next();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }
    }

    private void testConsumer(final ResultConsumer<String> consumer) {

        final String input = "test";
        final Routine<String, String> routine =
                jrt().routineOf(Classification.ofType(DelaySubRoutine.class),
                                TimeDuration.millis(0));

        assertThat(routine.run(input).bind(consumer).waitDone()).isTrue();
        assertThat(routine.runAsyn(input).bind(consumer).waitDone()).isTrue();
        assertThat(routine.runPar(input).bind(consumer).waitDone()).isTrue();
        assertThat(routine.launch().push(input).close().bind(consumer).waitDone()).isTrue();
        assertThat(routine.launchAsyn().push(input).close().bind(consumer).waitDone()).isTrue();
        assertThat(routine.launchPar().push(input).close().bind(consumer).waitDone()).isTrue();
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

            routine.run(input).takeAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            routine.run(input).iterator().next();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            routine.runAsyn(input).takeAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            routine.runAsyn(input).iterator().next();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            routine.runPar(input).takeAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            routine.runPar(input).iterator().next();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            routine.launch().push(input).close().takeAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            routine.launch().push(input).close().iterator().next();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            routine.launchAsyn().push(input).close().takeAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            routine.launchAsyn().push(input).close().iterator().next();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            routine.launchPar().push(input).close().takeAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            routine.launchPar().push(input).close().iterator().next();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }
    }

    private static class DelaySubRoutine extends SubRoutineAdapter<String, String> {

        private final TimeDuration mDelay;

        public DelaySubRoutine(final TimeDuration delay) {

            mDelay = delay;
        }

        @Override
        public void onInput(final String s, final ResultChannel<String> results) {

            results.after(mDelay).push(s);
        }
    }

    private static class PassThroughSubRoutine extends SubRoutineAdapter<String, String> {

        @Override
        public void onInput(final String s, final ResultChannel<String> results) {

            results.push(s);
        }
    }
}