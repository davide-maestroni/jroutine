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

import com.bmd.jrt.channel.InputChannel;
import com.bmd.jrt.routine.Routine;
import com.bmd.jrt.subroutine.ResultPublisher;
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
            public void run(final List<? extends Integer> integers,
                    final ResultPublisher<Integer> results) {

                int sum = 0;

                for (final Integer integer : integers) {

                    sum += integer;
                }

                results.publish(sum);
            }
        };

        final Routine<Integer, Integer> sumRoutine =
                jrt().exec(Classification.of(sumSubRoutine), this);

        assertThat(sumRoutine.call(1, 2, 3, 4)).containsExactly(10);
        assertThat(sumRoutine.asynCall(1, 2, 3, 4)).containsExactly(10);
        assertThat(sumRoutine.run(1, 2, 3, 4).all()).containsExactly(10);
        assertThat(sumRoutine.asynRun(1, 2, 3, 4).all()).containsExactly(10);

        final SubRoutineLoopAdapter<Integer, Integer> squareSubRoutine =
                new SubRoutineLoopAdapter<Integer, Integer>() {

                    @Override
                    public void onInput(final Integer integer,
                            final ResultPublisher<Integer> results) {

                        final int input = integer;

                        results.publish(input * input);
                    }
                };

        final Routine<Integer, Integer> squareRoutine =
                jrt().loop(Classification.of(squareSubRoutine), this);

        assertThat(squareRoutine.onResult(sumRoutine).call(1, 2, 3, 4)).containsExactly(30);
        assertThat(squareRoutine.onResult(sumRoutine).asynCall(1, 2, 3, 4)).containsExactly(30);
        assertThat(squareRoutine.onResult(sumRoutine).run(1, 2, 3, 4).all()).containsExactly(30);
        assertThat(squareRoutine.onResult(sumRoutine).asynRun(1, 2, 3, 4).all()).containsExactly(
                30);

        final SubRoutineLoopAdapter<Integer, Integer> squareSumSubRoutine =
                new SubRoutineLoopAdapter<Integer, Integer>() {

                    private InputChannel<Integer, Integer> mChannel;

                    @Override
                    public void onInit() {

                        mChannel = sumRoutine.asynStart();
                    }

                    @Override
                    public void onInput(final Integer integer,
                            final ResultPublisher<Integer> results) {

                        mChannel.push(squareRoutine.asynCall(integer));
                    }

                    @Override
                    public void onReset(final ResultPublisher<Integer> results) {

                        mChannel.reset();
                    }

                    @Override
                    public void onResult(final ResultPublisher<Integer> results) {

                        results.publish(mChannel.end());
                    }
                };

        final Routine<Integer, Integer> squareSumRoutine =
                jrt().loop(Classification.of(squareSumSubRoutine), this, sumRoutine, squareRoutine);

        assertThat(squareSumRoutine.call(1, 2, 3, 4)).containsExactly(30);
        assertThat(squareSumRoutine.asynCall(1, 2, 3, 4)).containsExactly(30);
        assertThat(squareSumRoutine.run(1, 2, 3, 4).all()).containsExactly(30);
        assertThat(squareSumRoutine.asynRun(1, 2, 3, 4).all()).containsExactly(30);
    }
}