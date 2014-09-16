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
import com.bmd.jrt.channel.RoutineChannel;
import com.bmd.jrt.routine.Routine;
import com.bmd.jrt.subroutine.SubRoutine;
import com.bmd.jrt.subroutine.SubRoutineLoopAdapter;
import com.bmd.jrt.util.Classification;

import junit.framework.TestCase;

import java.util.List;

import static com.bmd.jrt.routine.JRoutine.jrt;
import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Unit test for
 * <p/>
 * Created by davide on 9/9/14.
 */
public class RoutineTest extends TestCase {

    public void testError() {

    }

    public void testRoutine() {

        final SubRoutine<Integer, Integer> sumSubRoutine = new SubRoutine<Integer, Integer>() {

            @Override
            public void onRun(final List<? extends Integer> integers,
                    final ResultChannel<Integer> results) {

                System.out.println(">>>>>> SUM: " + this + " T: " + Thread.currentThread());

                int sum = 0;

                for (final Integer integer : integers) {

                    sum += integer;
                }

                results.push(sum);
            }
        };

        final Routine<Integer, Integer> sumRoutine =
                jrt().exec(Classification.of(sumSubRoutine), this);

        assertThat(sumRoutine.call(1, 2, 3, 4)).containsExactly(10);
        assertThat(sumRoutine.callAsyn(1, 2, 3, 4)).containsExactly(10);
        assertThat(sumRoutine.run(1, 2, 3, 4).all()).containsExactly(10);
        assertThat(sumRoutine.runAsyn(1, 2, 3, 4).all()).containsExactly(10);

        final SubRoutineLoopAdapter<Integer, Integer> squareSubRoutine =
                new SubRoutineLoopAdapter<Integer, Integer>() {

                    @Override
                    public void onInput(final Integer integer,
                            final ResultChannel<Integer> results) {

                        System.out.println(">>>>>> SQUARE: " + integer + " I: " + this + " T: "
                                                   + Thread.currentThread());

                        final int input = integer;

                        results.push(input * input);
                    }
                };

        final Routine<Integer, Integer> squareRoutine =
                jrt().loop(Classification.of(squareSubRoutine), this);

        assertThat(sumRoutine.call(squareRoutine.run(1, 2, 3, 4))).containsExactly(30);
        assertThat(sumRoutine.callAsyn(squareRoutine.run(1, 2, 3, 4))).containsExactly(30);
        assertThat(sumRoutine.run(squareRoutine.run(1, 2, 3, 4)).all()).containsExactly(30);
        assertThat(sumRoutine.runAsyn(squareRoutine.run(1, 2, 3, 4)).all()).containsExactly(30);

        assertThat(sumRoutine.call(squareRoutine.runAsyn(1, 2, 3, 4))).containsExactly(30);
        assertThat(sumRoutine.callAsyn(squareRoutine.runAsyn(1, 2, 3, 4))).containsExactly(30);
        assertThat(sumRoutine.run(squareRoutine.runAsyn(1, 2, 3, 4)).all()).containsExactly(30);
        assertThat(sumRoutine.runAsyn(squareRoutine.runAsyn(1, 2, 3, 4)).all()).containsExactly(30);

        final SubRoutineLoopAdapter<Integer, Integer> squareSumSubRoutine =
                new SubRoutineLoopAdapter<Integer, Integer>() {

                    private RoutineChannel<Integer, Integer> mChannel;

                    @Override
                    public void onInit() {

                        System.out.println(
                                ">>>>>> SS INIT I: " + this + " T: " + Thread.currentThread());

                        mChannel = sumRoutine.startAsyn();
                    }

                    @Override
                    public void onInput(final Integer integer,
                            final ResultChannel<Integer> results) {

                        System.out.println(">>>>>> SS INPUT: " + integer + " I:" + this + " T: "
                                                   + Thread.currentThread());

                        mChannel.push(squareRoutine.runAsyn(integer));
                    }

                    @Override
                    public void onReset(final Throwable throwable) {

                        System.out.println(">>>>>> SS RESET: " + throwable + " I: " + this + " T: "
                                                   + Thread.currentThread());

                        mChannel.reset(throwable);
                    }

                    @Override
                    public void onResult(final ResultChannel<Integer> results) {

                        System.out.println(
                                ">>>>>> SS RESULT: " + this + " T: " + Thread.currentThread());

                        results.push(mChannel.close());
                    }
                };

        final Routine<Integer, Integer> squareSumRoutine =
                jrt().loop(Classification.of(squareSumSubRoutine), this, sumRoutine, squareRoutine);

        System.out.println(">>>>>> SS TEST1");
        assertThat(squareSumRoutine.call(1, 2, 3, 4)).containsExactly(30);
        System.out.println(">>>>>> SS TEST2");
        assertThat(squareSumRoutine.callAsyn(1, 2, 3, 4)).containsExactly(30);
        System.out.println(">>>>>> SS TEST3");
        assertThat(squareSumRoutine.run(1, 2, 3, 4).all()).containsExactly(30);
        System.out.println(">>>>>> SS TEST4");
        assertThat(squareSumRoutine.runAsyn(1, 2, 3, 4).all()).containsExactly(30);
    }
}