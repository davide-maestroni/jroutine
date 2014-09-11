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

import com.bmd.jrt.channel.InputChannel;
import com.bmd.jrt.channel.OutputChannel;
import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.runner.Runners;

import java.util.List;

/**
 * Created by davide on 9/7/14.
 */
public abstract class AbstractRoutine<INPUT, OUTPUT> implements Routine<INPUT, OUTPUT> {

    protected static final Object[] NO_ARGS = new Object[0];

    private final Runner mRunner;

    public AbstractRoutine(final Runner runner) {

        mRunner = runner;
    }

    @Override
    public List<OUTPUT> asynCall() {

        return asynStart().end().all();
    }

    @Override
    public List<OUTPUT> asynCall(final INPUT input) {

        return asynStart().push(input).end().all();
    }

    @Override
    public List<OUTPUT> asynCall(final INPUT... inputs) {

        return asynStart().push(inputs).end().all();
    }

    @Override
    public List<OUTPUT> asynCall(final Iterable<? extends INPUT> inputs) {

        return asynStart().push(inputs).end().all();
    }

    @Override
    public OutputChannel<OUTPUT> asynRun() {

        return asynStart().end();
    }

    @Override
    public OutputChannel<OUTPUT> asynRun(final INPUT input) {

        return asynStart().push(input).end();
    }

    @Override
    public OutputChannel<OUTPUT> asynRun(final INPUT... inputs) {

        return asynStart().push(inputs).end();
    }

    @Override
    public OutputChannel<OUTPUT> asynRun(final Iterable<? extends INPUT> inputs) {

        return asynStart().push(inputs).end();
    }

    @Override
    public InputChannel<INPUT, OUTPUT> asynStart() {

        return start(mRunner);
    }

    @Override
    public List<OUTPUT> call() {

        return start().end().all();
    }

    @Override
    public List<OUTPUT> call(final INPUT input) {

        return start().push(input).end().all();
    }

    @Override
    public List<OUTPUT> call(final INPUT... inputs) {

        return start().push(inputs).end().all();
    }

    @Override
    public List<OUTPUT> call(final Iterable<? extends INPUT> inputs) {

        return start().push(inputs).end().all();
    }

    @Override
    public Routine<INPUT, OUTPUT> onResults(final ResultFilter<OUTPUT> filter) {

        return new ResultFilterRoutine<INPUT, OUTPUT>(this, filter);
    }

    @Override
    public <TRANSFORMED> Routine<INPUT, TRANSFORMED> onResults(
            final InputChannel<OUTPUT, TRANSFORMED> channel) {

        return new OutputChannelRoutine<INPUT, OUTPUT, TRANSFORMED>(this, channel);
    }

    @Override
    public OutputChannel<OUTPUT> run() {

        return start().end();
    }

    @Override
    public OutputChannel<OUTPUT> run(final INPUT input) {

        return start().push(input).end();
    }

    @Override
    public OutputChannel<OUTPUT> run(final INPUT... inputs) {

        return start().push(inputs).end();
    }

    @Override
    public OutputChannel<OUTPUT> run(final Iterable<? extends INPUT> inputs) {

        return start().push(inputs).end();
    }

    @Override
    public InputChannel<INPUT, OUTPUT> start() {

        return start(Runners.sync());
    }

    protected abstract RecyclableUnitProcessor<INPUT, OUTPUT> createProcessor();

    protected Runner getRunner() {

        return mRunner;
    }

    protected InputChannel<INPUT, OUTPUT> start(final Runner runner) {

        return new DefaultRoutineChannel<INPUT, OUTPUT>(createProcessor(), runner);
    }
}