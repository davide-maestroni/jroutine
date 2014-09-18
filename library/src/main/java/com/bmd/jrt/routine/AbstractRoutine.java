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
import com.bmd.jrt.channel.RoutineChannel;
import com.bmd.jrt.routine.DefaultRoutineChannel.SubRoutineProvider;
import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.runner.Runners;
import com.bmd.jrt.subroutine.SubRoutine;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by davide on 9/7/14.
 */
public abstract class AbstractRoutine<INPUT, OUTPUT> implements Routine<INPUT, OUTPUT> {

    private final int mMaxRetained;

    private final Object mMutex = new Object();

    private final Runner mRunner;

    private LinkedList<SubRoutine<INPUT, OUTPUT>> mSubRoutines =
            new LinkedList<SubRoutine<INPUT, OUTPUT>>();

    public AbstractRoutine(final Runner runner, final int maxRetained) {

        if (runner == null) {

            throw new IllegalArgumentException();
        }

        if (maxRetained < 1) {

            throw new IllegalArgumentException();
        }

        mRunner = runner;
        mMaxRetained = maxRetained;
    }

    @Override
    public List<OUTPUT> call() {

        return run().all();
    }

    @Override
    public List<OUTPUT> call(final INPUT input) {

        return run(input).all();
    }

    @Override
    public List<OUTPUT> call(final INPUT... inputs) {

        return run(inputs).all();
    }

    @Override
    public List<OUTPUT> call(final Iterable<? extends INPUT> inputs) {

        return run(inputs).all();
    }

    @Override
    public List<OUTPUT> call(final OutputChannel<? extends INPUT> inputs) {

        return run(inputs).all();
    }

    @Override
    public List<OUTPUT> callAsyn() {

        return runAsyn().all();
    }

    @Override
    public List<OUTPUT> callAsyn(final INPUT input) {

        return runAsyn(input).all();
    }

    @Override
    public List<OUTPUT> callAsyn(final INPUT... inputs) {

        return runAsyn(inputs).all();
    }

    @Override
    public List<OUTPUT> callAsyn(final Iterable<? extends INPUT> inputs) {

        return runAsyn(inputs).all();
    }

    @Override
    public List<OUTPUT> callAsyn(final OutputChannel<? extends INPUT> inputs) {

        return runAsyn(inputs).all();
    }

    @Override
    public RoutineChannel<INPUT, OUTPUT> launch() {

        return start(false);
    }

    @Override
    public RoutineChannel<INPUT, OUTPUT> launchAsyn() {

        return start(true);
    }

    @Override
    public OutputChannel<OUTPUT> run() {

        return launch().close();
    }

    @Override
    public OutputChannel<OUTPUT> run(final INPUT input) {

        return launch().push(input).close();
    }

    @Override
    public OutputChannel<OUTPUT> run(final INPUT... inputs) {

        return launch().push(inputs).close();
    }

    @Override
    public OutputChannel<OUTPUT> run(final Iterable<? extends INPUT> inputs) {

        return launch().push(inputs).close();
    }

    @Override
    public OutputChannel<OUTPUT> run(final OutputChannel<? extends INPUT> inputs) {

        return launch().push(inputs).close();
    }

    @Override
    public OutputChannel<OUTPUT> runAsyn() {

        return launchAsyn().close();
    }

    @Override
    public OutputChannel<OUTPUT> runAsyn(final INPUT input) {

        return launchAsyn().push(input).close();
    }

    @Override
    public OutputChannel<OUTPUT> runAsyn(final INPUT... inputs) {

        return launchAsyn().push(inputs).close();
    }

    @Override
    public OutputChannel<OUTPUT> runAsyn(final Iterable<? extends INPUT> inputs) {

        return launchAsyn().push(inputs).close();
    }

    @Override
    public OutputChannel<OUTPUT> runAsyn(final OutputChannel<? extends INPUT> inputs) {

        return launchAsyn().push(inputs).close();
    }

    @Override
    public OutputChannel<OUTPUT> runPar() {

        return launchPar().close();
    }

    @Override
    public OutputChannel<OUTPUT> runPar(final INPUT input) {

        return launchPar().push(input).close();
    }

    @Override
    public OutputChannel<OUTPUT> runPar(final INPUT... inputs) {

        return launchPar().push(inputs).close();
    }

    @Override
    public OutputChannel<OUTPUT> runPar(final Iterable<? extends INPUT> inputs) {

        return launchPar().push(inputs).close();
    }

    @Override
    public OutputChannel<OUTPUT> runPar(final OutputChannel<? extends INPUT> inputs) {

        return launchPar().push(inputs).close();
    }

    protected abstract SubRoutine<INPUT, OUTPUT> createSubRoutine(final boolean async);

    protected int getMaxRetained() {

        return mMaxRetained;
    }

    protected Runner getRunner() {

        return mRunner;
    }

    protected RoutineChannel<INPUT, OUTPUT> start(final boolean async) {

        return new DefaultRoutineChannel<INPUT, OUTPUT>(new RoutineSubRoutineProvider(async),
                                                        (async) ? mRunner : Runners.sync());
    }

    private class RoutineSubRoutineProvider implements SubRoutineProvider<INPUT, OUTPUT> {

        private final boolean mAsync;

        private RoutineSubRoutineProvider(final boolean async) {

            mAsync = async;
        }

        @Override
        public SubRoutine<INPUT, OUTPUT> create() {

            synchronized (mMutex) {

                final LinkedList<SubRoutine<INPUT, OUTPUT>> routines = mSubRoutines;

                if (!routines.isEmpty()) {

                    return routines.removeFirst();
                }

                return createSubRoutine(mAsync);
            }
        }

        @Override
        public void recycle(final SubRoutine<INPUT, OUTPUT> routine) {

            synchronized (mMutex) {

                final LinkedList<SubRoutine<INPUT, OUTPUT>> routines = mSubRoutines;

                if (routines.size() < mMaxRetained) {

                    routines.add(routine);
                }
            }
        }
    }
}