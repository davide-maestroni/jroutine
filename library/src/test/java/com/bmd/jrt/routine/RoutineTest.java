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

import com.bmd.jrt.channel.ResultChannel;
import com.bmd.jrt.channel.ResultConsumer;
import com.bmd.jrt.channel.ResultConsumerAdapter;
import com.bmd.jrt.channel.RoutineChannel;
import com.bmd.jrt.subroutine.SubRoutine;
import com.bmd.jrt.subroutine.SubRoutineAdapter;
import com.bmd.jrt.subroutine.SubRoutineFunction;
import com.bmd.jrt.time.TimeDuration;
import com.bmd.jrt.util.ClassAdapter;
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

    public void testChainedRoutine() {

        final SubRoutineFunction<Integer, Integer> sumSubRoutine =
                new SubRoutineFunction<Integer, Integer>() {

                    @Override
                    public void onRun(final List<? extends Integer> integers,
                            final ResultChannel<Integer> results) {

                        int sum = 0;

                        for (final Integer integer : integers) {

                            sum += integer;
                        }

                        results.pass(sum);
                    }
                };

        final Routine<Integer, Integer> sumRoutine =
                jrt().withArgs(this).routineOf(ClassAdapter.classOf(sumSubRoutine));

        final SubRoutineAdapter<Integer, Integer> squareSubRoutine =
                new SubRoutineAdapter<Integer, Integer>() {

                    @Override
                    public void onInput(final Integer integer,
                            final ResultChannel<Integer> results) {

                        final int input = integer;

                        results.pass(input * input);
                    }
                };

        final Routine<Integer, Integer> squareRoutine =
                jrt().withArgs(this).routineOf(ClassAdapter.classOf(squareSubRoutine));

        assertThat(sumRoutine.call(squareRoutine.invoke(1, 2, 3, 4))).containsExactly(30);
        assertThat(sumRoutine.callAsyn(squareRoutine.invoke(1, 2, 3, 4))).containsExactly(30);
        assertThat(sumRoutine.invoke(squareRoutine.invoke(1, 2, 3, 4)).readAll()).containsExactly(
                30);
        assertThat(
                sumRoutine.invokeAsyn(squareRoutine.invoke(1, 2, 3, 4)).readAll()).containsExactly(
                30);

        assertThat(sumRoutine.call(squareRoutine.invokeAsyn(1, 2, 3, 4))).containsExactly(30);
        assertThat(sumRoutine.callAsyn(squareRoutine.invokeAsyn(1, 2, 3, 4))).containsExactly(30);
        assertThat(
                sumRoutine.invoke(squareRoutine.invokeAsyn(1, 2, 3, 4)).readAll()).containsExactly(
                30);
        assertThat(sumRoutine.invokeAsyn(squareRoutine.invokeAsyn(1, 2, 3, 4))
                             .readAll()).containsExactly(30);

        assertThat(sumRoutine.call(squareRoutine.invokeParall(1, 2, 3, 4))).containsExactly(30);
        assertThat(sumRoutine.callAsyn(squareRoutine.invokeParall(1, 2, 3, 4))).containsExactly(30);
        assertThat(sumRoutine.invoke(squareRoutine.invokeParall(1, 2, 3, 4))
                             .readAll()).containsExactly(30);
        assertThat(sumRoutine.invokeAsyn(squareRoutine.invokeParall(1, 2, 3, 4))
                             .readAll()).containsExactly(30);
    }

    public void testComposedRoutine() {

        final SubRoutineFunction<Integer, Integer> sumSubRoutine =
                new SubRoutineFunction<Integer, Integer>() {

                    @Override
                    public void onRun(final List<? extends Integer> integers,
                            final ResultChannel<Integer> results) {

                        int sum = 0;

                        for (final Integer integer : integers) {

                            sum += integer;
                        }

                        results.pass(sum);
                    }
                };

        final Routine<Integer, Integer> sumRoutine =
                jrt().withArgs(this).routineOf(ClassAdapter.classOf(sumSubRoutine));

        final SubRoutineAdapter<Integer, Integer> squareSubRoutine =
                new SubRoutineAdapter<Integer, Integer>() {

                    @Override
                    public void onInput(final Integer integer,
                            final ResultChannel<Integer> results) {

                        final int input = integer;

                        results.pass(input * input);
                    }
                };

        final Routine<Integer, Integer> squareRoutine =
                jrt().withArgs(this).routineOf(ClassAdapter.classOf(squareSubRoutine));

        final SubRoutineAdapter<Integer, Integer> squareSumSubRoutine =
                new SubRoutineAdapter<Integer, Integer>() {

                    private RoutineChannel<Integer, Integer> mChannel;

                    @Override
                    public void onAbort(final Throwable throwable) {

                        mChannel.abort(throwable);
                    }

                    @Override
                    public void onInit() {

                        mChannel = sumRoutine.launchAsyn();
                    }

                    @Override
                    public void onInput(final Integer integer,
                            final ResultChannel<Integer> results) {

                        mChannel.pass(squareRoutine.invokeAsyn(integer));
                    }

                    @Override
                    public void onResult(final ResultChannel<Integer> results) {

                        results.pass(mChannel.close());
                    }
                };

        final Routine<Integer, Integer> squareSumRoutine =
                jrt().withArgs(this, sumRoutine, squareRoutine)
                     .routineOf(ClassAdapter.classOf(squareSumSubRoutine));

        assertThat(squareSumRoutine.call(1, 2, 3, 4)).containsExactly(30);
        assertThat(squareSumRoutine.callAsyn(1, 2, 3, 4)).containsExactly(30);
        assertThat(squareSumRoutine.invoke(1, 2, 3, 4).readAll()).containsExactly(30);
        assertThat(squareSumRoutine.invokeAsyn(1, 2, 3, 4).readAll()).containsExactly(30);
    }

    public void testErrorConsumerOnResult() {

        final ResultConsumerAdapter<String> exceptionConsumer =
                new ResultConsumerAdapter<String>() {

                    @Override
                    public void onResult(final String result) {

                        throw new NullPointerException(result);
                    }
                };

        testConsumer(exceptionConsumer);
    }

    public void testErrorConsumerOnReturn() {

        final ResultConsumerAdapter<String> exceptionConsumer =
                new ResultConsumerAdapter<String>() {

                    @Override
                    public void onReturn() {

                        throw new NullPointerException("test2");
                    }
                };

        testConsumer(exceptionConsumer);
    }

    public void testErrorOnInit() {

        final SubRoutine<String, String> exceptionOnInit = new SubRoutineAdapter<String, String>() {

            @Override
            public void onInit() {

                throw new NullPointerException("test1");
            }
        };

        final Routine<String, String> exceptionRoutine =
                jrt().withArgs(this).routineOf(ClassAdapter.classOf(exceptionOnInit));

        testException(exceptionRoutine, "test", "test1");

        final Routine<String, String> passThroughRoutine =
                jrt().routineOf(ClassAdapter.adapt(PassThroughSubRoutine.class));

        testChained(passThroughRoutine, exceptionRoutine, "test", "test1");
        testChained(exceptionRoutine, passThroughRoutine, "test", "test1");
    }

    public void testErrorOnInput() {

        final SubRoutine<String, String> exceptionOnInput =
                new SubRoutineAdapter<String, String>() {

                    @Override
                    public void onInput(final String s, final ResultChannel<String> results) {

                        throw new NullPointerException(s);
                    }
                };

        final Routine<String, String> exceptionRoutine =
                jrt().withArgs(this).routineOf(ClassAdapter.classOf(exceptionOnInput));

        testException(exceptionRoutine, "test2", "test2");

        final Routine<String, String> passThroughRoutine =
                jrt().routineOf(ClassAdapter.adapt(PassThroughSubRoutine.class));

        testChained(passThroughRoutine, exceptionRoutine, "test2", "test2");
        testChained(exceptionRoutine, passThroughRoutine, "test2", "test2");
    }

    public void testErrorOnResult() {

        final SubRoutine<String, String> exceptionOnResult =
                new SubRoutineAdapter<String, String>() {

                    @Override
                    public void onResult(final ResultChannel<String> results) {

                        throw new NullPointerException("test3");
                    }
                };

        final Routine<String, String> exceptionRoutine =
                jrt().withArgs(this).routineOf(ClassAdapter.classOf(exceptionOnResult));

        testException(exceptionRoutine, "test", "test3");

        final Routine<String, String> passThroughRoutine =
                jrt().routineOf(ClassAdapter.adapt(PassThroughSubRoutine.class));

        testChained(passThroughRoutine, exceptionRoutine, "test", "test3");
        testChained(exceptionRoutine, passThroughRoutine, "test", "test3");
    }

    public void testErrorOnReturn() {

        final SubRoutine<String, String> exceptionOnReturn =
                new SubRoutineAdapter<String, String>() {

                    @Override
                    public void onInput(final String s, final ResultChannel<String> results) {

                        results.pass(s);
                    }

                    @Override
                    public void onReturn() {

                        throw new NullPointerException("test4");
                    }
                };

        final Routine<String, String> exceptionRoutine =
                jrt().withArgs(this).routineOf(ClassAdapter.classOf(exceptionOnReturn));

        assertThat(exceptionRoutine.call("test")).containsExactly("test");
        assertThat(exceptionRoutine.callAsyn("test")).containsExactly("test");
        assertThat(exceptionRoutine.invoke("test").readAll()).containsExactly("test");
        assertThat(exceptionRoutine.invokeAsyn("test").readAll()).containsExactly("test");
        assertThat(exceptionRoutine.invokeParall("test").readAll()).containsExactly("test");
        assertThat(exceptionRoutine.invoke("test").iterator().next()).isEqualTo("test");
        assertThat(exceptionRoutine.invokeAsyn("test").iterator().next()).isEqualTo("test");
        assertThat(exceptionRoutine.invokeParall("test").iterator().next()).isEqualTo("test");
        assertThat(exceptionRoutine.launch().pass("test").close().readAll()).containsExactly(
                "test");
        assertThat(exceptionRoutine.launchAsyn().pass("test").close().readAll()).containsExactly(
                "test");
        assertThat(exceptionRoutine.launchParall().pass("test").close().readAll()).containsExactly(
                "test");
        assertThat(exceptionRoutine.launch().pass("test").close().iterator().next()).isEqualTo(
                "test");
        assertThat(exceptionRoutine.launchAsyn().pass("test").close().iterator().next()).isEqualTo(
                "test");
        assertThat(
                exceptionRoutine.launchParall().pass("test").close().iterator().next()).isEqualTo(
                "test");

        final Routine<String, String> passThroughRoutine =
                jrt().routineOf(ClassAdapter.adapt(PassThroughSubRoutine.class));

        assertThat(passThroughRoutine.call(exceptionRoutine.invoke("test"))).containsExactly(
                "test");
        assertThat(passThroughRoutine.callAsyn(exceptionRoutine.invoke("test"))).containsExactly(
                "test");
        assertThat(passThroughRoutine.invoke(exceptionRoutine.invoke("test"))
                                     .readAll()).containsExactly("test");
        assertThat(passThroughRoutine.invokeAsyn(exceptionRoutine.invoke("test"))
                                     .readAll()).containsExactly("test");
        assertThat(passThroughRoutine.invokeParall(exceptionRoutine.invoke("test"))
                                     .readAll()).containsExactly("test");
        assertThat(passThroughRoutine.invoke(exceptionRoutine.invoke("test"))
                                     .iterator()
                                     .next()).isEqualTo("test");
        assertThat(passThroughRoutine.invokeAsyn(exceptionRoutine.invoke("test")).iterator().next())
                .isEqualTo("test");
        assertThat(passThroughRoutine.invokeParall(exceptionRoutine.invoke("test"))
                                     .iterator()
                                     .next()).isEqualTo("test");
        assertThat(
                passThroughRoutine.launch().pass(exceptionRoutine.invoke("test")).close().readAll())
                .containsExactly("test");
        assertThat(passThroughRoutine.launchAsyn()
                                     .pass(exceptionRoutine.invoke("test"))
                                     .close()
                                     .readAll()).containsExactly("test");
        assertThat(passThroughRoutine.launchParall()
                                     .pass(exceptionRoutine.invoke("test"))
                                     .close()
                                     .readAll()).containsExactly("test");
        assertThat(passThroughRoutine.launch()
                                     .pass(exceptionRoutine.invoke("test"))
                                     .close()
                                     .iterator()
                                     .next()).isEqualTo("test");
        assertThat(passThroughRoutine.launchAsyn()
                                     .pass(exceptionRoutine.invoke("test"))
                                     .close()
                                     .iterator()
                                     .next()).isEqualTo("test");
        assertThat(passThroughRoutine.launchParall()
                                     .pass(exceptionRoutine.invoke("test"))
                                     .close()
                                     .iterator()
                                     .next()).isEqualTo("test");

        assertThat(passThroughRoutine.call(exceptionRoutine.invokeAsyn("test"))).containsExactly(
                "test");
        assertThat(
                passThroughRoutine.callAsyn(exceptionRoutine.invokeAsyn("test"))).containsExactly(
                "test");
        assertThat(passThroughRoutine.invoke(exceptionRoutine.invokeAsyn("test"))
                                     .readAll()).containsExactly("test");
        assertThat(passThroughRoutine.invokeAsyn(exceptionRoutine.invokeAsyn("test"))
                                     .readAll()).containsExactly("test");
        assertThat(passThroughRoutine.invokeParall(exceptionRoutine.invokeAsyn("test"))
                                     .readAll()).containsExactly("test");
        assertThat(passThroughRoutine.invoke(exceptionRoutine.invokeAsyn("test")).iterator().next())
                .isEqualTo("test");
        assertThat(passThroughRoutine.invokeAsyn(exceptionRoutine.invokeAsyn("test"))
                                     .iterator()
                                     .next()).isEqualTo("test");
        assertThat(passThroughRoutine.invokeParall(exceptionRoutine.invokeAsyn("test"))
                                     .iterator()
                                     .next()).isEqualTo("test");
        assertThat(passThroughRoutine.launch()
                                     .pass(exceptionRoutine.invokeAsyn("test"))
                                     .close()
                                     .readAll()).containsExactly("test");
        assertThat(passThroughRoutine.launchAsyn()
                                     .pass(exceptionRoutine.invokeAsyn("test"))
                                     .close()
                                     .readAll()).containsExactly("test");
        assertThat(passThroughRoutine.launchParall()
                                     .pass(exceptionRoutine.invokeAsyn("test"))
                                     .close()
                                     .readAll()).containsExactly("test");
        assertThat(passThroughRoutine.launch()
                                     .pass(exceptionRoutine.invokeAsyn("test"))
                                     .close()
                                     .iterator()
                                     .next()).isEqualTo("test");
        assertThat(passThroughRoutine.launchAsyn()
                                     .pass(exceptionRoutine.invokeAsyn("test"))
                                     .close()
                                     .iterator()
                                     .next()).isEqualTo("test");
        assertThat(passThroughRoutine.launchParall()
                                     .pass(exceptionRoutine.invokeAsyn("test"))
                                     .close()
                                     .iterator()
                                     .next()).isEqualTo("test");

        assertThat(passThroughRoutine.call(exceptionRoutine.invokeParall("test"))).containsExactly(
                "test");
        assertThat(
                passThroughRoutine.callAsyn(exceptionRoutine.invokeParall("test"))).containsExactly(
                "test");
        assertThat(passThroughRoutine.invoke(exceptionRoutine.invokeParall("test"))
                                     .readAll()).containsExactly("test");
        assertThat(passThroughRoutine.invokeAsyn(exceptionRoutine.invokeParall("test"))
                                     .readAll()).containsExactly("test");
        assertThat(passThroughRoutine.invokeParall(exceptionRoutine.invokeParall("test")).readAll())
                .containsExactly("test");
        assertThat(passThroughRoutine.invoke(exceptionRoutine.invokeParall("test"))
                                     .iterator()
                                     .next()).isEqualTo("test");
        assertThat(passThroughRoutine.invokeAsyn(exceptionRoutine.invokeParall("test"))
                                     .iterator()
                                     .next()).isEqualTo("test");
        assertThat(passThroughRoutine.invokeParall(exceptionRoutine.invokeParall("test"))
                                     .iterator()
                                     .next()).isEqualTo("test");
        assertThat(passThroughRoutine.launch()
                                     .pass(exceptionRoutine.invokeParall("test"))
                                     .close()
                                     .readAll()).containsExactly("test");
        assertThat(passThroughRoutine.launchAsyn()
                                     .pass(exceptionRoutine.invokeParall("test"))
                                     .close()
                                     .readAll()).containsExactly("test");
        assertThat(passThroughRoutine.launchParall()
                                     .pass(exceptionRoutine.invokeParall("test"))
                                     .close()
                                     .readAll()).containsExactly("test");
        assertThat(passThroughRoutine.launchAsyn()
                                     .pass(exceptionRoutine.invokeParall("test"))
                                     .close()
                                     .iterator()
                                     .next()).isEqualTo("test");
        assertThat(passThroughRoutine.launchParall()
                                     .pass(exceptionRoutine.invokeParall("test"))
                                     .close()
                                     .iterator()
                                     .next()).isEqualTo("test");

        assertThat(exceptionRoutine.call(passThroughRoutine.invoke("test"))).containsExactly(
                "test");
        assertThat(exceptionRoutine.callAsyn(passThroughRoutine.invoke("test"))).containsExactly(
                "test");
        assertThat(exceptionRoutine.invoke(passThroughRoutine.invoke("test"))
                                   .readAll()).containsExactly("test");
        assertThat(exceptionRoutine.invokeAsyn(passThroughRoutine.invoke("test"))
                                   .readAll()).containsExactly("test");
        assertThat(exceptionRoutine.invokeParall(passThroughRoutine.invoke("test"))
                                   .readAll()).containsExactly("test");
        assertThat(exceptionRoutine.invoke(passThroughRoutine.invoke("test"))
                                   .iterator()
                                   .next()).isEqualTo("test");
        assertThat(exceptionRoutine.invokeAsyn(passThroughRoutine.invoke("test")).iterator().next())
                .isEqualTo("test");
        assertThat(exceptionRoutine.invokeParall(passThroughRoutine.invoke("test"))
                                   .iterator()
                                   .next()).isEqualTo("test");
        assertThat(
                exceptionRoutine.launch().pass(passThroughRoutine.invoke("test")).close().readAll())
                .containsExactly("test");
        assertThat(exceptionRoutine.launchAsyn()
                                   .pass(passThroughRoutine.invoke("test"))
                                   .close()
                                   .readAll()).containsExactly("test");
        assertThat(exceptionRoutine.launchParall()
                                   .pass(passThroughRoutine.invoke("test"))
                                   .close()
                                   .readAll()).containsExactly("test");
        assertThat(exceptionRoutine.launch()
                                   .pass(passThroughRoutine.invoke("test"))
                                   .close()
                                   .iterator()
                                   .next()).isEqualTo("test");
        assertThat(exceptionRoutine.launchAsyn()
                                   .pass(passThroughRoutine.invoke("test"))
                                   .close()
                                   .iterator()
                                   .next()).isEqualTo("test");
        assertThat(exceptionRoutine.launchParall()
                                   .pass(passThroughRoutine.invoke("test"))
                                   .close()
                                   .iterator()
                                   .next()).isEqualTo("test");

        assertThat(exceptionRoutine.call(passThroughRoutine.invokeAsyn("test"))).containsExactly(
                "test");
        assertThat(
                exceptionRoutine.callAsyn(passThroughRoutine.invokeAsyn("test"))).containsExactly(
                "test");
        assertThat(exceptionRoutine.invoke(passThroughRoutine.invokeAsyn("test"))
                                   .readAll()).containsExactly("test");
        assertThat(exceptionRoutine.invokeAsyn(passThroughRoutine.invokeAsyn("test"))
                                   .readAll()).containsExactly("test");
        assertThat(exceptionRoutine.invokeParall(passThroughRoutine.invokeAsyn("test"))
                                   .readAll()).containsExactly("test");
        assertThat(exceptionRoutine.invoke(passThroughRoutine.invokeAsyn("test")).iterator().next())
                .isEqualTo("test");
        assertThat(exceptionRoutine.invokeAsyn(passThroughRoutine.invokeAsyn("test"))
                                   .iterator()
                                   .next()).isEqualTo("test");
        assertThat(exceptionRoutine.invokeParall(passThroughRoutine.invokeAsyn("test"))
                                   .iterator()
                                   .next()).isEqualTo("test");
        assertThat(exceptionRoutine.launch()
                                   .pass(passThroughRoutine.invokeAsyn("test"))
                                   .close()
                                   .readAll()).containsExactly("test");
        assertThat(exceptionRoutine.launchAsyn()
                                   .pass(passThroughRoutine.invokeAsyn("test"))
                                   .close()
                                   .readAll()).containsExactly("test");
        assertThat(exceptionRoutine.launchParall()
                                   .pass(passThroughRoutine.invokeAsyn("test"))
                                   .close()
                                   .readAll()).containsExactly("test");
        assertThat(exceptionRoutine.launch()
                                   .pass(passThroughRoutine.invokeAsyn("test"))
                                   .close()
                                   .iterator()
                                   .next()).isEqualTo("test");
        assertThat(exceptionRoutine.launchAsyn()
                                   .pass(passThroughRoutine.invokeAsyn("test"))
                                   .close()
                                   .iterator()
                                   .next()).isEqualTo("test");
        assertThat(exceptionRoutine.launchParall()
                                   .pass(passThroughRoutine.invokeAsyn("test"))
                                   .close()
                                   .iterator()
                                   .next()).isEqualTo("test");

        assertThat(exceptionRoutine.call(passThroughRoutine.invokeParall("test"))).containsExactly(
                "test");
        assertThat(
                exceptionRoutine.callAsyn(passThroughRoutine.invokeParall("test"))).containsExactly(
                "test");
        assertThat(exceptionRoutine.invoke(passThroughRoutine.invokeParall("test"))
                                   .readAll()).containsExactly("test");
        assertThat(exceptionRoutine.invokeAsyn(passThroughRoutine.invokeParall("test"))
                                   .readAll()).containsExactly("test");
        assertThat(exceptionRoutine.invokeParall(passThroughRoutine.invokeParall("test")).readAll())
                .containsExactly("test");
        assertThat(exceptionRoutine.invoke(passThroughRoutine.invokeParall("test"))
                                   .iterator()
                                   .next()).isEqualTo("test");
        assertThat(exceptionRoutine.invokeAsyn(passThroughRoutine.invokeParall("test"))
                                   .iterator()
                                   .next()).isEqualTo("test");
        assertThat(exceptionRoutine.invokeParall(passThroughRoutine.invokeParall("test"))
                                   .iterator()
                                   .next()).isEqualTo("test");
        assertThat(exceptionRoutine.launch()
                                   .pass(passThroughRoutine.invokeParall("test"))
                                   .close()
                                   .readAll()).containsExactly("test");
        assertThat(exceptionRoutine.launchAsyn()
                                   .pass(passThroughRoutine.invokeParall("test"))
                                   .close()
                                   .readAll()).containsExactly("test");
        assertThat(exceptionRoutine.launchParall()
                                   .pass(passThroughRoutine.invokeParall("test"))
                                   .close()
                                   .readAll()).containsExactly("test");
        assertThat(exceptionRoutine.launch()
                                   .pass(passThroughRoutine.invokeParall("test"))
                                   .close()
                                   .iterator()
                                   .next()).isEqualTo("test");
        assertThat(exceptionRoutine.launchAsyn()
                                   .pass(passThroughRoutine.invokeParall("test"))
                                   .close()
                                   .iterator()
                                   .next()).isEqualTo("test");
        assertThat(exceptionRoutine.launchParall()
                                   .pass(passThroughRoutine.invokeParall("test"))
                                   .close()
                                   .iterator()
                                   .next()).isEqualTo("test");
    }

    public void testRoutine() {

        final SubRoutineAdapter<Integer, Integer> squareSubRoutine =
                new SubRoutineAdapter<Integer, Integer>() {

                    @Override
                    public void onInput(final Integer integer,
                            final ResultChannel<Integer> results) {

                        final int input = integer;

                        results.pass(input * input);
                    }
                };

        final Routine<Integer, Integer> squareRoutine =
                jrt().withArgs(this).routineOf(ClassAdapter.classOf(squareSubRoutine));

        assertThat(squareRoutine.call(1, 2, 3, 4)).containsExactly(1, 4, 9, 16);
        assertThat(squareRoutine.callAsyn(1, 2, 3, 4)).containsExactly(1, 4, 9, 16);
        assertThat(squareRoutine.invoke(1, 2, 3, 4).readAll()).containsExactly(1, 4, 9, 16);
        assertThat(squareRoutine.invokeAsyn(1, 2, 3, 4).readAll()).containsExactly(1, 4, 9, 16);
        assertThat(squareRoutine.invokeParall(1, 2, 3, 4).readAll()).contains(1, 4, 9, 16);
    }

    public void testRoutineFunction() {

        final SubRoutineFunction<Integer, Integer> sumSubRoutine =
                new SubRoutineFunction<Integer, Integer>() {

                    @Override
                    public void onRun(final List<? extends Integer> integers,
                            final ResultChannel<Integer> results) {

                        int sum = 0;

                        for (final Integer integer : integers) {

                            sum += integer;
                        }

                        results.pass(sum);
                    }
                };

        final Routine<Integer, Integer> sumRoutine =
                jrt().withArgs(this).routineOf(ClassAdapter.classOf(sumSubRoutine));

        assertThat(sumRoutine.call(1, 2, 3, 4)).containsExactly(10);
        assertThat(sumRoutine.callAsyn(1, 2, 3, 4)).containsExactly(10);
        assertThat(sumRoutine.invoke(1, 2, 3, 4).readAll()).containsExactly(10);
        assertThat(sumRoutine.invokeAsyn(1, 2, 3, 4).readAll()).containsExactly(10);
    }

    private void testChained(final Routine<String, String> before,
            final Routine<String, String> after, final String input, final String expected) {

        try {

            before.call(after.invoke(input));

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.callAsyn(after.invoke(input));

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.invoke(after.invoke(input)).readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.invoke(after.invoke(input)).iterator().next();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.invokeAsyn(after.invoke(input)).readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.invokeAsyn(after.invoke(input)).iterator().next();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.invokeParall(after.invoke(input)).readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.invokeParall(after.invoke(input)).iterator().next();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.launch().pass(after.invoke(input)).close().readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.launch().pass(after.invoke(input)).close().iterator().next();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.launchAsyn().pass(after.invoke(input)).close().readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.launchAsyn().pass(after.invoke(input)).close().iterator().next();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.launchParall().pass(after.invoke(input)).close().readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.launchParall().pass(after.invoke(input)).close().iterator().next();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.call(after.invokeAsyn(input));

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.callAsyn(after.invokeAsyn(input));

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.invoke(after.invokeAsyn(input)).readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.invoke(after.invokeAsyn(input)).iterator().next();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.invokeAsyn(after.invokeAsyn(input)).readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.invokeAsyn(after.invokeAsyn(input)).iterator().next();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.launch().pass(after.invokeAsyn(input)).close().readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.launch().pass(after.invokeAsyn(input)).close().iterator().next();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.launchAsyn().pass(after.invokeAsyn(input)).close().readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.launchAsyn().pass(after.invokeAsyn(input)).close().iterator().next();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.call(after.invokeParall(input));

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.callAsyn(after.invokeParall(input));

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.invoke(after.invokeParall(input)).readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.invoke(after.invokeParall(input)).iterator().next();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.invokeAsyn(after.invokeParall(input)).readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.invokeAsyn(after.invokeParall(input)).iterator().next();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.launch().pass(after.invokeParall(input)).close().readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.launch().pass(after.invokeParall(input)).close().iterator().next();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.launchAsyn().pass(after.invokeParall(input)).close().readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            before.launchAsyn().pass(after.invokeParall(input)).close().iterator().next();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }
    }

    private void testConsumer(final ResultConsumer<String> consumer) {

        final String input = "test";
        final Routine<String, String> routine = jrt().withArgs(TimeDuration.millis(0))
                                                     .routineOf(ClassAdapter.adapt(
                                                             DelaySubRoutine.class));

        assertThat(routine.invoke(input).bind(consumer).waitDone()).isTrue();
        assertThat(routine.invokeAsyn(input).bind(consumer).waitDone()).isTrue();
        assertThat(routine.invokeParall(input).bind(consumer).waitDone()).isTrue();
        assertThat(routine.launch().pass(input).close().bind(consumer).waitDone()).isTrue();
        assertThat(routine.launchAsyn().pass(input).close().bind(consumer).waitDone()).isTrue();
        assertThat(routine.launchParall().pass(input).close().bind(consumer).waitDone()).isTrue();
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

            routine.invoke(input).readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            routine.invoke(input).iterator().next();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            routine.invokeAsyn(input).readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            routine.invokeAsyn(input).iterator().next();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            routine.invokeParall(input).readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            routine.invokeParall(input).iterator().next();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            routine.launch().pass(input).close().readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            routine.launch().pass(input).close().iterator().next();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            routine.launchAsyn().pass(input).close().readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            routine.launchAsyn().pass(input).close().iterator().next();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            routine.launchParall().pass(input).close().readAll();

            fail();

        } catch (final RoutineException e) {

            assertThat(e.getCause().getMessage()).isEqualTo(expected);
        }

        try {

            routine.launchParall().pass(input).close().iterator().next();

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

            results.after(mDelay).pass(s);
        }
    }

    private static class PassThroughSubRoutine extends SubRoutineAdapter<String, String> {

        @Override
        public void onInput(final String s, final ResultChannel<String> results) {

            results.pass(s);
        }
    }
}