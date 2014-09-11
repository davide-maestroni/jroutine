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

import com.bmd.jrt.process.Processor;
import com.bmd.jrt.process.ResultPublisher;
import com.bmd.jrt.process.UnitProcessor;
import com.bmd.jrt.routine.Routine;
import com.bmd.wtf.fll.Classification;

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

        final Processor<Integer, Integer> sumProcessor = new Processor<Integer, Integer>() {

            @Override
            public void onExecute(final List<? extends Integer> integers,
                    final ResultPublisher<Integer> results) {

                int sum = 0;

                for (final Integer integer : integers) {

                    sum += integer;
                }

                results.publish(sum);
            }
        };

        //noinspection unchecked
        final Routine<Integer, Integer> sumRoutine = jrt().onExecute(
                (Classification<Processor<Integer, Integer>>) Classification.ofType(
                        sumProcessor.getClass()), this);

        assertThat(sumRoutine.call(1, 2, 3, 4)).containsExactly(10);
        assertThat(sumRoutine.asynCall(1, 2, 3, 4)).containsExactly(10);
        assertThat(sumRoutine.run(1, 2, 3, 4).all()).containsExactly(10);
        assertThat(sumRoutine.asynRun(1, 2, 3, 4).all()).containsExactly(10);

        final UnitProcessor<Integer, Integer> squareProcessor =
                new UnitProcessor<Integer, Integer>() {

                    @Override
                    public void onInput(final Integer integer,
                            final ResultPublisher<Integer> results) {

                        final int input = integer;

                        results.publish(input * input);
                    }

                    @Override
                    public void onReset(final ResultPublisher<Integer> results) {

                    }

                    @Override
                    public void onResult(final ResultPublisher<Integer> results) {

                    }
                };

        //noinspection unchecked
        final Routine<Integer, Integer> squareRoutine = jrt().onProcess(
                (Classification<UnitProcessor<Integer, Integer>>) Classification.ofType(
                        squareProcessor.getClass()), this);

        assertThat(squareRoutine.onResults(sumRoutine.start()).call(1, 2, 3, 4)).containsExactly(
                30);
        assertThat(
                squareRoutine.onResults(sumRoutine.start()).asynCall(1, 2, 3, 4)).containsExactly(
                30);
        assertThat(
                squareRoutine.onResults(sumRoutine.asynStart()).call(1, 2, 3, 4)).containsExactly(
                30);
        assertThat(squareRoutine.onResults(sumRoutine.asynStart())
                                .asynCall(1, 2, 3, 4)).containsExactly(30);

        assertThat(
                squareRoutine.onResults(sumRoutine.start()).run(1, 2, 3, 4).all()).containsExactly(
                30);
        assertThat(squareRoutine.onResults(sumRoutine.start())
                                .asynRun(1, 2, 3, 4)
                                .all()).containsExactly(30);
        assertThat(squareRoutine.onResults(sumRoutine.asynStart())
                                .run(1, 2, 3, 4)
                                .all()).containsExactly(30);
        assertThat(squareRoutine.onResults(sumRoutine.asynStart())
                                .asynRun(1, 2, 3, 4)
                                .all()).containsExactly(30);
    }
}